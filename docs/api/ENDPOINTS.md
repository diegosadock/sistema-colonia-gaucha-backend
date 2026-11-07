# Contratos HTTP

Base local: `http://localhost:8080`. As rotas de negócio exigem `Authorization: Bearer <accessToken>` e role indicada. JSON de erro segue `application/problem+json` com `type`, `title`, `status`, `detail` e `code`.

## Auth

| Método | Caminho | Permissão | Resposta |
|---|---|---|---|
| GET | `/api/v1/auth/csrf` | público | cookie `XSRF-TOKEN` e nomes de cookie/header |
| POST | `/api/v1/auth/login` | público + CSRF | 200, access token e usuário público; refresh por cookie HttpOnly |
| POST | `/api/v1/auth/refresh` | público + CSRF | 200, novo access token e refresh cookie rotacionado |
| POST | `/api/v1/auth/logout` | público + CSRF | 204, revoga família e apaga cookie |
| GET | `/api/v1/auth/me` | autenticado | 200, usuário e roles do JWT |

Login recebe `{ "email": "admin@example.com", "password": "..." }`. Refresh não aceita token no body; usa apenas o cookie.

## Produtos

| Método | Caminho | Role | Resposta |
|---|---|---|---|
| POST | `/api/v1/products` | ADMIN | 201 |
| GET | `/api/v1/products` | ADMIN, STAFF | 200 paginado |
| GET | `/api/v1/products/{id}` | ADMIN, STAFF | 200 |
| PATCH | `/api/v1/products/{id}` | ADMIN | 200 |
| PATCH | `/api/v1/products/{id}/deactivate` | ADMIN | 204 |
| PATCH | `/api/v1/products/{id}/reactivate` | ADMIN | 204 |

Exemplo de criação:

```json
{
  "name": "Farinha de trigo",
  "description": "Tipo 1",
  "category": "Mercearia",
  "unitOfMeasure": "g",
  "price": "5.90"
}
```

Preço precisa ser maior que zero e ter até duas casas decimais. Unidades aceitas: `g`, `ml`, `un`. Listagem usa `page` (padrão 0), `size` (padrão 20, máximo 100), `sort`, `direction`, `name`, `category`, `active` e `unitOfMeasure`. Nome normalizado é único inclusive para produto inativo.

## Fornecedores

| Método | Caminho | Role | Resposta |
|---|---|---|---|
| POST | `/api/v1/suppliers` | ADMIN | 201 |
| GET | `/api/v1/suppliers` | ADMIN, STAFF | 200 paginado |
| GET | `/api/v1/suppliers/{id}` | ADMIN, STAFF | 200 |
| PATCH | `/api/v1/suppliers/{id}` | ADMIN | 200 |
| PATCH | `/api/v1/suppliers/{id}/deactivate` | ADMIN | 204 |
| PATCH | `/api/v1/suppliers/{id}/reactivate` | ADMIN | 204 |

Exemplo: `{ "name": "Distribuidora Exemplo", "email": "compras@example.com", "phone": "+55 51 3333-0000" }`. E-mail e telefone são opcionais. PATCH atualiza campos não nulos.

## Estoque

| Método | Caminho | Role | Resposta |
|---|---|---|---|
| POST | `/api/v1/stock/movements` | ADMIN, STAFF | 201 |
| GET | `/api/v1/stock/movements` | ADMIN, STAFF | 200 paginado |
| GET | `/api/v1/stock/balances?productId={id}` | ADMIN, STAFF | 200 saldo do produto |
| GET | `/api/v1/stock/balances` | ADMIN, STAFF | 200 página de saldos |

Exemplos:

```json
{ "productId": "UUID", "type": "ENTRY", "quantity": "12.500", "supplierId": "UUID" }
```

```json
{ "productId": "UUID", "type": "ADJUSTMENT", "quantity": "-0.500", "reason": "Contagem física" }
```

`ENTRY` e `EXIT` recebem magnitude positiva; `ADJUSTMENT` é delta assinado não nulo e requer justificativa. Fornecedor só se aplica a entrada. `createdBy` vem do subject autenticado e não é aceito do cliente. Filtros de histórico: `productId`, `movementType`, `supplierId`, `from`, `to`, paginação e ordenação.

## Códigos

- `400`: Bean Validation ou regra de domínio;
- `401`: credencial, ausência ou invalidade do token;
- `403`: role insuficiente ou CSRF inválido;
- `404`: recurso inexistente/inativo exigido;
- `409`: constraint/conflict de dados;
- `500`: erro interno sem detalhes de SQL ou stack trace.
