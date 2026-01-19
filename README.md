# Reviso Demand Manager

B2B demand management system for advertising agencies. Tracks client requests from briefing to delivery with audit trail and metrics.

**Stack**: Java 21, Spring Boot 4.0.1, PostgreSQL 16, Angular 19

## Quick Start

### Development (localhost)
```bash
cd infra/dev
cp .env.dev.example .env.dev
# Edit .env.dev and set DB_PASSWORD

docker compose -f docker-compose.dev.yml up -d --build

# Seed database
docker compose -f docker-compose.dev.yml exec -T postgres \
  psql -U reviso -d reviso < ../../db/seed_data.sql
```

- Frontend: http://localhost:4200
- Backend: http://localhost:8080
- PostgreSQL: localhost:5433

### Demo/Production (public)
```bash
cd infra/demo
cp .env.demo.example .env.demo
# Edit .env.demo with real values

# Setup Nginx + SSL
chmod +x nginx/setup.sh
./nginx/setup.sh

# Deploy
chmod +x deploy-demo.sh
./deploy-demo.sh

# Hardening (SSH + Database)
chmod +x harden-ssh.sh setup-db-security.sh
./harden-ssh.sh
./setup-db-security.sh
```

- API: https://api.seudominio.com

## Project Structure

```
backend/          Spring Boot API
frontend/         Angular SPA
infra/
  dev/            Development environment
  demo/           Demo/production environment
docs/             Documentation
db/               Database seeds
```

## Environments

| Environment | Profile | CORS | Logs | DB Port | Nginx |
|-------------|---------|------|------|---------|-------|
| **Dev** | `dev` | localhost | DEBUG | 5433 | No |
| **Demo** | `demo` | Domain only | INFO | Not exposed | Yes |

## Documentation

- [Architecture](docs/ARCHITECTURE.md) - System architecture and multi-tenancy
- [API Reference](docs/API.md) - Endpoints and examples
- [Onboarding Flow](docs/ONBOARDING.md) - SaaS signup with Stripe
- [Security](docs/SECURITY.md) - Auth, webhooks, secrets, rate limiting
- [Deployment](docs/DEPLOY.md) - Production setup
- [Runbook](docs/RUNBOOK.md) - Troubleshooting guide
- [Infrastructure](infra/README.md) - Environment setup

### Security Guides
- [Security Checklist](docs/SECURITY_CHECKLIST_DEMO.md) - Complete demo checklist
- [Rate Limiting](docs/SECURITY_RATE_LIMITING.md) - Strategy and monitoring
- [HTTP Hardening](docs/SECURITY_HTTP_HARDENING.md) - Security headers

## Request Workflow

```
NEW → IN_PROGRESS → IN_REVIEW → CHANGES_REQUESTED ↔ IN_PROGRESS
                              → APPROVED → DELIVERED → CLOSED
```

## License

Private/Experimental
