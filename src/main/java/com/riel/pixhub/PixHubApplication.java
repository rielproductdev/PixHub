package com.riel.pixhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principal da aplicação PixHub.
 *
 * @SpringBootApplication combina 3 annotations:
 * - @Configuration: marca a classe como fonte de configurações (beans)
 * - @EnableAutoConfiguration: Spring Boot configura automaticamente
 *   dependências detectadas no classpath (ex: viu PostgreSQL driver? configura DataSource)
 * - @ComponentScan: escaneia todos os pacotes a partir de com.riel.pixhub
 *   procurando classes com @Component, @Service, @Repository, @Controller
 *
 * O método main() chama SpringApplication.run() que:
 * 1. Cria o ApplicationContext (container de dependências)
 * 2. Inicia o servidor Tomcat embutido
 * 3. Executa as migrations Flyway
 * 4. Registra todos os beans (controllers, services, etc)
 */
@SpringBootApplication
public class PixHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(PixHubApplication.class, args);
    }

}
