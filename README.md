# PixHub — API REST de Simulação do Sistema PIX

API completa simulando o ecossistema PIX brasileiro: contas bancárias, chaves PIX, transações, QR Code, auditoria e webhooks.

Projeto de portfolio para vagas em **Squad Banking**, construído com assistência do [Claude](https://claude.ai) (Anthropic) como par de programação e instrutor técnico.

## Stack

| Tecnologia | Versão | Papel |
|---|---|---|
| Java | 17 | Linguagem principal |
| Spring Boot | 3.2 | Framework web + DI + Security |
| PostgreSQL | 16 | Banco relacional (ACID) |
| Apache Kafka | KRaft | Mensageria assíncrona |
| Flyway | — | Migrations versionadas |
| Docker | Compose | Infraestrutura local |
| Bucket4j | 8.10 | Rate limiting (Token Bucket) |
| JWT (HMAC-SHA256) | — | Autenticação stateless |

## O que já foi implementado

### Fase 01 — Setup
- Docker Compose (PostgreSQL + Kafka KRaft + Kafka UI)
- Estrutura de pacotes, profiles (dev/test/prod), OpenAPI/Swagger

### Fase 02 — Domínio e Serviço
- 7 entidades JPA (Account, PixKey, Transaction, QrCode, AuditLog, WebhookConfig, BaseEntity)
- 10 enums como vocabulário tipado do sistema
- 9 migrations Flyway (V1–V9)
- Validação de CPF/CNPJ (módulo 11)
- AccountService, AccountController, DTOs, Mapper
- GlobalExceptionHandler com exceções customizadas
- AuditService para rastreabilidade

### Fase 03 — Segurança
- Autenticação JWT (register, login, refresh) com access token (15min) e refresh token (7d)
- Spring Security: filter chain, CORS, CSRF desabilitado (stateless), @EnableMethodSecurity
- Proteção contra brute force (5 tentativas → bloqueio 30min)
- Rate limiting com Bucket4j (auth: 5/min por IP, transações: 30/min, consultas: 60/min)
- Criptografia AES-256-GCM em dados sensíveis (CPF, chave PIX) via JPA AttributeConverter
- Security headers (X-Frame-Options, X-Content-Type-Options, CSP, Cache-Control)
- Sanitização XSS (@NoHtml)
- SecurityEventLogger dedicado

### Fase 04 — Chaves PIX *(em andamento)*
- Hash companion (SHA-256) para busca em campos criptografados
- CRUD de chaves PIX com validadores por tipo
- Mascaramento de dados sensíveis na resposta

## Como rodar

```bash
# 1. Subir infraestrutura
docker compose up -d

# 2. Compilar
mvn clean compile

# 3. Rodar a API
mvn spring-boot:run

# 4. Acessar o Swagger
# http://localhost:8080/swagger-ui.html
```

## Jornada de Aprendizado

Este projeto tem um **diário de aprendizado** documentando cada conceito aprendido durante o desenvolvimento — com explicações, analogias, comparações entre linguagens e decisões técnicas.

- [Jornada de Aprendizado](https://rielproductdev.github.io/PixHub/jornada-de-aprendizado/)
- [Mapa da Arquitetura](https://rielproductdev.github.io/PixHub/jornada-de-aprendizado/arquitetura.html)

## Estrutura do Projeto

```
com.riel.pixhub
├── config/       → Configurações (Security, JWT, Kafka, Rate Limiting, Encryption)
├── controller/   → Endpoints REST
├── dto/          → Objetos de request/response
├── entity/       → Entidades JPA
├── enums/        → Enumerações tipadas
├── exception/    → Exceções customizadas + GlobalExceptionHandler
├── mapper/       → Conversão DTO ↔ Entity
├── repository/   → Interfaces JPA
├── service/      → Lógica de negócio
├── validation/   → Validadores customizados (@NoHtml, DocumentValidator)
└── audit/        → Auditoria de operações
```

## Sobre o desenvolvimento com IA

Este projeto é construído com assistência do **Claude (Anthropic)** como par de programação. O Claude escreve código, explica decisões técnicas e documenta conceitos — tudo dentro do contexto real do projeto. A transparência é intencional: IA é ferramenta de aprendizado, não atalho.

Série de posts no LinkedIn documentando a jornada: [em publicação]
