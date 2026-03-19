package com.riel.pixhub.enums;

/**
 * Tipos de ação registrados no log de auditoria.
 *
 * Toda operação que modifica dados deve gerar um registro de auditoria.
 * O BACEN pode solicitar logs de qualquer operação — sem auditoria,
 * a fintech toma multa e pode perder a licença.
 *
 * - CREATE: novo registro criado (ex: nova conta, nova chave PIX)
 * - UPDATE: registro alterado (ex: nome do titular atualizado)
 * - DELETE: registro removido (raro em fintech — preferimos soft delete)
 * - STATUS_CHANGE: mudança de status (ex: conta ACTIVE → BLOCKED)
 *   Separado de UPDATE porque mudanças de status são mais sensíveis
 *   e podem ter regras de aprovação diferentes.
 */
public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    STATUS_CHANGE
}
