package com.riel.pixhub.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção lançada quando tentamos operar em uma conta bloqueada.
 *
 * Retorna HTTP 403 (Forbidden).
 *
 * Por que 403 e não 422?
 * - 422 = "a regra não permite" (temporário, pode mudar)
 * - 403 = "você não tem permissão" (a conta está BLOQUEADA, não pode operar)
 *
 * No contexto bancário, conta bloqueada significa:
 * - Decisão judicial (ex: investigação de fraude)
 * - Bloqueio preventivo por atividade suspeita
 * - Bloqueio administrativo
 *
 * Nenhuma operação é permitida em conta BLOCKED:
 * - Não pode enviar PIX
 * - Não pode receber PIX
 * - Não pode cadastrar chave
 * - Só pode consultar dados (read-only)
 *
 * A única ação possível é DESBLOQUEAR (unblock) — que requer
 * permissão de administrador.
 *
 * Exemplo no Service:
 *   if (account.getStatus() == AccountStatus.BLOCKED) {
 *       throw new AccountBlockedException(account.getId());
 *   }
 */
public class AccountBlockedException extends BusinessException {

    public AccountBlockedException(String message) {
        super(message, HttpStatus.FORBIDDEN, "ACCOUNT_BLOCKED");
    }

    /**
     * Construtor por ID da conta — gera mensagem padronizada.
     * Ex: new AccountBlockedException(uuid)
     *     → "Conta bloqueada. ID: uuid-123. Nenhuma operação permitida."
     */
    public AccountBlockedException(java.util.UUID accountId) {
        super(
            "Conta bloqueada. ID: " + accountId + ". Nenhuma operação permitida.",
            HttpStatus.FORBIDDEN,
            "ACCOUNT_BLOCKED"
        );
    }
}
