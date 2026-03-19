package com.riel.pixhub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riel.pixhub.entity.AuditLog;
import com.riel.pixhub.enums.AuditAction;
import com.riel.pixhub.repository.AuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service responsável por registrar operações no audit log.
 *
 * Funciona como uma câmera de segurança do sistema: não impede nada,
 * não bloqueia nada — só registra tudo que acontece. Se o BACEN perguntar
 * "quem bloqueou essa conta e quando?", a resposta está aqui.
 *
 * É chamado por outros Services (AccountService, PixKeyService, etc.)
 * após cada operação de escrita (create, update, delete, status_change).
 *
 * ObjectMapper (da biblioteca Jackson, que já vem com Spring Boot):
 * Converte qualquer objeto Java em uma string JSON. Isso permite salvar
 * o estado completo de uma entidade nos campos oldValue/newValue.
 */
@Service
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Spring injeta ambas as dependências automaticamente:
     * - AuditLogRepository: para salvar os logs no banco
     * - ObjectMapper: para converter objetos em JSON
     *   (Spring Boot já cria um ObjectMapper configurado — não precisamos fazer new)
     */
    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Registra a criação de uma entidade.
     *
     * oldValue é null porque não havia estado anterior (é uma criação).
     * newValue é o snapshot JSON do que foi criado.
     *
     * Exemplo de uso:
     *   auditService.logCreate("Account", account.getId().toString(), "system", account);
     *
     * Resultado no banco:
     * | entity_type | entity_id | action | performed_by | old_value | new_value          |
     * |-------------|-----------|--------|--------------|-----------|--------------------|
     * | Account     | uuid-123  | CREATE | system       | null      | {"holderName":...} |
     *
     * @param entityType  Tipo da entidade ("Account", "PixKey", "Transaction")
     * @param entityId    ID da entidade (UUID como String)
     * @param performedBy Quem executou ("system", ou userId quando tivermos JWT)
     * @param newValue    Objeto criado (será convertido para JSON)
     */
    public void logCreate(String entityType, String entityId,
                          String performedBy, Object newValue) {
        AuditLog auditLog = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(AuditAction.CREATE)
                .performedBy(performedBy)
                .oldValue(null)
                .newValue(toJson(newValue))
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Audit: {} {} criado por {}", entityType, entityId, performedBy);
    }

    /**
     * Registra a atualização de uma entidade.
     *
     * Salva o estado ANTES e DEPOIS — comparando os dois, sabemos
     * exatamente o que mudou.
     *
     * @param entityType  Tipo da entidade
     * @param entityId    ID da entidade
     * @param performedBy Quem executou
     * @param oldValue    Estado ANTES da alteração
     * @param newValue    Estado DEPOIS da alteração
     */
    public void logUpdate(String entityType, String entityId,
                          String performedBy, Object oldValue, Object newValue) {
        AuditLog auditLog = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(AuditAction.UPDATE)
                .performedBy(performedBy)
                .oldValue(toJson(oldValue))
                .newValue(toJson(newValue))
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Audit: {} {} atualizado por {}", entityType, entityId, performedBy);
    }

    /**
     * Registra uma mudança de status.
     *
     * Caso mais comum em fintech: ACTIVE → BLOCKED, PENDING → COMPLETED, etc.
     * O BACEN pode perguntar "quando e por quem essa conta foi bloqueada?" —
     * este log tem a resposta.
     *
     * Resultado no banco:
     * | action        | old_value            | new_value              |
     * |---------------|----------------------|------------------------|
     * | STATUS_CHANGE | {"status":"ACTIVE"}  | {"status":"BLOCKED"}   |
     *
     * @param entityType  Tipo da entidade
     * @param entityId    ID da entidade
     * @param performedBy Quem executou
     * @param oldStatus   Status antes (ex: "ACTIVE")
     * @param newStatus   Status depois (ex: "BLOCKED")
     */
    public void logStatusChange(String entityType, String entityId,
                                String performedBy, String oldStatus, String newStatus) {
        AuditLog auditLog = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(AuditAction.STATUS_CHANGE)
                .performedBy(performedBy)
                .oldValue("{\"status\": \"" + oldStatus + "\"}")
                .newValue("{\"status\": \"" + newStatus + "\"}")
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Audit: {} {} status {} → {} por {}",
                entityType, entityId, oldStatus, newStatus, performedBy);
    }

    /**
     * Registra a exclusão (soft delete) de uma entidade.
     *
     * Em fintech, nunca deletamos dados fisicamente. Mas registramos
     * que a "exclusão" (desativação) aconteceu.
     *
     * @param entityType  Tipo da entidade
     * @param entityId    ID da entidade
     * @param performedBy Quem executou
     * @param oldValue    Estado antes da exclusão
     */
    public void logDelete(String entityType, String entityId,
                          String performedBy, Object oldValue) {
        AuditLog auditLog = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(AuditAction.DELETE)
                .performedBy(performedBy)
                .oldValue(toJson(oldValue))
                .newValue(null)
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Audit: {} {} excluído por {}", entityType, entityId, performedBy);
    }

    /**
     * Converte qualquer objeto Java em uma string JSON.
     *
     * O ObjectMapper (Jackson) transforma o objeto percorrendo seus getters:
     *   Account conta = ... → {"holderName":"Riel","status":"ACTIVE",...}
     *
     * Se a conversão falhar (objeto não serializável), usa toString() como fallback
     * em vez de lançar exceção — o audit log é importante mas não deve
     * quebrar a operação principal por falha de serialização.
     */
    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("Falha ao serializar objeto para audit log: {}", e.getMessage());
            return obj.toString();
        }
    }
}
