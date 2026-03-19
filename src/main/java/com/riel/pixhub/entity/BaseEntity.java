package com.riel.pixhub.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Classe base abstrata que todas as entidades herdam.
 *
 * @MappedSuperclass: Diz ao JPA que essa classe NÃO tem tabela própria,
 * mas suas colunas (id, createdAt, updatedAt) aparecem nas tabelas dos filhos.
 * É diferente de @Entity com herança — não cria tabela "base_entity".
 *
 * Por que usar herança aqui?
 * Sem BaseEntity, teríamos que repetir id/createdAt/updatedAt em CADA entidade.
 * Com 6 entidades, são 18 campos duplicados. Se precisar adicionar um campo
 * (ex: "version" para controle de concorrência), alteramos só aqui.
 *
 * Por que UUID e não auto-increment (1, 2, 3...)?
 * 1. Segurança: IDs sequenciais são previsíveis (atacante adivinha o próximo)
 * 2. Sistemas distribuídos: UUID pode ser gerado em qualquer servidor sem conflito
 * 3. Padrão em fintech: o PIX real usa identificadores não-sequenciais
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    /**
     * Chave primária UUID gerada automaticamente.
     *
     * GenerationType.UUID: O Hibernate gera o UUID em memória (Java),
     * sem precisar consultar o banco. Mais performático que usar
     * a extensão uuid-ossp do PostgreSQL para cada insert.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Data de criação — setada automaticamente, nunca alterada.
     *
     * updatable = false: Mesmo que alguém tente alterar, o JPA ignora.
     * Isso é uma camada extra de proteção além do @PrePersist.
     * Em fintech, a data de criação é evidência legal — não pode mudar.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Data da última atualização — atualizada automaticamente.
     *
     * Responde perguntas como: "quando essa conta foi bloqueada pela última vez?"
     * O BACEN pode pedir essa informação em auditorias.
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Lifecycle callback: executado automaticamente ANTES do primeiro INSERT.
     *
     * @PrePersist é um "gancho" do JPA — o desenvolvedor não precisa lembrar
     * de setar createdAt manualmente. É impossível esquecer.
     * Isso é especialmente importante em equipes grandes onde cada dev
     * pode criar registros em diferentes partes do código.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Lifecycle callback: executado automaticamente ANTES de cada UPDATE.
     *
     * @PreUpdate atualiza o timestamp sempre que qualquer campo da entidade muda.
     * Combinado com AuditLog, permite rastrear QUANDO cada alteração aconteceu.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
