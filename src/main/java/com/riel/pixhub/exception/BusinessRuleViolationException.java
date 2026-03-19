package com.riel.pixhub.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção lançada quando uma regra de negócio é violada.
 *
 * Retorna HTTP 422 (Unprocessable Entity).
 *
 * Esta é a exceção mais usada no sistema. 422 significa:
 * "Entendi sua requisição (formato OK), mas não posso processá-la
 *  porque viola uma regra de negócio."
 *
 * Quando usar:
 * - CPF/CNPJ inválido (formato correto mas dígitos verificadores errados)
 * - Limite de chaves PIX atingido (máx 5 PF, 20 PJ)
 * - Tentar operar em conta inativa
 * - Valor de transferência negativo ou zero
 * - Qualquer regra do BACEN violada
 *
 * Exemplo no Service:
 *   if (!DocumentValidator.isValidCpf(document)) {
 *       throw new BusinessRuleViolationException("CPF inválido");
 *   }
 *
 * Diferença entre 400 e 422:
 * - 400: "Não entendi o que você mandou" (JSON quebrado, campo faltando)
 * - 422: "Entendi, mas não pode" (CPF inválido, limite atingido)
 */
public class BusinessRuleViolationException extends BusinessException {

    public BusinessRuleViolationException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "BUSINESS_RULE_VIOLATION");
    }
}
