package com.riel.pixhub.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção lançada quando a autenticação falha.
 *
 * Casos de uso:
 * - Email não cadastrado → "Credenciais inválidas" (NUNCA dizer "email não existe")
 * - Senha incorreta → "Credenciais inválidas" (NUNCA dizer "senha errada")
 * - Conta bloqueada → "Conta temporariamente bloqueada"
 * - Refresh token inválido → "Token de renovação inválido"
 *
 * IMPORTANTE para segurança:
 * A mensagem NUNCA deve revelar se o email existe ou não no sistema.
 * Se dissermos "email não encontrado", um atacante pode enumerar emails
 * válidos testando um por um. A mensagem genérica "Credenciais inválidas"
 * não dá pista sobre QUAL campo está errado.
 *
 * Retorna HTTP 401 Unauthorized (não autenticado).
 * Diferente de 403 Forbidden (autenticado, mas sem permissão).
 */
public class AuthenticationFailedException extends BusinessException {

    public AuthenticationFailedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED");
    }
}
