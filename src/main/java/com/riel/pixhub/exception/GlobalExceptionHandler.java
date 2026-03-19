package com.riel.pixhub.exception;

import com.riel.pixhub.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Tratamento global de exceções da API.
 *
 * @RestControllerAdvice: Intercepta exceções lançadas por QUALQUER controller.
 * Em vez de cada controller ter seu próprio try/catch, centralizamos aqui.
 * Isso garante respostas de erro consistentes em toda a API.
 *
 * Ordem dos handlers importa! O Spring procura o handler MAIS ESPECÍFICO primeiro:
 * 1. ResourceNotFoundException → 404 (mais específico)
 * 2. BusinessException → usa o httpStatus da exceção (genérico para negócio)
 * 3. MethodArgumentNotValidException → 400 (validação de DTO)
 * 4. Exception → 500 (fallback — tudo que não foi capturado acima)
 *
 * Atualização da Fase 03:
 * - Substituímos Map<String, Object> por ErrorResponse (classe tipada)
 * - Adicionamos handler para BusinessException (captura TODAS as exceções de negócio)
 * - Logs com nível adequado: warn para 4xx (erro do cliente), error para 5xx (bug)
 *
 * @Slf4j: Annotation do Lombok que cria automaticamente:
 * private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handler para TODAS as exceções de negócio (BusinessException e filhas).
     *
     * Como funciona:
     * - BusinessException é a classe mãe de ResourceNotFoundException,
     *   ResourceAlreadyExistsException, BusinessRuleViolationException, etc.
     * - Cada filha define seu próprio httpStatus e errorCode no construtor
     * - Este handler pega esses valores e monta a resposta automaticamente
     *
     * Isso significa que quando criamos uma nova exceção (ex: PixKeyLimitExceededException),
     * ela já funciona aqui sem precisar criar um novo handler — basta herdar BusinessException.
     *
     * Log level: WARN (não ERROR) porque 4xx é erro do CLIENTE, não bug do servidor.
     * O time de operações filtra logs por nível: ERROR = "acorde alguém", WARN = "normal".
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, WebRequest request) {

        log.warn("Erro de negócio: [{}] {}", ex.getErrorCode(), ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getHttpStatus().value())
                .error(ex.getHttpStatus().getReasonPhrase())
                .message(ex.getMessage())
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    /**
     * Trata erros de validação do Bean Validation (@Valid falhou).
     *
     * Quando um DTO chega no controller com campos inválidos
     * (ex: holderName em branco, document com formato errado),
     * o Spring lança MethodArgumentNotValidException ANTES de chamar o Service.
     *
     * Este handler coleta TODOS os campos que falharam e monta uma mensagem
     * legível. O frontend pode usar os nomes dos campos para destacá-los
     * em vermelho na tela.
     *
     * Exemplo de resposta:
     * {
     *   "status": 400,
     *   "error": "Bad Request",
     *   "message": "holderName: Nome é obrigatório; holderDocument: Documento é obrigatório"
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, WebRequest request) {

        // Coleta todos os erros de validação: campo → mensagem
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        // Monta mensagem legível: "campo1: erro1; campo2: erro2"
        String message = fieldErrors.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Erro de validação");

        log.warn("Erro de validação: {}", message);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(message)
                .path(extractPath(request))
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Fallback: trata qualquer exceção não prevista.
     *
     * Se uma exceção não foi capturada por nenhum handler acima,
     * cai aqui. Retorna 500 Internal Server Error.
     *
     * IMPORTANTE para segurança:
     * - NÃO expõe detalhes internos ao cliente (stack trace, SQL, etc)
     * - O stack trace completo vai para o LOG (só devs veem)
     * - O cliente recebe mensagem genérica
     *
     * Log level: ERROR (não WARN) porque se chegou aqui, é bug real.
     * O time de operações precisa investigar.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllUncaughtExceptions(
            Exception ex, WebRequest request) {

        // Log com stack trace completo — visível apenas no servidor
        log.error("Erro não tratado: ", ex);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("Ocorreu um erro interno. Tente novamente mais tarde.")
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Extrai o caminho da requisição do WebRequest.
     *
     * WebRequest.getDescription(false) retorna "uri=/api/v1/accounts/uuid-123"
     * Removemos o prefixo "uri=" para ficar limpo: "/api/v1/accounts/uuid-123"
     */
    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
