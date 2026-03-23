package com.riel.pixhub.service;

import com.riel.pixhub.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Serviço responsável por criar e validar tokens JWT.
 *
 * JWT (JSON Web Token) funciona em 3 partes, separadas por pontos:
 *   header.payload.signature
 *
 * Exemplo real:
 *   eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGVtYWlsLmNvbSJ9.assinatura
 *
 * - Header: {"alg": "HS256"} → algoritmo de assinatura
 * - Payload: {"sub": "user@email.com", "role": "USER", "exp": 1234567890}
 *   → dados do usuário (chamados "claims")
 * - Signature: HMAC-SHA256(header + payload, secret)
 *   → garante que ninguém alterou o token
 *
 * O fluxo é:
 * 1. Login → servidor gera token com os dados do usuário → envia para o cliente
 * 2. Próximas requests → cliente envia token no header Authorization
 * 3. Servidor valida a assinatura (sem consultar banco!) → permite ou bloqueia
 *
 * Por que "sem consultar banco"?
 * A assinatura HMAC garante que o token não foi alterado. Se o token foi
 * assinado com a secret do servidor, e a assinatura bate, os dados são confiáveis.
 * Isso torna a autenticação muito rápida (sem query a cada request).
 *
 * Este serviço gera dois tipos de token:
 * - Access Token (15 min): usado para acessar endpoints protegidos
 * - Refresh Token (7 dias): usado apenas para renovar o access token
 */
@Service
@Slf4j
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    /**
     * Construtor — inicializa a chave de assinatura uma única vez.
     *
     * Keys.hmacShaKeyFor() converte a string da secret em uma chave
     * criptográfica adequada para HMAC-SHA256. A chave é criada uma vez
     * e reutilizada em toda geração/validação de token.
     *
     * Por que não criar a chave a cada chamada?
     * 1. Performance — criar chave criptográfica é custoso
     * 2. Consistência — mesma chave para assinar e validar
     */
    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Gera um access token JWT para o usuário.
     *
     * Claims incluídos no token:
     * - sub (subject): email do usuário (identificador principal)
     * - userId: UUID do usuário (para queries no banco)
     * - role: papel do usuário (USER/ADMIN — para autorização)
     * - iat (issued at): quando o token foi criado
     * - exp (expiration): quando expira (15 min após criação)
     *
     * @param userId UUID do usuário
     * @param email  email do usuário (será o "subject" do token)
     * @param role   papel do usuário (USER ou ADMIN)
     * @return string JWT assinada
     */
    public String generateAccessToken(UUID userId, String email, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                // subject: identificador principal do token (quem é o dono)
                .subject(email)
                // claims customizados: dados extras que queremos no token
                .claim("userId", userId.toString())
                .claim("role", role)
                // tipo do token: diferencia access de refresh na validação
                .claim("type", "access")
                // timestamps
                .issuedAt(now)
                .expiration(expiration)
                // assinatura: HMAC-SHA256 com nossa secret key
                .signWith(signingKey)
                // compacta as 3 partes (header.payload.signature) em uma string
                .compact();
    }

    /**
     * Gera um refresh token JWT.
     *
     * O refresh token é mais simples que o access token:
     * - Contém apenas subject (email) e userId
     * - NÃO contém role (não é usado para autorização)
     * - Tem expiração mais longa (7 dias)
     * - Tipo "refresh" para diferenciar do access token
     *
     * Por que dois tokens em vez de um?
     * Se usássemos um token só com validade de 7 dias, um token roubado
     * daria acesso por 7 dias inteiros. Com dois tokens:
     * - Access token vaza? Atacante tem só 15 minutos
     * - Refresh token vaza? Implementamos rotação (novo refresh a cada uso)
     *
     * @param userId UUID do usuário
     * @param email  email do usuário
     * @return string JWT assinada (refresh token)
     */
    public String generateRefreshToken(UUID userId, String email) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId.toString())
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Valida um token JWT e retorna os claims (dados) se for válido.
     *
     * A validação verifica:
     * 1. A assinatura bate? (ninguém alterou o token)
     * 2. O token expirou? (exp < agora)
     * 3. O formato está correto? (3 partes separadas por pontos)
     *
     * Se qualquer verificação falhar, retorna null (token inválido).
     * O JwtAuthenticationFilter usa esse retorno para decidir se
     * permite ou bloqueia a request.
     *
     * @param token string JWT a validar
     * @return Claims com os dados do token, ou null se inválido
     */
    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    // define a chave para verificar a assinatura
                    .verifyWith(signingKey)
                    .build()
                    // faz o parse e valida (assinatura + expiração)
                    .parseSignedClaims(token)
                    // retorna o payload (claims) se tudo estiver ok
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            // Token expirado — situação normal (access token dura só 15 min)
            // O cliente deve usar o refresh token para obter um novo
            log.debug("Token expirado para: {}", ex.getClaims().getSubject());
            return null;
        } catch (JwtException ex) {
            // Qualquer outro problema: assinatura inválida, formato errado,
            // token adulterado, etc. Pode ser tentativa de ataque.
            log.warn("Token JWT inválido: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Extrai o email (subject) de um token JWT.
     *
     * Atalho para não precisar fazer validateToken().getSubject() toda vez.
     * Retorna null se o token for inválido.
     *
     * @param token string JWT
     * @return email do usuário ou null se token inválido
     */
    public String getEmailFromToken(String token) {
        Claims claims = validateToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * Extrai o tipo do token (access ou refresh).
     *
     * Usado para garantir que um refresh token não seja usado como access
     * (e vice-versa). Sem essa verificação, alguém poderia usar um refresh
     * token para acessar endpoints protegidos — anulando a proteção dos
     * dois tokens separados.
     *
     * @param token string JWT
     * @return "access" ou "refresh", ou null se token inválido
     */
    public String getTokenType(String token) {
        Claims claims = validateToken(token);
        return claims != null ? claims.get("type", String.class) : null;
    }
}
