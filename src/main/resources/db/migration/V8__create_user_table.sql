-- ============================================================
-- V8__create_user_table.sql — Tabela de usuários (autenticação)
-- ============================================================
-- Armazena credenciais e dados de autenticação, separados dos
-- dados bancários (tabela accounts). Essa separação é padrão
-- em sistemas financeiros: a "identidade" (quem você é) é
-- diferente da "conta" (onde está seu dinheiro).
--
-- Decisões:
-- - email como login (padrão fintech, naturalmente único)
-- - password VARCHAR(72): hash BCrypt tem ~60 chars
-- - account_id nullable: admin pode não ter conta bancária
-- - failed_login_attempts: proteção brute force persistente
-- - locked_until: bloqueio temporário automático
-- ============================================================

CREATE TABLE users (
    id                      UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    email                   VARCHAR(150)    NOT NULL,
    password                VARCHAR(72)     NOT NULL,
    full_name               VARCHAR(120)    NOT NULL,
    role                    VARCHAR(10)     NOT NULL DEFAULT 'USER',
    status                  VARCHAR(10)     NOT NULL DEFAULT 'ACTIVE',
    account_id              UUID            NULL,
    failed_login_attempts   INTEGER         NOT NULL DEFAULT 0,
    locked_until            TIMESTAMP       NULL,
    created_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP       NOT NULL DEFAULT NOW(),

    -- Constraint: email deve ser único (previne contas duplicadas)
    CONSTRAINT uq_user_email UNIQUE (email),
    -- Constraint: role só aceita valores válidos
    CONSTRAINT chk_user_role CHECK (role IN ('USER', 'ADMIN')),
    -- Constraint: status só aceita valores válidos
    CONSTRAINT chk_user_status CHECK (status IN ('ACTIVE', 'LOCKED')),
    -- FK opcional para a conta bancária vinculada
    CONSTRAINT fk_user_account FOREIGN KEY (account_id)
        REFERENCES accounts (id) ON DELETE SET NULL
);

-- Índice único no email — usado em toda query de autenticação (login, busca por email)
CREATE UNIQUE INDEX idx_user_email ON users (email);

-- Índice no status — útil para queries administrativas (listar users bloqueados)
CREATE INDEX idx_user_status ON users (status);

COMMENT ON TABLE users IS 'Usuários do sistema — credenciais e dados de autenticação';
COMMENT ON COLUMN users.email IS 'Identificador único para login';
COMMENT ON COLUMN users.password IS 'Hash BCrypt da senha — NUNCA armazenar texto puro';
COMMENT ON COLUMN users.account_id IS 'FK opcional — nem todo usuário tem conta bancária (ex: admin)';
COMMENT ON COLUMN users.failed_login_attempts IS 'Contador de senhas erradas consecutivas — proteção brute force';
COMMENT ON COLUMN users.locked_until IS 'Bloqueio temporário: null = não bloqueado, data futura = bloqueado até';
