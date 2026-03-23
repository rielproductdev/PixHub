package com.riel.pixhub.enums;

/**
 * Papéis (roles) de usuário no sistema.
 *
 * Usados pelo Spring Security para controlar acesso a endpoints.
 * O JWT carrega a role como claim, e o SecurityFilterChain
 * usa @PreAuthorize ou hasRole() para restringir acesso.
 *
 * - USER: cliente comum, pode gerenciar suas contas e chaves PIX
 * - ADMIN: acesso administrativo (bloquear contas, ver audit logs, etc)
 *
 * Por que só dois papéis?
 * Em fintechs reais existem dezenas (COMPLIANCE, SUPPORT, etc),
 * mas para um MVP o binário USER/ADMIN é suficiente.
 * Se precisar expandir, basta adicionar novos valores ao enum.
 */
public enum UserRole {
    USER,
    ADMIN
}
