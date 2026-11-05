# Arquitetura

## Forma da aplicação

O backend começa como um monólito modular: uma aplicação FastAPI, um processo de API e uma base
PostgreSQL. Os limites dos módulos acompanham capacidades do domínio, como produtos, fornecedores,
estoque, notas fiscais, lotes, perdas e relatórios. Cada módulo pode ter suas próprias regras e
persistência sem introduzir chamadas de rede ou operação distribuída.

Essa forma mantém deploy, depuração e transações simples para uma equipe inicial de duas pessoas.
Se um módulo vier a exigir escala, disponibilidade ou ciclo de entrega independentes, os limites
internos permitirão avaliar sua extração com evidência. Não há microsserviços nesta fase.

## Dependências e configuração

`app.main` é o ponto de entrada ASGI. `app.factory.create_app` monta FastAPI, documentação, CORS,
handlers de erro e o router `/api/v1`. As configurações vivem em `app.core.config.Settings`, lidas do
ambiente e, no desenvolvimento local, de `.env`. Essa separação permite construir instâncias de app
isoladas nos testes.

`GET /health` confirma apenas que o processo HTTP está disponível. Ele não consulta o banco, então
seu resultado não substitui o health check do PostgreSQL nem uma verificação de dependências.

## Organização por domínio

- **Router:** contrato HTTP, códigos de status e dependências da requisição.
- **Schema:** entrada e saída validadas com Pydantic.
- **Service:** regras e coordenação de casos de uso.
- **Repository:** consultas e persistência quando o acesso a dados justificar essa separação.
- **Model:** mapeamento das entidades persistidas com SQLAlchemy.

Essas peças são introduzidas por necessidade. Não se cria uma camada vazia para cada tipo de arquivo.
O router versionado fica em `app/api/v1`; os módulos e suas responsabilidades ficam em
`app/modules/<dominio>`.

## Persistência

O projeto usa SQLAlchemy 2.x síncrono com o driver Psycopg 3. `app.db.base.Base` concentra os
metadados declarativos; `app.db.session` cria o engine com verificação de conexão e a fábrica de
sessões. `get_db` provê e fecha uma sessão por uso. Alembic importa `Base.metadata` para que as
revisões futuras possam comparar os modelos com o schema.

Não há entidades de negócio na Fase 0. O URL do banco vem de `DATABASE_URL`; não se versionam
credenciais de ambiente.

## Erros e observabilidade inicial

Erros HTTP, validação de entrada e falhas não tratadas usam um envelope JSON consistente. Detalhes
de erros internos são registrados no log do servidor, mas não enviados ao cliente. A resposta de
validação omite valores de entrada para evitar expor dados enviados pelo usuário.

## Evolução SaaS

Multiestabelecimento é uma decisão futura de domínio e segurança, não somente uma coluna adicionada
às tabelas. Antes de habilitá-lo, será necessário definir identidade do estabelecimento, isolamento
de consultas, autorização, unicidade composta, auditoria e estratégia de migração. A estrutura atual
mantém essa possibilidade em vista sem antecipar abstrações sem requisitos.
