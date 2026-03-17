package com.riel.pixhub.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Configuração básica do Apache Kafka.
 *
 * Kafka é um sistema de mensageria distribuído (message broker).
 * Funciona com o conceito de "tópicos" (canais de comunicação):
 *
 * Producer (quem envia) → [Tópico] → Consumer (quem recebe)
 *
 * No PixHub, usamos Kafka para comunicação assíncrona:
 * - Quando um PIX é criado → publica evento no tópico "pix-transactions"
 * - Consumer processa o evento (notificação, auditoria, etc)
 *
 * Conceitos-chave:
 * - Tópico: canal nomeado onde mensagens são publicadas
 * - Partição: subdivisão do tópico para paralelismo
 * - Réplica: cópia da partição em outro broker (alta disponibilidade)
 *
 * Esta é a configuração BÁSICA (Fase 01). Cria os tópicos necessários.
 * Na Fase de Mensageria, adicionaremos producers e consumers.
 */
@Configuration
public class KafkaConfig {

    /**
     * Tópico para eventos de transações PIX.
     *
     * TopicBuilder configura:
     * - name: nome do tópico (convenção: kebab-case)
     * - partitions(3): divide o tópico em 3 partições para paralelismo.
     *   Cada partição pode ser consumida por um consumer diferente.
     *   3 partições = até 3 consumers processando em paralelo.
     * - replicas(1): apenas 1 cópia (suficiente para dev).
     *   Em produção, usar 3 réplicas para alta disponibilidade.
     */
    @Bean
    public NewTopic pixTransactionsTopic() {
        return TopicBuilder.name("pix-transactions")
            .partitions(3)
            .replicas(1)
            .build();
    }

    /**
     * Tópico para eventos de notificação.
     *
     * Quando uma transação PIX é concluída, um evento de notificação
     * é publicado aqui para que o serviço de notificação envie
     * confirmações ao remetente e destinatário.
     */
    @Bean
    public NewTopic pixNotificationsTopic() {
        return TopicBuilder.name("pix-notifications")
            .partitions(3)
            .replicas(1)
            .build();
    }

}
