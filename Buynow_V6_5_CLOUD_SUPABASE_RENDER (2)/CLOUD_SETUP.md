# Buynow V6.5 Cloud
Arquitetura: Android -> HTTPS Render -> PostgreSQL Supabase.

1. Crie um projeto no Supabase.
2. Em Connect, copie a connection string PostgreSQL e guarde-a como DATABASE_URL.
3. No Render, crie um Web Service para a pasta backend.
4. Build: npm install | Start: npm start | Health: /health.
5. Configure DATABASE_URL e JWT_SECRET nas Environment Variables do Render.
6. Depois que o Render gerar https://SEU-NOME.onrender.com, troque no Android o host antigo pela URL real.
O backend cria as tabelas automaticamente na primeira inicialização.
