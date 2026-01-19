# Security Checklist - Demo Público

## 1. SSH Hardening

### Configuração
```bash
cd infra/demo
chmod +x harden-ssh.sh
./harden-ssh.sh
```

**Implementado:**
- ✅ Password authentication disabled
- ✅ Root login disabled
- ✅ Key-only authentication
- ✅ Fail2ban (SSH + Nginx rate limit)

**Verificar:**
```bash
sudo fail2ban-client status
sudo fail2ban-client status sshd
```

## 2. Database Security

### Senha Forte
```bash
# Gerar senha (mínimo 20 caracteres)
openssl rand -base64 32
```

### Backup Automático
```bash
cd infra/demo
chmod +x setup-db-security.sh
./setup-db-security.sh
```

**Implementado:**
- ✅ Backup diário (2 AM)
- ✅ Retenção 7 dias
- ✅ SCRAM-SHA-256 auth
- ✅ Container não-root (user: postgres)
- ✅ no-new-privileges
- ✅ Volume com permissões corretas

**Verificar:**
```bash
ls -lh /var/backups/reviso/
crontab -l | grep backup
```

## 3. Container Security

### PostgreSQL
- ✅ Sem porta exposta
- ✅ User: postgres (não root)
- ✅ SCRAM-SHA-256 authentication
- ✅ no-new-privileges
- ✅ tmpfs para /tmp e /run
- ✅ Volume read-write mínimo

### Backend
- ✅ User: 1000:1000 (não root)
- ✅ no-new-privileges
- ✅ Bind apenas 127.0.0.1

## 4. HTTP Security

**Headers:**
- ✅ HSTS (preload)
- ✅ CSP
- ✅ X-Frame-Options
- ✅ X-Content-Type-Options
- ✅ Referrer-Policy
- ✅ Permissions-Policy
- ✅ server_tokens off

**Testar:**
```bash
curl -I https://api.seudominio.com
```

## 5. Rate Limiting

**Nginx (Rede):**
- ✅ /auth/*: 10 req/min por IP
- ✅ /recover-*: 5 req/min por IP

**Aplicação (Negócio):**
- ✅ Login por IP: 10/min
- ✅ Login por Email: 10/min
- ✅ Logs de tentativas

## 6. Firewall (UFW)

```bash
sudo ufw status
```

**Esperado:**
- ✅ 22/tcp (SSH)
- ✅ 80/tcp (HTTP)
- ✅ 443/tcp (HTTPS)
- ❌ 5432/5433 (PostgreSQL) - NÃO exposto

## 7. Atualizações Automáticas

```bash
sudo systemctl status unattended-upgrades
```

**Implementado:**
- ✅ Security updates automáticos
- ✅ Kernel cleanup
- ✅ Sem reboot automático

## 8. Secrets Management

**Validação obrigatória (demo profile):**
- ✅ JWT_SECRET (fail if missing)
- ✅ DB_PASSWORD (fail if missing)
- ✅ STRIPE_API_KEY (fail if missing)
- ✅ STRIPE_WEBHOOK_SECRET (fail if missing)
- ✅ RESEND_API_KEY (fail if missing)

## 9. Monitoramento

### Logs de Segurança
```bash
# Failed logins
grep "Failed login attempt" /var/log/reviso/application.log

# Fail2ban bans
sudo fail2ban-client status sshd

# Nginx rate limits
grep "limiting requests" /var/log/nginx/reviso-api-error.log
```

### Alertas Recomendados
- [ ] Failed logins > 50/5min
- [ ] Fail2ban bans > 10/hour
- [ ] Disk usage > 80%
- [ ] Backup failures

## 10. Backup & Recovery

### Backup Manual
```bash
/usr/local/bin/backup-reviso-db.sh
```

### Restore
```bash
gunzip < /var/backups/reviso/reviso_20260119.sql.gz | \
  docker compose -f infra/demo/docker-compose.demo.yml exec -T postgres \
  psql -U reviso reviso
```

## Checklist Final

### Pré-Deploy
- [ ] SSH key configurada
- [ ] DB_PASSWORD forte (20+ chars)
- [ ] JWT_SECRET gerado (64+ chars)
- [ ] Stripe keys de produção
- [ ] Resend API key configurada
- [ ] DNS A record configurado

### Pós-Deploy
- [ ] SSH hardening executado
- [ ] Fail2ban ativo
- [ ] Backup automático configurado
- [ ] Unattended-upgrades ativo
- [ ] UFW configurado
- [ ] Nginx + SSL funcionando
- [ ] Headers testados (securityheaders.com)
- [ ] Rate limiting testado
- [ ] Logs funcionando
- [ ] Backup manual testado

### Manutenção Mensal
- [ ] Verificar backups
- [ ] Revisar logs de segurança
- [ ] Verificar Fail2ban bans
- [ ] Testar restore de backup
- [ ] Atualizar dependências
