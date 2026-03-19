package com.riel.pixhub.enums;

/**
 * Máquina de estados de uma transação PIX.
 *
 * Fluxo normal:
 *   PENDING → PROCESSING → COMPLETED
 *
 * Fluxo com falha:
 *   PENDING → PROCESSING → FAILED
 *   PENDING → FAILED (validação falhou antes de processar)
 *
 * Estorno:
 *   COMPLETED → REFUNDED
 *
 * Regras importantes:
 * - FAILED é estado final — não tem volta
 * - Só COMPLETED pode virar REFUNDED
 * - PENDING → PROCESSING acontece quando o Kafka consome a mensagem
 * - PROCESSING → COMPLETED acontece quando o saldo é debitado/creditado
 *
 * No mercado, respeitar a máquina de estados é crítico.
 * Uma transação FAILED que "volta" seria uma falha grave de segurança.
 */
public enum TransactionStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUNDED
}
