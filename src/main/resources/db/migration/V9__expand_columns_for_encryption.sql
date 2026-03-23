-- ============================================================
-- V9__expand_columns_for_encryption.sql — Preparar colunas para criptografia
-- ============================================================
-- Dados sensíveis (CPF/CNPJ, chaves PIX) serão criptografados com AES-256-GCM.
-- O dado criptografado é MAIOR que o texto original porque inclui:
--   - IV (Initialization Vector): 12 bytes
--   - Ciphertext: mesmo tamanho do texto original
--   - Authentication Tag: 16 bytes
--   - Base64 encoding: expande ~33%
--
-- Cálculo para holder_document (max 14 chars):
--   Base64(12 + 14 + 16) = Base64(42 bytes) = 56 caracteres
--
-- Cálculo para key_value (max 77 chars):
--   Base64(12 + 77 + 16) = Base64(105 bytes) = 140 caracteres
--
-- Usamos VARCHAR(255) para margem de segurança em ambos os casos.
-- ============================================================

-- Expandir holder_document de VARCHAR(14) para VARCHAR(255)
-- Dado original: CPF (11 chars) ou CNPJ (14 chars)
-- Dado criptografado: ~56 chars em Base64
ALTER TABLE accounts ALTER COLUMN holder_document TYPE VARCHAR(255);

-- Expandir key_value de VARCHAR(77) para VARCHAR(255)
-- Dado original: até 77 chars (e-mail é o maior tipo de chave PIX)
-- Dado criptografado: ~140 chars em Base64
ALTER TABLE pix_keys ALTER COLUMN key_value TYPE VARCHAR(255);

COMMENT ON COLUMN accounts.holder_document IS 'CPF/CNPJ criptografado com AES-256-GCM (Base64). Texto puro via JPA Converter';
COMMENT ON COLUMN pix_keys.key_value IS 'Valor da chave PIX criptografado com AES-256-GCM (Base64). Texto puro via JPA Converter';
