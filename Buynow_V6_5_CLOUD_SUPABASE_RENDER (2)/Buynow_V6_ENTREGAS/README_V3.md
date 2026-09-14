# Buynow V3 — Login real + Backend corrigido

## O que foi corrigido
- Erro de compilação da `AuthScreen` causado pela chamada do `Button`.
- `modifier` do `Button` agora é passado corretamente como argumento nomeado.
- Executor de rede é encerrado quando a tela sai de composição.
- O backend agora possui uma rota `/`, então `http://localhost:3000` não retorna mais "Rota não encontrada".
- `/health`, cadastro e login continuam disponíveis.

## Backend
Na pasta `backend`:

```powershell
npm start
```

Deve aparecer:

```text
Buynow API: http://localhost:3000
```

Testes no navegador:
- `http://localhost:3000`
- `http://localhost:3000/health`

## Android Studio
1. Abra a pasta raiz `Buynow_V3_LOGIN_CORRIGIDO`.
2. Aguarde o Gradle Sync.
3. Execute o app.
4. Use um emulador Android.
5. O app usa `http://10.0.2.2:3000` para acessar o backend do computador.

### Celular físico
Se estiver usando um aparelho físico, troque em `MainActivity.kt`:

```kotlin
private const val API_BASE_URL = "http://10.0.2.2:3000"
```

pelo IP local do computador, por exemplo:

```kotlin
private const val API_BASE_URL = "http://192.168.0.10:3000"
```

O celular e o computador precisam estar na mesma rede.

## Fluxo de teste
1. Inicie o backend.
2. Verifique `/health`.
3. Rode o aplicativo.
4. Toque em "Criar uma nova conta".
5. Cadastre um e-mail de teste.
6. O app salva o token localmente e entra na Home.
7. Use o botão de sair para testar o logout.
8. Entre novamente com o mesmo e-mail e senha.

> Esta versão é para desenvolvimento. Para produção, o próximo passo é migrar usuários para banco de dados, usar HTTPS, tokens com expiração/refresh, rate limiting, logs e controles de segurança/LGPD.
