package com.riel.pixhub.repository;

import com.riel.pixhub.entity.Transaction;
import com.riel.pixhub.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório JPA para a entidade Transaction.
 *
 * Queries focadas em: busca por identificadores únicos,
 * filtragem por status, e listagem por conta.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /** Busca pelo endToEndId — identificador único no ecossistema PIX */
    Optional<Transaction> findByEndToEndId(String endToEndId);

    /**
     * Busca pela chave de idempotência.
     * Se encontrar, a transação já foi processada — retorna a existente
     * em vez de criar duplicata.
     */
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    /** Verifica se já existe transação com essa chave de idempotência */
    boolean existsByIdempotencyKey(String idempotencyKey);

    /** Lista transações enviadas por uma conta */
    List<Transaction> findBySenderAccountId(UUID senderAccountId);

    /** Lista transações recebidas por uma conta */
    List<Transaction> findByReceiverAccountId(UUID receiverAccountId);

    /** Filtra transações por status (ex: todas as PENDING para processamento) */
    List<Transaction> findByStatus(TransactionStatus status);
}
