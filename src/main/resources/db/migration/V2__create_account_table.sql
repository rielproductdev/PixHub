-- ============================================================
-- V2__create_account_table.sql — Tabela de contas bancárias
-- ============================================================
-- Armazena as contas que participam do sistema PIX.
-- Cada conta pertence a um titular (CPF ou CNPJ) e pode ter
-- múltiplas chaves PIX vinculadas.
--
-- Decisões:
-- - id UUID: seguro, sem conflito distribuído (ver BaseEntity)
-- - balance DECIMAL(15,2): BigDecimal no Java, nunca Double
-- - holder_document sem UNIQUE: uma pessoa pode ter várias contas
-- - Índices em holder_document e (bank_code, branch, account_number)
-- ============================================================

CREATE TABLE accounts (
    id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    holder_name     VARCHAR(120)    NOT NULL,
    holder_document VARCHAR(14)     NOT NULL,
    bank_code       VARCHAR(8)      NOT NULL,
    branch          VARCHAR(4)      NOT NULL,
    account_number  VARCHAR(10)     NOT NULL,
    account_type    VARCHAR(10)     NOT NULL,
    balance         DECIMAL(15, 2)  NOT NULL DEFAULT 0.00,
    status          VARCHAR(10)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    -- Constraint para garantir que account_type só aceita valores válidos
    CONSTRAINT chk_account_type CHECK (account_type IN ('CHECKING', 'SAVINGS')),
    -- Constraint para garantir que status só aceita valores válidos
    CONSTRAINT chk_account_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED'))
);

-- Índice para busca por documento do titular (consultas frequentes por CPF/CNPJ)
CREATE INDEX idx_account_holder_document ON accounts (holder_document);

-- Índice composto: buscar conta por banco + agência + número
CREATE INDEX idx_account_bank_branch_number ON accounts (bank_code, branch, account_number);

COMMENT ON TABLE accounts IS 'Contas bancárias participantes do sistema PIX';
COMMENT ON COLUMN accounts.holder_document IS 'CPF (11 dígitos) ou CNPJ (14 dígitos) sem formatação';
COMMENT ON COLUMN accounts.bank_code IS 'ISPB — Identificador do Sistema de Pagamentos Brasileiro';
COMMENT ON COLUMN accounts.balance IS 'Saldo em reais. DECIMAL para evitar erros de ponto flutuante';
