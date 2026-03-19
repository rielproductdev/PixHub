package com.riel.pixhub.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção lançada quando um recurso não é encontrado no banco de dados.
 *
 * Retorna HTTP 404 (Not Found).
 *
 * Quando usar:
 * - Buscar conta por ID e não existir
 * - Buscar chave PIX por valor e não existir
 * - Buscar transação por endToEndId e não existir
 *
 * Exemplo no Service:
 *   Account account = accountRepository.findById(id)
 *       .orElseThrow(() -> new ResourceNotFoundException("Conta", id));
 *
 * Resposta para o cliente:
 * {
 *   "status": 404,
 *   "error": "Not Found",
 *   "errorCode": "RESOURCE_NOT_FOUND",
 *   "message": "Conta não encontrada com ID: uuid-123"
 * }
 */
public class ResourceNotFoundException extends BusinessException {

    /**
     * Construtor com mensagem livre.
     * Ex: new ResourceNotFoundException("Chave PIX não encontrada")
     */
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    /**
     * Construtor por recurso + identificador.
     * Gera mensagem padronizada automaticamente.
     * Ex: new ResourceNotFoundException("Conta", id)
     *     → "Conta não encontrada com ID: uuid-123"
     */
    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(
            resourceName + " não encontrada com ID: " + identifier,
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND"
        );
    }
}
