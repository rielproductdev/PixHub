-- ============================================================
-- V5__create_qrcode_table.sql — Tabela de QR Codes PIX
-- ============================================================
-- QR Codes seguem o padrão BRCode do BACEN (EMV QRCPS).
-- STATIC: sem valor definido, uso ilimitado.
-- DYNAMIC: com valor e prazo, uso único.
-- ============================================================

CREATE TABLE qr_codes (
    id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    type            VARCHAR(10)     NOT NULL,
    pix_key         VARCHAR(77)     NOT NULL,
    merchant_name   VARCHAR(25)     NOT NULL,
    merchant_city   VARCHAR(15)     NOT NULL,
    amount          DECIMAL(15, 2),
    tx_id           VARCHAR(25),
    description     VARCHAR(140),
    expires_at      TIMESTAMP,
    payload         TEXT,
    status          VARCHAR(10)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_qrcode_type CHECK (type IN ('STATIC', 'DYNAMIC')),
    CONSTRAINT chk_qrcode_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'USED'))
);

CREATE INDEX idx_qrcode_pix_key ON qr_codes (pix_key);
CREATE INDEX idx_qrcode_tx_id ON qr_codes (tx_id);
CREATE INDEX idx_qrcode_status ON qr_codes (status);

COMMENT ON TABLE qr_codes IS 'QR Codes PIX — padrão BRCode do BACEN';
COMMENT ON COLUMN qr_codes.payload IS 'String EMV codificada no QR Code. O app do banco decodifica ao escanear';
COMMENT ON COLUMN qr_codes.tx_id IS 'Transaction ID — obrigatório para QR DYNAMIC';
