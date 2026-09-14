# Buynow V2 — Cadastro e Login

Esta versão adiciona cadastro e login reais usando uma API local.

## Backend
Requer Node.js 18+.

```bash
cd backend
npm start
```

Teste no navegador: http://localhost:3000/health

## Android Studio
Abra a pasta `Buynow_V2_LOGIN_REAL`.

O emulador Android usa `http://10.0.2.2:3000`.
No celular físico, altere `API_BASE_URL` no `MainActivity.kt` para o IP do computador.

## Fluxo
Cadastro -> API -> usuário salvo -> token -> sessão no Android -> Home.

As senhas não são armazenadas em texto puro: o backend usa `scrypt` com salt.

Para produção, ainda devemos trocar o armazenamento JSON por PostgreSQL, usar HTTPS, segredo forte, tokens com expiração/refresh, rate limiting, logs, antifraude e requisitos LGPD.
