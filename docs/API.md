# API Reference

## Autenticação

### Login (Agência)
```http
POST /auth/login
Content-Type: application/json

{
  "email": "admin@reviso.com",
  "password": "admin123"
}
```

**Resposta:**
```json
{
  "token": "eyJhbGc...",
  "userId": "uuid",
  "fullName": "Admin Reviso",
  "email": "admin@reviso.com",
  "role": "AGENCY_ADMIN",
  "companyId": "uuid",
  "agencyId": "uuid"
}
```

### Login (Cliente)
```http
POST /auth/login-client
Content-Type: application/json

{
  "companyCode": "CLIE-CL-ABC",
  "email": "maria@clienteabc.com",
  "password": "cliente123"
}
```

### Recuperação de Senha
```http
POST /auth/recover-agency-password
Content-Type: application/json

{
  "email": "admin@reviso.com"
}
```

```http
POST /auth/recover-agency-password/confirm
Content-Type: application/json

{
  "email": "admin@reviso.com",
  "token": "123456",
  "newPassword": "nova_senha"
}
```

## Admin (AGENCY_ADMIN)

Todos os endpoints requerem: `Authorization: Bearer {token}`

### Empresas
```http
POST /admin/companies
{
  "name": "Cliente XYZ",
  "type": "CLIENT"
}

GET /admin/companies

PATCH /admin/companies/{id}
{
  "name": "Cliente XYZ Atualizado",
  "active": true
}
```

### Usuários
```http
POST /admin/users
{
  "fullName": "João Silva",
  "email": "joao@empresa.com",
  "password": "senha123",
  "role": "CLIENT_USER",
  "companyCode": "CLIE-CL-ABC"
}

GET /admin/users

PATCH /admin/users/{id}
{
  "fullName": "João Silva",
  "email": "joao@empresa.com",
  "role": "CLIENT_USER",
  "active": true
}

DELETE /admin/users/{id}
```

## Agência (AGENCY_ADMIN, AGENCY_USER)

### Briefings
```http
GET /agency/briefings?status=PENDING

POST /agency/briefings/{id}/convert
{
  "title": "Campanha Black Friday",
  "description": "Descrição da demanda",
  "type": "SOCIAL_MEDIA",
  "priority": "HIGH",
  "department": "DESIGN",
  "dueDate": "2026-12-31T23:59:59-03:00"
}

PATCH /agency/briefings/{id}/reject
```

## Cliente (CLIENT_USER)

### Briefings
```http
POST /briefings
{
  "title": "Campanha Black Friday 2026",
  "description": "Precisamos de 15 artes para Instagram Stories"
}

GET /briefings/mine
```

### Demandas
```http
GET /requests/mine

GET /requests/{id}

GET /requests/{id}/events?onlyVisibleToClient=true
```

## Demandas (AGENCY_ADMIN, AGENCY_USER)

### Gerenciamento
```http
POST /requests
{
  "title": "Nova demanda",
  "description": "Descrição",
  "type": "SOCIAL_MEDIA",
  "priority": "MEDIUM",
  "department": "DESIGN",
  "companyCode": "CLIE-CL-ABC",
  "dueDate": "2026-12-31T23:59:59-03:00"
}

GET /requests

GET /requests/{id}

PATCH /requests/{id}/status
{
  "status": "IN_PROGRESS",
  "comment": "Iniciando desenvolvimento"
}

PATCH /requests/{id}/assign
{
  "assigneeId": "uuid"
}
```

### Comentários
```http
POST /requests/{id}/comments
{
  "text": "Comentário sobre a demanda",
  "visibleToClient": true
}
```

## Onboarding (Público)

### Signup SaaS
```http
GET /onboarding/plans

POST /onboarding/signup
{
  "planId": "uuid",
  "agencyName": "Minha Agência",
  "adminEmail": "admin@agencia.com",
  "adminPassword": "senha_forte"
}
```

**Resposta:**
```json
{
  "sessionUrl": "https://checkout.stripe.com/..."
}
```

### Webhook Stripe
```http
POST /onboarding/webhook/stripe
Stripe-Signature: t=...,v1=...

{
  "id": "evt_...",
  "type": "checkout.session.completed",
  "data": { ... }
}
```

## Roles e Permissões

| Endpoint | AGENCY_ADMIN | AGENCY_USER | CLIENT_USER |
|----------|--------------|-------------|-------------|
| `/admin/*` | ✅ | ❌ | ❌ |
| `/agency/*` | ✅ | ✅ | ❌ |
| `/briefings` (POST) | ❌ | ❌ | ✅ |
| `/briefings/mine` | ❌ | ❌ | ✅ |
| `/requests/mine` | ❌ | ❌ | ✅ |
| `/requests` (GET/POST) | ✅ | ✅ | ❌ |

## Status de Demandas

```
NEW → IN_PROGRESS → IN_REVIEW → CHANGES_REQUESTED ↔ IN_PROGRESS
                              → APPROVED → DELIVERED → DONE
                              
Qualquer status → CANCELED
```

## Tipos de Demanda

- `SOCIAL_MEDIA` - Redes sociais
- `EMAIL` - Email marketing
- `LANDING_PAGE` - Landing page
- `VIDEO` - Vídeo
- `DESIGN` - Design gráfico
- `OTHER` - Outros

## Prioridades

- `LOW` - Baixa
- `MEDIUM` - Média
- `HIGH` - Alta
- `URGENT` - Urgente

## Departamentos

- `DESIGN` - Design
- `COPY` - Copywriting
- `VIDEO` - Vídeo
- `DEV` - Desenvolvimento
