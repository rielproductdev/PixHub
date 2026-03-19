package com.riel.pixhub.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * DTO padrão para respostas de erro da API.
 *
 * Toda vez que algo dá errado (404, 409, 422, 500...), a API retorna
 * este objeto como JSON. Isso garante que TODAS as respostas de erro
 * tenham a mesma estrutura — o frontend pode confiar no formato.
 *
 * Por que uma classe em vez de Map?
 * - Map<String, Object> aceita qualquer chave (inclusive typos)
 * - ErrorResponse tem campos FIXOS — o compilador protege contra erros
 * - O Jackson serializa os getters automaticamente
 *
 * Exemplo de resposta:
 * {
 *   "timestamp": "2026-03-18T14:30:00",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Conta não encontrada com ID: uuid-123",
 *   "path": "/api/v1/accounts/uuid-123"
 * }
 *
 * @JsonInclude(NON_NULL): Se um campo for null, NÃO aparece no JSON.
 * Ex: se "field" for null, o JSON não terá "field": null.
 * Isso deixa a resposta mais limpa — só mostra o que é relevante.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * Quando o erro aconteceu.
     * Útil para correlacionar com logs do servidor.
     * Ex: "2026-03-18T14:30:00"
     */
    private LocalDateTime timestamp;

    /**
     * Código HTTP numérico (400, 404, 409, 422, 500...).
     * O frontend usa este campo para decidir o que mostrar:
     * 4xx = erro do usuário (mensagem clara)
     * 5xx = erro do sistema (mensagem genérica + suporte)
     */
    private int status;

    /**
     * Nome do erro HTTP (ex: "Not Found", "Conflict", "Unprocessable Entity").
     * Mais legível que o número para quem lê logs.
     */
    private String error;

    /**
     * Mensagem descritiva do erro em português.
     * Esta é a mensagem que o frontend pode mostrar ao usuário.
     * Ex: "Conta não encontrada", "CPF inválido", "Saldo insuficiente"
     */
    private String message;

    /**
     * Caminho da requisição que causou o erro.
     * Ex: "/api/v1/accounts/uuid-123"
     * Útil para debug — saber qual endpoint foi chamado.
     */
    private String path;
}
