package com.riel.pixhub.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propriedades configuráveis de rate limiting, lidas do application.yml.
 *
 * Rate limiting é o controle de "quantas requisições por minuto" cada
 * usuário/IP pode fazer. Sem isso, um atacante poderia:
 * - Fazer brute force no login (tentar milhares de senhas/minuto)
 * - Sobrecarregar a API com requisições (DoS — Denial of Service)
 * - Abusar de endpoints caros (transações, consultas pesadas)
 *
 * Cada tipo de endpoint tem um limite diferente porque o RISCO é diferente:
 * - Auth (login): limite APERTADO (5/min) — brute force é o maior risco
 * - Transações: limite MODERADO (30/min) — operações com dinheiro, custo alto
 * - Consultas: limite RELAXADO (60/min) — leitura, baixo risco
 *
 * Os valores são configuráveis via application.yml para que possamos ajustar
 * sem recompilar o código (ex: aumentar limites em período de alto volume).
 *
 * @ConfigurationProperties("rate-limit"): lê automaticamente as chaves
 * que começam com "rate-limit" no application.yml.
 * Ex: rate-limit.auth-requests-per-minute: 5
 *     → this.authRequestsPerMinute = 5
 */
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
@Getter
@Setter
public class RateLimitProperties {

    /**
     * Requisições por minuto para endpoints de autenticação (/api/v1/auth/**).
     * Limite POR IP (porque o usuário ainda não está autenticado).
     * Valor padrão: 5 — suficiente para login normal, impede brute force.
     */
    private int authRequestsPerMinute = 5;

    /**
     * Requisições por minuto para endpoints de transação (POST /api/v1/transactions/**).
     * Limite POR USUÁRIO (identificado pelo email no JWT).
     * Valor padrão: 30 — suficiente para uso normal de um sistema PIX.
     */
    private int transactionRequestsPerMinute = 30;

    /**
     * Requisições por minuto para endpoints de consulta (GET em qualquer /api/v1/**).
     * Limite POR USUÁRIO (identificado pelo email no JWT).
     * Valor padrão: 60 — mais relaxado porque leitura é barata.
     */
    private int queryRequestsPerMinute = 60;

    /**
     * Se o rate limiting está habilitado.
     * Útil para desabilitar em testes ou durante desenvolvimento.
     */
    private boolean enabled = true;
}
