# Sistema Colônia Gaúcha — Backend

Backend Java 25 e Spring Boot 4.1.2 para operações de autenticação, produtos, fornecedores e estoque. A aplicação é um monólito modular: cada módulo mantém domínio, casos de uso e adapters próprios; PostgreSQL e Flyway são a fonte de verdade dos dados e do esquema.

## Requisitos

- JDK 25
- Docker Desktop com Compose
- O Maven Wrapper incluído (`mvnw.cmd` no Windows ou `./mvnw` no Linux/macOS)
- Docker disponível para executar os testes Testcontainers

## Banco e execução pela IDE

Copie o exemplo de ambiente, altere a senha local e inicie somente o PostgreSQL:

```powershell
Copy-Item .env.example .env
docker compose up -d db
```

Em seguida execute o Spring Boot pela IDE ou pelo wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

O starter Docker Compose do Spring Boot conecta a aplicação local ao serviço `db`. Flyway aplica as migrations na inicialização e Hibernate usa `ddl-auto=validate`.

Para executar API e banco em containers:

```powershell
docker compose --profile full up --build
```

No modo completo, a API fica em `http://localhost:8000`; rodando pela IDE, a porta padrão é `8080`. O Swagger UI fica em `/swagger-ui.html` e os documentos OpenAPI em `/v3/api-docs`.

## Configuração

As configurações ficam em `src/main/resources/application.yaml` e nos profiles `dev`, `test` e `prod`. O arquivo `.env.example` documenta as variáveis locais; copie para `.env`, que é ignorado pelo Git. Não use os valores de desenvolvimento em produção.

O profile `dev` permite uma chave RSA efêmera explícita para facilitar o desenvolvimento. Isso invalida access tokens ao reiniciar a aplicação. Em produção use `SPRING_PROFILES_ACTIVE=prod`, configure `JWT_PRIVATE_KEY` e `JWT_PUBLIC_KEY` com os valores PEM/DER em ambiente seguro e use HTTPS. Nunca coloque chaves privadas no repositório.

O primeiro ADMIN é provisionado somente quando `BOOTSTRAP_ADMIN_EMAIL` e `BOOTSTRAP_ADMIN_PASSWORD` são fornecidos ao iniciar a aplicação, e apenas se ainda não existir ADMIN. Use um segredo temporário e remova essas variáveis depois do provisionamento. Não há cadastro público.

## Testes e build

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
```

Os testes de persistência e HTTP usam PostgreSQL 17 pelo Testcontainers; não há substituição por H2. O Docker precisa estar disponível. As migrations do Flyway também são exercitadas durante a inicialização do contexto de teste.

## Módulos e decisões

- **Auth**: login com BCrypt, access token JWT RS256 de 15 minutos, refresh token opaco de 256 bits em cookie HttpOnly com rotação e revogação persistida por hash.
- **Product**: catálogo com nome normalizado e único, preço `BigDecimal`, unidades `g`, `ml`, `un`, atualização parcial e soft delete.
- **Supplier**: cadastro simples de fornecedor e dados de contato opcionais, com ativação/desativação lógica.
- **Stock**: histórico imutável de movimentos; saldo calculado pelo histórico. Entrada e saída recebem quantidade positiva; ajuste recebe delta assinado e justificativa.

As entidades de domínio não dependem de JPA, Spring ou HTTP. Ports de aplicação isolam a persistência e contratos compartilhados entre módulos não expõem repositórios JPA de outros módulos.

## Endpoints

Todas as rotas de negócio exigem access token Bearer e role adequada. Consulte [docs/api/ENDPOINTS.md](docs/api/ENDPOINTS.md) para payloads e filtros e [docs/security/AUTHENTICATION.md](docs/security/AUTHENTICATION.md) para login, CSRF, refresh e integração do frontend.

| Módulo | Caminhos |
|---|---|
| Auth | `/api/v1/auth/csrf`, `/login`, `/refresh`, `/logout`, `/me` |
| Product | `/api/v1/products` |
| Supplier | `/api/v1/suppliers` |
| Stock | `/api/v1/stock/movements`, `/api/v1/stock/balances` |

## Segurança e pendências do produto

O backend não inclui frontend, recuperação de senha, envio de e-mail nem cadastro público de usuários. JWT já emitido permanece utilizável até expirar (15 minutos); logout e detecção de reuso revogam refresh sessions, mas não invalidam access tokens emitidos antes.

Leia [docs/architecture/ARCHITECTURE.md](docs/architecture/ARCHITECTURE.md), [docs/modules/PRODUCT.md](docs/modules/PRODUCT.md), [docs/modules/SUPPLIER.md](docs/modules/SUPPLIER.md) e [docs/modules/STOCK.md](docs/modules/STOCK.md) para detalhes dos módulos.
