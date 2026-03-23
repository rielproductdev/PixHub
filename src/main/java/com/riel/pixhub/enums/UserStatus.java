package com.riel.pixhub.enums;

/**
 * Status possíveis de um usuário no sistema de autenticação.
 *
 * Máquina de estados:
 * - ACTIVE: pode fazer login normalmente
 * - LOCKED: bloqueado temporariamente após múltiplas tentativas de login falhas
 *           (proteção contra brute force — o sistema desbloqueia automaticamente
 *           após o tempo configurado em lockedUntil)
 *
 * Por que separar UserStatus de AccountStatus?
 * Um usuário pode ter várias contas, e o bloqueio de autenticação (LOCKED)
 * é diferente do bloqueio de conta bancária (BLOCKED).
 * Ex: um admin pode estar LOCKED por errar a senha, mas as contas que ele
 * gerencia continuam ACTIVE para operações via outros canais.
 */
public enum UserStatus {
    ACTIVE,
    LOCKED
}
