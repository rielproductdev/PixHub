package com.riel.pixhub.entity;

import com.riel.pixhub.enums.UserRole;
import com.riel.pixhub.enums.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade de autenticação — representa um usuário que pode fazer login no sistema.
 *
 * Mapeia para a tabela "users" no PostgreSQL.
 *
 * Por que separar User de Account?
 * No PIX real, a "conta" é do banco (dados bancários, saldo) e o "usuário"
 * é quem acessa o sistema (credenciais, sessão). Um usuário pode ter várias contas
 * (ex: conta corrente + poupança), e no futuro um admin pode gerenciar contas
 * sem ser titular de nenhuma. Essa separação é o padrão em sistemas financeiros.
 *
 * O campo accountId é nullable porque:
 * 1. Um admin pode não ter conta bancária vinculada
 * 2. O registro do usuário pode acontecer antes da criação da conta
 * 3. Permite fluxo de onboarding em etapas (registra → cria conta → vincula)
 *
 * Segurança implementada nesta entidade:
 * - Senha armazenada com BCrypt (hash irreversível, com salt automático)
 * - Contador de tentativas falhas de login (proteção contra brute force)
 * - Bloqueio temporário após 5 falhas (lockedUntil define quando desbloqueia)
 * - Email como identificador único (previne contas duplicadas)
 */
@Entity
@Table(name = "users", indexes = {
    // Índice único no email — usado em toda autenticação (login, refresh token)
    @Index(name = "idx_user_email", columnList = "email", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    /**
     * Email do usuário — identificador único para login.
     *
     * Por que email e não username?
     * 1. É naturalmente único (cada pessoa tem o seu)
     * 2. Permite recuperação de senha sem campo extra
     * 3. Padrão em fintechs (Nubank, Inter, etc usam email/CPF)
     */
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter formato válido")
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    /**
     * Senha do usuário — armazenada como hash BCrypt.
     *
     * NUNCA armazene senhas em texto puro. BCrypt é a escolha certa porque:
     * 1. Inclui salt automático (cada hash é diferente mesmo para senhas iguais)
     * 2. É propositalmente lento (dificulta ataques de força bruta)
     * 3. O "cost factor" pode ser aumentado conforme hardware evolui
     *
     * O hash BCrypt tem ~60 caracteres, por isso length = 72 (margem de segurança).
     * A senha em texto puro NUNCA chega ao banco — o Service faz o hash antes de salvar.
     */
    @NotBlank(message = "Senha é obrigatória")
    @Column(name = "password", nullable = false, length = 72)
    private String password;

    /**
     * Nome completo do usuário.
     * Usado em respostas da API e logs de auditoria.
     */
    @NotBlank(message = "Nome completo é obrigatório")
    @Size(min = 2, max = 120, message = "Nome deve ter entre 2 e 120 caracteres")
    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    /**
     * Papel do usuário: USER ou ADMIN.
     *
     * Determina quais endpoints o usuário pode acessar.
     * O Spring Security lê essa role do JWT e verifica nos filtros.
     * Padrão: USER (contas admin são criadas manualmente ou por seed).
     */
    @NotNull(message = "Role é obrigatória")
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    @Builder.Default
    private UserRole role = UserRole.USER;

    /**
     * Status do usuário: ACTIVE ou LOCKED.
     *
     * LOCKED significa que o usuário errou a senha muitas vezes seguidas.
     * O sistema bloqueia temporariamente para impedir ataques de brute force.
     * Padrão: ACTIVE.
     */
    @NotNull(message = "Status é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    /**
     * FK opcional para a conta bancária vinculada.
     *
     * Nullable porque nem todo usuário tem conta (ex: admin).
     * Não usamos @ManyToOne aqui para evitar acoplamento circular —
     * a consulta da conta associada é feita via AccountRepository quando necessário.
     * Em um sistema maior, usaríamos um serviço de identidade separado (IAM).
     */
    @Column(name = "account_id")
    private UUID accountId;

    /**
     * Contador de tentativas de login falhas consecutivas.
     *
     * A cada login com senha errada, incrementa.
     * Ao atingir 5, o status muda para LOCKED e lockedUntil é definido.
     * Ao fazer login com sucesso, reseta para 0.
     *
     * Por que no banco e não em cache?
     * Se o servidor reiniciar, o cache some e o atacante ganha novas tentativas.
     * No banco, o contador persiste entre deploys.
     */
    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    /**
     * Data/hora até quando o usuário está bloqueado.
     *
     * null = não está bloqueado.
     * Se LocalDateTime.now() > lockedUntil, o bloqueio expirou e o usuário
     * pode tentar login novamente (o Service reseta o status automaticamente).
     *
     * Duração padrão do bloqueio: 30 minutos (configurável no Service).
     */
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;
}
