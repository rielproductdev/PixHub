# PixHub — Instruções para o Claude

## Sobre o projeto
API REST de simulação do sistema PIX brasileiro. Portfolio para vaga de Squad Banking.
Stack: Spring Boot 3.2, Java 17, PostgreSQL 16, Kafka (KRaft), Docker.

## Estrutura de pacotes
```
com.riel.pixhub
├── config/       → Configurações (Security, Kafka, OpenAPI)
├── controller/   → Endpoints REST (@RestController)
├── dto/          → Objetos de request/response (nunca expor Entity na API)
├── entity/       → Entidades JPA (@Entity, mapeiam tabelas)
├── enums/        → Enumerações (PixKeyType, TransactionStatus, etc)
├── exception/    → Exceções customizadas + GlobalExceptionHandler
├── repository/   → Interfaces JPA (extends JpaRepository)
├── service/      → Lógica de negócio (@Service)
├── validation/   → Validadores customizados
└── audit/        → Auditoria de operações
```

## Convenções
- **Idioma**: código em inglês, comentários/documentação em português (projeto acadêmico)
- **Comentários**: explicar TODA decisão técnica e por quê (o Riel está aprendendo)
- **API versioning**: /api/v1/...
- **DTOs**: sempre separar Request e Response (nunca expor Entity)
- **Migrations**: Flyway, nunca alterar migrations já executadas
- **Testes**: usar Testcontainers (PostgreSQL e Kafka reais, não mocks)
- **Profiles**: dev (Docker local), test (Testcontainers), prod (env vars)

## Comandos úteis
```bash
mvn clean compile              # Compilar
mvn spring-boot:run            # Rodar localmente
mvn test                       # Executar testes
docker compose up -d           # Subir infra (Postgres + Kafka)
docker compose down            # Parar infra
docker compose down -v         # Parar e limpar dados
```

## Jornada de aprendizado
Atualizar `jornada-de-aprendizado/index.html` a cada conceito novo aprendido.
