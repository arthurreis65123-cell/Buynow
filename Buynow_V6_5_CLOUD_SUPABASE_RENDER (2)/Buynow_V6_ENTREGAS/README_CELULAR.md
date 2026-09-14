# Buynow V4 — Configurado para celular físico

O aplicativo foi configurado para acessar o backend do computador em:

`http://192.168.0.8:3000`

## Para testar

1. No computador, entre em `backend`.
2. Execute `npm install` (se ainda não executou).
3. Execute `npm start`.
4. Confirme `Buynow API: http://localhost:3000`.
5. Computador e celular devem estar na mesma rede Wi-Fi.
6. Rode o aplicativo no celular pelo Android Studio.

## Teste da API no celular

Abra no navegador do celular:

`http://192.168.0.8:3000/health`

Se retornar os dados da API, a conexão está correta.

## Importante

O IP `192.168.0.8` é o endereço atual do computador na rede local. Se o roteador mudar esse IP no futuro, será necessário atualizar o endereço no aplicativo.
