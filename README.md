# Reviso Gestão de Demandas

Reviso é um sistema B2B para agência de publicidade gerenciar demandas de clientes do briefing à entrega, com histórico (audit trail) e métricas por relatórios.

O que ele resolve:

Centraliza pedidos (peças, campanhas, landing pages etc.), controla status, prazos e revisões e permite enxergar gargalos e atrasos.

## Fluxo principal

- NEW → IN_PROGRESS → IN_REVIEW → (CHANGES_REQUESTED ↔ IN_PROGRESS) → APPROVED → DELIVERED → CLOSED

## Tecnologias

- Java 21, Spring Boot 4.0.1 (Web MVC, Data JPA, Validation, Actuator)
- PostgreSQL 16 + Flyway para migrations
- Maven para build
- HTML/CSS/JS estático em `src/main/resources/static`

## Requisitos

- JDK 21+
- Maven Wrapper (`./mvnw`) incluído
- PostgreSQL rodando em `localhost:5432` com database `reviso` e usuário/senha `reviso` (ajuste em `src/main/resources/application.properties` se necessário)

## Endpoints principais

- `POST /clients` – cria cliente
- `GET /clients` – lista clientes
- `POST /requests` – cria demanda (enums alinhados aos tipos do PostgreSQL)
- `GET /requests/{id}` – consulta demanda

<<<<<<< HEAD
## Arquitetura e Foundation

### Schema preparado para crescimento

O schema do banco de dados (migrations Flyway) já inclui tabelas e tipos preparados para funcionalidades futuras:

- **Tabelas**: `users`, `projects`, `request_events` (audit trail)
- **Enums nativos PostgreSQL**: `request_type`, `request_priority`, `request_status`, `request_event_type`

**Estado atual do código**: implementado `Client` e `Request` (core funcional Day 2).  
**Próximas fases**: autenticação (`users`), agrupamento de demandas (`projects`), histórico completo (`request_events`).

Essa abordagem "foundation-first" facilita evolução incremental sem quebrar migrations ou exigir refatorações de schema.

## Interface web

Uma página de testes estática em [src/main/resources/static/index.html](src/main/resources/static/index.html) para criar e consultar clientes/requests.

## Monitoramento

- Actuator habilitado: `http://localhost:8080/actuator/health`

## Licença

Projeto privado/experimental (licença não definida).
