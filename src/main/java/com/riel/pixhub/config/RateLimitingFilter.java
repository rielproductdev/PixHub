package com.riel.pixhub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riel.pixhub.dto.ErrorResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtro de Rate Limiting — controla quantas requisições cada usuário/IP pode fazer.
 *
 * Este filtro implementa o algoritmo "Token Bucket" usando a biblioteca Bucket4j.
 * O Token Bucket funciona como uma analogia real:
 *
 * Imagine um balde (bucket) que comporta N fichas (tokens):
 * 1. O balde começa cheio (ex: 5 fichas para auth, 60 para consultas)
 * 2. Cada requisição CONSOME 1 ficha do balde
 * 3. O balde é REABASTECIDO automaticamente ao longo do tempo
 *    (ex: 5 fichas por minuto para auth)
 * 4. Se o balde está vazio → 429 Too Many Requests
 * 5. O reabastecimento é gradual: não espera 1 minuto inteiro,
 *    adiciona fichas proporcionalmente ao tempo que passou
 *
 * Por que Token Bucket e não outras estratégias?
 * - Fixed Window (ex: "máx 60 req/min"): tem o problema de "burst na borda"
 *   — 60 reqs no segundo 59 + 60 reqs no segundo 61 = 120 reqs em 2 segundos
 * - Sliding Window: resolve o burst, mas é mais complexo de implementar
 * - Token Bucket: compromisso ideal — permite bursts curtos (o balde tem
 *   capacidade), mas limita o throughput médio. É o algoritmo que a AWS,
 *   Google e Stripe usam em suas APIs.
 *
 * Este filtro fica DEPOIS do JwtAuthenticationFilter na cadeia de segurança:
 *   Request → CORS → CSRF → JwtFilter → RateLimitFilter → Authorization → Controller
 *
 * Por que DEPOIS do JwtFilter?
 * Porque precisamos saber QUEM está fazendo a request:
 * - Para /auth/** → limitamos por IP (usuário ainda não se identificou)
 * - Para outros endpoints → limitamos por email (extraído do JWT)
 *
 * Armazenamento dos buckets:
 * Usamos ConcurrentHashMap (em memória). Em produção com múltiplas instâncias,
 * usaríamos Redis para compartilhar os buckets entre servidores.
 * Para o PixHub (instância única), ConcurrentHashMap é suficiente.
 *
 * @Component: Spring gerencia este filtro como bean.
 */
