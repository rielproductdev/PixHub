-- ============================================================
-- V7__create_webhook_config_table.sql — Tabela de configuração de webhooks
-- ============================================================
-- Webhooks notificam clientes quando eventos acontecem
-- (ex: transação completada). Evita polling.
--
-- O secret é usado para assinar o payload com HMAC,
-- garantindo que a notificação é legítima.
-- ============================================================

CREATE TABLE webhook_configs (
    id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id      UUID            NOT NULL,
    url             VARCHAR(500)    NOT NULL,
    events          VARCHAR(500)    NOT NULL,
    secret          VARCHAR(64)     NOT NULL,
    status          VARCHAR(10)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_webhook_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT chk_webhook_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_webhook_account_id ON webhook_configs (account_id);
CREATE INDEX idx_webhook_status ON webhook_configs (status);

COMMENT ON TABLE webhook_configs IS 'Configurações de webhook para notificação assíncrona';
COMMENT ON COLUMN webhook_configs.secret IS 'Chave para assinatura HMAC — verifica autenticidade da notificação';
COMMENT ON COLUMN webhook_configs.events IS 'Eventos separados por vírgula: TRANSACTION_COMPLETED, KEY_REGISTERED, etc';
