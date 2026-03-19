package com.riel.pixhub.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção lançada quando uma requisição com idempotencyKey duplicada é detectada.
 *
 * Retorna HTTP 200 (OK) — NÃO é um erro!
 *
 * Por que 200 e não 409 (Conflict)?
 * Idempotência é um COMPORTAMENTO ESPERADO, não um erro.
 * Quando o cliente manda a mesma idempotencyKey duas vezes,
 * o sistema retorna a transação existente como se fosse sucesso.
 * O cliente não precisa saber que houve duplicidade — para ele,
 * a operação "funcionou" (retornou o resultado esperado).
 *
 * Fluxo:
 * 1. Cliente manda POST /transactions com idempotencyKey "abc-123"
 * 2. Transação é criada → 201 Created
 * 3. Cliente manda novamente (clique duplo, retry, timeout)
 * 4. Sistema detecta "abc-123" já existe
 * 5. Retorna a transação existente → 200 OK
 * 6. Cliente NÃO é cobrado duas vezes
 *
 * O Service captura esta exceção de forma especial:
 * em vez de retornar ErrorResponse, retorna a transação existente.
 *
 * Exemplo no Service (Fase 06):
 *   Optional<Transaction> existing = transactionRepository
 *       .findByIdempotencyKey(request.getIdempotencyKey());
 *   if (existing.isPresent()) {
 *       throw new IdempotencyConflictException(existing.get().getId());
 *   }
 */
public class IdempotencyConflictException extends BusinessException {

    /**
     * ID da transação existente — para que o handler possa retorná-la.
     */
    private final java.util.UUID existingTransactionId;

    public IdempotencyConflictException(java.util.UUID existingTransactionId) {
        super(
            "Transação já processada com esta idempotencyKey. ID: " + existingTransactionId,
            HttpStatus.OK,
            "IDEMPOTENCY_CONFLICT"
        );
        this.existingTransactionId = existingTransactionId;
    }

    public java.util.UUID getExistingTransactionId() {
        return existingTransactionId;
    }
}
