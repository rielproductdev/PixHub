package com.riel.pixhub.enums;

/**
 * Status de um QR Code PIX.
 *
 * - ACTIVE: QR válido, pode ser escaneado e pago
 * - EXPIRED: prazo expirou (só para DYNAMIC)
 * - USED: já foi pago (só para DYNAMIC — impede pagamento duplo)
 *
 * QR STATIC nunca fica EXPIRED ou USED — fica ACTIVE para sempre
 * (a menos que seja manualmente desativado).
 */
public enum QrCodeStatus {
    ACTIVE,
    EXPIRED,
    USED
}
