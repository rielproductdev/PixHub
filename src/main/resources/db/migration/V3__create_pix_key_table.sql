-- ============================================================
-- V3__create_pix_key_table.sql — Tabela de chaves PIX
-- ============================================================
-- Cada chave PIX é um "apelido" para uma conta bancária.
-- A chave é ÚNICA no Brasil inteiro (constraint UNIQUE em key_value).
--
-- Soft delete: chaves não são deletadas, recebem deactivated_at.
-- O BACEN pode perguntar "essa chave existiu?" — precisamos do histórico.
-- ============================================================

CREATE TABLE pix_keys (
    id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    key_type        VARCHAR(10)     NOT NULL,
    key_value       VARCHAR(77)     NOT NULL,
    account_id      UUID            NOT NULL,
    status          VARCHAR(10)     NOT NULL DEFAULT 'ACTIVE',
    deactivated_at  TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    -- FK: cada chave pertence a uma conta
    CONSTRAINT fk_pix_key_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    -- UNIQUE: uma chave PIX é única no sistema inteiro
    CONSTRAINT uk_pix_key_value UNIQUE (key_value),
    -- Validação dos tipos de chave
    CONSTRAINT chk_pix_key_type CHECK (key_type IN ('CPF', 'CNPJ', 'EMAIL', 'PHONE', 'RANDOM')),
    CONSTRAINT chk_pix_key_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_pix_key_account_id ON pix_keys (account_id);
CREATE INDEX idx_pix_key_status ON pix_keys (status);

COMMENT ON TABLE pix_keys IS 'Chaves PIX — identificadores únicos para receber transferências';
COMMENT ON COLUMN pix_keys.key_value IS 'Valor da chave: CPF, e-mail, telefone ou UUID aleatório';
COMMENT ON COLUMN pix_keys.deactivated_at IS 'Soft delete — NULL = ativa, preenchido = desativada';
