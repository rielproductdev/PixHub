package com.riel.pixhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuração central de segurança da aplicação.
 *
 * Esta classe define TRÊS coisas fundamentais:
 * 1. QUEM pode acessar O QUE (regras de autorização por endpoint)
 * 2. COMO a autenticação funciona (filtro JWT na cadeia)
 * 3. DE ONDE podem vir requisições (CORS para o frontend React)
 *
 * @Configuration: Marca como fonte de beans (objetos gerenciados pelo Spring).
 * @EnableWebSecurity: Ativa o Spring Security para interceptar todas as requests.
 * @EnableMethodSecurity: Permite usar @PreAuthorize nos métodos dos Controllers.
 *   Exemplo: @PreAuthorize("hasRole('ADMIN')") → só admins acessam esse endpoint.
 *   Sem esta annotation, @PreAuthorize é ignorada silenciosamente.
 *
 * Cadeia de filtros (ordem que cada request atravessa):
 *   Request HTTP
 *     → CORS Filter (origem permitida?)
 *     → CSRF Filter (desabilitado — API stateless)
 *     → JwtAuthenticationFilter (tem token? é válido? autentica!)
 *     → AuthorizationFilter (endpoint público ou protegido?)
 *     → Controller (só chega aqui se passou em tudo)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final RateLimitingFilter rateLimitingFilter;

    /**
     * Spring injeta todas as dependências automaticamente.
     * Todas foram criadas como @Component, então o Spring já as conhece.
     */
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                          RateLimitingFilter rateLimitingFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.rateLimitingFilter = rateLimitingFilter;
    }

    /**
     * Configura a cadeia de filtros de segurança.
     *
     * Esta é a configuração DEFINITIVA da Fase 03. As portas agora estão FECHADAS:
     * - Endpoints públicos: auth, swagger, actuator (acessíveis sem token)
     * - Todos os demais: exigem JWT válido no header Authorization
     *
     * O JwtAuthenticationFilter é inserido ANTES do filtro padrão do Spring
     * (UsernamePasswordAuthenticationFilter). Assim, nosso filtro JWT roda
     * primeiro e já autentica a request antes de qualquer outra verificação.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CORS: permite que o frontend React (localhost:5173 em dev) acesse a API.
            // Sem CORS, o navegador bloqueia requisições de uma origem diferente.
            // A configuração detalhada está no método corsConfigurationSource() abaixo.
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // CSRF desabilitado: APIs REST stateless usam JWT, não cookies.
            // CSRF protege contra ataques via formulários HTML — não se aplica aqui.
            .csrf(csrf -> csrf.disable())

            // ====== SECURITY HEADERS (Passo 3.5) ======
            // Headers HTTP que instruem o navegador a se proteger contra ataques comuns.
            // São "instruções de segurança" que o servidor envia junto com cada resposta.
            // O navegador lê esses headers e ativa proteções automaticamente.
            .headers(headers -> headers
                // X-Content-Type-Options: nosniff
                // Impede o navegador de "adivinhar" o tipo do conteúdo.
                // Sem isso: navegador pode interpretar JSON como HTML → XSS.
                // Ataque: atacante envia JSON malicioso que contém <script>,
                // o navegador "adivinha" que é HTML e executa o script.
                .contentTypeOptions(cto -> {})  // Spring já aplica "nosniff" por padrão

                // X-Frame-Options: DENY
                // Impede que a API seja carregada dentro de um <iframe>.
                // Protege contra clickjacking: site malicioso coloca nossa API
                // num iframe invisível e engana o usuário para clicar em botões.
                .frameOptions(frame -> frame.deny())

                // Cache-Control: no-cache, no-store, max-age=0, must-revalidate
                // Impede que respostas com dados sensíveis sejam cacheadas.
                // Sem isso: token JWT ou dados bancários podem ficar no cache
                // do navegador/proxy e ser acessados por outro usuário.
                .cacheControl(cache -> {})  // Spring aplica headers de no-cache por padrão

                // X-XSS-Protection: 0
                // Desabilita o filtro XSS embutido do navegador (obsoleto).
                // Parece contraintuitivo, mas o filtro antigo do Chrome tinha BUGS
                // que podiam ser explorados para CAUSAR XSS. A proteção moderna
                // é o Content-Security-Policy (abaixo), não esse header legado.
                .xssProtection(xss -> xss.disable())

                // Content-Security-Policy: default-src 'none'; frame-ancestors 'none'
                // CSP é a proteção MODERNA contra XSS. Diz ao navegador:
                // "não carregue scripts, estilos, imagens ou frames de NENHUMA origem".
                // Para uma API REST (que só retorna JSON), isso é perfeito:
                // não há razão para o navegador carregar qualquer recurso.
                .contentSecurityPolicy(csp ->
                    csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
            )

            // Sessão STATELESS: servidor não guarda estado entre requests.
            // Cada request traz sua autenticação (JWT no header).
            // Permite escalar horizontalmente (qualquer servidor atende qualquer request).
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Regras de autorização — QUEM pode acessar O QUE:
            .authorizeHttpRequests(auth -> auth
                // ENDPOINTS PÚBLICOS (sem token):
                // - /api/v1/auth/** → login, register, refresh (o usuário PRECISA acessar sem token)
                // - /actuator/** → health checks (Docker, load balancer)
                // - /swagger-ui/** e /api-docs/** → documentação interativa da API
                .requestMatchers(
                    "/api/v1/auth/**",
                    "/actuator/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api-docs/**",
                    "/v3/api-docs/**"
                ).permitAll()

                // TUDO MAIS → AUTENTICADO (precisa de JWT válido)
                // Se a request não tiver token ou o token for inválido → 401 Unauthorized
                // Se o token for válido → request prossegue para o Controller
                .anyRequest().authenticated()
            )

            // Quando a autenticação falha (sem token ou token inválido em endpoint protegido),
            // usar nosso EntryPoint para retornar JSON padronizado em vez de HTML genérico.
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            )

            // INSERIR nosso filtro JWT ANTES do filtro padrão do Spring Security.
            // O filtro padrão (UsernamePasswordAuthenticationFilter) espera username+senha
            // em formulário HTML — não é o nosso caso. Nosso JwtAuthenticationFilter
            // lê o token do header e faz a autenticação antes.
            .addFilterBefore(jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class)

            // INSERIR o Rate Limiting DEPOIS do JWT Filter.
            // A ordem importa: o RateLimitingFilter precisa do SecurityContext
            // (preenchido pelo JwtAuthenticationFilter) para identificar QUEM
            // está fazendo a request — e aplicar o limite correto por usuário.
            //
            // Cadeia atualizada:
            //   Request → CORS → CSRF → JwtFilter → RateLimitFilter → Authorization → Controller
            .addFilterAfter(rateLimitingFilter,
                    JwtAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configuração de CORS (Cross-Origin Resource Sharing).
     *
     * CORS é uma política do NAVEGADOR que bloqueia requisições de uma "origem"
     * (domínio + porta) para outra. Exemplo:
     * - Frontend React roda em http://localhost:5173
     * - API roda em http://localhost:8080
     * - O navegador BLOQUEIA a requisição porque as portas são diferentes
     *
     * Esta configuração diz ao navegador: "pode deixar, eu confio nessas origens".
     *
     * Em PRODUÇÃO, trocar "localhost" pelas URLs reais do frontend.
     * O ideal é ler de variável de ambiente: ${CORS_ALLOWED_ORIGINS}
     *
     * IMPORTANTE: CORS só afeta NAVEGADORES. Ferramentas como curl, Postman
     * e apps mobile NÃO são afetados — eles ignoram CORS.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Origens permitidas: quais URLs podem fazer requisições para a API.
        // Em dev: React (5173), Vite preview (4173).
        // Em produção: adicionar a URL real do frontend.
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",    // Vite dev server (React)
                "http://localhost:4173",    // Vite preview
                "http://localhost:3000"     // Alternativa (Create React App)
        ));

        // Métodos HTTP permitidos: GET, POST, PUT, PATCH, DELETE, OPTIONS.
        // OPTIONS é necessário para preflight requests (o navegador envia OPTIONS
        // antes de POST/PUT/DELETE para verificar se o CORS permite).
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Headers permitidos: quais headers o frontend pode enviar.
        // Authorization: onde vai o JWT ("Bearer eyJ...")
        // Content-Type: tipo do body (application/json)
        // "*" permitiria qualquer header — usamos lista explícita por segurança.
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "Accept", "Origin",
                "X-Requested-With"));

        // Headers expostos: quais headers da RESPOSTA o frontend pode ler.
        // Por padrão, o navegador só permite ler alguns headers básicos.
        // Expomos Authorization (se o backend devolver token no header)
        // e X-RateLimit-Remaining (para rate limiting na Fase 3.4).
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization", "X-RateLimit-Remaining"));

        // allowCredentials: permite enviar cookies/Authorization em requisições CORS.
        // Necessário para que o header Authorization seja incluído nas requests.
        configuration.setAllowCredentials(true);

        // maxAge: por quanto tempo o navegador pode cachear o resultado do preflight.
        // 3600 = 1 hora. Evita que o navegador envie OPTIONS antes de cada request.
        configuration.setMaxAge(3600L);

        // Aplica esta configuração para TODAS as rotas (/**)
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * PasswordEncoder — responsável por hash e verificação de senhas.
     *
     * BCrypt é o algoritmo escolhido porque:
     * 1. Inclui SALT automático: mesmo duas senhas iguais geram hashes diferentes
     * 2. É propositalmente LENTO: ~100ms por hash (dificulta brute force)
     * 3. O "cost factor" pode ser aumentado conforme hardware evolui
     *
     * Como funciona:
     * - passwordEncoder.encode("senha123") → "$2a$10$xKz8Gn..." (hash irreversível)
     * - passwordEncoder.matches("senha123", "$2a$10$xKz8Gn...") → true
     * - passwordEncoder.matches("outra", "$2a$10$xKz8Gn...") → false
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
