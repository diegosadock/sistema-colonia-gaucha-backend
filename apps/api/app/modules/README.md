# Módulos de domínio

Os módulos de negócio serão criados aqui quando cada funcionalidade começar. Os nomes previstos são
`products`, `suppliers`, `inventory`, `invoices`, `batches`, `losses` e `reports`.

Cada módulo deve começar pequeno. Adicione router, schemas, service, repository e models conforme uma
necessidade real surgir; não crie camadas vazias. Os routers HTTP ficam registrados em
`app/api/v1/router.py`, enquanto decisões e regras do domínio permanecem no módulo correspondente.
