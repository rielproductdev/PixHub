package com.riel.pixhub.dto.account;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de entrada para atualização de conta bancária.
 *
 * Contém APENAS os campos que PODEM ser alterados após a criação.
 * Campos imutáveis (holderDocument, bankCode, branch, accountNumber)
 * NÃO estão aqui — o titular não muda de CPF nem de número da conta.
 *
 * Por que um DTO separado do CreateAccountRequest?
 * - Na criação, holderDocument é obrigatório (@NotBlank)
 * - Na atualização, holderDocument nem existe (não pode mudar)
 * - Se usássemos o mesmo DTO, teríamos que lidar com campos
 *   opcionais/obrigatórios em contextos diferentes — confuso
 *
 * Campos sem @NotBlank: são opcionais na atualização.
 * Se o cliente não mandar um campo, ele chega como null.
 * O Service verifica: se não é null, atualiza; se é null, mantém o atual.
 *
 * Exemplo de JSON:
 * {
 *   "holderName": "Riel Santos da Silva"
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAccountRequest {

    /**
     * Novo nome do titular (opcional).
     * Se enviado, será atualizado. Se null, mantém o atual.
     *
     * @Size sem @NotBlank: aceita null (não enviado), mas se enviado,
     * precisa ter entre 2 e 120 caracteres.
     */
    @Size(min = 2, max = 120, message = "Nome deve ter entre 2 e 120 caracteres")
    private String holderName;
}
