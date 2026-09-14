# Buynow V5.2 — Build corrigido

Correções desta versão:
- Corrigida a chamada do `IconButton` do carrinho.
- Adicionado o modelo `Store` que estava faltando.
- Corrigido `Modifier.padding` com quatro valores.
- Tornadas algumas chamadas de `LazyColumn.items` e `OutlinedTextField` mais explícitas para o compilador Kotlin.
- Mantida a API local `http://192.168.0.8:3000`.

Teste:
1. Abra esta versão no Android Studio.
2. Faça Build > Clean Project.
3. Faça Build > Rebuild Project.
4. Rode no celular físico.
5. Mantenha o backend V5 rodando com `npm start`.

Se o build chegar a `BUILD SUCCESSFUL`, teste o fluxo: criar loja -> cadastrar produto -> Home -> carrinho -> endereço -> pedido.
