package com.riel.pixhub.repository;

import com.riel.pixhub.entity.WebhookConfig;
import com.riel.pixhub.enums.WebhookStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repositório JPA para a entidade WebhookConfig.
 */
@Repository
public interface WebhookConfigRepository extends JpaRepository<WebhookConfig, UUID> {

    /** Lista webhooks de uma conta específica */
    List<WebhookConfig> findByAccountId(UUID accountId);

    /** Lista webhooks ativos de uma conta (para disparar notificações) */
    List<WebhookConfig> findByAccountIdAndStatus(UUID accountId, WebhookStatus status);

    /** Lista todos os webhooks ativos do sistema */
    List<WebhookConfig> findByStatus(WebhookStatus status);
}
