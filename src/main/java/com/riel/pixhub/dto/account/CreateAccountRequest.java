package com.riel.pixhub.dto.account;

import com.riel.pixhub.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de entrada para criação de conta bancária.
 *
 * Contém APENAS os campos que o cliente tem permissão de enviar.
 * Campos como id, balance, status, createdAt são definidos pelo sistema,
 * não pelo cliente — por isso NÃO existem aqui.
 *
 * As annotations de validação (@NotBlank, @Size, @Pattern) são verificadas
 * automaticamente pelo Spring quando o Controller usa @Valid:
 *   public ResponseEntity create(@Valid @RequestBody CreateAccountRequest request)
 *
 * Se qualquer validação falhar, o Spring lança MethodArgumentNotValidException
 * ANTES de chamar o Service — o GlobalExceptionHandler retorna 400.
 *
 * Exemplo de JSON válido:
 * {
 *   "holderName": "Riel Santos",
 *   "holderDocument": "12345678901",
 *   "bankCode": "00000000",
 *   "branch": "0001",
 *   "accountNumber": "123456",
 *   "accountType": "CHECKING"
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccountRequest {

    /**
     * Nome completo do titular da conta.
     *
     * @NotBlank: não pode ser null, vazio ("") nem só espaços ("   ")
     * @Size: entre 2 e 120 caracteres
     */
    @NotBlank(message = "Nome do titular é obrigatório")
    @Size(min = 2, max = 120, message = "Nome deve ter entre 2 e 120 caracteres")
    private String holderName;

    /**
     * CPF (11 dígitos) ou CNPJ (14 dígitos) do titular.
     *
     * @Pattern: só aceita dígitos, com 11 ou 14 caracteres.
     * A validação dos dígitos verificadores (módulo 11) será feita
     * no Service pelo DocumentValidator — @Pattern só garante o formato.
     *
     * Por que duas validações (Pattern + Service)?
     * - @Pattern é rápida e rejeita "abc", "123", formatos absurdos
     * - DocumentValidator é mais pesada (cálculo) e valida se é CPF/CNPJ real
     * - Se o @Pattern já rejeita, o Service nem é chamado (economia)
     */
    @NotBlank(message = "Documento do titular é obrigatório")
    @Pattern(
            regexp = "^\\d{11}$|^\\d{14}$",
            message = "Documento deve ser CPF (11 dígitos) ou CNPJ (14 dígitos)"
    )
    private String holderDocument;

    /**
     * Código do banco (ISPB — 8 dígitos).
     * Ex: "00000000" (Banco do Brasil), "20018183" (Nubank)
     */
    @NotBlank(message = "Código do banco é obrigatório")
    @Pattern(regexp = "^\\d{1,8}$", message = "Código do banco deve ter até 8 dígitos")
    private String bankCode;

    /**
     * Número da agência bancária (até 4 dígitos).
     */
    @NotBlank(message = "Agência é obrigatória")
    @Size(max = 4, message = "Agência deve ter no máximo 4 caracteres")
    private String branch;

    /**
     * Número da conta (com dígito verificador, até 10 caracteres).
     */
    @NotBlank(message = "Número da conta é obrigatório")
    @Size(max = 10, message = "Número da conta deve ter no máximo 10 caracteres")
    private String accountNumber;

    /**
     * Tipo da conta: CHECKING (corrente) ou SAVINGS (poupança).
     *
     * @NotNull (não @NotBlank): Enums não são String — @NotBlank só funciona com String.
     * Se o cliente mandar "INVALID", o Jackson falha ao converter e retorna 400 automaticamente.
     * Se mandar "CHECKING" ou "SAVINGS", funciona.
     */
    @NotNull(message = "Tipo da conta é obrigatório")
    private AccountType accountType;
}
