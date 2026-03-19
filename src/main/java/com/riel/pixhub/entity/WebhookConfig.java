package com.riel.pixhub.entity;

import com.riel.pixhub.enums.WebhookStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Configuração de webhook para notificação assíncrona.
 *
 * Quando um evento acontece (ex: transação completada), o sistema envia
 * um POST HTTP para a URL configurada pelo cliente. Assim, o cliente
 * não precisa ficar fazendo polling ("já completou? já completou?").
 *
 * Toda API financeira moderna usa webhooks (Stripe, PagSeguro, PIX).
 *
 * Segurança: o campo "secret" é usado para assinar o payload com HMAC.
 * O cliente recebe a notificação + assinatura e pode verificar que
 * veio realmente do nosso sistema (não de um atacante fazendo spoofing).
 */
@Entity
@Table(name = "webhook_configs", indexes = {
    @Index(name = "idx_webhook_account_id", columnList = "account_id"),
    @Index(name = "idx_webhook_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookConfig extends BaseEntity {

    /**
     * Conta que configurou este webhook.
     * Uma conta pode ter múltiplos webhooks (para diferentes URLs/eventos).
     */
    @NotNull(message = "Conta é obrigatória")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    /**
     * URL que receberá as notificações via POST.
     * Deve ser HTTPS em produção (segurança).
     * O sistema enviará JSON com os dados do evento.
     */
    @NotBlank(message = "URL é obrigatória")
    @Column(name = "url", nullable = false, length = 500)
    private String url;

    /**
     * Lista de eventos que ativam este webhook (separados por vírgula).
     * Ex: "TRANSACTION_COMPLETED,TRANSACTION_FAILED,KEY_REGISTERED"
     *
     * Em uma implementação mais robusta, seria uma tabela separada (N:N).
     * Para simplificar, usamos String com valores separados por vírgula.
     */
    @NotBlank(message = "Eventos são obrigatórios")
    @Column(name = "events", nullable = false, length = 500)
    private String events;

    /**
     * Chave secreta para assinatura HMAC.
     *
     * O sistema gera um hash do payload usando este secret.
     * O cliente recebe: payload + hash. Ele recalcula o hash
     * com seu secret e compara. Se bater, a notificação é legítima.
     *
     * Sem isso, um atacante poderia enviar notificações falsas
     * para a URL do cliente (ex: "transação de R$10.000 recebida").
     */
    @NotBlank(message = "Secret é obrigatório")
    @Column(name = "secret", nullable = false, length = 64)
    private String secret;

    /** Status: ACTIVE ou INACTIVE */
    @NotNull(message = "Status é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    @Builder.Default
    private WebhookStatus status = WebhookStatus.ACTIVE;
}
