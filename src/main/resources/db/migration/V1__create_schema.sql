-- ============================================================
-- V1__create_schema.sql — Migration inicial (baseline)
-- ============================================================
-- Flyway executa este arquivo automaticamente na inicialização.
-- Convenção de nomes: V{numero}__{descricao}.sql (dois underscores!)
--   V1 = versão 1 (primeira migration)
--   __create_schema = descrição legível
--
-- REGRA DE OURO: migrations já executadas NUNCA são alteradas.
-- Para modificar o schema, crie uma nova migration (V2, V3, etc).
-- Flyway rastreia quais migrations já rodaram na tabela flyway_schema_history.
-- ============================================================

-- Schema baseline — será populado nas próximas fases.
-- Esta migration existe para que o Flyway inicialize sua tabela de controle
-- e valide que a conexão com o banco está funcionando.

-- Extensão UUID do PostgreSQL: gera UUIDs v4 diretamente no banco.
-- UUIDs são melhores que IDs sequenciais para:
-- 1. Segurança: não expõe quantidade de registros
-- 2. Distribuição: pode ser gerado sem coordenação central
-- 3. Merge: sem conflito ao unir dados de múltiplas instâncias
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
