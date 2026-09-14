# Buynow V4.1 — Correção para celular físico

Esta versão corrige a comunicação Android -> API usando HTTP local.

- IP configurado: `192.168.0.8`
- API: `http://192.168.0.8:3000`
- Android permite HTTP local (`usesCleartextTraffic=true`)
- Backend cria automaticamente a pasta `data`
- Mensagens de erro agora mostram o detalhe técnico

## Teste

No computador:
```powershell
cd backend
npm install
npm start
```

No celular:
`http://192.168.0.8:3000/health`

Depois rode a V4.1 pelo Android Studio. Se a API continuar indisponível, a própria tela exibirá o detalhe técnico.
