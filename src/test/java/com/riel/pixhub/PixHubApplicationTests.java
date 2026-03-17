package com.riel.pixhub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Teste de smoke (fumaça) — verifica se a aplicação inicia sem erros.
 *
 * @SpringBootTest: Carrega o contexto COMPLETO do Spring Boot,
 * como se a aplicação estivesse iniciando de verdade. Testa:
 * - Todas as configurações estão corretas
 * - Todas as dependências são resolvidas
 * - Beans são criados sem conflitos
 *
 * @ActiveProfiles("test"): Ativa o profile de teste, que usa
 * Testcontainers em vez do banco de desenvolvimento.
 *
 * Se este teste passa, sabemos que a aplicação pelo menos INICIA.
 * É o teste mais básico e mais importante de ter.
 */
@SpringBootTest
@ActiveProfiles("test")
class PixHubApplicationTests {

    /**
     * contextLoads(): Se o contexto do Spring carrega sem exceções,
     * o teste passa automaticamente (não precisa de assertions).
     * Falha se: bean faltando, configuração errada, driver não encontrado, etc.
     */
    @Test
    void contextLoads() {
        // Intencionalmente vazio — o teste é apenas carregar o contexto.
    }

}
