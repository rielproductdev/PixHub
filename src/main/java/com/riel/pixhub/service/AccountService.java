package com.riel.pixhub.service;

import com.riel.pixhub.dto.account.AccountResponse;
import com.riel.pixhub.dto.account.CreateAccountRequest;
import com.riel.pixhub.dto.account.UpdateAccountRequest;
import com.riel.pixhub.entity.Account;
import com.riel.pixhub.enums.AccountStatus;
import com.riel.pixhub.exception.AccountBlockedException;
import com.riel.pixhub.exception.BusinessRuleViolationException;
import com.riel.pixhub.exception.ResourceAlreadyExistsException;
import com.riel.pixhub.exception.ResourceNotFoundException;
import com.riel.pixhub.mapper.AccountMapper;
import com.riel.pixhub.repository.AccountRepository;
import com.riel.pixhub.validation.DocumentValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service que contém TODA a lógica de negócio de contas bancárias.
 *
 * É o "cérebro" — ele decide o que pode e o que não pode acontecer.
 * O Controller só recebe a requisição e passa para cá.
 * O Repository só sabe salvar/buscar no banco.
 * O Service é quem VALIDA, EXECUTA, AUDITA e RETORNA.
 *
 * Padrão de todo método:
 * 1. VALIDAR  — regras de negócio (CPF válido? Conta existe? Está bloqueada?)
 * 2. EXECUTAR — salvar/atualizar no banco via Repository
 * 3. AUDITAR  — registrar a operação no AuditService (compliance)
 * 4. RETORNAR — converter Entity para DTO via Mapper
 *
 * @Service: Spring cria uma instância e gerencia (injeção de dependência)
 * @Slf4j: Lombok cria o logger automaticamente
 * @Transactional: Spring garante que operações no banco são atômicas —
 *   se algo falhar no meio, tudo é desfeito (rollback). Isso é CRÍTICO
 *   em fintech: não pode debitar sem creditar, não pode salvar sem auditar.
 */
