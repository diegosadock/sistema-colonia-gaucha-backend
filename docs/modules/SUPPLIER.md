# Módulo Supplier

## Responsabilidade

Mantém nome e dados de contato opcionais (e-mail e telefone), ativo e timestamps. Dados fiscais não foram adicionados sem requisito. Fornecedores são desativados logicamente e preservam chaves usadas por movimentos históricos.

## Operações e autorização

ADMIN cria, atualiza, desativa e reativa; ADMIN e STAFF consultam. Listagem pagina até 100 itens, filtra nome e status e executa filtro/contagem no PostgreSQL. Dados inválidos retornam `400`; UUID inexistente retorna `404`.

Consulte [ENDPOINTS.md](../api/ENDPOINTS.md) para payloads completos. Stock valida por contrato de aplicação se um fornecedor enviado está ativo, sem acessar implementação JPA do módulo.
