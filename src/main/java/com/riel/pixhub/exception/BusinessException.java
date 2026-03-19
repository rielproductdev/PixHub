package com.riel.pixhub.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exceção base para TODOS os erros de negócio da aplicação.
 *
 * Por que uma classe base?
 * Sem ela, o GlobalExceptionHandler precisaria de um @ExceptionHandler
 * para CADA exceção individual. Com ela, podemos ter um handler genérico
 * que captura BusinessException e trata TODAS as filhas de uma vez.
 *
 * Hierarquia:
 *   RuntimeException (Java)
 *     └── BusinessException (nossa base)
 *           ├── ResourceNotFoundException (404)
 *           ├── ResourceAlreadyExistsException (409)
 *           ├── BusinessRuleViolationException (422)
 *           ├── InsufficientBalanceException (422)
 *           ├── AccountBlockedException (403)
 *           └── IdempotencyConflictException (200 — retorna existente)
 *
 * Por que extends RuntimeException e não Exception?
 * Em Java existem dois tipos de exceção:
 * - Exception (checked): o compilador OBRIGA você a tratar com try/catch
 *   em todo lugar que chama. Isso polui o código com try/catch em cascata.
 * - RuntimeException (unchecked): o compilador NÃO obriga try/catch.
 *   O GlobalExceptionHandler captura centralizadamente.
 *
 * Em APIs REST, a convenção moderna é usar RuntimeException + handler global.
 * Isso mantém o código do Service limpo — ele só lança a exceção e pronto,
 * o handler cuida de transformar em resposta HTTP.
 *
 * Cada exceção filha define seu próprio httpStatus e errorCode:
 * - httpStatus: qual código HTTP retornar (404, 409, 422...)
 * - errorCode: código de erro para o frontend (ex: "RESOURCE_NOT_FOUND")
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * Código HTTP que o GlobalExceptionHandler vai retornar.
     * Ex: HttpStatus.NOT_FOUND (404), HttpStatus.CONFLICT (409)
     *
     * Cada exceção filha define o seu no construtor:
     *   super("mensagem", HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
     */
    private final HttpStatus httpStatus;

    /**
     * Código de erro legível para máquinas (frontend, apps mobile).
     * Ex: "RESOURCE_NOT_FOUND", "INSUFFICIENT_BALANCE"
     *
     * O frontend pode usar esse código para decidir o que mostrar:
     *   if (errorCode === "INSUFFICIENT_BALANCE") showBalanceWarning();
     *
     * É diferente da message: a message é para humanos lerem,
     * o errorCode é para o código do frontend interpretar.
     */
    private final String errorCode;

    /**
     * Construtor completo — usado pelas exceções filhas.
     *
     * @param message    Mensagem descritiva (para humanos)
     * @param httpStatus Código HTTP a retornar
     * @param errorCode  Código de erro (para máquinas/frontend)
     */
    public BusinessException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);          // Passa a mensagem para RuntimeException
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}
