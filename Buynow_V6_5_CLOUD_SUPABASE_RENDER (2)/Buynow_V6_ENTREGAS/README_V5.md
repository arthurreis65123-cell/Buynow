# Buynow V5 — Compra completa

## Novidades
- Home marketplace com busca e categorias
- Produtos vindos do SQLite
- Detalhe do produto
- Carrinho com quantidade
- Cálculo de subtotal, frete e total
- Cadastro de endereço
- Checkout
- Escolha de PIX, cartão ou boleto (seleção apenas nesta versão; integração de pagamento real será posterior)
- Criação de pedido no backend
- Baixa automática do estoque ao criar pedido
- Histórico de pedidos
- Criação de loja pelo usuário
- Banco SQLite ampliado com endereços, pedidos e itens de pedido

## Backend
Na pasta `backend`:

```powershell
npm install
npm start
```

O servidor deve mostrar:

```text
Buynow API V5: http://localhost:3000
```

No celular, teste:

`http://192.168.0.8:3000/health`

## Android
Abra a pasta no Android Studio e rode no celular físico.
O endereço da API já está configurado como:

`http://192.168.0.8:3000`

Celular e computador precisam estar na mesma rede Wi-Fi.

## Fluxo para testar
1. Crie/entre em uma conta.
2. Crie uma loja em "Conta".
3. Nesta V5, o cadastro de produtos ainda é via API/estrutura existente; a tela visual completa do vendedor será expandida na próxima versão.
4. Em Conta, selecione sua loja e cadastre produtos pelo botão 'Cadastrar produto'.
5. Os produtos cadastrados aparecem na Home.
6. Toque em um produto para abrir detalhes.
7. Adicione ao carrinho.
8. Abra Carrinho.
9. Continue para entrega e pagamento.
10. Informe o endereço.
11. Confirme o pedido.
12. O pedido aparece em "Pedidos" e o estoque é reduzido.

> PIX, cartão e boleto são apenas opções registradas no pedido nesta V5. Nenhum pagamento real é processado ainda. A integração financeira será feita em uma etapa própria, com segurança, webhooks e idempotência.
