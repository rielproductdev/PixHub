package com.riel.pixhub.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Validador que implementa a lógica da annotation @NoHtml.
 *
 * Detecta tags HTML em campos de texto para prevenir ataques XSS.
 * Funciona rejeitando valores que contenham:
 * - Tags HTML abertas: <script>, <img>, <div>, <a href=...>, etc.
 * - Tags HTML fechadas: </script>, </div>, etc.
 * - Event handlers inline: onerror=, onclick=, onload=, etc.
 * - Entidades HTML codificadas: &lt;script&gt; (tentativa de bypass)
 *
 * A estratégia é REJEITAR (não sanitizar). Em uma API REST que lida
 * com dados bancários, não faz sentido aceitar HTML em nenhum campo.
 * Se o campo se chama "holderName", o valor deve ser um nome — não HTML.
 *
 * Por que não sanitizar (remover tags) em vez de rejeitar?
 * 1. Sanitizar pode alterar dados legítimos (ex: nomes com < ou >)
 * 2. Sanitizar dá falsa sensação de segurança (bypass é mais fácil)
 * 3. Rejeitar é mais seguro: "se tem HTML, não é um nome válido"
 * 4. O erro 400 deixa claro para o frontend que o input é inválido
 *
 * ConstraintValidator<NoHtml, String>:
 * - NoHtml: a annotation que este validador implementa
 * - String: o tipo de dado que valida (campos String)
 */
public class NoHtmlValidator implements ConstraintValidator<NoHtml, String> {

    /**
     * Regex que detecta padrões de HTML/XSS.
     *
     * Detecta:
     * - <tag...>       → tags HTML abertas (com ou sem atributos)
     * - </tag>         → tags HTML fechadas
     * - on\w+=         → event handlers (onclick=, onerror=, onload=)
     * - javascript:    → URLs com protocolo javascript:
     * - &lt; &gt; &amp; → entidades HTML codificadas (tentativa de bypass)
     *
     * CASE_INSENSITIVE: <SCRIPT>, <Script>, <sCrIpT> — todos são capturados.
     *
     * NOTA: esta regex cobre os vetores de ataque mais comuns.
     * Para proteção mais completa, considerar bibliotecas como
     * OWASP Java HTML Sanitizer ou Jsoup.
     */
    private static final Pattern HTML_PATTERN = Pattern.compile(
            "<[^>]+>"                               // Tags HTML: <script>, <img src=...>, </div>
            + "|\\bon\\w+\\s*="                     // Event handlers: onclick=, onerror=, onload=
            + "|javascript\\s*:"                    // URLs javascript: href="javascript:alert(1)"
            + "|&(lt|gt|amp|quot|#\\d+|#x[0-9a-f]+);",  // Entidades HTML codificadas
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Inicialização do validador (chamada uma vez quando o Spring cria a instância).
     * Não precisamos de configuração — mas o método é obrigatório.
     */
    @Override
    public void initialize(NoHtml constraintAnnotation) {
        // Sem configuração adicional necessária
    }

    /**
     * Método de validação — chamado pelo Bean Validation para CADA campo anotado.
     *
     * @param value   o valor do campo (ex: "Riel Santos" ou "<script>alert('xss')</script>")
     * @param context contexto da validação (permite customizar mensagem de erro)
     * @return true se o valor é VÁLIDO (sem HTML), false se contém HTML (inválido)
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Campos null ou vazios são responsabilidade de @NotBlank/@NotNull.
        // @NoHtml só se preocupa com conteúdo HTML.
        if (value == null || value.isEmpty()) {
            return true;
        }

        // Se o regex encontrar qualquer padrão HTML → inválido (false)
        // Se não encontrar nada → válido (true)
        return !HTML_PATTERN.matcher(value).find();
    }
}
