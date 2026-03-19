package com.riel.pixhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração de segurança da aplicação.
 *
 * @Configuration: Marca esta classe como fonte de beans (objetos gerenciados pelo Spring).
 * O Spring processa classes @Configuration na inicialização e registra os beans retornados
 * por métodos @Bean no ApplicationContext (container de dependências).
 *
 * @EnableWebSecurity: Ativa o Spring Security para interceptar todas as requisições HTTP.
 * Sem esta annotation, o Security estaria no classpath mas não filtraria nada.
 *
 * SecurityFilterChain: Cadeia de filtros que toda request HTTP atravessa.
 * Cada filtro decide se a request pode continuar ou deve ser bloqueada.
 * Funciona como um pipeline: Request → Filtro1 → Filtro2 → ... → Controller
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configura a cadeia de filtros de segurança.
     *
     * @Bean: Registra o objeto retornado no container do Spring.
     * Outros componentes podem injetá-lo via @Autowired.
     *
     * Esta é a configuração BÁSICA (Fase 01). Na Fase de Autenticação,
     * adicionaremos:
     * - JwtAuthenticationFilter (valida tokens JWT)
     * - UserDetailsService (carrega usuários do banco)
     * - PasswordEncoder (hash de senhas com BCrypt)
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF (Cross-Site Request Forgery): proteção contra ataques
            // onde um site malicioso faz requests em nome do usuário.
            // Desabilitamos porque APIs REST são stateless (usam JWT, não cookies).
            // Em apps com formulários HTML, NUNCA desabilitar.
            .csrf(csrf -> csrf.disable())

            // Política de sessão STATELESS: o servidor NÃO guarda estado entre requests.
            // Cada request precisa trazer sua própria autenticação (JWT no header).
            // Isso permite escalar horizontalmente (qualquer servidor pode atender qualquer request).
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Regras de autorização por endpoint:
            .authorizeHttpRequests(auth -> auth
                // Endpoints públicos (não requerem autenticação):
                // - /actuator/** → health checks (Docker, load balancer precisam acessar)
                // - /swagger-ui/** e /api-docs/** → documentação da API
                // - /v3/api-docs/** → especificação OpenAPI em JSON
                // - /api/** → endpoints REST (temporário até implementar JWT na Fase de Auth)
                .requestMatchers(
                    "/actuator/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api-docs/**",
                    "/v3/api-docs/**",
                    "/api/**"
                ).permitAll()

                // Todos os outros endpoints requerem autenticação.
                // Quando implementarmos JWT, moveremos /api/** para authenticated()
                // e apenas /auth/** (login, register) ficará em permitAll().
                .anyRequest().authenticated()
            );

        return http.build();
    }

}
