package com.riel.pixhub.enums;

/**
 * Tipos de QR Code PIX conforme padrão BRCode do BACEN.
 *
 * - STATIC: QR fixo, sem valor definido. Usado em lojinhas, ambulantes.
 *   O pagador digita o valor na hora. Pode ser usado infinitas vezes.
 *
 * - DYNAMIC: QR com valor e prazo. Usado para cobranças específicas.
 *   Tem txId vinculado, expira, e só pode ser pago uma vez.
 *   Similar a um boleto, mas instantâneo.
 */
public enum QrCodeType {
    STATIC,
    DYNAMIC
}
