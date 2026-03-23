package com.riel.pixhub.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * DTO de resposta para operações de autenticação (login, register, refresh).
 *
 * Retorna os tokens JWT que o frontend deve armazenar e enviar
 * em toda requisição autenticada.
 *
 * O frontend usa assim:
 * 1. Recebe esta resposta após login/register
 * 2. Armazena accessToken (em memória) e refreshToken (em httpOnly cookie ou localStorage)
 * 3. Em cada request: header "Authorization: Bearer {accessToken}"
 * 4. Quando access token expira (401): usa refreshToken para renovar
 *
 * @JsonInclude(NON_NULL): Se algum campo for null, não aparece no JSON.
 * Exemplo: no register, podemos não retornar mensagem extra.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    /**
     * Token JWT de acesso — curta duração (15 minutos).
     * Enviado no header Authorization de toda request.
     */
    private String accessToken;

    /**
     * Token JWT de renovação — longa duração (7 dias).
     * Usado apenas para obter um novo accessToken quando o atual expira.
     */
    private String refreshToken;

    /**
     * Tipo do token — sempre "Bearer" (padrão OAuth2).
     * O frontend monta o header: "Authorization: Bearer {accessToken}"
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Tempo de expiração do access token em segundos.
     * O frontend pode usar para agendar renovação automática.
     * Ex: 900 = 15 minutos → renovar aos 14 minutos.
     */
    private long expiresIn;
}
