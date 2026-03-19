package com.riel.pixhub.enums;

/**
 * Os 5 tipos de chave PIX definidos pelo Banco Central do Brasil.
 *
 * Cada tipo tem regras de validação diferentes:
 * - CPF: 11 dígitos, validação de dígitos verificadores
 * - CNPJ: 14 dígitos, validação de dígitos verificadores
 * - EMAIL: formato válido de e-mail
 * - PHONE: formato +5511999999999 (com DDI e DDD)
 * - RANDOM: UUID gerado pelo sistema (chave aleatória)
 *
 * Essas validações serão implementadas na Fase 03 (Service layer).
 * O enum aqui apenas define os tipos possíveis — type safety.
 */
public enum PixKeyType {
    CPF,
    CNPJ,
    EMAIL,
    PHONE,
    RANDOM
}
