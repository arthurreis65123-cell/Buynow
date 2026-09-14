# Buynow V4 — Marketplace + Banco de Dados

Esta versão evolui a V3 para um marketplace funcional de desenvolvimento.

## Novidades
- Banco SQLite persistente no backend (`backend/data/buynow.db`).
- Tabelas: usuários, lojas, categorias e produtos.
- Login/cadastro mantidos.
- GET `/products` e busca por `?q=`.
- POST `/products` protegido pelo usuário autenticado.
- GET/POST `/stores`.
- GET `/categories`.
- GET `/me`.
- Home do app com Produtos, Lojas e Conta.
- Cadastro de loja e produto pelo aplicativo.

## Como rodar
Na pasta `backend`:

```powershell
npm install
npm start
```

Deve aparecer:

```text
Buynow API V4: http://localhost:3000
```

Teste no navegador:
- `http://localhost:3000`
- `http://localhost:3000/health`

## Android
Para emulador, o app já usa:

```text
http://10.0.2.2:3000
```

Para celular físico, coloque o IP local do computador no `API_BASE_URL` do `MainActivity.kt` e mantenha computador e celular na mesma rede.

## Observação sobre cadastro de produto
Na V4, o cadastro de produto pede o ID da loja para manter a API simples. A próxima evolução deve colocar um seletor visual de "Minha loja", tela completa de produto, imagens, edição/exclusão e controle de estoque.

## Produção
SQLite/sql.js é adequado para desenvolvimento/local. Para produção com muitos usuários, migrar para PostgreSQL ou outro banco servidor, usar HTTPS, secrets seguros, tokens com expiração/refresh, rate limiting, validação, logs, backup, LGPD e controles antifraude.
