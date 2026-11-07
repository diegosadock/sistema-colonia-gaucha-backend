# Arquitetura

## Visão geral

O backend é um monólito modular organizado pelo domínio. A raiz Java é `br.com.coloniagaucha.backend`; `shared` contém convenções realmente comuns e `modules` contém os bounded contexts `auth`, `product`, `supplier` e `stock`.

Cada módulo usa:

- `domain`: entidades e invariantes sem Spring, HTTP ou JPA;
- `application`: casos de uso e ports de entrada/saída;
- `adapter/in/web`: controllers e DTOs;
- `adapter/out/persistence`: entidades JPA e adapters dos ports;
- `adapter/out/security`: BCrypt, JWT e adaptação CSRF no módulo auth.

Entidades JPA nunca são retornadas pela API nem usadas como entidades de domínio. Controllers apenas validam o contrato HTTP, chamam casos de uso e convertem respostas. Use cases delimitam transações de comandos.

`ArchitectureRulesTest` usa ArchUnit para impedir dependências de Spring, JPA, Servlet, Jackson e adapters a partir de pacotes de domínio.

## Persistência

PostgreSQL 17 é a única implementação de banco. Flyway é responsável pelo esquema; `spring.jpa.hibernate.ddl-auto=validate` detecta divergências na inicialização e `spring.jpa.open-in-view=false` evita sessões abertas durante serialização.

| Migration | Conteúdo |
|---|---|
| `V1` | usuários, roles `ADMIN`/`STAFF` e associação |
| `V2` | produtos, chave única do nome e índices |
| `V3` | fornecedores e índices |
| `V4` | movimentos, FKs, checks, índices e trigger de imutabilidade |
| `V5` | sessões de refresh, hashes, famílias, rotação e revogação |

UUID é usado como identificador; timestamps são `TIMESTAMPTZ` e valores de preço/quantidade são `NUMERIC`. Saldo de estoque deriva da soma de movimentos, sem segunda fonte de verdade.

## Fronteiras entre módulos

Product expõe `ProductCatalogPort` para que estoque bloqueie a linha do produto antes de verificar o saldo e registrar um movimento. Esse contrato retorna apenas UUID e unidade; não expõe entidade ou repositório JPA. `ProductStockUsagePort` impede trocar unidade após existir histórico. Supplier também expõe somente um contrato de consulta de estado ativo.

Em uma operação de estoque, a transação bloqueia a linha do produto com `SELECT ... FOR UPDATE`, calcula o saldo no PostgreSQL e grava o movimento. Todas as operações desse tipo usam o mesmo lock; isso serializa saídas concorrentes para o mesmo produto sem depender de locks em memória.

## Profiles e infraestrutura

- `dev`: CORS local, cookie sem `Secure` para HTTP local e chave efêmera explícita; tokens são invalidados ao reiniciar.
- `test`: Testcontainers PostgreSQL 17, sem inicialização automática de Compose e chave somente de teste.
- `prod`: cookie `Secure`, chaves JWT estáveis por configuração segura, CORS explícito e nenhum segredo padrão.

Compose pode iniciar só o banco para execução da API pela IDE ou usar o profile `full` para subir ambos. `SPRING_DOCKER_COMPOSE_ENABLED=false` no container evita a aplicação tentar iniciar outra stack dentro do container.
