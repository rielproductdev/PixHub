package com.riel.pixhub.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import lombok.extern.slf4j.Slf4j;

/**
 * Tratamento global de exceções da API.
 *
 * @RestControllerAdvice: Intercepta exceções lançadas por QUALQUER controller.
 * Em vez de cada controller ter seu próprio try/catch, centralizamos aqui.
 * Isso garante respostas de erro consistentes em toda a API.
 *
 * @Slf4j: Annotation do Lombok que cria automaticamente:
 * private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
 * Usado para registrar erros no log da aplicação.
 *
 * Padrão de resposta de erro:
 * {
 *   "timestamp": "2024-01-15T10:30:00",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Descrição do erro",
 *   "path": "/api/v1/pix/keys"
 * }
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Trata erros de validação (@Valid falhou).
     *
     * Quando um DTO chega no controller com campos inválidos
     * (ex: CPF com menos de 11 dígitos), o Spring lança
     * MethodArgumentNotValidException. Este handler captura
     * e retorna uma resposta 400 com os campos que falharam.
     *
     * Exemplo de resposta:
     * {
     *   "timestamp": "...",
     *   "status": 400,
     *   "error": "Validation Failed",
     *   "message": "cpf: must not be blank, keyType: must not be null",
     *   "path": "/api/v1/pix/keys"
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex, WebRequest request) {

        // Coleta todos os erros de validação: campo → mensagem
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> body = buildErrorBody(
            HttpStatus.BAD_REQUEST,
            "Validation Failed",
            fieldErrors.toString(),
            request
        );

        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Trata qualquer exceção não prevista (fallback).
     *
     * Se uma exceção não foi capturada por nenhum handler específico,
     * cai aqui. Retorna 500 Internal Server Error.
     * IMPORTANTE: não expõe detalhes internos ao cliente (segurança).
     * O stack trace completo vai para o log, não para a resposta.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllUncaughtExceptions(
            Exception ex, WebRequest request) {

        // Loga o stack trace completo para debug (visível apenas no servidor)
        log.error("Erro não tratado: ", ex);

        Map<String, Object> body = buildErrorBody(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            "Ocorreu um erro interno. Tente novamente mais tarde.",
            request
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    /**
     * Método auxiliar que monta o corpo padronizado de resposta de erro.
     * Centraliza a estrutura para garantir consistência entre todos os handlers.
     */
    private Map<String, Object> buildErrorBody(
            HttpStatus status, String error, String message, WebRequest request) {

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return body;
    }

}
