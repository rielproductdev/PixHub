package com.riel.pixhub.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção lançada quando tentamos criar algo que já existe.
 *
 * Retorna HTTP 409 (Conflict).
 *
 * Quando usar:
 * - Cadastrar conta com CPF que já tem conta no mesmo banco
 * - Registrar chave PIX com valor que já está cadastrado
 * - Qualquer operação de criação que viola unicidade
 *
 * Exemplo no Service:
 *   if (pixKeyRepository.existsByKeyValue(keyValue)) {
 *       throw new ResourceAlreadyExistsException("Chave PIX", keyValue);
 *   }
 *
 * Resposta para o cliente:
 * {
 *   "status": 409,
 *   "error": "Conflict",
 *   "errorCode": "RESOURCE_ALREADY_EXISTS",
 *   "message": "Chave PIX já cadastrada com valor: fulano@email.com"
 * }
 *
 * UX: O frontend pode sugerir alternativa ao usuário:
 * "Este CPF já possui chave PIX. Deseja cadastrar um e-mail?"
 */
public class ResourceAlreadyExistsException extends BusinessException {

    public ResourceAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT, "RESOURCE_ALREADY_EXISTS");
    }

    /**
     * Construtor por recurso + valor duplicado.
     * Ex: new ResourceAlreadyExistsException("Chave PIX", "fulano@email.com")
     *     → "Chave PIX já cadastrada com valor: fulano@email.com"
     */
    public ResourceAlreadyExistsException(String resourceName, Object identifier) {
        super(
            resourceName + " já cadastrada com valor: " + identifier,
            HttpStatus.CONFLICT,
            "RESOURCE_ALREADY_EXISTS"
        );
    }
}
