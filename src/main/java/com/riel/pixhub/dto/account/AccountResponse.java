package com.riel.pixhub.dto.account;

import com.riel.pixhub.enums.AccountStatus;
import com.riel.pixhub.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de saída para respostas com dados da conta bancária.
 *
 * Contém APENAS os campos que o cliente tem permissão de ver.
 * O Jackson serializa os getters desta classe para montar o JSON da resposta.
 *
 * Por que não retornar a Entity Account direto?
 * 1. SEGURANÇA: a Entity tem campos internos (updatedAt) que não devem ser expostos
 * 2. PRIVACIDADE: holderDocument será mascarado pelo Mapper (***456.789-**)
 * 3. DESACOPLAMENTO: se a Entity mudar, a API pública não quebra
 * 4. CONTROLE: o DTO define exatamente o que aparece no JSON
 *
 * Exemplo de JSON retornado:
 * {
 *   "id": "550e8400-e29b-41d4-a716-446655440000",
 *   "holderName": "Riel Santos",
 *   "holderDocument": "12345678901",
 *   "bankCode": "00000000",
 *   "branch": "0001",
 *   "accountNumber": "123456",
 *   "accountType": "CHECKING",
 *   "balance": 0.00,
 *   "status": "ACTIVE",
 *   "createdAt": "2026-03-18T14:30:00"
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {

    /** ID único da conta (UUID gerado pelo sistema) */
    private UUID id;

    /** Nome completo do titular */
    private String holderName;

    /**
     * Documento do titular (CPF ou CNPJ).
     *
     * No futuro, o Mapper pode mascarar este campo:
     * CPF: "***456.789-**" / CNPJ: "**.***.456/0001-**"
     * Por enquanto, retorna completo — mascaramento será
     * implementado quando adicionarmos segurança (JWT).
     */
    private String holderDocument;

    /** Código do banco (ISPB) */
    private String bankCode;

    /** Agência bancária */
    private String branch;

    /** Número da conta */
    private String accountNumber;

    /** Tipo: CHECKING (corrente) ou SAVINGS (poupança) */
    private AccountType accountType;

    /**
     * Saldo atual em reais.
     * Em um sistema com JWT, este campo só seria visível para o próprio titular.
     */
    private BigDecimal balance;

    /** Status: ACTIVE, INACTIVE ou BLOCKED */
    private AccountStatus status;

    /** Data de criação da conta */
    private LocalDateTime createdAt;

    // NÃO TEM updatedAt — dado interno, irrelevante para o cliente
}
