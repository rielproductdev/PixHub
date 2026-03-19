package com.riel.pixhub.entity;

import com.riel.pixhub.enums.TransactionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade que representa uma transação PIX (transferência).
 *
 * É a entidade mais complexa do sistema — envolve:
 * - Débito na conta do remetente
 * - Crédito na conta do destinatário
 * - Máquina de estados (PENDING → PROCESSING → COMPLETED/FAILED)
 * - Idempotência (evitar cobrar duas vezes)
 * - Identificação única no ecossistema PIX (endToEndId)
 *
 * Processamento assíncrono via Kafka:
 * 1. API recebe a requisição → cria Transaction com status PENDING
 * 2. Publica mensagem no Kafka → status muda para PROCESSING
 * 3. Consumer processa (debita/credita) → status COMPLETED ou FAILED
 */
@Entity
@Table(name = "transactions", indexes = {
    // endToEndId: consulta frequente — é o "recibo" do PIX
    @Index(name = "idx_transaction_end_to_end_id", columnList = "end_to_end_id"),
    // idempotencyKey: verificação de duplicidade a cada nova transação
    @Index(name = "idx_transaction_idempotency_key", columnList = "idempotency_key"),
    // status: filtrar transações por estado (ex: "todas as PENDING")
    @Index(name = "idx_transaction_status", columnList = "status"),
    // sender: listar transações enviadas por uma conta
    @Index(name = "idx_transaction_sender", columnList = "sender_account_id"),
    // receiver: listar transações recebidas por uma conta
    @Index(name = "idx_transaction_receiver", columnList = "receiver_account_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_transaction_end_to_end_id", columnNames = "end_to_end_id"),
    @UniqueConstraint(name = "uk_transaction_idempotency_key", columnNames = "idempotency_key")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

    /**
     * Identificador fim-a-fim — único em TODO o ecossistema PIX do Brasil.
     *
     * Formato definido pelo BACEN: E{ISPB}{data}{sequencial}
     * Ex: "E0000000020250317120000000001"
     * Com esse ID, qualquer banco participante encontra a mesma transação.
     * É o "número do recibo" que aparece no comprovante.
     */
    @NotBlank(message = "EndToEndId é obrigatório")
    @Column(name = "end_to_end_id", nullable = false, length = 32, unique = true)
    private String endToEndId;

    /**
     * Valor da transferência em reais.
     * BigDecimal com 2 casas decimais — nunca Double para dinheiro.
     * Deve ser positivo (validação no Service).
     */
    @NotNull(message = "Valor é obrigatório")
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /** Descrição/mensagem do PIX (opcional, até 140 caracteres como no PIX real) */
    @Size(max = 140, message = "Descrição deve ter no máximo 140 caracteres")
    @Column(name = "description", length = 140)
    private String description;

    /**
     * Conta que ENVIA o PIX (quem paga).
     * @ManyToOne LAZY: uma conta pode enviar muitas transações.
     */
    @NotNull(message = "Conta remetente é obrigatória")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_account_id", nullable = false)
    private Account senderAccount;

    /**
     * Conta que RECEBE o PIX (quem é pago).
     * @ManyToOne LAZY: uma conta pode receber muitas transações.
     */
    @NotNull(message = "Conta destinatária é obrigatória")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_account_id", nullable = false)
    private Account receiverAccount;

    /**
     * Chave PIX usada para identificar o destinatário.
     *
     * Salvo como String (NÃO FK) propositalmente:
     * Se a chave for desativada/portada depois, o histórico da transação
     * precisa continuar mostrando qual chave foi usada na época.
     * FK quebraria se a chave fosse deletada.
     */
    @Column(name = "receiver_pix_key", length = 77)
    private String receiverPixKey;

    /**
     * Status atual: PENDING, PROCESSING, COMPLETED, FAILED ou REFUNDED.
     * Segue a máquina de estados — transições ilegais devem ser bloqueadas no Service.
     */
    @NotNull(message = "Status é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    /**
     * Chave de idempotência — CRÍTICO para evitar cobranças duplicadas.
     *
     * Se o cliente clica "Pagar" duas vezes por causa de lag, ambas as requisições
     * chegam com a mesma idempotencyKey. O Service detecta que já existe uma
     * transação com essa chave e retorna a transação existente em vez de criar outra.
     *
     * Sem isso: cliente é cobrado duas vezes. Com isso: sistema é resiliente.
     * Toda API financeira séria (Stripe, PagSeguro, BACEN) exige idempotência.
     */
    @NotBlank(message = "Chave de idempotência é obrigatória")
    @Column(name = "idempotency_key", nullable = false, length = 36, unique = true)
    private String idempotencyKey;

    /** Quando a transação foi efetivamente processada (débito+crédito executados) */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * Motivo da falha, se status = FAILED.
     * Ex: "Saldo insuficiente", "Conta bloqueada", "Chave não encontrada"
     * Aparece no extrato do cliente para que ele entenda o que aconteceu.
     */
    @Column(name = "failure_reason", length = 255)
    private String failureReason;
}
