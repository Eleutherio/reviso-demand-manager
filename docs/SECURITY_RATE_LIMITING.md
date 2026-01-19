# Segurança - Rate Limiting e Monitoramento

## Rate Limiting em Camadas

### 1. Nginx (Camada de Rede)
- `/auth/*`: 10 req/min por IP
- `/recover-*`: 5 req/min por IP
- Proteção contra DDoS básico

### 2. Aplicação (Camada de Negócio)
- **Login por IP**: 10 tentativas/min
- **Login por Email**: 10 tentativas/min
- Proteção contra credential stuffing distribuído

## Logs de Segurança

### Failed Login Attempts
```
WARN - Failed login attempt - email: user@example.com, ip: 192.168.1.1
WARN - Failed client login attempt - email: client@company.com, company: COMP-123, ip: 192.168.1.1
```

### Successful Logins
```
INFO - Successful login - email: user@example.com, ip: 192.168.1.1
INFO - Successful client login - email: client@company.com, company: COMP-123, ip: 192.168.1.1
```

## Monitoramento

### Alertas Recomendados

**Credential Stuffing:**
```bash
# Mais de 50 falhas de login em 5 minutos
grep "Failed login attempt" /var/log/reviso/application.log | \
  grep "$(date -d '5 minutes ago' '+%Y-%m-%d %H:%M')" | \
  wc -l
```

**Brute Force em Email Específico:**
```bash
# Mais de 10 falhas para mesmo email
grep "Failed login attempt - email: user@example.com" /var/log/reviso/application.log | \
  tail -20
```

**IPs Suspeitos:**
```bash
# IPs com mais falhas
grep "Failed login attempt" /var/log/reviso/application.log | \
  grep -oP 'ip: \K[0-9.]+' | \
  sort | uniq -c | sort -rn | head -10
```

## Proxy Confiável (Cloudflare/AWS)

Se usar proxy, configurar IP real:

**application-demo.yml:**
```yaml
server:
  forward-headers-strategy: native
  tomcat:
    remoteip:
      remote-ip-header: X-Forwarded-For
      protocol-header: X-Forwarded-Proto
```

**Nginx:**
```nginx
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
```

## Mitigação de Ataques

### Credential Stuffing
- ✅ Rate limit por email (10/min)
- ✅ Rate limit por IP (10/min)
- ✅ Logs de tentativas
- ⚠️ Considerar: CAPTCHA após 3 falhas

### Brute Force
- ✅ Rate limit duplo (IP + email)
- ✅ Mensagens genéricas (sem enumeração)
- ✅ Logs estruturados
- ⚠️ Considerar: Bloqueio temporário após 10 falhas

### Account Enumeration
- ✅ Mensagens genéricas ("Credenciais inválidas")
- ✅ Timing constante (bcrypt sempre executa)
- ✅ Recovery sempre retorna sucesso

## Checklist Produção

- [ ] Logs configurados em `/var/log/reviso/`
- [ ] Alertas de segurança configurados
- [ ] Monitoramento de IPs suspeitos
- [ ] Backup de logs (30 dias)
- [ ] Rate limit testado
- [ ] Proxy headers configurados (se aplicável)
