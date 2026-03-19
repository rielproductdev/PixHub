-- ============================================================
-- V6__create_audit_log_table.sql — Tabela de auditoria
-- ============================================================
-- Registra TODA operação que modifica dados no sistema.
-- O BACEN pode solicitar esses logs a qualquer momento.
-- Sem auditoria completa, a fintech toma multa.
--
-- Polymorphic association: entity_type + entity_id referenciam
-- qualquer entidade (Account, PixKey, Transaction, etc).
-- ============================================================

CREATE TABLE audit_logs (
    id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type     VARCHAR(50)     NOT NULL,
    entity_id       VARCHAR(36)     NOT NULL,
    action          VARCHAR(15)     NOT NULL,
    performed_by    VARCHAR(100),
    old_value       TEXT,
    new_value       TEXT,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_audit_action CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'STATUS_CHANGE'))
);

-- Índice composto para buscar logs de uma entidade específica
CREATE INDEX idx_audit_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_action ON audit_logs (action);
CREATE INDEX idx_audit_performed_by ON audit_logs (performed_by);

COMMENT ON TABLE audit_logs IS 'Log de auditoria — registra toda operação para compliance BACEN';
COMMENT ON COLUMN audit_logs.entity_type IS 'Nome da entidade: Account, PixKey, Transaction, etc';
COMMENT ON COLUMN audit_logs.old_value IS 'Snapshot JSON do estado ANTES da alteração';
COMMENT ON COLUMN audit_logs.new_value IS 'Snapshot JSON do estado DEPOIS da alteração';
