# Arquitetura

## Stack

- **Backend**: Java 21, Spring Boot 4.0.1
- **Frontend**: Angular 19
- **Database**: PostgreSQL 16
- **Infra**: Docker Compose, Nginx

## Multi-tenancy

**Single-database com discriminador:**
- Coluna `agency_id` em todas as tabelas
- Isolamento via query filters
- Sem databases separados

**Hierarquia:**
```
agencies
  └── companies (AGENCY ou CLIENT)
       └── users (AGENCY_ADMIN, AGENCY_USER, CLIENT_USER)
            └── requests
                 └── request_events (audit)
```

## Fluxo de Requisição

**Cliente:**
```
Login → Criar briefing → Agência converte → Acompanhar demanda
```

**Agência:**
```
Login → Ver briefings → Criar/atualizar demandas → Atribuir → Entregar
```

## Onboarding SaaS

```
Escolhe plano → Stripe Checkout → Pagamento
  → Webhook provisiona agência (inativa)
  → Webhook ativa após primeiro pagamento
```

**Segurança:**
- Senha hasheada (BCrypt) antes de criar Checkout
- Webhook validado (HMAC SHA256)
- Idempotência (event_id único)

Ver [ONBOARDING.md](ONBOARDING.md).

## Infraestrutura

| Ambiente | DB Port | CORS | Logs | Container |
|----------|---------|------|------|----------|
| Dev | 5433 | localhost | DEBUG | Root |
| Demo | Não exposto | Domínio | INFO | Non-root |

Ver [infra/README.md](../infra/README.md).
