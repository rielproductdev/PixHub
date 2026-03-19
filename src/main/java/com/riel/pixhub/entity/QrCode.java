package com.riel.pixhub.entity;

import com.riel.pixhub.enums.QrCodeStatus;
import com.riel.pixhub.enums.QrCodeType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade que representa um QR Code PIX.
 *
 * Segue o padrão BRCode do BACEN (baseado no EMV QRCPS).
 * Dois tipos:
 * - STATIC: QR fixo, sem valor, uso ilimitado (barraquinha de açaí)
 * - DYNAMIC: QR com valor e prazo, uso único (cobrança específica)
 *
 * O "payload" é a string codificada no QR Code — contém todas as informações
 * necessárias para o app do banco montar o pagamento ao escanear.
 */
@Entity
@Table(name = "qr_codes", indexes = {
    @Index(name = "idx_qrcode_pix_key", columnList = "pix_key"),
    @Index(name = "idx_qrcode_tx_id", columnList = "tx_id"),
    @Index(name = "idx_qrcode_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrCode extends BaseEntity {

    /** Tipo: STATIC ou DYNAMIC */
    @NotNull(message = "Tipo do QR Code é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private QrCodeType type;

    /** Chave PIX do recebedor (vinculada ao QR) */
    @NotBlank(message = "Chave PIX é obrigatória")
    @Column(name = "pix_key", nullable = false, length = 77)
    private String pixKey;

    /** Nome do comerciante (aparece no app do pagador ao escanear) */
    @NotBlank(message = "Nome do comerciante é obrigatório")
    @Column(name = "merchant_name", nullable = false, length = 25)
    private String merchantName;

    /** Cidade do comerciante (obrigatório no padrão BRCode) */
    @NotBlank(message = "Cidade do comerciante é obrigatória")
    @Column(name = "merchant_city", nullable = false, length = 15)
    private String merchantCity;

    /**
     * Valor do QR Code (obrigatório para DYNAMIC, opcional para STATIC).
     * Se NULL em STATIC, o pagador digita o valor na hora do pagamento.
     */
    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    /**
     * Identificador da transação (Transaction ID).
     * Obrigatório para QR DYNAMIC. Usado para conciliar o pagamento.
     * Máx 25 caracteres conforme padrão BACEN.
     */
    @Column(name = "tx_id", length = 25)
    private String txId;

    /** Descrição adicional (aparece para o pagador) */
    @Column(name = "description", length = 140)
    private String description;

    /**
     * Data de expiração — só para QR DYNAMIC.
     * Após essa data, o QR não pode mais ser pago (status → EXPIRED).
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * Payload EMV — a string que é codificada no QR Code.
     *
     * Segue o padrão BRCode do BACEN. Contém: tipo, chave, valor, merchantName,
     * merchantCity, txId, etc. É isso que o app do banco decodifica ao escanear.
     *
     * Armazenamos como TEXT porque o tamanho varia conforme os dados.
     */
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    /** Status: ACTIVE, EXPIRED ou USED */
    @NotNull(message = "Status é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    @Builder.Default
    private QrCodeStatus status = QrCodeStatus.ACTIVE;
}
