package com.riel.pixhub.service;

import com.riel.pixhub.config.JwtProperties;
import com.riel.pixhub.dto.auth.*;
import com.riel.pixhub.entity.User;
import com.riel.pixhub.enums.UserStatus;
import com.riel.pixhub.exception.AccountLockedException;
import com.riel.pixhub.exception.AuthenticationFailedException;
import com.riel.pixhub.exception.ResourceAlreadyExistsException;
import com.riel.pixhub.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service que contém TODA a lógica de autenticação (register, login, refresh).
 *
 * Segue o mesmo padrão do AccountService: VALIDAR → EXECUTAR → AUDITAR → RETORNAR.
 *
 * Responsabilidades:
 * 1. Registro: criar usuário com senha criptografada (BCrypt)
 * 2. Login: validar credenciais, gerar tokens, controlar brute force
 * 3. Refresh: renovar tokens expirados (rotação de refresh token)
 *
 * Segurança implementada:
 * - Senhas NUNCA armazenadas em texto puro (BCrypt hash)
 * - Mensagens genéricas para não revelar se email existe
 * - Bloqueio temporário após 5 tentativas falhas (brute force protection)
 * - Desbloqueio automático após 30 minutos
 * - Validação de tipo de token (access vs refresh)
 */
@Service
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final SecurityEventLogger securityEventLogger;

    /**
     * Máximo de tentativas de login antes de bloquear.
     * 5 é o padrão da indústria (OWASP recomenda entre 3 e 5).
     */
    private static final int MAX_FAILED_ATTEMPTS = 5;

    /**
     * Duração do bloqueio em minutos.
     * 30 minutos é suficiente para desincentivar brute force
     * sem prejudicar demais o usuário legítimo que esqueceu a senha.
     */
    private static final int LOCK_DURATION_MINUTES = 30;

    public AuthService(UserRepository userRepository,
                       JwtService jwtService,
                       JwtProperties jwtProperties,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService,
                       SecurityEventLogger securityEventLogger) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.securityEventLogger = securityEventLogger;
    }

    /**
     * Registra um novo usuário no sistema.
     *
     * Fluxo:
     * 1. Verifica se o email já está em uso (409 Conflict se já existe)
     * 2. Cria o User com senha criptografada via BCrypt
     * 3. Salva no banco
     * 4. Gera par de tokens (access + refresh)
     * 5. Registra no audit log
     * 6. Retorna tokens para o frontend
     *
     * O usuário já fica logado após o registro (recebe tokens imediatamente).
     * Isso melhora a UX — não precisa fazer login separado após registrar.
     *
     * @param request DTO com fullName, email, password
     * @return AuthResponse com tokens JWT
     * @throws ResourceAlreadyExistsException se email já estiver em uso
     */
    public AuthResponse register(RegisterRequest request) {
        log.info("Registrando novo usuário: {}", request.getEmail());

        // VALIDAR — email já em uso?
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException(
                    "Já existe um usuário com o email: " + request.getEmail());
        }

        // EXECUTAR — criar usuário com senha criptografada
        // passwordEncoder.encode() aplica BCrypt: "senha123" → "$2a$10$xK..."
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        // role e status já têm @Builder.Default (USER e ACTIVE)

        User saved = userRepository.save(user);

        // AUDITAR
        auditService.logCreate("User", saved.getId().toString(), saved.getEmail(), saved);

        log.info("Usuário registrado com sucesso. ID: {}", saved.getId());
        securityEventLogger.logRegistration(saved.getEmail());

        // RETORNAR — gerar tokens e devolver
        return generateTokenResponse(saved);
    }

    /**
     * Autentica um usuário e retorna tokens JWT.
     *
     * Fluxo:
     * 1. Busca usuário pelo email
     * 2. Verifica se está bloqueado (pode ter expirado → desbloqueia)
     * 3. Compara a senha (BCrypt)
     * 4. Se errou: incrementa contador, bloqueia se atingiu limite
     * 5. Se acertou: reseta contador, gera tokens
     *
     * SEGURANÇA CRÍTICA:
     * - A mensagem de erro é SEMPRE "Credenciais inválidas" — nunca revela
     *   se o email existe ou se é a senha que está errada. Isso impede
     *   "user enumeration" (atacante descobrir emails válidos).
     * - O bloqueio é por conta (failedLoginAttempts no banco), não por IP.
     *   Assim protege contra ataques distribuídos (múltiplos IPs).
     *
     * @param request DTO com email e password
     * @return AuthResponse com tokens JWT
     * @throws AuthenticationFailedException se credenciais forem inválidas
     * @throws AccountLockedException se usuário estiver bloqueado
     */
    public AuthResponse login(LoginRequest request) {
        log.info("Tentativa de login: {}", request.getEmail());

        // Busca o usuário — se não existir, lança erro genérico
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    securityEventLogger.logLoginFailed(request.getEmail(), "email não encontrado");
                    return new AuthenticationFailedException("Credenciais inválidas");
                });

        // Verifica bloqueio (pode ter expirado → desbloqueia automaticamente)
        checkAndHandleLock(user);

        // Compara a senha digitada com o hash BCrypt armazenado
        // passwordEncoder.matches("senha123", "$2a$10$xK...") → true/false
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handleFailedLogin(user);
            // Mensagem genérica — NUNCA dizer "senha incorreta"
            throw new AuthenticationFailedException("Credenciais inválidas");
        }

        // Login OK — reseta contador de falhas
        resetFailedAttempts(user);

        log.info("Login realizado com sucesso. User ID: {}", user.getId());
        securityEventLogger.logLoginSuccess(user.getEmail());
        return generateTokenResponse(user);
    }

    /**
     * Renova os tokens JWT usando o refresh token.
     *
     * Fluxo:
     * 1. Valida o refresh token (assinatura + expiração)
     * 2. Verifica que é do tipo "refresh" (não "access")
     * 3. Busca o usuário pelo email no token
     * 4. Gera um NOVO par de tokens (rotação)
     *
     * Rotação de refresh token:
     * A cada uso, o refresh token é substituído por um novo.
     * O antigo continua tecnicamente válido (não fazemos blacklist),
     * mas o cliente deve descartar e usar o novo.
     * Em sistemas mais robustos, usaríamos Redis para invalidar o antigo.
     *
     * @param request DTO com o refresh token
     * @return AuthResponse com novos tokens
     * @throws AuthenticationFailedException se token for inválido
     */
    public AuthResponse refresh(RefreshTokenRequest request) {
        String token = request.getRefreshToken();

        // VALIDAR — token é válido e é do tipo refresh?
        String tokenType = jwtService.getTokenType(token);
        if (!"refresh".equals(tokenType)) {
            securityEventLogger.logTokenRefreshFailed("token inválido ou tipo errado");
            throw new AuthenticationFailedException("Token de renovação inválido ou expirado");
        }

        // Extrair email do token
        String email = jwtService.getEmailFromToken(token);

        // Buscar usuário atualizado no banco (pode ter sido bloqueado desde o último token)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationFailedException(
                        "Token de renovação inválido ou expirado"));

        // Verificar se o usuário ainda está ativo
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AccountLockedException(
                    "Conta bloqueada. Tente novamente após " + user.getLockedUntil());
        }

        securityEventLogger.logTokenRefresh(email);
        return generateTokenResponse(user);
    }

    // ==========================================
    // MÉTODOS AUXILIARES (private)
    // ==========================================

    /**
     * Gera a resposta com access token + refresh token.
     *
     * Método reutilizado por register, login e refresh
     * para não repetir a mesma lógica de geração de tokens.
     */
    private AuthResponse generateTokenResponse(User user) {
        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(
                user.getId(), user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                // Converte milissegundos para segundos (padrão OAuth2)
                .expiresIn(jwtProperties.getAccessTokenExpiration() / 1000)
                .build();
    }

    /**
     * Verifica se o usuário está bloqueado e desbloqueia se o tempo expirou.
     *
     * Fluxo:
     * 1. Se status é ACTIVE → retorna (nada a fazer)
     * 2. Se status é LOCKED e lockedUntil já passou → desbloqueia automaticamente
     * 3. Se status é LOCKED e lockedUntil ainda não passou → lança exceção
     *
     * O desbloqueio automático é feito aqui (no momento do login)
     * em vez de um cron job porque:
     * - Não precisa de infraestrutura extra (scheduler, Redis, etc)
     * - Só executa quando alguém realmente tenta fazer login
     * - É mais simples e suficiente para o nosso caso
     */
    private void checkAndHandleLock(User user) {
        if (user.getStatus() != UserStatus.LOCKED) {
            return;
        }

        // Bloqueio expirou? Desbloqueia automaticamente
        if (user.getLockedUntil() != null && LocalDateTime.now().isAfter(user.getLockedUntil())) {
            securityEventLogger.logAccountUnlocked(user.getEmail());
            user.setStatus(UserStatus.ACTIVE);
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
            return;
        }

        // Ainda bloqueado — informa quando poderá tentar novamente
        throw new AccountLockedException(
                "Conta temporariamente bloqueada por excesso de tentativas. " +
                "Tente novamente após " + user.getLockedUntil());
    }

    /**
     * Trata uma tentativa de login falha — incrementa contador ou bloqueia.
     *
     * Fluxo:
     * 1. Incrementa failedLoginAttempts
     * 2. Se atingiu MAX_FAILED_ATTEMPTS:
     *    - Muda status para LOCKED
     *    - Define lockedUntil (agora + 30 minutos)
     *    - Registra no audit log (incidente de segurança)
     * 3. Salva no banco
     *
     * O contador persiste no banco (não em cache) para sobreviver a restarts.
     */
    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        securityEventLogger.logLoginFailed(user.getEmail(),
                "senha incorreta — tentativa " + attempts + "/" + MAX_FAILED_ATTEMPTS);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            // Bloqueia a conta temporariamente
            user.setStatus(UserStatus.LOCKED);
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));

            securityEventLogger.logAccountLocked(user.getEmail(), attempts);

            // Audita o bloqueio como incidente de segurança
            auditService.logStatusChange("User", user.getId().toString(),
                    user.getEmail(), "ACTIVE", "LOCKED");
        }

        userRepository.save(user);
    }

    /**
     * Reseta o contador de falhas após login bem-sucedido.
     *
     * Só salva no banco se havia falhas anteriores (evita query desnecessária).
     */
    private void resetFailedAttempts(User user) {
        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }
    }
}
