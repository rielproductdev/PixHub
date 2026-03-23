package com.riel.pixhub.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Classe que mapeia as propriedades de JWT do application.yml.
 *
 * @ConfigurationProperties(prefix = "jwt"):
 * O Spring lê o application.yml e injeta os valores que começam com "jwt."
 * automaticamente nesta classe. Exemplo:
 *   jwt.secret → this.secret
 *   jwt.access-token-expiration → this.accessTokenExpiration
 *
 * Por que uma classe em vez de @Value em cada lugar?
 * 1. Centraliza todas as configs de JWT em um único lugar
 * 2. Se o nome de uma propriedade mudar, altera só aqui
 * 3. Facilita testes — pode criar JwtProperties com valores de teste
 * 4. O Spring valida que as propriedades existem ao iniciar (fail-fast)
 *
 * O Spring converte kebab-case (access-token-expiration) para
 * camelCase (accessTokenExpiration) automaticamente — isso se chama
 * "relaxed binding".
 */
@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    /**
     * Chave secreta para assinar tokens JWT com HMAC-SHA256.
     * Em produção, vem da variável de ambiente JWT_SECRET.
     * Mínimo 256 bits (32 caracteres) para HMAC-SHA256.
     */
    private String secret;

    /**
     * Tempo de vida do access token em milissegundos.
     * Padrão: 900000 (15 minutos).
     * Access tokens são curtos para limitar danos se vazarem.
     */
    private long accessTokenExpiration;

    /**
     * Tempo de vida do refresh token em milissegundos.
     * Padrão: 604800000 (7 dias).
     * Refresh tokens são longos para o usuário não precisar
     * fazer login toda hora.
     */
    private long refreshTokenExpiration;
}
