-- ============================================================
-- V4__create_transaction_table.sql — Tabela de transações PIX
-- ============================================================
-- Entidade mais complexa: envolve débito, crédito, máquina de estados,
-- idempotência e identificação única no ecossistema PIX.
--
-- Índices em: end_to_end_id, idempotency_key, status, sender, receiver
-- Essas colunas são consultadas constantemente. Sem índice = full table scan.
-- ============================================================

CREATE TABLE transactions (
    id                  UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    end_to_end_id       VARCHAR(32)     NOT NULL,
    amount              DECIMAL(15, 2)  NOT NULL,
    description         VARCHAR(140),
    sender_account_id   UUID            NOT NULL,
    receiver_account_id UUID            NOT NULL,
    receiver_pix_key    VARCHAR(77),
    status              VARCHAR(12)     NOT NULL DEFAULT 'PENDING',
    idempotency_key     VARCHAR(36)     NOT NULL,
    processed_at        TIMESTAMP,
    failure_reason      VARCHAR(255),
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),

    -- FKs para as contas envolvidas
    CONSTRAINT fk_transaction_sender FOREIGN KEY (sender_account_id) REFERENCES accounts (id),
    CONSTRAINT fk_transaction_receiver FOREIGN KEY (receiver_account_id) REFERENCES accounts (id),
    -- Unicidade: endToEndId é único no ecossistema PIX inteiro
    CONSTRAINT uk_transaction_end_to_end_id UNIQUE (end_to_end_id),
    -- Unicidade: idempotency key evita cobranças duplicadas
    CONSTRAINT uk_transaction_idempotency_key UNIQUE (idempotency_key),
    -- Validação dos status
    CONSTRAINT chk_transaction_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED')),
    -- Valor deve ser positivo
    CONSTRAINT chk_transaction_amount CHECK (amount > 0)
);

CREATE INDEX idx_transaction_end_to_end_id ON transactions (end_to_end_id);
CREATE INDEX idx_transaction_idempotency_key ON transactions (idempotency_key);
CREATE INDEX idx_transaction_status ON transactions (status);
CREATE INDEX idx_transaction_sender ON transactions (sender_account_id);
CREATE INDEX idx_transaction_receiver ON transactions (receiver_account_id);

COMMENT ON TABLE transactions IS 'Transações PIX — transferências entre contas';
COMMENT ON COLUMN transactions.end_to_end_id IS 'ID único no ecossistema PIX. Formato BACEN: E + ISPB + data + sequencial';
COMMENT ON COLUMN transactions.idempotency_key IS 'Evita cobranças duplicadas. Mesma chave = mesma transação';
COMMENT ON COLUMN transactions.receiver_pix_key IS 'Chave usada na época (texto, não FK — chave pode ser desativada depois)';
