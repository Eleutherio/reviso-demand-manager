# Deploy

## Ambientes

### Development
```bash
docker compose up -d --build
```

- Frontend: http://localhost:4200
- Backend: http://localhost:8080
- PostgreSQL: localhost:5433

### Production

Requer:
- PostgreSQL 16+
- Java 21+
- Node 20+ (build do frontend)
- HTTPS (Let's Encrypt, Cloudflare, etc)

## Variáveis de Ambiente

### Obrigatórias

```bash
# Database
DB_NAME=reviso_prod
DB_USER=reviso_prod
DB_PASSWORD=senha_forte_aqui

# JWT (gerar com: openssl rand -base64 64)
JWT_SECRET=seu_secret_forte_256bits_minimo

# Stripe
STRIPE_API_KEY=sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...

# Email (Resend)
RESEND_API_KEY=re_...
RESEND_FROM=noreply@seudominio.com
```

### Opcionais

```bash
# Portas
DB_PORT=5432
API_PORT=8080
FRONTEND_PORT=4200

# Frontend URL (para redirect do backend)
FRONTEND_BASE_URL=https://seudominio.com

# JWT
JWT_EXPIRATION_HOURS=24

# Email
MAIL_REPLY_TO=contato@seudominio.com
RESEND_BASE_URL=https://api.resend.com
RESEND_TIMEOUT_SECONDS=10

# Email Queue
EMAIL_QUEUE_MAX_ATTEMPTS=5
EMAIL_QUEUE_RETRY_DELAY_SECONDS=60
EMAIL_QUEUE_QUOTA_DELAY_MINUTES=60
EMAIL_QUEUE_BATCH_SIZE=20
EMAIL_QUEUE_FIXED_DELAY_MS=30000

# Recovery
AGENCY_PASSWORD_RECOVERY_TOKEN_TTL_MINUTES=15
```

## Setup Stripe

### 1. Criar Produtos

Dashboard Stripe → Products → New:

**Starter**
- Nome: Starter
- Preço: R$ 99/mês
- Copiar `price_id`

**Professional**
- Nome: Professional
- Preço: R$ 299/mês
- Copiar `price_id`

**Enterprise**
- Nome: Enterprise
- Preço: R$ 999/mês
- Copiar `price_id`

### 2. Atualizar Banco

```sql
UPDATE subscription_plans 
SET stripe_price_id = 'price_...' 
WHERE code = 'STARTER';

UPDATE subscription_plans 
SET stripe_price_id = 'price_...' 
WHERE code = 'PROFESSIONAL';

UPDATE subscription_plans 
SET stripe_price_id = 'price_...' 
WHERE code = 'ENTERPRISE';
```

### 3. Configurar Webhook

Dashboard Stripe → Webhooks → Add endpoint:

**URL**: `https://seudominio.com/onboarding/webhook/stripe`

**Events**:
- `checkout.session.completed`
- `invoice.paid`
- `customer.subscription.updated`
- `customer.subscription.deleted`

**Signing secret**: Copiar `whsec_...` e configurar em `STRIPE_WEBHOOK_SECRET`.

### 4. Testar Webhook

```bash
# Instalar Stripe CLI
stripe listen --forward-to localhost:8080/onboarding/webhook/stripe

# Trigger evento de teste
stripe trigger checkout.session.completed
```

## Setup Resend

1. Criar conta: https://resend.com
2. Verificar domínio (DNS records)
3. Criar API key
4. Configurar `RESEND_API_KEY` e `RESEND_FROM`

## Build

### Backend

```bash
cd backend
./mvnw clean package -DskipTests
java -jar target/reviso-demand-manager-backend-0.0.1-SNAPSHOT.jar
```

### Frontend

```bash
cd frontend
npm install
npm run build
```

Servir `frontend/dist/` com Nginx ou CDN.

## Nginx (Exemplo)

```nginx
server {
    listen 80;
    server_name seudominio.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name seudominio.com;

    ssl_certificate /etc/letsencrypt/live/seudominio.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/seudominio.com/privkey.pem;

    # Frontend
    location / {
        root /var/www/reviso/frontend/dist;
        try_files $uri $uri/ /index.html;
    }

    # Backend API
    location /api/ {
        proxy_pass http://localhost:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## Database Migrations

Flyway roda automaticamente no startup.

Migrations: `backend/src/main/resources/db/migration/V*.sql`

**Rollback manual** (se necessário):
```sql
-- Ver histórico
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC;

-- Reverter última migration (CUIDADO!)
DELETE FROM flyway_schema_history WHERE version = 'X';
-- Reverter mudanças manualmente
```

## Seed Data

**Development**:
```bash
docker compose exec -T postgres psql -U reviso -d reviso < db/seed_data.sql
```

**Production**: NÃO usar seed. Criar dados via API.

## Health Check

```bash
curl https://seudominio.com/api/actuator/health
```

Resposta esperada:
```json
{
  "groups": ["liveness", "readiness"],
  "status": "UP"
}
```

## Monitoramento

### Logs

Backend:
```bash
tail -f logs/spring.log
```

PostgreSQL:
```bash
tail -f /var/log/postgresql/postgresql-16-main.log
```

### Métricas

Actuator endpoints (proteger em produção):
- `/actuator/health`
- `/actuator/info`

### Alertas

Configurar:
- Erro 500 > 10/min
- Webhook falha > 5/min
- Database connection pool > 80%
- Disk space < 20%

## Backup

### Database

```bash
# Backup diário
pg_dump -U reviso_prod reviso_prod > backup_$(date +%Y%m%d).sql

# Restore
psql -U reviso_prod reviso_prod < backup_20260119.sql
```

### Arquivos

Se houver upload de arquivos, backup de:
- `/var/www/reviso/uploads/`

## Rollback

1. Parar aplicação
2. Restore database backup
3. Deploy versão anterior
4. Reiniciar aplicação

## Checklist Deploy

- [ ] Variáveis de ambiente configuradas
- [ ] `JWT_SECRET` forte (64+ chars)
- [ ] Stripe produtos criados e `price_id` atualizados
- [ ] Stripe webhook configurado e testado
- [ ] Resend domínio verificado
- [ ] HTTPS habilitado
- [ ] CORS configurado com domínio real
- [ ] Nginx configurado
- [ ] Database migrations aplicadas
- [ ] Health check respondendo
- [ ] Logs configurados
- [ ] Backup automático configurado
- [ ] Monitoramento ativo
