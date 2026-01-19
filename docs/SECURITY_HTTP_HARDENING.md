# HTTP Security Hardening

## Security Headers Implementados

### 1. Strict-Transport-Security (HSTS)
```nginx
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
```
- Força HTTPS por 1 ano
- Inclui subdomínios
- Preload: elegível para lista HSTS do navegador

### 2. Content-Security-Policy (CSP)
```nginx
add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'" always;
```

**Política:**
- `default-src 'self'`: Apenas recursos do mesmo domínio
- `script-src 'self'`: Scripts apenas do domínio
- `style-src 'self' 'unsafe-inline'`: CSS inline permitido (Angular)
- `img-src 'self' data: https:`: Imagens locais, data URIs e HTTPS
- `frame-ancestors 'none'`: Não pode ser embedado em iframe
- `form-action 'self'`: Forms apenas para mesmo domínio

### 3. X-Frame-Options
```nginx
add_header X-Frame-Options "DENY" always;
```
- Previne clickjacking
- Redundante com CSP `frame-ancestors 'none'`

### 4. X-Content-Type-Options
```nginx
add_header X-Content-Type-Options "nosniff" always;
```
- Previne MIME sniffing
- Força respeitar Content-Type

### 5. X-XSS-Protection
```nginx
add_header X-XSS-Protection "1; mode=block" always;
```
- Ativa filtro XSS do navegador (legacy)
- CSP é mais efetivo

### 6. Referrer-Policy
```nginx
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
```
- Envia apenas origem em cross-origin
- Previne vazamento de URLs sensíveis

### 7. Permissions-Policy
```nginx
add_header Permissions-Policy "geolocation=(), microphone=(), camera=()" always;
```
- Desabilita APIs sensíveis
- Geolocalização, microfone, câmera bloqueados

### 8. Server Tokens
```nginx
server_tokens off;
```
- Oculta versão do Nginx
- Dificulta reconnaissance

## Teste de Headers

### Online
- https://securityheaders.com
- https://observatory.mozilla.org

### CLI
```bash
curl -I https://api.seudominio.com | grep -i "x-\|content-security\|strict-transport\|permissions"
```

### Esperado
```
strict-transport-security: max-age=31536000; includeSubDomains; preload
x-frame-options: DENY
x-content-type-options: nosniff
x-xss-protection: 1; mode=block
referrer-policy: strict-origin-when-cross-origin
permissions-policy: geolocation=(), microphone=(), camera=()
content-security-policy: default-src 'self'; ...
```

## Ajustes para Frontend

Se servir frontend Angular no mesmo domínio, ajustar CSP:

```nginx
# Para Angular com inline styles
add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self'; connect-src 'self' https://api.stripe.com; frame-ancestors 'none'; base-uri 'self'; form-action 'self'" always;
```

**Nota**: `connect-src` inclui `https://api.stripe.com` se usar Stripe.js no frontend.

## Hardening Adicional

### 1. Ocultar Server Header
```nginx
# nginx.conf (http block)
more_clear_headers 'Server';
```
Requer módulo `headers-more-nginx-module`.

### 2. Limitar Métodos HTTP
```nginx
if ($request_method !~ ^(GET|POST|PUT|PATCH|DELETE|OPTIONS)$ ) {
    return 405;
}
```

### 3. Timeout Agressivo
```nginx
client_body_timeout 10s;
client_header_timeout 10s;
keepalive_timeout 30s;
send_timeout 10s;
```

## Checklist

- [x] HSTS habilitado (1 ano)
- [x] CSP configurado
- [x] X-Frame-Options: DENY
- [x] X-Content-Type-Options: nosniff
- [x] Referrer-Policy configurado
- [x] Permissions-Policy configurado
- [x] server_tokens off
- [ ] Testar em securityheaders.com
- [ ] Ajustar CSP se necessário (frontend)
- [ ] Considerar HSTS preload

## Score Esperado

**securityheaders.com**: A ou A+
**Mozilla Observatory**: A ou A+
