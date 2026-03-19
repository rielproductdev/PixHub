package com.riel.pixhub.enums;

/**
 * Status de uma chave PIX.
 *
 * - ACTIVE: chave funcional, pode receber PIX
 * - INACTIVE: chave desativada (soft delete)
 *
 * No PIX real, quando o titular faz "portabilidade" da chave
 * (transfere para outro banco), a chave antiga fica INACTIVE.
 * Nunca deletamos — o histórico precisa existir para auditoria.
 */
public enum PixKeyStatus {
    ACTIVE,
    INACTIVE
}
