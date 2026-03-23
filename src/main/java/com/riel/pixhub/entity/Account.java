package com.riel.pixhub.entity;

import com.riel.pixhub.config.EncryptedStringConverter;
import com.riel.pixhub.enums.AccountStatus;
import com.riel.pixhub.enums.AccountType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * Entidade que representa uma conta bancária no sistema PIX.
 *
 * Mapeia para a tabela "accounts" no PostgreSQL.
 * Cada conta pode ter múltiplas chaves PIX (relação 1:N com PixKey),
 * enviar/receber transações e configurar webhooks.
 *
 * Decisões de modelagem:
 * - holderDocument armazena CPF ou CNPJ (a validação distingue pelo tamanho)
 * - balance usa BigDecimal: Double tem erros de ponto flutuante
 *   (0.1 + 0.2 = 0.30000000000000004 com Double)
 * - status controla se a conta pode operar (BLOCKED impede tudo)
 */
@Entity
@Table(name = "accounts", indexes = {
    // Índice no documento do titular — consultas frequentes por CPF/CNPJ
    @Index(name = "idx_account_holder_document", columnList = "holder_document"),
    // Índice composto — buscar conta por banco + agência + número
    @Index(name = "idx_account_bank_branch_number", columnList = "bank_code, branch, account_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account extends BaseEntity {

    /**
     * Nome completo do titular da conta.
     * Obrigatório e com tamanho limitado para evitar abuso.
     */
    @NotBlank(message = "Nome do titular é obrigatório")
    @Size(min = 2, max = 120, message = "Nome deve ter entre 2 e 120 caracteres")
    @Column(name = "holder_name", nullable = false, length = 120)
    private String holderName;

    /**
     * CPF (11 dígitos) ou CNPJ (14 dígitos) do titular.
     *
     * Armazenamos só os dígitos, sem formatação (pontos, traços, barras).
     * A validação de dígitos verificadores será feita no Service.
     * Este campo determina se a conta é PF ou PJ (afeta limite de chaves PIX).
     *
     * @Convert: criptografa automaticamente com AES-256-GCM antes de salvar no banco.
     * No banco fica "dGhpcyBpcyBh..." (Base64), no Java fica "12345678901" (texto puro).
     * Transparente para o restante da aplicação — Service e Controller não sabem
     * que o dado é criptografado. O JPA cuida de tudo via AttributeConverter.
     */
    @NotBlank(message = "Documento do titular é obrigatório")
    @Column(name = "holder_document", nullable = false, length = 255)
    @Convert(converter = EncryptedStringConverter.class)
    private String holderDocument;

    /**
     * Código do banco (ISPB — Identificador do Sistema de Pagamentos Brasileiro).
     * No PIX real, identifica a instituição participante do SPI.
     * Ex: "00000000" = Banco do Brasil, "20018183" = Nubank
     */
    @NotBlank(message = "Código do banco é obrigatório")
    @Column(name = "bank_code", nullable = false, length = 8)
    private String bankCode;

    /** Número da agência bancária */
    @NotBlank(message = "Agência é obrigatória")
    @Column(name = "branch", nullable = false, length = 4)
    private String branch;

    /** Número da conta (com dígito verificador) */
    @NotBlank(message = "Número da conta é obrigatório")
    @Column(name = "account_number", nullable = false, length = 10)
    private String accountNumber;

    /**
     * Tipo da conta: CHECKING (corrente) ou SAVINGS (poupança).
     *
     * @Enumerated(EnumType.STRING): Salva o NOME do enum no banco ("CHECKING"),
     * não o índice (0, 1). Se usássemos ORDINAL e alguém reordenasse o enum,
     * todos os dados ficariam inconsistentes. STRING é mais seguro.
     */
    @NotNull(message = "Tipo da conta é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 10)
    private AccountType accountType;

    /**
     * Saldo atual da conta em reais.
     *
     * BigDecimal: tipo correto para valores monetários.
     * precision = 15: até 999.999.999.999,99 (trilhões)
     * scale = 2: sempre 2 casas decimais (centavos)
     *
     * Inicializado com ZERO para contas novas.
     */
    @NotNull(message = "Saldo é obrigatório")
    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    /**
     * Status da conta: ACTIVE, INACTIVE ou BLOCKED.
     * Contas BLOCKED não podem fazer nenhuma operação PIX.
     * Inicializado como ACTIVE para contas novas.
     */
    @NotNull(message = "Status é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;
}
