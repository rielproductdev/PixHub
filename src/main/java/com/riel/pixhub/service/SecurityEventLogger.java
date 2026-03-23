package com.riel.pixhub.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Logger dedicado para eventos de segurança.
 *
 * Centraliza o registro de eventos sensíveis de segurança com informações
 * de contexto completas: IP do cliente, User-Agent, e timestamp automático.
 *
 * Por que um logger dedicado em vez de log.warn() espalhado pelo código?
 * 1. PADRONIZAÇÃO: todos os eventos de segurança seguem o mesmo formato
 * 2. CONTEXTO: IP e User-Agent são adicionados automaticamente
 * 3. AUDITORIA: facilita buscar eventos no Kibana/Grafana por prefixo "[SECURITY]"
 * 4. DETECÇÃO: equipe de segurança filtra logs por "[SECURITY]" para detectar ataques
 *
 * Formato dos logs:
 *   [SECURITY] EVENTO — detalhes | IP: 192.168.1.100 | UA: Mozilla/5.0...
 *
 * O prefixo [SECURITY] permite filtrar APENAS eventos de segurança:
 *   grep "[SECURITY]" application.log
 *   Em Kibana: message: "[SECURITY]"
 *
 * Tipos de eventos registrados:
 * - LOGIN_SUCCESS: login bem-sucedido (rastrear acessos legítimos)
 * - LOGIN_FAILED: tentativa de login falha (detectar brute force)
 * - ACCOUNT_LOCKED: conta bloqueada por excesso de tentativas
 * - ACCOUNT_UNLOCKED: desbloqueio automático por expiração
 * - REGISTRATION: novo usuário registrado
 * - TOKEN_REFRESH: renovação de tokens
 * - TOKEN_REFRESH_FAILED: tentativa de refresh com token inválido
 *
 * @Component: Spring gerencia como bean — pode ser injetado em qualquer Service.
 */
@Component
@Slf4j
public class SecurityEventLogger {

    /**
     * Registra um login bem-sucedido.
     * Útil para detectar acessos legítimos e montar perfil de uso.
     */
    public void logLoginSuccess(String email) {
        log.info("[SECURITY] LOGIN_SUCCESS — Usuário autenticado: {} | {}",
                email, getRequestContext());
    }

    /**
     * Registra uma tentativa de login com credenciais inválidas.
     * O campo 'reason' nunca deve conter detalhes específicos
     * (ex: "senha errada" ou "email não existe") — apenas "credenciais inválidas".
     */
    public void logLoginFailed(String email, String reason) {
        log.warn("[SECURITY] LOGIN_FAILED — Email: {} | Motivo: {} | {}",
                email, reason, getRequestContext());
    }

    /**
     * Registra o bloqueio de uma conta por excesso de tentativas.
     * Evento de alta importância — pode indicar ataque de brute force.
     */
    public void logAccountLocked(String email, int attempts) {
        log.warn("[SECURITY] ACCOUNT_LOCKED — Usuário bloqueado: {} | Tentativas: {} | {}",
                email, attempts, getRequestContext());
    }

    /**
     * Registra o desbloqueio automático de uma conta.
     */
    public void logAccountUnlocked(String email) {
        log.info("[SECURITY] ACCOUNT_UNLOCKED — Desbloqueio automático: {} | {}",
                email, getRequestContext());
    }

    /**
     * Registra o registro de um novo usuário.
     */
    public void logRegistration(String email) {
        log.info("[SECURITY] REGISTRATION — Novo usuário registrado: {} | {}",
                email, getRequestContext());
    }

    /**
     * Registra a renovação bem-sucedida de tokens.
     */
    public void logTokenRefresh(String email) {
        log.info("[SECURITY] TOKEN_REFRESH — Tokens renovados para: {} | {}",
                email, getRequestContext());
    }

    /**
     * Registra uma tentativa falha de renovação de tokens.
     * Pode indicar tentativa de usar token roubado/expirado.
     */
    public void logTokenRefreshFailed(String reason) {
        log.warn("[SECURITY] TOKEN_REFRESH_FAILED — Motivo: {} | {}",
                reason, getRequestContext());
    }

    /**
     * Monta o contexto da request (IP e User-Agent).
     *
     * RequestContextHolder é um mecanismo do Spring que dá acesso
     * à request HTTP atual de QUALQUER camada (Service, Component, etc),
     * sem precisar receber o HttpServletRequest como parâmetro.
     *
     * Funciona porque o Spring armazena a request em um ThreadLocal
     * (o mesmo conceito do SecurityContextHolder).
     *
     * @return String no formato "IP: 192.168.1.100 | UA: Mozilla/5.0..."
     */
    private String getRequestContext() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return "IP: desconhecido | UA: desconhecido";
            }

            HttpServletRequest request = attrs.getRequest();
            String ip = getClientIp(request);
            String userAgent = request.getHeader("User-Agent");

            return String.format("IP: %s | UA: %s",
                    ip != null ? ip : "desconhecido",
                    userAgent != null ? truncate(userAgent, 100) : "desconhecido");
        } catch (Exception e) {
            return "IP: desconhecido | UA: desconhecido";
        }
    }

    /**
     * Extrai o IP real do cliente (mesma lógica do RateLimitingFilter).
     * Verifica X-Forwarded-For para suportar proxy reverso.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Trunca strings longas para evitar logs gigantes.
     * User-Agent pode ter 200+ caracteres — cortamos em 100.
     */
    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
