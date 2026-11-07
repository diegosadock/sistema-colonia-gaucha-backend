# Módulo Product

## Responsabilidade e modelo

Catálogo cadastral de produtos, sem movimentações de estoque ou fornecedores. `Product` contém UUID, nome, descrição/categoria opcionais, unidade base, preço `BigDecimal`, estado ativo e timestamps. O preço é obrigatório e estritamente positivo, com até duas casas decimais (`NUMERIC(12,2)`).

Unidades base: `g`, `ml`, `un`. Os códigos são os mesmos na API, domínio e banco. Nome aplica NFKC, colapsa espaços e remove espaços externos; `name_key` em minúsculas possui unique constraint no PostgreSQL. O limite é 120 codepoints; a chave suporta expansão Unicode.

## Operações

Use cases: criar, consultar por UUID, listar, atualizar parcialmente, desativar e reativar. Listagem filtra nome, categoria, ativo e unidade, usa paginação com máximo 100 e ordenação determinística no banco. Ativação/desativação é soft state; não há exclusão física.

ADMIN cria/atualiza/altera estado; ADMIN e STAFF consultam. Payloads e endpoints completos estão em [ENDPOINTS.md](../api/ENDPOINTS.md).

## Estoque

O módulo não mantém saldo nem movimentações. `ProductCatalogPort` permite que Stock obtenha estado ativo e unidade sob lock. Após existir um movimento, a unidade do produto não pode mudar, preservando a interpretação histórica das quantidades.

Produto utilizado em estoque nunca é apagado fisicamente. A constraint `uk_products_name_key` protege duplicidade também quando requisições concorrentes passam pela checagem da aplicação.
