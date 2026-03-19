package com.riel.pixhub.enums;

/**
 * Status possíveis de uma conta bancária.
 *
 * Isso é uma máquina de estados simplificada:
 * - ACTIVE: conta operacional, pode fazer/receber PIX
 * - INACTIVE: conta desativada pelo titular, sem operações
 * - BLOCKED: conta bloqueada por suspeita de fraude ou ordem judicial
 *
 * No mercado, o status BLOCKED impede TODAS as operações.
 * A lógica de negócio (Service) deve verificar o status antes
 * de permitir qualquer transação.
 */
public enum AccountStatus {
    ACTIVE,
    INACTIVE,
    BLOCKED
}
