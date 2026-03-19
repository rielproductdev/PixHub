package com.riel.pixhub.entity;

import com.riel.pixhub.enums.PixKeyStatus;
import com.riel.pixhub.enums.PixKeyType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entidade que representa uma chave PIX.
 *
 * No sistema PIX do BACEN, a chave é o "apelido" da conta.
 * Em vez de informar banco, agência e conta, o pagador só precisa
 * da chave (CPF, e-mail, telefone ou aleatória) para fazer um PIX.
 *
 * Regras reais do BACEN:
 * - Máx 5 chaves por conta de pessoa física (PF)
 * - Máx 20 chaves por conta de pessoa jurídica (PJ)
 * - Uma chave é ÚNICA no Brasil inteiro (garantido por índice UNIQUE)
 * - Portabilidade: chave pode ser transferida entre bancos
 *
 * Relacionamento: N chaves → 1 conta (@ManyToOne)
 */
@Entity
@Table(name = "pix_keys", indexes = {
    @Index(name = "idx_pix_key_account_id", columnList = "account_id"),
    @Index(name = "idx_pix_key_status", columnList = "status")
}, uniqueConstraints = {
    // UNIQUE: garante que não existem duas chaves com o mesmo valor em todo o sistema
    // Mesmo que o código tenha bug, o banco rejeita a duplicata
    @UniqueConstraint(name = "uk_pix_key_value", columnNames = "key_value")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PixKey extends BaseEntity {

    /**
     * Tipo da chave: CPF, CNPJ, EMAIL, PHONE ou RANDOM.
     * Cada tipo tem validação diferente (implementada no Service).
     */
    @NotNull(message = "Tipo da chave é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "key_type", nullable = false, length = 10)
    private PixKeyType keyType;

    /**
     * Valor da chave PIX — o identificador em si.
     * Ex: "12345678901" (CPF), "fulano@email.com", "+5581999999999", UUID aleatório.
     *
     * UNIQUE constraint garante unicidade no banco inteiro.
     * length = 77: maior chave possível é e-mail (até 77 caracteres no padrão BACEN).
     */
    @NotBlank(message = "Valor da chave é obrigatório")
    @Column(name = "key_value", nullable = false, length = 77, unique = true)
    private String keyValue;

    /**
     * Conta dona desta chave PIX.
     *
     * @ManyToOne: Muitas chaves podem pertencer a uma conta.
     * FetchType.LAZY: Não carrega a conta automaticamente — só quando acessar.
     *   Isso é importante para performance: se listarmos 100 chaves,
     *   não queremos 100 queries extras para carregar as contas.
     *
     * @JoinColumn: Define a coluna FK no banco. "account_id" aponta para accounts.id.
     */
    @NotNull(message = "Conta é obrigatória")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    /** Status: ACTIVE ou INACTIVE (soft delete) */
    @NotNull(message = "Status é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    @Builder.Default
    private PixKeyStatus status = PixKeyStatus.ACTIVE;

    /**
     * Data em que a chave foi desativada (soft delete).
     *
     * NULL = chave ativa. Preenchido = chave desativada.
     * Em fintech, NUNCA deletamos dados — o BACEN pode perguntar
     * "essa chave existiu em algum momento?". Sem este campo, não há resposta.
     */
    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;
}
