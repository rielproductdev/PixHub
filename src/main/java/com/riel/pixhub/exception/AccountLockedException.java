package com.riel.pixhub.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção lançada quando o usuário tenta fazer login mas está bloqueado.
 *
 * O bloqueio acontece após 5 tentativas consecutivas de login com senha errada.
 * É temporário — expira após o tempo definido em lockedUntil (padrão: 30 min).
 *
 * Retorna HTTP 423 Locked — código específico para "recurso bloqueado".
 * Usamos 423 em vez de 401 para o frontend diferenciar:
 * - 401 = credenciais erradas → mostrar "email ou senha incorretos"
 * - 423 = conta bloqueada → mostrar "tente novamente em X minutos"
 */
public class AccountLockedException extends BusinessException {

    public AccountLockedException(String message) {
        super(message, HttpStatus.LOCKED, "ACCOUNT_LOCKED");
    }
}
