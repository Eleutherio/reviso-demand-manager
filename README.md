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
- Angular em `frontend/` (frontend oficial)

## Requisitos

- JDK 21+
- Maven Wrapper (`./mvnw`) incluído
- PostgreSQL rodando em `localhost:5432` com database `reviso` e usuário/senha `reviso` (ajuste em `src/main/resources/application.properties` se necessário)

## Endpoints principais

- Auth
  - `POST /auth/login` – login e retorno do JWT
- Briefings (cliente)
  - `POST /briefings` – cria briefing (payload: `title`, `description`)
  - `GET /briefings/mine` – lista briefings do cliente logado
- Demandas
  - `POST /requests` – cria demanda
  - `GET /requests` – lista demandas (agência)
  - `GET /requests/mine` – lista demandas do cliente logado
  - `GET /requests/{id}/events?onlyVisibleToClient=true` – eventos visíveis ao cliente

## Arquitetura e Foundation

### Schema preparado para crescimento

O schema do banco de dados (migrations Flyway) já inclui tabelas e tipos preparados para funcionalidades futuras:

- **Tabelas**: `users`, `projects`, `request_events` (audit trail)
- **Enums nativos PostgreSQL**: `request_type`, `request_priority`, `request_status`, `request_event_type`

**Estado atual do código**: implementado `Client` e `Request` (core funcional Day 2).  
**Próximas fases**: autenticação (`users`), agrupamento de demandas (`projects`), histórico completo (`request_events`).

Essa abordagem "foundation-first" facilita evolução incremental sem quebrar migrations ou exigir refatorações de schema.

## Interface web

- Frontend (Angular via Nginx): `http://localhost:4200`
- Backend (API): `http://localhost:8080`
  - Observação: `GET /` no backend redireciona para o frontend.

## Docker Compose (backend + frontend desacoplado)

Subir tudo (Postgres + API + Angular via Nginx):

```bash
docker compose up --build
```

- Backend (API): `http://localhost:8080`
- Frontend Angular (Nginx): `http://localhost:4200`

Para ajustar o redirect do backend `/`, defina `FRONTEND_BASE_URL` no compose/env (padrão: `http://localhost:4200`).

## Frontend Angular (Core)

O projeto Angular fica em [frontend](frontend). Nesta etapa ele contém apenas o App Shell + Auth + rotas (telas placeholder), consumindo o backend via proxy.

Rodar no dev:

- Backend (Spring) no ar em `http://localhost:8080`
- Frontend (Angular):
  - `cd frontend`
  - `npm install`
  - `npm start`

O `npm start` usa `frontend/proxy.conf.json` para:

- Chamar o backend como `/api/*` no Angular
- Proxy para `http://localhost:8080` com rewrite removendo o prefixo `/api`

Exemplo: `POST /api/auth/login` (Angular) → `POST /auth/login` (backend)

Observação: no Portal do Cliente, os campos extras (Tipo, Prioridade e Vencimento) são anexados ao texto da descrição do briefing para manter compatibilidade com a API atual do backend.

## Monitoramento

- Actuator habilitado: `http://localhost:8080/actuator/health`

## Licença

Projeto privado/experimental (licença não definida).
