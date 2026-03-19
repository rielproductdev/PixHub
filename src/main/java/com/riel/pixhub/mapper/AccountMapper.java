package com.riel.pixhub.mapper;

import com.riel.pixhub.dto.account.AccountResponse;
import com.riel.pixhub.dto.account.CreateAccountRequest;
import com.riel.pixhub.entity.Account;

/**
 * Mapper manual para converter entre Account (Entity) e DTOs.
 *
 * O que é um Mapper?
 * É um tradutor entre formatos diferentes. Cada formato (DTO, Entity) tem
 * campos diferentes. O Mapper sabe como copiar os campos de um para outro.
 *
 * Por que manual e não MapStruct?
 * MapStruct é uma biblioteca que gera o código de conversão automaticamente.
 * Neste projeto acadêmico, fazemos manual para entender exatamente o que
 * acontece em cada conversão. Em projetos reais com muitas entidades,
 * MapStruct economiza tempo.
 *
 * Por que métodos static?
 * O Mapper não tem estado (não guarda dados entre chamadas).
 * Ele só transforma A em B. Por isso não precisa de instância —
 * chamamos direto: AccountMapper.toEntity(request)
 * Sem static, teríamos que fazer: new AccountMapper().toEntity(request)
 * O static evita criar um objeto desnecessário.
 *
 * Fluxo:
 *   Cliente manda JSON
 *     → Spring converte em CreateAccountRequest (DTO)
 *       → AccountMapper.toEntity() converte em Account (Entity)
 *         → Repository salva no banco
 *           → AccountMapper.toResponse() converte em AccountResponse (DTO)
 *             → Spring converte em JSON e retorna ao cliente
 */
public class AccountMapper {

    /**
     * Converte DTO de entrada → Entity (para salvar no banco).
     *
     * Copia APENAS os campos que o cliente informou.
     * Campos automáticos (id, balance, status, createdAt, updatedAt)
     * NÃO são copiados — o JPA e @Builder.Default cuidam deles:
     * - id: gerado pelo @GeneratedValue(strategy = GenerationType.UUID)
     * - balance: @Builder.Default = BigDecimal.ZERO
     * - status: @Builder.Default = AccountStatus.ACTIVE
     * - createdAt/updatedAt: @PrePersist e @PreUpdate
     *
     * @param request DTO com os dados enviados pelo cliente
     * @return Entity pronta para ser salva pelo Repository
     */
    public static Account toEntity(CreateAccountRequest request) {
        return Account.builder()
                .holderName(request.getHolderName())
                .holderDocument(request.getHolderDocument())
                .bankCode(request.getBankCode())
                .branch(request.getBranch())
                .accountNumber(request.getAccountNumber())
                .accountType(request.getAccountType())
                .build();
    }

    /**
     * Converte Entity → DTO de saída (para retornar na API).
     *
     * Copia os campos que o cliente tem permissão de ver.
     * updatedAt NÃO é copiado — é dado interno.
     *
     * No futuro, holderDocument poderá ser mascarado aqui:
     *   .holderDocument(maskDocument(account.getHolderDocument()))
     * Por enquanto retorna completo — mascaramento vem com JWT.
     *
     * @param account Entity carregada do banco
     * @return DTO pronto para ser serializado como JSON
     */
    public static AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .holderName(account.getHolderName())
                .holderDocument(account.getHolderDocument())
                .bankCode(account.getBankCode())
                .branch(account.getBranch())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
