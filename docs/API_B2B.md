# Reviso Demand Manager - Documentação da API B2B

## Status do Projeto

✅ **Aplicação rodando em:** http://localhost:8080  
✅ **Database:** PostgreSQL na porta 5433  
✅ **Migrations aplicadas:** V1 a V5

---

## Implementação B2B - 4 Etapas

### Etapa 1 - Auth + RBAC ✅

#### Roles Disponíveis

- `AGENCY_ADMIN` - Administrador da agência (gerencia companies e users)
- `AGENCY_USER` - Colaborador da agência (visualiza briefings e converte em demandas)
- `CLIENT_USER` - Usuário do cliente (envia briefings e visualiza demandas)

#### Endpoints de Autenticação

**POST /auth/login**

```json
{
  "email": "admin@agencia.com",
  "password": "senha123"
}
```

**Resposta:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": "uuid",
  "fullName": "Nome Completo",
  "email": "admin@agencia.com",
  "role": "AGENCY_ADMIN",
  "companyId": "uuid-ou-null"
}
```

**POST /auth/login-client**

Login para portal do cliente (companyCode + email + senha):

```json
{
  "companyCode": "CABC-CL-XXX",
  "email": "maria@clienteabc.com",
  "password": "senha123"
}
```

**POST /auth/recover-company-code**

Recupera companyCode por email (envio por provedor transacional):

```json
{
  "email": "maria@clienteabc.com"
}
```

**POST /auth/recover-agency-password**

Envia token de recuperacao de senha para usuarios de agencia:

```json
{
  "email": "usuario@agencia.com"
}
```

**POST /auth/recover-agency-password/confirm**

Confirma token e define nova senha:

```json
{
  "email": "usuario@agencia.com",
  "token": "123456",
  "newPassword": "nova123"
}
```

---

### Etapa 2 - Companies + Users (Admin) ✅

**Requer:** Role `AGENCY_ADMIN`  
**Header:** `Authorization: Bearer {token}`

#### POST /admin/companies

Criar empresa cliente:

```json
{
  "name": "Empresa XYZ Ltda",
  "type": "CLIENT"
}
```

#### GET /admin/companies

Lista todas as empresas cadastradas.

#### POST /admin/users

Criar usuário (agency ou client):

```json
{
  "fullName": "João Silva",
  "email": "joao@empresa.com",
  "password": "senha123",
  "role": "CLIENT_USER",
  "companyCode": "CABC-CL-XXX"
}
```

**Validações:**

- `CLIENT_USER` **deve** ter `companyId` **ou** `companyCode`
- `AGENCY_ADMIN` e `AGENCY_USER` podem ter `companyId`/`companyCode` null
- Se enviar ambos, eles devem apontar para a mesma empresa

#### PATCH /admin/users/{id}

Atualizar usuario:

```json
{
  "fullName": "Joao Silva",
  "email": "joao@empresa.com",
  "role": "CLIENT_USER",
  "companyCode": "CABC-CL-XXX",
  "active": true
}
```

Observacoes:

- Para `CLIENT_USER`, informe `companyId` ou `companyCode`
- Se enviar ambos, eles devem apontar para a mesma empresa

#### GET /admin/users

Lista todos os usuários cadastrados.

---

### Etapa 3 - Briefings (Portal Cliente) ✅

**Requer:** Role `CLIENT_USER`  
**Header:** `Authorization: Bearer {token}`  
**Tenant Isolation:** O `companyId` é extraído automaticamente do token JWT.

#### POST /briefings

Cliente envia um briefing:

```json
{
  "title": "Campanha Black Friday 2026",
  "description": "Precisamos de 10 posts para Instagram e 3 vídeos para TikTok"
}
```

**Observação:** O `companyId` e `createdByUserId` são extraídos do token automaticamente.

#### GET /briefings/mine

Lista todos os briefings enviados pela empresa do cliente autenticado.

#### GET /requests/mine

Lista todas as demandas (requests) da empresa do cliente autenticado.

#### GET /requests/{id}

Visualiza uma demanda específica (com validação de tenant - só pode ver se for da sua empresa).

---

### Etapa 4 - Inbox de Briefings + Conversão (Agência) ✅

**Requer:** Role `AGENCY_ADMIN` ou `AGENCY_USER`  
**Header:** `Authorization: Bearer {token}`

#### GET /agency/briefings

Lista briefings que chegaram (com filtro opcional por status):

```
GET /agency/briefings?status=PENDING
```

**Status disponíveis:**

- `PENDING` - Aguardando análise
- `CONVERTED` - Convertido em demanda
- `REJECTED` - Rejeitado

#### POST /agency/briefings/{id}/convert

Converte um briefing em uma demanda (Request):

- Cria uma nova entrada em `requests` com status `NEW`
- Atualiza o status do briefing para `CONVERTED`
- Vincula a demanda à mesma `companyId` do briefing

#### PATCH /agency/briefings/{id}/reject

Rejeita um briefing:

- Atualiza o status do briefing para `REJECTED`

---

## Fluxo de Status das Demandas (Requests)

### Status Expandidos ✅

- `NEW` → Demanda criada
- `IN_PROGRESS` → Em desenvolvimento
- `IN_REVIEW` → Em revisão
- `CHANGES_REQUESTED` → Mudanças solicitadas
- `APPROVED` → Aprovada
- `DELIVERED` → Entregue
- `DONE` → Concluída ✨ **novo**
- `CANCELED` → Cancelada ✨ **novo**
- `CLOSED` → Fechada (legacy)

### Transições Permitidas

```
NEW → IN_PROGRESS, CANCELED
IN_PROGRESS → IN_REVIEW, CANCELED
IN_REVIEW → APPROVED, CHANGES_REQUESTED, CANCELED
CHANGES_REQUESTED → IN_PROGRESS, CANCELED
APPROVED → DELIVERED, CANCELED
DELIVERED → DONE, CLOSED
```

---

## Padronização de Datas ✅

Todos os campos de data/hora utilizam:

- **Backend:** `OffsetDateTime` (Java)
- **Database:** `TIMESTAMPTZ` (PostgreSQL)
- **Timezone:** America/Sao_Paulo (configurado em `application.properties`)

---

## Exemplo de Fluxo Completo

### 1. Admin cria empresa cliente

```bash
POST /admin/companies
Authorization: Bearer {token-admin}
{
  "name": "Cliente ABC",
  "type": "CLIENT"
}
# Resposta: { "id": "company-uuid", ... }
```

### 2. Admin cria usuário CLIENT_USER

```bash
POST /admin/users
Authorization: Bearer {token-admin}
{
  "fullName": "Maria Santos",
  "email": "maria@clienteabc.com",
  "password": "senha123",
  "role": "CLIENT_USER",
  "companyCode": "CABC-CL-XXX"
}
```

### 3. Cliente faz login

```bash
POST /auth/login
{
  "email": "maria@clienteabc.com",
  "password": "senha123"
}
# Resposta: { "token": "jwt-token", "companyId": "company-uuid", ... }
```

### 4. Cliente envia briefing

```bash
POST /briefings
Authorization: Bearer {jwt-token}
{
  "title": "Campanha Páscoa",
  "description": "Conteúdo para redes sociais"
}
# Resposta: { "id": "briefing-uuid", "status": "PENDING", ... }
```

### 5. Agência visualiza briefing

```bash
GET /agency/briefings?status=PENDING
Authorization: Bearer {token-agency-user}
# Resposta: [ { "id": "briefing-uuid", "title": "Campanha Páscoa", ... } ]
```

### 6. Agência converte em demanda

```bash
POST /agency/briefings/briefing-uuid/convert
Authorization: Bearer {token-agency-user}
# Resposta: { "id": "request-uuid", "status": "NEW", ... }
```

### 7. Cliente visualiza sua demanda

```bash
GET /requests/mine
Authorization: Bearer {jwt-token}
# Resposta: [ { "id": "request-uuid", "title": "Campanha Páscoa", "status": "NEW", ... } ]
```

---

## Segurança

### Endpoints Públicos

- `POST /auth/login`
- `GET /` e `/index.html` (redirect para o frontend)
- `GET /actuator/**`

### Endpoints Protegidos por Role

- `/admin/**` → `AGENCY_ADMIN` apenas
- `/agency/**` → `AGENCY_ADMIN` ou `AGENCY_USER`
- `POST /briefings` → `CLIENT_USER` apenas
- `GET /briefings/mine` → `CLIENT_USER` apenas
- `GET /requests/mine` → `CLIENT_USER` apenas

### JWT Token

- **Formato:** `Bearer {token}`
- **Claims:** userId, email, role, companyId
- **Expiração:** 24 horas (configurável via `jwt.expiration-hours`)
- **Secret:** Configurável via `jwt.secret` (altere em produção!)

---

## Configurações

### application.properties

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5433/reviso
spring.datasource.username=reviso
spring.datasource.password=reviso

# JWT (sobrescreva em produção)
jwt.secret=change-me-in-production-use-a-long-random-secret-key-at-least-256-bits
jwt.expiration-hours=24

# Timezone
spring.jackson.time-zone=America/Sao_Paulo
```

---

## Testes Rápidos

### Criar usuário admin inicial (via SQL)

```sql
INSERT INTO companies (id, name, company_type, active, created_at)
VALUES ('00000000-0000-0000-0000-000000000001', 'Agência Reviso', 'AGENCY', true, now());

INSERT INTO users (id, full_name, email, password_hash, role, company_id, active, created_at)
VALUES (
  '00000000-0000-0000-0000-000000000002',
  'Admin Reviso',
  'admin@reviso.com',
  '$2a$10$N9qo8uLOickgx2ZMRZoMye.Jx3/kfRYPZCjQ3vZGZY.2K1zVvQmIy', -- senha: admin123
  'AGENCY_ADMIN',
  '00000000-0000-0000-0000-000000000001',
  true,
  now()
);
```

Depois faça login:

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@reviso.com","password":"admin123"}'
```

---

## Próximos Passos (Opcional)

- [ ] Refresh token
- [ ] Paginação nos endpoints de listagem
- [ ] Filtros avançados em `/agency/briefings`
- [ ] Upload de arquivos em briefings
- [ ] Notificações por email
- [ ] Logs de auditoria
- [ ] Rate limiting

---

**Desenvolvido com:** Spring Boot 4.0.1 + PostgreSQL 16 + JWT  
**Status:** ✅ Pronto para uso
