package com.riel.pixhub.dto.auth;

import com.riel.pixhub.validation.NoHtml;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO para requisição de registro de novo usuário.
 *
 * O frontend envia estes campos para POST /api/v1/auth/register.
 * Campos que o usuário NÃO controla (id, role, status, createdAt)
 * são definidos pelo backend — nunca aparecem no request.
 *
 * Validações com Bean Validation:
 * - @NotBlank: campo obrigatório e não pode ser só espaços
 * - @Email: deve ter formato de email válido
 * - @Size: senha com mínimo de 8 caracteres (recomendação OWASP)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Nome completo é obrigatório")
    @Size(min = 2, max = 120, message = "Nome deve ter entre 2 e 120 caracteres")
    @NoHtml
    private String fullName;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter formato válido")
    private String email;

    /**
     * Senha em texto puro — será convertida em hash BCrypt pelo AuthService.
     *
     * Mínimo 8 caracteres: recomendação OWASP para senhas seguras.
     * O frontend pode ter validação adicional (maiúscula, número, etc),
     * mas o backend garante pelo menos o tamanho mínimo.
     */
    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, max = 100, message = "Senha deve ter entre 8 e 100 caracteres")
    private String password;
}