@Service
@Slf4j
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final AuditService auditService;

    /**
     * Constructor Injection — o Spring vê os parâmetros e injeta automaticamente.
     *
     * Por que Constructor Injection e não @Autowired no campo?
     * 1. Os campos podem ser "final" (não mudam depois de criados) — mais seguro
     * 2. Se esquecer uma dependência, o código NÃO compila — o erro é óbvio
     * 3. Facilita testes unitários — você pode passar mocks no construtor
     */
    public AccountService(AccountRepository accountRepository, AuditService auditService) {
        this.accountRepository = accountRepository;
        this.auditService = auditService;
    }

    /**
     * Cria uma nova conta bancária.
     *
     * Fluxo:
     * 1. Valida o documento (CPF/CNPJ válido pelo algoritmo módulo 11)
     * 2. Verifica se já existe conta com esse documento no mesmo banco
     * 3. Converte DTO → Entity via Mapper
     * 4. Salva no banco
     * 5. Registra no audit log
     * 6. Retorna DTO de resposta
     *
     * @param request DTO com dados da conta a criar
     * @return AccountResponse com dados da conta criada
     * @throws BusinessRuleViolationException se CPF/CNPJ for inválido
     * @throws ResourceAlreadyExistsException se já existir conta duplicada
     */
    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Criando conta para documento: {}****",
                request.getHolderDocument().substring(0, 3));

        // VALIDAR — documento é válido?
        if (!DocumentValidator.isValidDocument(request.getHolderDocument())) {
            throw new BusinessRuleViolationException(
                    "Documento inválido: CPF ou CNPJ com dígitos verificadores incorretos");
        }

        // VALIDAR — já existe conta com esse documento neste banco?
        if (accountRepository.existsByHolderDocumentAndBankCode(
                request.getHolderDocument(), request.getBankCode())) {
            throw new ResourceAlreadyExistsException(
                    "Já existe conta para este documento no banco " + request.getBankCode());
        }

        // EXECUTAR — converter e salvar
        Account account = AccountMapper.toEntity(request);
        Account saved = accountRepository.save(account);

        // AUDITAR — registrar criação
        auditService.logCreate("Account", saved.getId().toString(), "system", saved);

        log.info("Conta criada com sucesso. ID: {}", saved.getId());

        // RETORNAR — converter para DTO
        return AccountMapper.toResponse(saved);
    }

    /**
     * Busca uma conta por ID.
     *
     * @param id UUID da conta
     * @return AccountResponse com dados da conta
     * @throws ResourceNotFoundException se a conta não existir
     */
    @Transactional(readOnly = true)
    public AccountResponse getById(UUID id) {
        Account account = findAccountOrThrow(id);
        return AccountMapper.toResponse(account);
    }

    /**
     * Busca uma conta por documento (CPF ou CNPJ).
     *
     * @param document CPF (11 dígitos) ou CNPJ (14 dígitos)
     * @return AccountResponse com dados da conta
     * @throws ResourceNotFoundException se não encontrar
     */
    @Transactional(readOnly = true)
    public AccountResponse getByDocument(String document) {
        // findByHolderDocument retorna List (uma pessoa pode ter várias contas).
        // Pegamos a primeira encontrada, ou lançamos 404 se a lista estiver vazia.
        // stream().findFirst() converte List → Optional (primeiro elemento ou vazio).
        Account account = accountRepository.findByHolderDocument(document)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conta não encontrada para documento: " + document));
        return AccountMapper.toResponse(account);
    }

    /**
     * Atualiza dados permitidos de uma conta.
     *
     * Apenas campos que PODEM mudar são atualizados.
     * Campos imutáveis (holderDocument, bankCode, etc.) não estão no
     * UpdateAccountRequest — o cliente nem consegue enviá-los.
     *
     * Se o campo vier null no request, mantém o valor atual.
     *
     * @param id      UUID da conta
     * @param request DTO com campos a atualizar
     * @return AccountResponse com dados atualizados
     * @throws ResourceNotFoundException se a conta não existir
     * @throws AccountBlockedException se a conta estiver bloqueada
     */
    public AccountResponse updateAccount(UUID id, UpdateAccountRequest request) {
        Account account = findAccountOrThrow(id);
        validateAccountNotBlocked(account);

        // Salva estado anterior para audit
        String oldName = account.getHolderName();

        // Atualiza apenas campos não-null do request
        if (request.getHolderName() != null) {
            account.setHolderName(request.getHolderName());
        }

        Account saved = accountRepository.save(account);

        // Audita se houve mudança real
        if (request.getHolderName() != null && !request.getHolderName().equals(oldName)) {
            auditService.logUpdate("Account", id.toString(), "system",
                    "{\"holderName\": \"" + oldName + "\"}",
                    "{\"holderName\": \"" + saved.getHolderName() + "\"}");
        }

        log.info("Conta atualizada. ID: {}", id);
        return AccountMapper.toResponse(saved);
    }

    /**
     * Bloqueia uma conta — muda status para BLOCKED.
     *
     * Conta bloqueada não pode fazer nenhuma operação PIX
     * (nem enviar, nem receber, nem cadastrar chave).
     *
     * @param id UUID da conta
     * @return AccountResponse com status atualizado
     * @throws ResourceNotFoundException se a conta não existir
     * @throws BusinessRuleViolationException se já estiver bloqueada
     */
    public AccountResponse blockAccount(UUID id) {
        Account account = findAccountOrThrow(id);

        if (account.getStatus() == AccountStatus.BLOCKED) {
            throw new BusinessRuleViolationException("Conta já está bloqueada");
        }

        String oldStatus = account.getStatus().name();
        account.setStatus(AccountStatus.BLOCKED);
        Account saved = accountRepository.save(account);

        auditService.logStatusChange("Account", id.toString(),
                "system", oldStatus, AccountStatus.BLOCKED.name());

        log.info("Conta bloqueada. ID: {}", id);
        return AccountMapper.toResponse(saved);
    }

    /**
     * Desbloqueia uma conta — muda status para ACTIVE.
     *
     * @param id UUID da conta
     * @return AccountResponse com status atualizado
     * @throws ResourceNotFoundException se a conta não existir
     * @throws BusinessRuleViolationException se não estiver bloqueada
     */
    public AccountResponse unblockAccount(UUID id) {
        Account account = findAccountOrThrow(id);

        if (account.getStatus() != AccountStatus.BLOCKED) {
            throw new BusinessRuleViolationException("Conta não está bloqueada");
        }

        account.setStatus(AccountStatus.ACTIVE);
        Account saved = accountRepository.save(account);

        auditService.logStatusChange("Account", id.toString(),
                "system", AccountStatus.BLOCKED.name(), AccountStatus.ACTIVE.name());

        log.info("Conta desbloqueada. ID: {}", id);
        return AccountMapper.toResponse(saved);
    }

    /**
     * Desativa uma conta — soft delete (status INACTIVE).
     *
     * A conta continua no banco de dados (nunca deletamos fisicamente),
     * mas não pode mais operar. Usado quando o cliente encerra a conta.
     *
     * @param id UUID da conta
     * @throws ResourceNotFoundException se a conta não existir
     * @throws AccountBlockedException se a conta estiver bloqueada
     */
    public void deactivateAccount(UUID id) {
        Account account = findAccountOrThrow(id);
        validateAccountNotBlocked(account);

        if (account.getStatus() == AccountStatus.INACTIVE) {
            throw new BusinessRuleViolationException("Conta já está inativa");
        }

        String oldStatus = account.getStatus().name();
        account.setStatus(AccountStatus.INACTIVE);
        accountRepository.save(account);

        auditService.logStatusChange("Account", id.toString(),
                "system", oldStatus, AccountStatus.INACTIVE.name());

        log.info("Conta desativada. ID: {}", id);
    }

    // ==========================================
    // MÉTODOS AUXILIARES (private)
    // ==========================================

    /**
     * Busca conta por ID ou lança 404.
     *
     * Método auxiliar usado por vários métodos públicos.
     * Evita repetir o mesmo código de busca + exceção em cada método.
     * "DRY" = Don't Repeat Yourself.
     *
     * findById retorna Optional<Account>:
     * - Se existe → retorna a conta
     * - Se não existe → orElseThrow lança ResourceNotFoundException
     */
    private Account findAccountOrThrow(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta", id));
    }

    /**
     * Verifica se a conta está bloqueada e lança exceção se estiver.
     *
     * Usado antes de operações que não podem ser feitas em conta bloqueada
     * (update, deactivate). Bloquear/desbloquear não usa este método —
     * é justamente a operação que muda o estado de bloqueio.
     */
    private void validateAccountNotBlocked(Account account) {
        if (account.getStatus() == AccountStatus.BLOCKED) {
            throw new AccountBlockedException(account.getId());
        }
    }
}
