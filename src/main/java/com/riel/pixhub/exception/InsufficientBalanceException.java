package com.riel.pixhub.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção lançada quando o saldo da conta é insuficiente para a operação.
 *
 * Retorna HTTP 422 (Unprocessable Entity).
 *
 * Poderia ser BusinessRuleViolationException, mas criamos uma exceção
 * separada porque:
 * 1. É o erro mais comum em transações financeiras
 * 2. O frontend precisa tratar de forma especial (mostrar saldo disponível)
 * 3. O errorCode "INSUFFICIENT_BALANCE" permite ação específica no app
 *
 * Exemplo no Service (Fase 06 — Transações):
 *   if (sender.getBalance().compareTo(amount) < 0) {
 *       throw new InsufficientBalanceException(sender.getBalance(), amount);
 *   }
 *
 * Resposta para o cliente:
 * {
 *   "status": 422,
 *   "errorCode": "INSUFFICIENT_BALANCE",
 *   "message": "Saldo insuficiente. Disponível: R$ 30,00. Necessário: R$ 50,00"
 * }
 *
 * UX: "Saldo insuficiente para esta transferência."
 */
public class InsufficientBalanceException extends BusinessException {

    public InsufficientBalanceException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_BALANCE");
    }

    /**
     * Construtor com valores — gera mensagem informativa automaticamente.
     * Ex: new InsufficientBalanceException(BigDecimal.valueOf(30), BigDecimal.valueOf(50))
     *     → "Saldo insuficiente. Disponível: 30.00. Necessário: 50.00"
     */
    public InsufficientBalanceException(java.math.BigDecimal available, java.math.BigDecimal required) {
        super(
            "Saldo insuficiente. Disponível: " + available + ". Necessário: " + required,
            HttpStatus.UNPROCESSABLE_ENTITY,
            "INSUFFICIENT_BALANCE"
        );
    }
}
