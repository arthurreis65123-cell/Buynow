
const http=require("http"), crypto=require("crypto");
const {URL}=require("url");
const {Pool}=require("pg");
const PORT=Number(process.env.PORT||3000);
const pool=new Pool({connectionString:process.env.DATABASE_URL,
  ssl:process.env.DATABASE_URL?{rejectUnauthorized:false}:undefined,
  max:Number(process.env.DB_POOL_MAX||10),connectionTimeoutMillis:10000});
const SECRET=process.env.JWT_SECRET||"CHANGE_ME";
const STAT=["PENDING","PAID","PROCESSING","SHIPPED","DELIVERED","CANCELLED"];

function out(res,status,obj){res.writeHead(status,{"Content-Type":"application/json; charset=utf-8",
"Access-Control-Allow-Origin":process.env.ALLOWED_ORIGIN||"*","Access-Control-Allow-Headers":"Content-Type, Authorization",
"Access-Control-Allow-Methods":"GET,POST,PUT,OPTIONS"});res.end(JSON.stringify(obj));}
function body(req){return new Promise((ok,no)=>{let s="";req.on("data",c=>s+=c);req.on("end",()=>{try{ok(s?JSON.parse(s):{})}catch(e){no(new Error("JSON inválido"))}});req.on("error",no)})}
function hp(p,s=crypto.randomBytes(16).toString("hex")){return {salt:s,hash:crypto.scryptSync(p,s,64).toString("hex")}}
function verify(p,s,h){return crypto.timingSafeEqual(Buffer.from(hp(p,s).hash,"hex"),Buffer.from(h,"hex"))}
function b64(x){return Buffer.from(JSON.stringify(x)).toString("base64url")}
function token(u) {
  const header = b64({
    alg: "HS256",
    typ: "JWT"
  });

  const payload = b64({
    sub: String(u.id),
    email: u.email,
    exp: Math.floor(Date.now() / 1000) + 2592000
  });

  const data = header + "." + payload;

  const signature = crypto
    .createHmac("sha256", JWT_SECRET)
    .update(data)
    .digest("base64url");

  return data + "." + signature;
}
  const header = b64({
    alg: "HS256",
    typ: "JWT"
  });

  const payload = b64({
    sub: String(u.id),
    email: u.email,
    exp: Math.floor(Date.now() / 1000) + 2592000
  });

  const data = header + "." + payload;

  const signature = crypto
    .createHmac("sha256", JWT_SECRET)
    .update(data)
    .digest("base64url");

  return data + "." + signature;
}
const q=(s,p=[])=>pool.query(s,p);

