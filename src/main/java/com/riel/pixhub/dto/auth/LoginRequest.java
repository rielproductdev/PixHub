package com.riel.pixhub.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * DTO para requisição de login.
 *
 * O frontend envia email + senha para POST /api/v1/auth/login.
 * O AuthService valida as credenciais e retorna tokens JWT.
 *
 * A senha viaja em texto puro no JSON (protegida por HTTPS em produção).
 * NUNCA trafegar senhas sem HTTPS — qualquer intermediário pode ler.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter formato válido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    private String password;
}
