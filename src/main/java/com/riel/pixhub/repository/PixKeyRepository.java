package com.riel.pixhub.repository;

import com.riel.pixhub.entity.PixKey;
import com.riel.pixhub.enums.PixKeyStatus;
import com.riel.pixhub.enums.PixKeyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório JPA para a entidade PixKey.
 *
 * Queries mais específicas aqui porque chaves PIX têm regras complexas:
 * - Busca por valor (para encontrar o destinatário de um PIX)
 * - Contagem por conta (para validar limite de 5 PF / 20 PJ)
 * - Filtro por tipo e status
 */
@Repository
public interface PixKeyRepository extends JpaRepository<PixKey, UUID> {

    /**
     * Busca chave PIX pelo valor exato.
     * É a query mais importante: quando alguém faz um PIX para "fulano@email.com",
     * o sistema usa este método para encontrar a conta destinatária.
     */
    Optional<PixKey> findByKeyValue(String keyValue);

    /** Verifica se já existe uma chave com esse valor (unicidade) */
    boolean existsByKeyValue(String keyValue);

    /** Lista todas as chaves de uma conta (ativas e inativas) */
    List<PixKey> findByAccountId(UUID accountId);

    /** Lista só as chaves ativas de uma conta */
    List<PixKey> findByAccountIdAndStatus(UUID accountId, PixKeyStatus status);

    /**
     * Conta quantas chaves ativas uma conta tem.
     * Usado para validar o limite: máx 5 para PF, 20 para PJ.
     */
    long countByAccountIdAndStatus(UUID accountId, PixKeyStatus status);

    /** Busca chaves por tipo (ex: listar todas as chaves CPF do sistema) */
    List<PixKey> findByKeyType(PixKeyType keyType);
}
