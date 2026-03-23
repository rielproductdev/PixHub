package com.riel.pixhub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riel.pixhub.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Ponto de entrada para requisições NÃO AUTENTICADAS em endpoints protegidos.
 *
 * Quando alguém tenta acessar um endpoint que exige JWT sem enviar token
 * (ou com token inválido), o Spring Security chama este handler.
 *
 * Sem ele, o Spring retorna um HTML genérico:
 *   <html><body><h1>Whitelabel Error Page</h1>...</html>
 *
 * Com ele, retornamos JSON padronizado (ErrorResponse) — consistente
 * com todos os outros erros da API:
 *   {"status": 401, "error": "Unauthorized", "message": "..."}
 *
 * O frontend pode tratar este 401 para redirecionar ao login:
 *   if (response.status === 401) → window.location = '/login'
 *
 * AuthenticationEntryPoint é uma interface do Spring Security.
 * O método commence() é chamado quando a autenticação falha.
 */
@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Chamado quando uma request não autenticada tenta acessar endpoint protegido.
     *
     * Em vez de delegar para o mecanismo padrão (que retorna HTML),
     * escrevemos a resposta JSON diretamente no HttpServletResponse.
     *
     * @param request       a requisição HTTP que foi rejeitada
     * @param response      onde escrevemos a resposta de erro
     * @param authException detalhes da falha de autenticação
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.warn("Acesso não autenticado a: {} {}", request.getMethod(), request.getRequestURI());

        // Monta a resposta no mesmo formato que o GlobalExceptionHandler usa
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message("Autenticação necessária. Envie um token JWT válido no header Authorization.")
                .path(request.getRequestURI())
                .build();

        // Escreve o JSON diretamente na resposta HTTP
        // (não podemos usar ResponseEntity aqui porque estamos fora do Controller)
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
