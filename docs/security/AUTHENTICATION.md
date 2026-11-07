# Autenticação e segurança do frontend

## Tokens e sessão

`POST /api/v1/auth/login` valida e-mail, senha, estado ativo e senha BCrypt. A resposta JSON contém `accessToken`, `tokenType`, `expiresIn` e dados públicos mínimos do usuário. Access tokens são JWT RS256, expiram em 15 minutos e validam assinatura, expiração, issuer, audience e `token_use=access`; incluem `sub`, `iat`, `exp`, `jti` e roles.

O access token pertence somente à memória volátil do frontend. Não persista em localStorage, sessionStorage, IndexedDB, cookie ou URL. Envie-o no header `Authorization: Bearer <accessToken>` nas rotas de negócio.

O refresh token é uma string aleatória opaca de 32 bytes, não um JWT. A API envia somente por cookie `refresh_token` com `HttpOnly`, `Path=/api/v1/auth`, `SameSite=Lax`, validade padrão de sete dias e `Secure=true` em produção. O banco guarda apenas SHA-256, a família da sessão, expiração, rotação, revogação e indicador de reuso.

## CSRF e CORS

Antes de login/refresh/logout, chame `GET /api/v1/auth/csrf` com `credentials: "include"`. A resposta informa os nomes do cookie (`XSRF-TOKEN`) e do header (`X-XSRF-TOKEN`); o cookie CSRF é legível por JavaScript e diferente do refresh token HttpOnly. Leia seu valor e envie-o em `X-XSRF-TOKEN` em `POST /login`, `/refresh` e `/logout`.

As rotas de negócio usam Bearer token e têm CSRF ignorado; rotas auth que usam cookies continuam protegidas. CORS aceita somente origens configuradas em `CORS_ORIGINS`, com credentials; wildcard não é aceito. Em chamadas auth do browser, use `credentials: "include"`.

## Fluxos de integração

### Login

1. Obtenha `XSRF-TOKEN` por `GET /csrf`.
2. Envie e-mail e senha para `POST /login`, com `X-XSRF-TOKEN` e credentials.
3. Mantenha `accessToken` apenas na memória; deixe o browser armazenar o cookie HttpOnly.

### Restauração após recarregar a página

1. O reload elimina o access token de memória.
2. O frontend chama `GET /csrf` e depois `POST /refresh` com cookies incluídos e header CSRF.
3. Guarde o novo access token somente em memória.
4. Use single-flight no cliente: mantenha uma única promise de refresh compartilhada por chamadas que recebem 401. A API também bloqueia a linha da sessão e permite somente uma rotação do refresh anterior.

Reutilização de um refresh já rotacionado revoga toda a família e exige novo login. Um refresh inválido retorna 401 e limpa o cookie.

### Logout

Envie `POST /logout` com credentials e CSRF. A API revoga a família de refresh e apaga o cookie com os mesmos Path/SameSite/Secure/HttpOnly. O frontend também deve apagar o access token em memória. Um JWT já emitido continua válido até expirar.

## Provisionar o primeiro ADMIN

Não existe cadastro público. Defina temporariamente `BOOTSTRAP_ADMIN_EMAIL` e `BOOTSTRAP_ADMIN_PASSWORD` usando um mecanismo de secrets, inicie a API e remova as variáveis assim que o log confirmar o provisionamento. O processo só cria ADMIN se ainda não houver um e-mail cadastrado/admin; não altera contas existentes. Não há senha padrão no código.

## Chaves JWT

Dev/test pode gerar uma chave RSA efêmera somente porque `JWT_EPHEMERAL_DEV_KEY` é explicitamente habilitado nesses profiles. O log informa que tokens expiram na reinicialização. Produção exige par RSA persistente, em armazenamento de secrets, definido por `JWT_PRIVATE_KEY` (PKCS#8) e `JWT_PUBLIC_KEY` (X.509), sem salvar chaves no Git. O profile `prod` proíbe chave efêmera.

## Cookies em dev e produção

- Dev HTTP: `Secure=false`, apenas local.
- Produção: HTTPS obrigatório e `Secure=true`; `HttpOnly=true`, `SameSite=Lax`, sem `Domain`, `Path=/api/v1/auth`.
- Não relaxe o profile de produção para contornar problemas de localhost. `SameSite=Lax` não substitui o token CSRF.

## CORS, XSS e limites

Configure origens exatas por ambiente; credentials nunca usam `*`. O frontend deve adotar CSP restritiva, escapar conteúdo e evitar scripts inline para reduzir risco de XSS. XSS pode agir em nome do usuário mesmo sem ler cookies HttpOnly.

JWT não tem introspecção nem revogação imediata: um access token emitido continua válido até expirar. Refresh/logout revogam sessões futuras. O backend não inclui frontend, recuperação de senha, cadastro público ou gestão de usuários.
