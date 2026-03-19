package com.riel.pixhub.repository;

import com.riel.pixhub.entity.AuditLog;
import com.riel.pixhub.enums.AuditAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repositório JPA para a entidade AuditLog.
 *
 * Queries focadas em: buscar logs por entidade, por ação, e por quem fez.
 * Essas queries são usadas para investigação e relatórios de compliance.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Busca todos os logs de uma entidade específica.
     * Ex: "me dê todo o histórico de alterações dessa conta"
     */
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId);

    /** Filtra por tipo de ação (ex: todas as STATUS_CHANGE do sistema) */
    List<AuditLog> findByAction(AuditAction action);

    /** Busca logs por quem executou (investigação) */
    List<AuditLog> findByPerformedBy(String performedBy);
}
