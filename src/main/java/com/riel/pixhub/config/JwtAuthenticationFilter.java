package com.riel.pixhub.config;

import com.riel.pixhub.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro JWT que intercepta TODA requisição HTTP antes de chegar no Controller.
 *
 * Este é o "segurança na catraca" do sistema. Para cada request, ele:
 * 1. Procura o header "Authorization: Bearer <token>"
 * 2. Se encontrou: valida o token (assinatura + expiração + tipo)
 * 3. Se válido: extrai os dados do usuário e "autentica" a request
 * 4. Se inválido/ausente: deixa passar sem autenticar (o Spring Security
 *    decidirá se o endpoint exige autenticação ou não)
 *
 * Por que extends OncePerRequestFilter?
 * O Spring pode processar uma request por múltiplos dispatchers (forward,
 * include, error). OncePerRequestFilter garante que nosso filtro roda
 * EXATAMENTE UMA VEZ por request, evitando validar o mesmo token duas vezes.
 *
 * Onde este filtro fica na cadeia?
 * SecurityConfig insere ele ANTES do UsernamePasswordAuthenticationFilter.
 * A cadeia fica assim:
 *   Request → CORS → CSRF(off) → JWT Filter (nós) → Authorization → Controller
 *
 * @Component: Spring gerencia este filtro como bean (pode ser injetado no SecurityConfig).
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Método principal — executado para TODA request HTTP.
     *
     * O fluxo é:
     * 1. Extrair token do header Authorization
     * 2. Se não tem token → passa adiante (filterChain.doFilter)
     *    O Spring Security decidirá: se o endpoint é público, OK. Se não, 401.
     * 3. Se tem token → valida com JwtService
     * 4. Se válido → cria autenticação no SecurityContext
     * 5. Se inválido → ignora (não autentica, não bloqueia)
     *    O Spring Security tratará como não autenticado.
     *
     * IMPORTANTE: este filtro NUNCA retorna 401 diretamente.
     * Ele só "autentica" ou "não autentica" a request.
     * Quem decide se a request pode continuar é o SecurityFilterChain
     * (configurado no SecurityConfig com authorizeHttpRequests).
     *
     * @param request     a requisição HTTP recebida
     * @param response    a resposta HTTP (não usamos, só passamos adiante)
     * @param filterChain a cadeia de filtros (chamamos doFilter para continuar)
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 1. Extrair o token do header "Authorization: Bearer eyJ..."
        String token = extractToken(request);

        // 2. Se não tem token, passa adiante sem autenticar
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Validar o token (assinatura + expiração)
        Claims claims = jwtService.validateToken(token);

        if (claims == null) {
            // Token inválido ou expirado — não autentica, mas não bloqueia.
            // O Spring Security decidirá: se o endpoint é público, continua normalmente.
            // Se o endpoint exige autenticação, retornará 401.
            filterChain.doFilter(request, response);
            return;
        }

        // 4. Verificar que é um access token (não refresh)
        // Sem essa verificação, alguém poderia usar um refresh token
        // para acessar endpoints protegidos — anulando a separação dos tokens.
        String tokenType = claims.get("type", String.class);
        if (!"access".equals(tokenType)) {
            log.warn("Tentativa de usar token do tipo '{}' como access token", tokenType);
            filterChain.doFilter(request, response);
            return;
        }

        // 5. Token válido! Extrair dados do usuário dos claims
        String email = claims.getSubject();
        String role = claims.get("role", String.class);

        // 6. Verificar se já existe autenticação no contexto
        // (evita processar duas vezes em caso de redirecionamento)
        if (SecurityContextHolder.getContext().getAuthentication() == null) {

            // 7. Criar o objeto de autenticação do Spring Security
            //
            // UsernamePasswordAuthenticationToken é a "ficha de identidade" que
            // o Spring Security usa internamente. Apesar do nome, não tem senha —
            // o primeiro argumento é o "principal" (quem é o usuário),
            // o segundo é "credentials" (null porque já validamos o JWT),
            // o terceiro são as "authorities" (permissões/roles).
            //
            // SimpleGrantedAuthority("ROLE_USER") → permite usar @PreAuthorize("hasRole('USER')")
            // O prefixo "ROLE_" é convenção do Spring Security.
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,                                          // principal (quem é)
                            null,                                           // credentials (não precisa, JWT já validou)
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))  // authorities (o que pode fazer)
                    );

            // Adiciona detalhes da request (IP, session ID) à autenticação.
            // Útil para logs de auditoria: "quem fez o quê, de onde".
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

            // 8. Registrar a autenticação no SecurityContext
            //
            // O SecurityContext é um "container de identidade" que vive durante
            // a request. Depois que setamos a autenticação aqui, qualquer parte
            // do código pode consultar: SecurityContextHolder.getContext().getAuthentication()
            // para saber quem é o usuário logado.
            //
            // O AccountService, por exemplo, poderá usar isso para saber quem
            // está fazendo a operação (audit trail com userId real, não "system").
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("Usuário autenticado via JWT: {} [{}]", email, role);
        }

        // 9. Continuar a cadeia de filtros → request chega no Controller
        filterChain.doFilter(request, response);
    }

    /**
     * Extrai o token JWT do header Authorization.
     *
     * O padrão OAuth2 define que o token vem no header assim:
     *   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
     *
     * "Bearer" é o tipo de autenticação (existem outros: Basic, Digest, etc).
     * Removemos o prefixo "Bearer " para ficar só com o token.
     *
     * @param request a requisição HTTP
     * @return o token JWT sem o prefixo "Bearer ", ou null se não encontrado
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        // Verifica se o header existe e começa com "Bearer "
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7); // Remove "Bearer " (7 caracteres)
        }

        return null;
    }
}
