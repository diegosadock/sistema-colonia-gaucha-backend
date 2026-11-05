# Restaurant Management

Sistema de gestão operacional para restaurantes, com foco planejado em estoque, fornecedores,
importação de notas fiscais, rastreabilidade de lotes, validade, perdas e relatórios. O projeto
começa como um monólito modular: uma API e um PostgreSQL, organizados por domínio.

Esta Fase 0 prepara o ambiente de desenvolvimento. Ainda não há CRUDs, autenticação, regras fiscais
ou aplicação frontend.

## Stack

- Python 3.13, FastAPI e Pydantic v2
- PostgreSQL, SQLAlchemy 2.x e Alembic
- Pytest, HTTPX e Ruff
- uv para dependências e lockfile
- Docker Compose para o ambiente local
- Next.js + TypeScript planejado para uma fase futura

## Pré-requisitos

- Python 3.13
- [uv](https://docs.astral.sh/uv/getting-started/installation/)
- Docker Desktop com Docker Compose
- Git

## Estrutura

```text
apps/
  api/                 FastAPI, persistência, migrations e testes
    app/
      api/v1/           Routers HTTP versionados
      core/             Configuração e tratamento de erros
      db/               Engine, sessões e base SQLAlchemy
      modules/          Convenções e futuros módulos de domínio
    alembic/            Ambiente e revisões do Alembic
    tests/
      unit/             Testes unitários de configuração e lógica isolada
      integration/      Testes HTTP com HTTPX
docs/
  ARCHITECTURE.md      Decisões e convenções de arquitetura
compose.yaml           API e PostgreSQL para desenvolvimento
```

## Variáveis de ambiente

Na raiz do repositório, copie o exemplo e ajuste os valores locais:

```powershell
Copy-Item .env.example .env
```

O arquivo `.env` é ignorado pelo Git. Os valores do exemplo são credenciais fictícias de
desenvolvimento; substitua-os antes de usar qualquer ambiente compartilhado. `DATABASE_URL` é a URL
para executar a API diretamente no host. No Compose, o serviço `api` usa o host interno `db`.

`CORS_ORIGINS` é uma lista JSON, por exemplo `["http://localhost:3000"]`.

## Instalação e execução local

```powershell
cd apps/api
uv sync --dev
uv run uvicorn app.main:app --reload
```

Para executar localmente, o PostgreSQL deve estar disponível no endereço informado em `DATABASE_URL`.
Como alternativa, suba a stack completa pelo Compose.

## Docker Compose

A partir da raiz do repositório:

```powershell
Copy-Item .env.example .env # apenas na primeira execução
docker compose up --build
```

O PostgreSQL mantém os dados no volume `postgres_data`. A API espera o health check do banco antes de
iniciar e recarrega o código da aplicação durante o desenvolvimento.

- Health: <http://localhost:8000/health>
- Swagger UI: <http://localhost:8000/docs>
- ReDoc: <http://localhost:8000/redoc>
- OpenAPI: <http://localhost:8000/openapi.json>

Para parar os serviços, use `docker compose down`. Para também apagar os dados locais do banco,
execute `docker compose down --volumes`.

## Testes e qualidade

Dentro de `apps/api`:

```powershell
uv run pytest
uv run ruff check .
uv run ruff format --check .
```

Para aplicar a formatação automaticamente:

```powershell
uv run ruff format .
```

## Migrations

Com o banco disponível e `DATABASE_URL` configurada, execute em `apps/api`:

```powershell
uv run alembic upgrade head
uv run alembic revision --autogenerate -m "describe_schema_change"
```

Revise sempre a migration gerada antes de integrá-la. Nesta fase não há modelos de domínio nem
migration inicial. A infraestrutura de metadados já está conectada ao Alembic.

## Organização do código

Os routers traduzem HTTP em chamadas à aplicação. Schemas Pydantic descrevem contratos, services
concentram regras de negócio, repositories isolam consultas e models SQLAlchemy representam dados
persistidos. Essas camadas devem ser adicionadas quando um caso de uso precisar delas; uma operação
simples não precisa de arquivos sem responsabilidade real. Veja [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Fluxo Git e Pull Requests

- `main` é a branch principal e recebe mudanças por Pull Request.
- Use `feat/nome-da-feature` para funcionalidades e `fix/nome-do-bug` para correções.
- Escreva mensagens com Conventional Commits, como `feat(inventory): add stock adjustment`.
- Abra PRs pequenos, descreva a validação e solicite revisão cruzada entre Diego e Erik.
- Integre somente depois da revisão e dos checks acordados.

O modelo de PR está em `.github/pull_request_template.md`.

## Conceitos para quem vem do Spring Boot

FastAPI usa funções Python para declarar rotas e dependências, em vez de controllers e injeção de
dependências por classes. O `Depends` funciona como composição explícita de dependências. Pydantic
valida contratos na borda HTTP; SQLAlchemy mapeia tabelas e fornece sessões, enquanto Alembic faz o
papel de versionamento de schema. Não há um contêiner de DI global obrigatório: a aplicação e suas
dependências são montadas explicitamente.
