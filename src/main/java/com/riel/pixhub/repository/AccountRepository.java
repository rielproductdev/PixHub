package com.riel.pixhub.repository;

import com.riel.pixhub.entity.Account;
import com.riel.pixhub.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório JPA para a entidade Account.
 *
 * Estende JpaRepository que já fornece: save(), findById(), findAll(),
 * delete(), count(), existsById(), etc — sem escrever SQL.
 *
 * Métodos customizados usam "query derivation": o Spring lê o nome
 * do método e gera o SQL automaticamente.
 * Ex: findByHolderDocument → SELECT * FROM accounts WHERE holder_document = ?
 *
 * @Repository: Marca como bean Spring + traduz exceções do banco
 * para exceções Spring (DataAccessException).
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    /**
     * Busca contas pelo documento do titular (CPF ou CNPJ).
     * Uma pessoa pode ter várias contas em bancos diferentes.
     */
    List<Account> findByHolderDocument(String holderDocument);

    /**
     * Busca conta por banco + agência + número (combinação única).
     * Útil para validar se a conta já existe no sistema.
     */
    Optional<Account> findByBankCodeAndBranchAndAccountNumber(
            String bankCode, String branch, String accountNumber);

    /** Lista todas as contas com um status específico */
    List<Account> findByStatus(AccountStatus status);

    /** Verifica se já existe conta com esse documento nesse banco */
    boolean existsByHolderDocumentAndBankCode(String holderDocument, String bankCode);
}
