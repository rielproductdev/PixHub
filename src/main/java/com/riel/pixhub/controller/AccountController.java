package com.riel.pixhub.controller;

import com.riel.pixhub.dto.account.AccountResponse;
import com.riel.pixhub.dto.account.CreateAccountRequest;
import com.riel.pixhub.dto.account.UpdateAccountRequest;
import com.riel.pixhub.service.AccountService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller REST para operações com contas bancárias.
 *
 * O Controller é a PORTA DE ENTRADA HTTP. Ele é burro de propósito:
 * - Recebe a requisição HTTP
 * - Passa para o Service (que tem a lógica)
 * - Retorna a resposta HTTP com o código correto
 *
 * Se você vê um "if" de regra de negócio no Controller, está no lugar errado.
 * O Controller é um GARÇOM: anota o pedido, leva para a cozinha (Service),
 * e traz o prato pronto. Ele não cozinha.
 *
 * @RestController: Combina @Controller + @ResponseBody.
 *   Diz ao Spring: "esta classe responde requisições HTTP e retorna JSON".
 *
 * @RequestMapping("/api/v1/accounts"): Prefixo de TODAS as rotas deste controller.
 *   Toda rota começa com /api/v1/accounts.
 *   /api = é uma API
 *   /v1 = versão 1 (se mudar a API no futuro, cria /v2 sem quebrar clientes antigos)
 *   /accounts = recurso (convenção REST: plural)
 *
 * @Slf4j: Logger automático do Lombok.
 */
@RestController
@RequestMapping("/api/v1/accounts")
@Slf4j
public class AccountController {

    private final AccountService accountService;

    /**
     * Spring injeta o AccountService automaticamente.
     * O AccountService, por sua vez, já recebeu o Repository e AuditService.
     * É uma cadeia de injeção: Spring monta tudo na inicialização.
     */
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * POST /api/v1/accounts — Criar conta bancária.
     *
     * @Valid: Antes de chamar este método, o Spring valida o DTO
     *   usando as annotations (@NotBlank, @Size, @Pattern).
     *   Se qualquer validação falhar, lança MethodArgumentNotValidException
     *   → GlobalExceptionHandler retorna 400 com os campos que falharam.
     *   O método NEM É CHAMADO se a validação falhar.
     *
     * @RequestBody: "Converta o JSON do corpo da requisição neste objeto."
     *   O Jackson lê o JSON e preenche os campos do CreateAccountRequest.
     *
     * ResponseEntity<AccountResponse>: A resposta HTTP completa.
     *   - status(201): 201 Created — padrão REST para criação de recurso
     *   - body(response): o JSON com os dados da conta criada
     *
     * Exemplo:
     *   POST /api/v1/accounts
     *   Body: { "holderName": "Riel", "holderDocument": "52998224725", ... }
     *   Resposta: 201 Created + { "id": "uuid-...", "holderName": "Riel", ... }
     */
    @PostMapping
    public ResponseEntity<AccountResponse> create(
            @Valid @RequestBody CreateAccountRequest request) {
        log.info("POST /api/v1/accounts — Criando conta");
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/accounts/{id} — Buscar conta por ID.
     *
     * @PathVariable: Extrai o valor de {id} da URL e coloca na variável.
     *   Ex: GET /api/v1/accounts/550e8400-e29b-41d4-a716-446655440000
     *   → id = UUID("550e8400-e29b-41d4-a716-446655440000")
     *
     * ResponseEntity.ok(): Atalho para status 200 OK.
     *
     * Se a conta não existir, o Service lança ResourceNotFoundException
     * → GlobalExceptionHandler retorna 404 automaticamente.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getById(@PathVariable UUID id) {
        log.info("GET /api/v1/accounts/{} — Buscando conta", id);
        return ResponseEntity.ok(accountService.getById(id));
    }

    /**
     * GET /api/v1/accounts/by-document/{document} — Buscar por CPF/CNPJ.
     *
     * Rota separada do getById porque o formato do identificador é diferente:
     * - {id} é UUID (formato fixo)
     * - {document} é CPF ou CNPJ (11 ou 14 dígitos)
     *
     * Ex: GET /api/v1/accounts/by-document/52998224725
     */
    @GetMapping("/by-document/{document}")
    public ResponseEntity<AccountResponse> getByDocument(
            @PathVariable String document) {
        log.info("GET /api/v1/accounts/by-document/{}**** — Buscando por documento",
                document.substring(0, Math.min(3, document.length())));
        return ResponseEntity.ok(accountService.getByDocument(document));
    }

    /**
     * PUT /api/v1/accounts/{id} — Atualizar dados da conta.
     *
     * PUT é usado para atualização completa ou parcial de dados.
     * O UpdateAccountRequest só tem campos que PODEM mudar.
     * Campos null no request são ignorados (mantém valor atual).
     *
     * @Valid valida as constraints do UpdateAccountRequest
     *   (ex: @Size no holderName).
     */
    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAccountRequest request) {
        log.info("PUT /api/v1/accounts/{} — Atualizando conta", id);
        return ResponseEntity.ok(accountService.updateAccount(id, request));
    }

    /**
     * PATCH /api/v1/accounts/{id}/block — Bloquear conta.
     *
     * Por que PATCH e não PUT?
     * - PUT = atualiza O RECURSO INTEIRO (manda todos os campos)
     * - PATCH = atualiza PARCIALMENTE (muda só o status)
     *
     * Bloquear muda APENAS o status → PATCH é o verbo correto.
     * Não precisa de @RequestBody — não há dados no corpo.
     * A ação está na URL (/block).
     */
    @PatchMapping("/{id}/block")
    public ResponseEntity<AccountResponse> block(@PathVariable UUID id) {
        log.info("PATCH /api/v1/accounts/{}/block — Bloqueando conta", id);
        return ResponseEntity.ok(accountService.blockAccount(id));
    }

    /**
     * PATCH /api/v1/accounts/{id}/unblock — Desbloquear conta.
     *
     * Mesma lógica do block — muda APENAS o status.
     */
    @PatchMapping("/{id}/unblock")
    public ResponseEntity<AccountResponse> unblock(@PathVariable UUID id) {
        log.info("PATCH /api/v1/accounts/{}/unblock — Desbloqueando conta", id);
        return ResponseEntity.ok(accountService.unblockAccount(id));
    }

    /**
     * DELETE /api/v1/accounts/{id} — Desativar conta (soft delete).
     *
     * NÃO deleta fisicamente do banco — muda status para INACTIVE.
     * A conta continua existindo para fins de auditoria e compliance.
     *
     * ResponseEntity.noContent(): Status 204 No Content.
     * 204 é o padrão REST para DELETE bem-sucedido:
     * "A operação foi feita, mas não há conteúdo para retornar."
     *
     * Retorna Void (sem corpo) porque a conta foi "deletada" —
     * não faz sentido retornar os dados de algo que acabou de ser removido.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        log.info("DELETE /api/v1/accounts/{} — Desativando conta", id);
        accountService.deactivateAccount(id);
        return ResponseEntity.noContent().build();
    }
}
