package com.riel.pixhub.repository;

import com.riel.pixhub.entity.User;
import com.riel.pixhub.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório JPA para a entidade User (autenticação).
 *
 * Os métodos aqui atendem os fluxos de segurança:
 * - findByEmail: usado no login (buscar user pelas credenciais)
 * - existsByEmail: usado no registro (verificar se email já existe)
 * - findByStatus: usado por admin (listar users bloqueados)
 *
 * O Spring Security vai usar findByEmail para carregar o usuário
 * no UserDetailsService customizado (implementado na próxima etapa).
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Busca usuário pelo email — usado em TODO fluxo de autenticação.
     *
     * Retorna Optional porque o email pode não existir no banco
     * (ex: tentativa de login com email não cadastrado).
     * O índice único idx_user_email garante performance O(1) nesta query.
     */
    Optional<User> findByEmail(String email);

    /**
     * Verifica se já existe um usuário com este email.
     *
     * Usado no endpoint de registro para retornar 409 Conflict
     * em vez de deixar o banco lançar uma exceção de constraint violation.
     * Mais elegante e gera mensagem de erro clara para o cliente.
     */
    boolean existsByEmail(String email);

    /**
     * Lista usuários por status (ACTIVE ou LOCKED).
     *
     * Útil para painel administrativo:
     * - Listar todos os usuários bloqueados para revisão manual
     * - Monitorar tentativas de brute force em massa
     */
    List<User> findByStatus(UserStatus status);
}
