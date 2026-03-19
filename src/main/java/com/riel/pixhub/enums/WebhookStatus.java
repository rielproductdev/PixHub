package com.riel.pixhub.enums;

/**
 * Status de uma configuração de webhook.
 *
 * - ACTIVE: webhook funcional, recebe notificações
 * - INACTIVE: webhook desativado, não recebe nada
 *
 * Um webhook pode ser desativado automaticamente se falhar muitas vezes
 * (URL retornando erro). Isso será implementado na lógica de retry.
 */
public enum WebhookStatus {
    ACTIVE,
    INACTIVE
}
