package com.riel.pixhub.entity;

import com.riel.pixhub.enums.AuditAction;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Entidade de auditoria — registra TODA operação que modifica dados.
 *
 * Em fintech, auditoria não é opcional. O BACEN pode solicitar logs
 * de qualquer operação a qualquer momento. Sem auditoria completa,
 * a empresa toma multa e pode perder a licença de operação.
 *
 * Design pattern: Polymorphic Association
 * Em vez de criar uma tabela de auditoria para cada entidade,
 * usamos entityType + entityId para referenciar QUALQUER entidade.
 * Ex: entityType="Account", entityId="uuid-da-conta"
 */
@Entity
@Table(name = "audit_logs", indexes = {
    // Buscar todos os logs de uma entidade específica
    @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id"),
    // Buscar logs por tipo de ação
    @Index(name = "idx_audit_action", columnList = "action"),
    // Buscar logs por quem executou (investigação de fraude)
    @Index(name = "idx_audit_performed_by", columnList = "performed_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog extends BaseEntity {

    /**
     * Tipo da entidade auditada: "Account", "PixKey", "Transaction", etc.
     * Junto com entityId, permite encontrar o registro que foi alterado.
     */
    @NotBlank(message = "Tipo da entidade é obrigatório")
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    /**
     * ID (UUID) da entidade auditada.
     * Armazenado como String para flexibilidade (polymorphic association).
     */
    @NotBlank(message = "ID da entidade é obrigatório")
    @Column(name = "entity_id", nullable = false, length = 36)
    private String entityId;

    /** Tipo de ação: CREATE, UPDATE, DELETE ou STATUS_CHANGE */
    @NotNull(message = "Ação é obrigatória")
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 15)
    private AuditAction action;

    /**
     * Quem executou a operação (ID ou username).
     * Em caso de fraude, é isso que a investigação usa.
     */
    @Column(name = "performed_by", length = 100)
    private String performedBy;

    /**
     * Snapshot JSON do estado ANTES da alteração.
     * Ex: {"status": "ACTIVE", "balance": "1000.00"}
     * NULL para ação CREATE (não havia estado anterior).
     *
     * columnDefinition = "TEXT": campo sem limite de tamanho.
     * JSON é salvo como texto — PostgreSQL tem tipo JSONB nativo,
     * mas TEXT é mais portável entre bancos.
     */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /**
     * Snapshot JSON do estado DEPOIS da alteração.
     * Ex: {"status": "BLOCKED", "balance": "1000.00"}
     * Comparando oldValue e newValue, sabemos exatamente o que mudou.
     */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /**
     * Endereço IP de quem executou a operação.
     * Junto com performedBy, permite rastrear a origem exata.
     * IPv6 pode ter até 45 caracteres.
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
}
