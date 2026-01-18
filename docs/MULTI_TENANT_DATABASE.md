# Multi-Tenant Database Implementation

## Arquitetura

Implementação de **database-per-tenant** usando Spring Boot + PostgreSQL.

### Componentes

1. **TenantContext** - ThreadLocal para armazenar o database name do tenant atual
2. **MultiTenantDataSource** - AbstractRoutingDataSource que roteia conexões baseado no tenant
3. **TenantInterceptor** - Intercepta requests e define o tenant baseado no JWT
4. **DataSourceConfig** - Configura o datasource multi-tenant
5. **TenantProvisioningService** - Cria databases e executa migrations por tenant

### Fluxo

1. Request chega com JWT contendo `agencyId`
2. TenantInterceptor extrai `agencyId` e busca `database_name` da agency
3. TenantContext armazena o `database_name` na thread
4. MultiTenantDataSource roteia a conexão para o database correto
5. Após o request, TenantContext é limpo

## Uso

### Provisionar database para uma agência

```bash
POST /admin/tenants/{agencyId}/provision
Authorization: Bearer <token>
```

Isso irá:
- Criar um novo database PostgreSQL
- Executar todas as migrations Flyway
- Atualizar a agency com o `database_name`

### Database naming

Formato: `tenant_{normalized_name}_{agency_id_prefix}`

Exemplo: `tenant_revisoagen_11111111`

## Configuração

No `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/reviso
spring.datasource.username=reviso
spring.datasource.password=reviso123
```

O database principal (`reviso`) armazena apenas a tabela `agencies` com o mapeamento de `agency_id` → `database_name`.

Todos os outros dados (companies, users, requests, briefings) ficam nos databases dos tenants.

## Limitações atuais

- Datasources são criados dinamicamente mas não são cacheados (TODO: implementar cache)
- Não há suporte para migração de dados existentes (TODO: script de migração)
- Connection pooling é básico (10 max, 2 min por tenant)

## Próximos passos

1. Implementar cache de datasources
2. Script de migração de dados do shared database para tenant databases
3. Monitoramento de conexões por tenant
4. Backup automático por tenant
