# Segurança

## Autenticação

**JWT** com HS384, 24h validade.

Claims: `sub` (userId), `email`, `role`, `companyId`, `agencyId`.

Secret mínimo 256 bits:
```bash
openssl rand -base64 64
```

## Senhas

**BCrypt** strength 10.

- ✅ Hash antes de salvar
- ❌ Nunca plaintext no banco
- ❌ Nunca em logs ou APIs externas

## Rate Limiting

**Nginx:**
- `/auth/*`: 10/min por IP
- `/recover-*`: 5/min por IP

**Aplicação:**
- Login: 10/min por IP + 10/min por email
- Recovery: 5/min por IP + 5/min por email
- Signup: 3/min por IP

**Logs:** Failed/successful login attempts com IP e email.

Ver [SECURITY_RATE_LIMITING.md](SECURITY_RATE_LIMITING.md).

## Webhook Stripe

**Validação:** SDK oficial (`stripe-java`), HMAC SHA256, timestamp 5 min.

**Idempotência:** Constraint único `event_id`.

```java
try {
    webhookEventRepository.save(new StripeWebhookEvent(eventId));
} catch (DataIntegrityViolationException e) {
    return 200; // Já processado
}
```

## Secrets

**Variáveis obrigatórias (demo):**
```bash
JWT_SECRET=...
DB_PASSWORD=...
STRIPE_API_KEY=...
STRIPE_WEBHOOK_SECRET=...
RESEND_API_KEY=...
```

Aplicação falha se não configurados (`${VAR:?required}`).

## HTTP Security

**Headers:** HSTS preload, CSP, X-Frame-Options, Permissions-Policy, server_tokens off.

Ver [SECURITY_HTTP_HARDENING.md](SECURITY_HTTP_HARDENING.md).

## Container Security

**PostgreSQL:**
- User: postgres (non-root)
- SCRAM-SHA-256 auth
- no-new-privileges
- Porta não exposta (demo)

**Backend:**
- User: 1000:1000 (non-root)
- no-new-privileges
- Bind 127.0.0.1 (demo)

## SSH & Host

- Key-only auth (password disabled)
- Root login disabled
- Fail2ban (SSH + Nginx)
- Unattended-upgrades
- Firewall (UFW): 22, 80, 443

Ver [SECURITY_CHECKLIST_DEMO.md](SECURITY_CHECKLIST_DEMO.md).

## LGPD

**Armazenado:** Nome, email, hash senha, Stripe IDs.

**NÃO armazenado:** Senha plaintext, dados pagamento.

**Retenção:** pending_signups 24h, demais indefinido.

## Checklist Produção

Ver [SECURITY_CHECKLIST_DEMO.md](SECURITY_CHECKLIST_DEMO.md).