@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;

    /**
     * Mapa de buckets: chave → bucket.
     *
     * A chave é composta por: "tipo:identificador"
     * Exemplos:
     *   "auth:192.168.1.100"        → bucket de auth para este IP
     *   "transaction:riel@email.com" → bucket de transações para este usuário
     *   "query:riel@email.com"       → bucket de consultas para este usuário
     *
     * ConcurrentHashMap é thread-safe: múltiplas threads (requests simultâneas)
     * podem acessar o mapa ao mesmo tempo sem corromper os dados.
     * Bucket4j também é thread-safe internamente.
     */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitingFilter(RateLimitProperties rateLimitProperties,
                              ObjectMapper objectMapper) {
        this.rateLimitProperties = rateLimitProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * Método principal — executado para CADA request HTTP.
     *
     * Fluxo:
     * 1. Rate limiting está habilitado? Se não, passa direto.
     * 2. Identificar a categoria do endpoint (auth, transaction, query)
     * 3. Identificar QUEM está fazendo a request (IP ou email)
     * 4. Buscar (ou criar) o bucket correspondente
     * 5. Tentar consumir 1 token do bucket
     * 6. Se consumiu → adiciona headers e continua
     * 7. Se não consumiu (balde vazio) → retorna 429
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 1. Se rate limiting está desabilitado, passa direto
        if (!rateLimitProperties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Identificar a categoria do endpoint
        String path = request.getRequestURI();
        String method = request.getMethod();
        EndpointCategory category = categorizeEndpoint(path, method);

        // Endpoints sem rate limiting (swagger, actuator) → passa direto
        if (category == EndpointCategory.NONE) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Identificar quem está fazendo a request
        String key = resolveKey(category, request);

        // 4. Buscar ou criar o bucket para esta chave
        // computeIfAbsent: se a chave não existe, cria um novo bucket.
        // Se já existe, retorna o existente. Thread-safe.
        Bucket bucket = buckets.computeIfAbsent(key,
                k -> createBucket(category));

        // 5. Tentar consumir 1 token
        // tryConsumeAndReturnRemaining: tenta consumir E retorna informações
        // sobre o estado do bucket (tokens restantes, tempo para refill).
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        // 6. Adicionar headers de rate limiting na resposta
        // Esses headers informam ao cliente quanto "crédito" ele ainda tem.
        // O frontend pode usar para mostrar um aviso antes de atingir o limite.
        long limit = getLimitForCategory(category);
        response.addHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.addHeader("X-RateLimit-Remaining",
                String.valueOf(probe.getRemainingTokens()));

        if (probe.isConsumed()) {
            // Token consumido com sucesso → request pode continuar
            filterChain.doFilter(request, response);
        } else {
            // 7. Balde vazio! → 429 Too Many Requests
            // nanosToWaitForRefill: quanto tempo falta para o próximo token
            long waitSeconds = Duration.ofNanos(
                    probe.getNanosToWaitForRefill()).toSeconds();

            response.addHeader("X-RateLimit-Retry-After-Seconds",
                    String.valueOf(waitSeconds));

            log.warn("Rate limit excedido: {} — aguarde {}s",
                    key, waitSeconds);

            writeErrorResponse(request, response, waitSeconds);
        }
    }

    /**
     * Categoriza o endpoint para aplicar o rate limiting correto.
     *
     * Categorias:
     * - AUTH: endpoints de autenticação (/api/v1/auth/**) — limite por IP
     * - TRANSACTION: operações de escrita em transações — limite por usuário
     * - QUERY: consultas gerais (/api/v1/**) — limite por usuário
     * - NONE: endpoints sem rate limiting (swagger, actuator)
     */
    private EndpointCategory categorizeEndpoint(String path, String method) {
        // Endpoints sem rate limiting
        if (path.startsWith("/swagger-ui") ||
                path.startsWith("/api-docs") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/actuator")) {
            return EndpointCategory.NONE;
        }

        // Endpoints de autenticação — limite mais apertado por IP
        if (path.startsWith("/api/v1/auth/")) {
            return EndpointCategory.AUTH;
        }

        // Endpoints de transação (POST/PUT em transactions) — limite moderado
        if (path.startsWith("/api/v1/transactions") &&
                ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method))) {
            return EndpointCategory.TRANSACTION;
        }

        // Todos os outros endpoints da API — limite relaxado
        if (path.startsWith("/api/")) {
            return EndpointCategory.QUERY;
        }

        return EndpointCategory.NONE;
    }

    /**
     * Resolve a chave do bucket baseado na categoria.
     *
     * Para AUTH: usa o IP do cliente (porque não há JWT ainda)
     * Para TRANSACTION/QUERY: usa o email do JWT (porque já foi autenticado)
     *
     * Se o endpoint é protegido mas não tem autenticação (edge case),
     * faz fallback para o IP.
     */
    private String resolveKey(EndpointCategory category, HttpServletRequest request) {
        if (category == EndpointCategory.AUTH) {
            // Auth → limitar por IP
            return "auth:" + getClientIp(request);
        }

        // Para endpoints protegidos → limitar por usuário (email do JWT)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null
                && !"anonymousUser".equals(auth.getName())) {
            String prefix = category == EndpointCategory.TRANSACTION
                    ? "transaction:" : "query:";
            return prefix + auth.getName();
        }

        // Fallback: se não tem autenticação, usa IP
        return "query:" + getClientIp(request);
    }

    /**
     * Extrai o IP real do cliente.
     *
     * Quando a API está atrás de um proxy reverso (Nginx, CloudFlare, AWS ALB),
     * o IP que chega no request.getRemoteAddr() é o IP do PROXY, não do cliente.
     * O proxy coloca o IP real no header "X-Forwarded-For".
     *
     * X-Forwarded-For pode conter múltiplos IPs (cadeia de proxies):
     *   X-Forwarded-For: ip-real, proxy1, proxy2
     * O primeiro IP é o do cliente original.
     *
     * NOTA DE SEGURANÇA: em produção, validar que X-Forwarded-For vem
     * de um proxy confiável (Spring TrustedProxies ou filtro customizado).
     * Sem isso, um atacante pode falsificar o header para bypassar rate limiting.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Pega o primeiro IP (o do cliente real)
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Cria um novo bucket (balde de tokens) para a categoria.
     *
     * Bucket4j usa o conceito de "Bandwidth" (largura de banda):
     * - capacity: quantos tokens o balde comporta (tamanho máximo)
     * - refillGreedy: reabastecer todos os tokens no intervalo definido
     *
     * Exemplo para AUTH (5 req/min):
     *   Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofMinutes(1))
     *   → Balde com 5 fichas, reabastece 5 fichas a cada 1 minuto.
     *   → Na prática, ~1 ficha a cada 12 segundos (distribuição gradual).
     *
     * refillGreedy vs refillIntervally:
     * - refillGreedy: distribui tokens gradualmente ao longo do intervalo
     *   Ex: 5 tokens/min → 1 token a cada 12 segundos
     * - refillIntervally: espera o intervalo COMPLETO e adiciona todos de uma vez
     *   Ex: 5 tokens/min → espera 1 minuto inteiro, depois adiciona 5
     * Usamos greedy porque é mais justo para o usuário.
     */
    private Bucket createBucket(EndpointCategory category) {
        int limit = getLimitForCategory(category);

        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(limit)
                .refillGreedy(limit, Duration.ofMinutes(1))
                .build();

        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }

    /**
     * Retorna o limite de requisições por minuto para a categoria.
     */
    private int getLimitForCategory(EndpointCategory category) {
        return switch (category) {
            case AUTH -> rateLimitProperties.getAuthRequestsPerMinute();
            case TRANSACTION -> rateLimitProperties.getTransactionRequestsPerMinute();
            case QUERY -> rateLimitProperties.getQueryRequestsPerMinute();
            case NONE -> 0; // nunca deveria chegar aqui
        };
    }

    /**
     * Escreve a resposta 429 Too Many Requests no formato ErrorResponse.
     *
     * Usa o MESMO formato de erro (ErrorResponse) que o GlobalExceptionHandler
     * e o JwtAuthenticationEntryPoint usam — consistência para o frontend.
     *
     * Por que não lançar uma exceção e deixar o GlobalExceptionHandler tratar?
     * Pelo mesmo motivo do EntryPoint: estamos FORA do Controller (na cadeia
     * de filtros). O GlobalExceptionHandler só captura exceções de Controllers.
     * Precisamos escrever a resposta manualmente no HttpServletResponse.
     */
    private void writeErrorResponse(HttpServletRequest request,
                                    HttpServletResponse response,
                                    long waitSeconds) throws IOException {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                .message("Limite de requisições excedido. Tente novamente em "
                        + waitSeconds + " segundos.")
                .path(request.getRequestURI())
                .build();

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * Categorias de endpoints para rate limiting.
     *
     * Cada categoria tem um limite diferente porque o RISCO é diferente:
     * - AUTH: 5/min por IP — brute force é o maior risco
     * - TRANSACTION: 30/min por usuário — operações financeiras, custo alto
     * - QUERY: 60/min por usuário — leitura, risco baixo
     * - NONE: sem rate limiting (swagger, actuator)
     */
    private enum EndpointCategory {
        AUTH,
        TRANSACTION,
        QUERY,
        NONE
    }
}
