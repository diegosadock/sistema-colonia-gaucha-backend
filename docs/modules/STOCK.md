# Módulo Stock

## Movimento e saldo

`StockMovement` é um evento imutável com UUID, produto, fornecedor opcional, tipo, quantidade `NUMERIC(18,3)`, justificativa opcional, snapshot da unidade, timestamp e usuário autenticado que registrou.

- `ENTRY`: quantidade positiva aumenta saldo; fornecedor opcional.
- `EXIT`: quantidade positiva reduz saldo.
- `ADJUSTMENT`: delta assinado não nulo; justificativa obrigatória.

Entrada e saída com sinal negativo são rejeitadas. Saldo é agregado do histórico em PostgreSQL; não há coluna concorrente que possa divergir. Migrations adicionam constraints, índices e trigger para bloquear UPDATE/DELETE.

## Transação e concorrência

Para criar movimento, Stock solicita via `ProductCatalogPort` um lock pessimista na linha ativa do produto, consulta o saldo atual e grava o evento dentro da mesma transação. Uma saída que deixaria saldo negativo falha com `400`. Duas saídas simultâneas para o mesmo produto são serializadas pelo PostgreSQL; locks JVM não são usados.

Unidade é definida pelo Product e copiada como snapshot no movimento. Product bloqueia mudança de unidade após o primeiro evento. Fornecedor só pode aparecer em entrada e precisa estar ativo. `createdBy` é obtido do subject do access token.

## Consultas e autorização

ADMIN e STAFF podem registrar movimentos e consultar histórico/saldos. Histórico oferece paginação, busca por produto/tipo/fornecedor/período e ordenação. `/balances` retorna página agregada com produtos sem movimento em saldo zero; `?productId={uuid}` retorna um produto específico.

Não existe edição ou exclusão HTTP de movimento. Correções devem ser novo ajuste justificado. Contratos estão em [ENDPOINTS.md](../api/ENDPOINTS.md).
