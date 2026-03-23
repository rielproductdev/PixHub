package com.riel.pixhub.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * DTO para requisição de renovação de token.
 *
 * Quando o access token expira (após 15 min), o frontend envia
 * o refresh token para POST /api/v1/auth/refresh e recebe
 * um novo par de tokens (access + refresh).
 *
 * Por que rotacionar o refresh token?
 * Se devolvêssemos o MESMO refresh token, um token roubado seria
 * válido por 7 dias inteiros. Com rotação, o refresh token antigo
 * é "descartado" (na verdade, não fazemos blacklist nesta versão,
 * mas o novo token tem validade renovada a partir de agora).
 *
 * Em sistemas mais robustos, o refresh token antigo seria adicionado
 * a uma blacklist (Redis) para invalidação imediata.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token é obrigatório")
    private String refreshToken;
}
