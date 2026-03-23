package com.riel.pixhub.controller;

import com.riel.pixhub.dto.auth.*;
import com.riel.pixhub.service.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para autenticação (register, login, refresh).
 *
 * Estes endpoints são PÚBLICOS — não requerem JWT para acessar.
 * Faz sentido: o usuário precisa fazer login ANTES de ter um token.
 * A configuração está no SecurityConfig (permitAll para /api/v1/auth/**).
 *
 * Fluxo completo de autenticação:
 * 1. POST /register → cria usuário, retorna tokens
 * 2. POST /login → valida credenciais, retorna tokens
 * 3. (... usa access token por 15 min ...)
 * 4. Access token expira → frontend recebe 401
 * 5. POST /refresh → envia refresh token, recebe novos tokens
 * 6. (... ciclo continua até refresh token expirar em 7 dias ...)
 * 7. Refresh token expirou → usuário precisa fazer login novamente
 */
@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/v1/auth/register — Registrar novo usuário.
     *
     * Retorna 201 Created + tokens JWT (o usuário já fica logado).
     *
     * Exemplo de request:
     * {
     *   "fullName": "Riel Santos",
     *   "email": "riel@email.com",
     *   "password": "minhasenha123"
     * }
     *
     * Exemplo de response (201):
     * {
     *   "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
     *   "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
     *   "tokenType": "Bearer",
     *   "expiresIn": 900
     * }
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/v1/auth/register — Registrando: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/v1/auth/login — Fazer login.
     *
     * Retorna 200 OK + tokens JWT se as credenciais forem válidas.
     * Retorna 401 se email/senha estiverem errados.
     * Retorna 423 se a conta estiver bloqueada por brute force.
     *
     * Exemplo de request:
     * {
     *   "email": "riel@email.com",
     *   "password": "minhasenha123"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        log.info("POST /api/v1/auth/login — Login: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/auth/refresh — Renovar tokens.
     *
     * O frontend chama este endpoint quando recebe 401 (access token expirado).
     * Envia o refresh token e recebe um novo par (access + refresh).
     *
     * Exemplo de request:
     * {
     *   "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
     * }
     *
     * Retorna 200 OK + novos tokens se refresh token for válido.
     * Retorna 401 se refresh token for inválido ou expirado.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("POST /api/v1/auth/refresh — Renovando tokens");
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }
}