async function schema(){
if(!process.env.DATABASE_URL)throw Error("DATABASE_URL não configurada");
await q(`
CREATE TABLE IF NOT EXISTS users(id SERIAL PRIMARY KEY,name TEXT NOT NULL,email TEXT UNIQUE NOT NULL,password_hash TEXT NOT NULL,password_salt TEXT NOT NULL,created_at TIMESTAMPTZ DEFAULT NOW());
CREATE TABLE IF NOT EXISTS categories(id SERIAL PRIMARY KEY,name TEXT UNIQUE NOT NULL);
CREATE TABLE IF NOT EXISTS stores(id SERIAL PRIMARY KEY,owner_id INT REFERENCES users(id) ON DELETE CASCADE,name TEXT NOT NULL,description TEXT DEFAULT '',created_at TIMESTAMPTZ DEFAULT NOW());
CREATE TABLE IF NOT EXISTS products(id SERIAL PRIMARY KEY,store_id INT REFERENCES stores(id) ON DELETE CASCADE,category_id INT REFERENCES categories(id) ON DELETE SET NULL,name TEXT NOT NULL,description TEXT DEFAULT '',price NUMERIC(12,2) DEFAULT 0,stock INT DEFAULT 0,image_url TEXT DEFAULT '',created_at TIMESTAMPTZ DEFAULT NOW());
CREATE TABLE IF NOT EXISTS addresses(id SERIAL PRIMARY KEY,user_id INT REFERENCES users(id) ON DELETE CASCADE,name TEXT DEFAULT '',street TEXT NOT NULL,number TEXT NOT NULL,complement TEXT DEFAULT '',neighborhood TEXT DEFAULT '',city TEXT NOT NULL,state TEXT NOT NULL,zip_code TEXT NOT NULL,created_at TIMESTAMPTZ DEFAULT NOW());
CREATE TABLE IF NOT EXISTS orders(id SERIAL PRIMARY KEY,user_id INT REFERENCES users(id),address_id INT REFERENCES addresses(id),status TEXT DEFAULT 'PENDING',payment_method TEXT DEFAULT 'PIX',subtotal NUMERIC(12,2) DEFAULT 0,shipping NUMERIC(12,2) DEFAULT 0,total NUMERIC(12,2) DEFAULT 0,tracking_code TEXT DEFAULT '',courier_name TEXT DEFAULT '',updated_at TIMESTAMPTZ DEFAULT NOW(),created_at TIMESTAMPTZ DEFAULT NOW());
CREATE TABLE IF NOT EXISTS order_items(id SERIAL PRIMARY KEY,order_id INT REFERENCES orders(id) ON DELETE CASCADE,product_id INT REFERENCES products(id),quantity INT NOT NULL,unit_price NUMERIC(12,2) NOT NULL);
CREATE INDEX IF NOT EXISTS idx_products_store ON products(store_id);CREATE INDEX IF NOT EXISTS idx_orders_user ON orders(user_id);
`);
for(const n of ["Eletrônicos","Casa","Moda","Beleza","Esportes","Automotivo","Games","Outros"])
 await q("INSERT INTO categories(name) VALUES($1) ON CONFLICT(name) DO NOTHING",[n]);
}
async function order(id,uid){
let r=await q(`SELECT o.*,a.street,a.number,a.complement,a.neighborhood,a.city,a.state,a.zip_code FROM orders o LEFT JOIN addresses a ON a.id=o.address_id WHERE o.id=$1 AND o.user_id=$2`,[id,uid]);
if(!r.rows[0])return null;let o=r.rows[0];
o.items=(await q(`SELECT oi.*,p.name product_name,p.image_url FROM order_items oi JOIN products p ON p.id=oi.product_id WHERE oi.order_id=$1 ORDER BY oi.id`,[id])).rows;return o;
}
async function app(req,res){
if(req.method==="OPTIONS")return out(res,204,{});
let u=new URL(req.url,`http://${req.headers.host||"x"}`),p=u.pathname;
try{
if(p==="/"&&req.method==="GET")return out(res,200,{ok:true,service:"Buynow API",version:"6.5",database:"PostgreSQL"});
if(p==="/health"&&req.method==="GET"){await q("SELECT 1");return out(res,200,{ok:true,service:"Buynow API",version:"6.5",database:"PostgreSQL"})}
if(p==="/auth/register"&&req.method==="POST"){let b=await body(req),name=String(b.name||"").trim(),email=String(b.email||"").trim().toLowerCase(),pw=String(b.password||"");
if(!name||!email||pw.length<6)return out(res,400,{error:"Nome, email e senha (mín. 6) são obrigatórios."});
if((await q("SELECT id FROM users WHERE email=$1",[email])).rows[0])return out(res,409,{error:"Email já cadastrado."});
let h=hp(pw),r=await q("INSERT INTO users(name,email,password_hash,password_salt) VALUES($1,$2,$3,$4) RETURNING id,name,email",[name,email,h.hash,h.salt]);
return out(res,201,{user:r.rows[0],token:token(r.rows[0])})}
if(p==="/auth/login"&&req.method==="POST"){let b=await body(req),r=await q("SELECT * FROM users WHERE email=$1",[String(b.email||"").trim().toLowerCase()]),u=r.rows[0];
if(!u||!verify(String(b.password||""),u.password_salt,u.password_hash))return out(res,401,{error:"Email ou senha inválidos."});
return out(res,200,{user:{id:u.id,name:u.name,email:u.email},token:token(u)})}
let me=auth(req);if(["/me","/my/stores","/addresses","/orders","/seller/orders"].some(x=>p===x||p.startsWith(x+"/"))&&!me)return out(res,401,{error:"Não autorizado."});
if(p==="/me"){let r=await q("SELECT id,name,email FROM users WHERE id=$1",[me.sub]);return out(res,200,{user:r.rows[0]})}
if(p==="/categories"){return out(res,200,(await q("SELECT id,name FROM categories ORDER BY name")).rows)}
if(p==="/stores"&&req.method==="GET")return out(res,200,(await q(`SELECT s.*,COUNT(p.id)::int product_count FROM stores s LEFT JOIN products p ON p.store_id=s.id GROUP BY s.id ORDER BY s.id DESC`)).rows);
if(p==="/stores"&&req.method==="POST"){let b=await body(req);let r=await q("INSERT INTO stores(owner_id,name,description) VALUES($1,$2,$3) RETURNING *",[me.sub,b.name,b.description||""]);return out(res,201,r.rows[0])}
if(p==="/my/stores")return out(res,200,(await q("SELECT * FROM stores WHERE owner_id=$1 ORDER BY id DESC",[me.sub])).rows);
if(p==="/products"&&req.method==="GET"){let term=u.searchParams.get("q")||"",r=await q(`SELECT p.*,s.name store_name,c.name category_name FROM products p JOIN stores s ON s.id=p.store_id LEFT JOIN categories c ON c.id=p.category_id ${term?"WHERE p.name ILIKE $1 OR p.description ILIKE $1":""} ORDER BY p.id DESC`,term?[`%${term}%`]:[]);return out(res,200,r.rows)}
if(p==="/products"&&req.method==="POST"){let b=await body(req),sid=Number(b.store_id||b.storeId),own=await q("SELECT id FROM stores WHERE id=$1 AND owner_id=$2",[sid,me.sub]);if(!own.rows[0])return out(res,403,{error:"Loja inválida."});
let r=await q("INSERT INTO products(store_id,category_id,name,description,price,stock,image_url) VALUES($1,$2,$3,$4,$5,$6,$7) RETURNING *",[sid,b.category_id||b.categoryId||null,b.name,b.description||"",Number(b.price||0),Number(b.stock||0),b.image_url||b.imageUrl||""]);return out(res,201,r.rows[0])}
let pm=p.match(/^\/products\/(\d+)$/);if(pm)return out(res,200,(await q(`SELECT p.*,s.name store_name,c.name category_name FROM products p JOIN stores s ON s.id=p.store_id LEFT JOIN categories c ON c.id=p.category_id WHERE p.id=$1`,[pm[1]])).rows[0]||{error:"Produto não encontrado."});
if(p==="/addresses"&&req.method==="GET")return out(res,200,(await q("SELECT * FROM addresses WHERE user_id=$1 ORDER BY id DESC",[me.sub])).rows);
if(p==="/addresses"&&req.method==="POST"){let b=await body(req),r=await q("INSERT INTO addresses(user_id,name,street,number,complement,neighborhood,city,state,zip_code) VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9) RETURNING *",[me.sub,b.name||"",b.street,b.number,b.complement||"",b.neighborhood||"",b.city,b.state,b.zip_code||b.zipCode]);return out(res,201,r.rows[0])}
if(p==="/orders"&&req.method==="GET"){let r=await q("SELECT o.*,a.street,a.number,a.complement,a.neighborhood,a.city,a.state,a.zip_code FROM orders o LEFT JOIN addresses a ON a.id=o.address_id WHERE o.user_id=$1 ORDER BY o.id DESC",[me.sub]);for(let o of r.rows)o.items=(await q("SELECT oi.*,p.name product_name,p.image_url FROM order_items oi JOIN products p ON p.id=oi.product_id WHERE oi.order_id=$1",[o.id])).rows;return out(res,200,r.rows)}
if(p==="/orders"&&req.method==="POST"){let b=await body(req),items=b.items||[],aid=Number(b.address_id||b.addressId),c=await pool.connect();try{await c.query("BEGIN");if(!(await c.query("SELECT id FROM addresses WHERE id=$1 AND user_id=$2",[aid,me.sub])).rows[0])throw Error("Endereço inválido.");let sub=0,rr=[];for(let i of items){let r=await c.query("SELECT * FROM products WHERE id=$1 FOR UPDATE",[Number(i.product_id||i.productId)]),x=r.rows[0],n=Math.max(1,Number(i.quantity||1));if(!x)throw Error("Produto não encontrado.");if(x.stock<n)throw Error("Estoque insuficiente.");sub+=Number(x.price)*n;rr.push([x,n])}let ship=sub>=200?0:19.9,total=sub+ship,o=(await c.query("INSERT INTO orders(user_id,address_id,status,payment_method,subtotal,shipping,total) VALUES($1,$2,'PENDING',$3,$4,$5,$6) RETURNING id",[me.sub,aid,String(b.payment_method||b.paymentMethod||"PIX"),sub,ship,total])).rows[0];
for(let [x,n] of rr){await c.query("INSERT INTO order_items(order_id,product_id,quantity,unit_price) VALUES($1,$2,$3,$4)",[o.id,x.id,n,x.price]);await c.query("UPDATE products SET stock=stock-$1 WHERE id=$2",[n,x.id])}await c.query("COMMIT");return out(res,201,await order(o.id,me.sub))}catch(e){await c.query("ROLLBACK");return out(res,400,{error:e.message})}finally{c.release()}}
let om=p.match(/^\/orders\/(\d+)$/);if(om&&req.method==="GET")return out(res,200,await order(om[1],me.sub));
if(p==="/seller/orders"&&req.method==="GET")return out(res,200,(await q(`SELECT DISTINCT o.*,u.name customer_name,u.email customer_email,a.street,a.number,a.complement,a.neighborhood,a.city,a.state,a.zip_code,s.name store_name FROM orders o JOIN order_items oi ON oi.order_id=o.id JOIN products p ON p.id=oi.product_id JOIN stores s ON s.id=p.store_id JOIN users u ON u.id=o.user_id LEFT JOIN addresses a ON a.id=o.address_id WHERE s.owner_id=$1 ORDER BY o.id DESC`,[me.sub])).rows);
let so=p.match(/^\/seller\/orders\/(\d+)$/);if(so&&req.method==="PUT"){let b=await body(req),st=String(b.status||"").toUpperCase();if(!STAT.includes(st))return out(res,400,{error:"Status inválido."});
let own=await q(`SELECT 1 FROM order_items oi JOIN products p ON p.id=oi.product_id JOIN stores s ON s.id=p.store_id WHERE oi.order_id=$1 AND s.owner_id=$2 LIMIT 1`,[so[1],me.sub]);if(!own.rows[0])return out(res,403,{error:"Sem permissão."});
let r=await q("UPDATE orders SET status=$1,tracking_code=$2,courier_name=$3,updated_at=NOW() WHERE id=$4 RETURNING *",[st,b.tracking_code||b.trackingCode||"",b.courier_name||b.courierName||"",so[1]]);return out(res,200,r.rows[0])}
return out(res,404,{error:"Rota não encontrada."})
}catch(e){console.error(e);return out(res,500,{error:"Erro interno.",detail:process.env.NODE_ENV==="production"?undefined:e.message})}}
async function start(){await schema();http.createServer(app).listen(PORT,"0.0.0.0",()=>console.log(`Buynow API V6.5 Cloud on ${PORT}`))}
start().catch(e=>{console.error(e);process.exit(1)})
