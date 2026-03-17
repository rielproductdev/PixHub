package com.riel.pixhub.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do Swagger/OpenAPI — documentação automática da API.
 *
 * OpenAPI é uma especificação (padrão da indústria) para descrever APIs REST.
 * O springdoc-openapi escaneia os controllers automaticamente e gera a
 * documentação. Esta classe customiza os metadados (título, versão, etc).
 *
 * Acessível em:
 * - /swagger-ui.html → interface visual interativa
 * - /api-docs → JSON da especificação (para ferramentas de geração de código)
 */
@Configuration
public class OpenApiConfig {

    /**
     * Customiza os metadados da documentação OpenAPI.
     *
     * SecurityScheme "bearerAuth" configura o botão "Authorize" no Swagger UI,
     * permitindo testar endpoints protegidos informando um token JWT.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("PixHub API")
                .version("0.1.0")
                .description(
                    "API de simulação do sistema PIX brasileiro. "
                    + "Implementa registro de chaves, transferências instantâneas "
                    + "e consultas com arquitetura event-driven (Kafka)."
                )
                .contact(new Contact()
                    .name("Riel")
                    .url("https://github.com/riel")
                )
                .license(new License()
                    .name("MIT")
                    .url("https://opensource.org/licenses/MIT")
                )
            )
            // Adiciona o esquema de segurança JWT à documentação.
            // Isso faz o Swagger UI mostrar o botão "Authorize"
            // onde o usuário cola o token JWT para testar endpoints protegidos.
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .schemaRequirement("bearerAuth", new SecurityScheme()
                // type=HTTP + scheme=bearer: padrão "Authorization: Bearer <token>"
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                // bearerFormat=JWT: indica que o token é um JWT (informativo).
                .bearerFormat("JWT")
                .description("Insira o token JWT obtido no endpoint de login")
            );
    }

}
