package com.buynow.app

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

private val Blue=Color(0xFF087CFF)
private val Navy=Color(0xFF0B1F52)
private val SoftBlue=Color(0xFFF0F6FF)
private const val API_BASE_URL="http://SEU-NOME.onrender.com"

class MainActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        setContent{BuynowApp(applicationContext)}
    }
}
class AuthStore(context:Context){
    private val p=context.getSharedPreferences("buynow_auth",Context.MODE_PRIVATE)
    fun token()=p.getString("token",null)
    fun name()=p.getString("name","Cliente")?:"Cliente"
    fun save(t:String,n:String)=p.edit().putString("token",t).putString("name",n).apply()
    fun clear()=p.edit().clear().apply()
}
private data class ApiResult(val ok:Boolean,val message:String,val json:JSONObject=JSONObject())
private fun call(method:String,path:String,body:JSONObject?=null,token:String?=null):ApiResult=try{
    val c=(URL(API_BASE_URL+path).openConnection() as HttpURLConnection)
    c.requestMethod=method
    c.connectTimeout=8000;c.readTimeout=8000
    c.setRequestProperty("Content-Type","application/json")
    token?.let{c.setRequestProperty("Authorization","Bearer $it")}
    if(body!=null){c.doOutput=true;c.outputStream.use{it.write(body.toString().toByteArray(StandardCharsets.UTF_8))}}
    val code=c.responseCode
    val stream=if(code in 200..299)c.inputStream else c.errorStream
    val txt=stream?.bufferedReader()?.use{it.readText()}?:"{}"
    c.disconnect()
    ApiResult(code in 200..299,JSONObject(txt).optString("message",if(code in 200..299)"OK" else "Erro"),JSONObject(txt))
}catch(e:Exception){
    ApiResult(false,"Não foi possível conectar à API.\n${e.javaClass.simpleName}: ${e.message ?: "sem detalhes"}")
}
private fun money(c:Int)=String.format("R$ %.2f",c/100.0)

data class Product(val id:String,val name:String,val description:String,val priceCents:Int,val stock:Int,val storeId:String,val storeName:String,val categoryId:String,val category:String)
data class Category(val id:String,val name:String)
data class Store(val id:String,val name:String,val description:String,val productCount:Int)
data class CartItem(val product:Product,var quantity:Int)
data class Address(val id:String,val recipient:String,val street:String,val number:String,val complement:String,val neighborhood:String,val city:String,val state:String,val zip:String)
data class Order(val id:String,val status:String,val subtotal:Int,val shipping:Int,val total:Int,val payment:String,val createdAt:String,val updatedAt:String,val trackingCode:String,val courierName:String,val items:List<String>)

@Composable fun BuynowApp(context:Context){
    val store=remember{AuthStore(context)}
    var logged by remember{mutableStateOf(store.token()!=null)}
    if(!logged) AuthScreen{t,n->store.save(t,n);logged=true}
    else MarketplaceScreen(store.name(),store.token()!!){store.clear();logged=false}
}
@Composable fun Logo(){
    Row(verticalAlignment=Alignment.CenterVertically){
        Text("Buy",color=Navy,fontSize=36.sp,fontWeight=FontWeight.Bold)
        Text("now",color=Blue,fontSize=36.sp,fontWeight=FontWeight.Bold)
    }
}
@Composable fun AuthScreen(onSuccess:(String,String)->Unit){
    var create by remember{mutableStateOf(false)}
    var name by remember{mutableStateOf("")};var email by remember{mutableStateOf("")};var password by remember{mutableStateOf("")}
    var msg by remember{mutableStateOf("")};var loading by remember{mutableStateOf(false)}
    val ex=remember{Executors.newSingleThreadExecutor()}
    DisposableEffect(Unit){onDispose{ex.shutdownNow()}}
    Column(Modifier.fillMaxSize().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally){
        Spacer(Modifier.height(50.dp));Logo()
        Text(if(create)"Crie sua conta" else "Bem-vindo ao Buynow",color=Navy,fontSize=24.sp,fontWeight=FontWeight.Bold)
        Text(if(create)"Cadastre-se para comprar e vender." else "Entre para continuar.",color=Color.Gray,modifier=Modifier.padding(start=6.dp,top=6.dp,end=6.dp,bottom=25.dp))
        if(create){OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),label={Text("Nome completo")},singleLine=true);Spacer(Modifier.height(10.dp))}
        OutlinedTextField(email,{email=it},Modifier.fillMaxWidth(),label={Text("E-mail")},singleLine=true)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(password,{password=it},Modifier.fillMaxWidth(),label={Text("Senha")},singleLine=true,visualTransformation=PasswordVisualTransformation())
        if(msg.isNotBlank())Text(msg,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(top=8.dp))
        Spacer(Modifier.height(18.dp))
        Button(onClick={
            loading=true;msg=""
            val b=JSONObject().apply{put("email",email.trim());put("password",password);if(create)put("name",name.trim())}
            ex.execute{
                val r=call("POST",if(create)"/auth/register" else "/auth/login",b)
                Handler(Looper.getMainLooper()).post{loading=false;if(r.ok)onSuccess(r.json.optString("token"),r.json.optString("name",name))else msg=r.message}
            }
        },enabled=!loading&&email.isNotBlank()&&password.length>=6&&(!create||name.isNotBlank()),modifier=Modifier.fillMaxWidth().height(52.dp)){
            Text(if(loading)"Conectando..." else if(create)"Criar conta" else "Entrar",fontSize=17.sp)
        }
        TextButton(onClick={create=!create;msg=""}){Text(if(create)"Já tenho uma conta" else "Criar uma nova conta",color=Blue)}
    }
}

@Composable fun MarketplaceScreen(name:String,token:String,logout:()->Unit){
    var tab by remember{mutableStateOf(0)}
    var cart by remember{mutableStateOf(listOf<CartItem>())}
    Scaffold(bottomBar={
        NavigationBar{
            NavigationBarItem(tab==0,{tab=0},{Icon(Icons.Default.Home,null);Text("Início")})
            NavigationBarItem(tab==1,{tab=1},{Icon(Icons.Default.ShoppingCart,null);Text("Carrinho")})
            NavigationBarItem(tab==2,{tab=2},{Icon(Icons.Default.ReceiptLong,null);Text("Pedidos")})
            NavigationBarItem(tab==3,{tab=3},{Icon(Icons.Default.Person,null);Text("Conta")})
        }
    }){pad->
        Column(Modifier.fillMaxSize().padding(pad)){
            Row(Modifier.fillMaxWidth().padding(horizontal=16.dp).padding(top=10.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                Logo()
                BadgedBox(badge={if(cart.sumOf{it.quantity}>0)Badge{Text(cart.sumOf{it.quantity}.toString())}}){
                    IconButton(onClick={tab=1}){Icon(Icons.Default.ShoppingCart,"Carrinho",tint=Navy)}
                }
            }
            when(tab){
                0->HomeTab(token,cart){p->cart=addCart(cart,p);tab=0}
                1->CartTab(token,cart,{p->cart=addCart(cart,p)},{id,qty->cart=cart.mapNotNull{if(it.product.id==id)it.copy(quantity=qty)else it}.filter{it.quantity>0}},{cart=emptyList();tab=2})
                2->OrdersTab(token)
                3->AccountTab(name,token,logout)
            }
        }
    }
}
fun addCart(current:List<CartItem>,product:Product):List<CartItem>{
    val out=current.map{it.copy()}
    val idx=out.indexOfFirst{it.product.id==product.id}
    if(idx>=0){out[idx].quantity=(out[idx].quantity+1).coerceAtMost(product.stock)}
    else out+CartItem(product,1)
    return out
}

@Composable fun HomeTab(token:String,cart:List<CartItem>,onAdd:(Product)->Unit){
    var products by remember{mutableStateOf(listOf<Product>())};var cats by remember{mutableStateOf(listOf<Category>())}
    var query by remember{mutableStateOf("")};var category by remember{mutableStateOf("")};var loading by remember{mutableStateOf(true)}
    var msg by remember{mutableStateOf("")};var selected by remember{mutableStateOf<Product?>(null)}
    val ex=remember{Executors.newSingleThreadExecutor()}
    fun load(){
        loading=true
        ex.execute{
            val qp=if(query.isBlank())"" else "?q="+URLEncoder.encode(query,"UTF-8")+(if(category.isBlank())"" else "&category="+URLEncoder.encode(category,"UTF-8"))
            val r=call("GET","/products$qp",token=token)
            val arr=r.json.optJSONArray("products")?:JSONArray();val list=mutableListOf<Product>()
            for(i in 0 until arr.length()){val o=arr.getJSONObject(i);list.add(Product(o.optString("id"),o.optString("name"),o.optString("description"),o.optInt("price_cents"),o.optInt("stock"),o.optString("store_id"),o.optString("store_name"),o.optString("category_id"),o.optString("category_name")))}
            val cr=call("GET","/categories",token=token);val ca=cr.json.optJSONArray("categories")?:JSONArray();val cl=mutableListOf<Category>()
            for(i in 0 until ca.length()){val o=ca.getJSONObject(i);cl.add(Category(o.optString("id"),o.optString("name")))}
            Handler(Looper.getMainLooper()).post{products=list;cats=cl;loading=false;if(!r.ok)msg=r.message}
        }
    }
    DisposableEffect(Unit){load();onDispose{ex.shutdownNow()}}
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){
        Text("Encontre o que você precisa",color=Navy,fontSize=22.sp,fontWeight=FontWeight.Bold)
        OutlinedTextField(query,{query=it},Modifier.fillMaxWidth().padding(top=10.dp),singleLine=true,placeholder={Text("Buscar produtos e lojas")},leadingIcon={Icon(Icons.Default.Search,null)})
        Row(Modifier.fillMaxWidth().padding(vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            FilterChip(selected=category.isBlank(),onClick={category="";load()},label={Text("Todos")})
            cats.take(5).forEach{c->FilterChip(selected=category==c.id,onClick={category=c.id;load()},label={Text(c.name)})}
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
            Text("Produtos",color=Navy,fontSize=20.sp,fontWeight=FontWeight.Bold)
            Text("${products.size} encontrados",color=Color.Gray,fontSize=12.sp)
        }
        if(loading)Box(Modifier.fillMaxWidth().padding(25.dp),contentAlignment=Alignment.Center){CircularProgressIndicator()}
        else if(products.isEmpty())Text(if(msg.isBlank())"Nenhum produto encontrado." else msg,color=Color.Gray,modifier=Modifier.padding(top=25.dp))
        LazyColumn(Modifier.fillMaxSize().padding(top=8.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            items(items=products){p->
                Card(Modifier.fillMaxWidth().clickable{selected=p},shape=RoundedCornerShape(18.dp)){
                    Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                        Box(Modifier.size(78.dp).background(SoftBlue,RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center){
                            Icon(Icons.Default.Inventory2,null,tint=Blue,modifier=Modifier.size(36.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)){
                            Text(p.name,color=Navy,fontSize=17.sp,fontWeight=FontWeight.Bold)
                            Text(p.storeName,color=Color.Gray,fontSize=12.sp)
                            if(p.category.isNotBlank())Text(p.category,color=Blue,fontSize=11.sp)
                            Text(money(p.priceCents),color=Blue,fontSize=19.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=5.dp))
                        }
                        IconButton(onClick={onAdd(p)}){Icon(Icons.Default.AddShoppingCart,"Adicionar",tint=Blue)}
                    }
                }
            }
        }
    }
    selected?.let{ProductDetailDialog(it,{onAdd(it);selected=null},{selected=null})}
}

@Composable fun ProductDetailDialog(p:Product,onAdd:()->Unit,onClose:()->Unit){
    AlertDialog(onDismissRequest=onClose,title={Text(p.name,color=Navy,fontWeight=FontWeight.Bold)},
        text={Column{
            Box(Modifier.fillMaxWidth().height(150.dp).background(SoftBlue,RoundedCornerShape(16.dp)),contentAlignment=Alignment.Center){Icon(Icons.Default.Inventory2,null,tint=Blue,modifier=Modifier.size(60.dp))}
            Text(money(p.priceCents),color=Blue,fontSize=26.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=12.dp))
            Text("Loja: ${p.storeName}",color=Color.Gray,modifier=Modifier.padding(top=3.dp))
            Text("Estoque: ${p.stock}",color=Color.Gray)
            if(p.description.isNotBlank())Text(p.description,modifier=Modifier.padding(top=12.dp))
        }},
        confirmButton={Button(onClick=onAdd,enabled=p.stock>0){Text(if(p.stock>0)"Adicionar ao carrinho" else "Sem estoque")}},
        dismissButton={TextButton(onClick=onClose){Text("Fechar")}})
}

@Composable fun CartTab(token:String,cart:List<CartItem>,onAdd:(Product)->Unit,onQty:(String,Int)->Unit,onOrderDone:()->Unit){
    var checkout by remember{mutableStateOf(false)}
    val subtotal=cart.sumOf{it.product.priceCents*it.quantity}
    val shipping=if(subtotal>=19900||subtotal==0)0 else 1990
    val total=subtotal+shipping
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){
        Text("Seu carrinho",color=Navy,fontSize=23.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=8.dp))
        if(cart.isEmpty()){
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.ShoppingCart,null,tint=Blue,modifier=Modifier.size(60.dp));Text("Seu carrinho está vazio.",color=Color.Gray,modifier=Modifier.padding(top=10.dp))}}
        }else{
            LazyColumn(Modifier.weight(1f).padding(top=10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                items(items=cart){item->
                    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)){
                        Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                            Column(Modifier.weight(1f)){Text(item.product.name,color=Navy,fontWeight=FontWeight.Bold);Text(money(item.product.priceCents),color=Blue,fontWeight=FontWeight.Bold);Text(item.product.storeName,color=Color.Gray,fontSize=12.sp)}
                            Row(verticalAlignment=Alignment.CenterVertically){
                                IconButton(onClick={onQty(item.product.id,item.quantity-1)}){Icon(Icons.Default.Remove,null)}
                                Text(item.quantity.toString(),fontWeight=FontWeight.Bold)
                                IconButton(onClick={onQty(item.product.id,(item.quantity+1).coerceAtMost(item.product.stock))}){Icon(Icons.Default.Add,null)}
                            }
                        }
                    }
                }
            }
            Card(Modifier.fillMaxWidth().padding(vertical=10.dp),shape=RoundedCornerShape(16.dp)){
                Column(Modifier.padding(16.dp)){
                    Text("Resumo",color=Navy,fontSize=18.sp,fontWeight=FontWeight.Bold)
                    SummaryRow("Subtotal",money(subtotal));SummaryRow("Frete",if(shipping==0)"Grátis" else money(shipping))
                    HorizontalDivider(Modifier.padding(vertical=8.dp));SummaryRow("Total",money(total),true)
                }
            }
            Button(onClick={checkout=true},Modifier.fillMaxWidth().height(52.dp)){Text("Continuar para entrega e pagamento",fontSize=16.sp)}
            Spacer(Modifier.height(8.dp))
        }
    }
    if(checkout)CheckoutDialog(token,cart,subtotal,shipping,total,{checkout=false;onOrderDone()},{checkout=false})
}
@Composable fun SummaryRow(label:String,value:String,bold:Boolean=false){
    Row(Modifier.fillMaxWidth().padding(vertical=3.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(label,fontWeight=if(bold)FontWeight.Bold else FontWeight.Normal,color=if(bold)Navy else Color.Gray);Text(value,fontWeight=if(bold)FontWeight.Bold else FontWeight.Normal,color=if(bold)Blue else Navy)}
}

@Composable fun CheckoutDialog(token:String,cart:List<CartItem>,subtotal:Int,shipping:Int,total:Int,onDone:()->Unit,onCancel:()->Unit){
    var addresses by remember{mutableStateOf(listOf<Address>())};var selectedId by remember{mutableStateOf("")}
    var recipient by remember{mutableStateOf("")};var street by remember{mutableStateOf("")};var number by remember{mutableStateOf("")}
    var neighborhood by remember{mutableStateOf("")};var city by remember{mutableStateOf("")};var state by remember{mutableStateOf("")};var zip by remember{mutableStateOf("")};var complement by remember{mutableStateOf("")}
    var payment by remember{mutableStateOf("PIX")};var msg by remember{mutableStateOf("")};var loading by remember{mutableStateOf(false)}
    val ex=remember{Executors.newSingleThreadExecutor()}
    fun load(){ex.execute{val r=call("GET","/addresses",token=token);val a=r.json.optJSONArray("addresses")?:JSONArray();val l=mutableListOf<Address>();for(i in 0 until a.length()){val o=a.getJSONObject(i);l.add(Address(o.optString("id"),o.optString("recipient"),o.optString("street"),o.optString("number"),o.optString("complement"),o.optString("neighborhood"),o.optString("city"),o.optString("state"),o.optString("zip")))};Handler(Looper.getMainLooper()).post{addresses=l;if(l.isNotEmpty())selectedId=l.first().id}}}
    LaunchedEffect(Unit){load()}
    DisposableEffect(Unit){onDispose{ex.shutdownNow()}}
    AlertDialog(onDismissRequest=onCancel,title={Text("Finalizar compra")},text={
        Column{
            Text("Entrega",color=Navy,fontWeight=FontWeight.Bold)
            if(addresses.isNotEmpty()){
                addresses.forEach{a->FilterChip(selected=selectedId==a.id,onClick={selectedId=a.id},label={Text("${a.street}, ${a.number} — ${a.city}")},modifier=Modifier.padding(vertical=2.dp))}
            }else{
                OutlinedTextField(recipient,{recipient=it},label={Text("Recebedor")},singleLine=true)
                OutlinedTextField(street,{street=it},label={Text("Rua")},singleLine=true)
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedTextField(value=number,onValueChange={number=it},modifier=Modifier.weight(1f),label={Text("Nº")},singleLine=true);OutlinedTextField(value=zip,onValueChange={zip=it},modifier=Modifier.weight(1.5f),label={Text("CEP")},singleLine=true)}
                OutlinedTextField(neighborhood,{neighborhood=it},label={Text("Bairro")},singleLine=true)
                OutlinedTextField(city,{city=it},label={Text("Cidade")},singleLine=true)
                OutlinedTextField(state,{state=it},label={Text("UF")},singleLine=true)
                OutlinedTextField(complement,{complement=it},label={Text("Complemento")},singleLine=true)
            }
            Text("Pagamento",color=Navy,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=10.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("PIX","CARTÃO","BOLETO").forEach{m->FilterChip(selected=payment==m,onClick={payment=m},label={Text(m)})}}
            Text("Total: ${money(total)}",color=Blue,fontSize=20.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=10.dp))
            if(msg.isNotBlank())Text(msg,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(top=6.dp))
        }},
        confirmButton={Button(onClick={
            loading=true;msg=""
            ex.execute{
                var addr=selectedId
                if(addr.isBlank()){
                    val ar=call("POST","/addresses",JSONObject().apply{put("recipient",recipient);put("street",street);put("number",number);put("complement",complement);put("neighborhood",neighborhood);put("city",city);put("state",state);put("zip",zip)},token)
                    if(!ar.ok){Handler(Looper.getMainLooper()).post{loading=false;msg=ar.message};return@execute}
                    addr=ar.json.optJSONObject("address")?.optString("id","")?:""
                }
                val items=JSONArray();cart.forEach{items.put(JSONObject().apply{put("productId",it.product.id);put("quantity",it.quantity)})}
                val r=call("POST","/orders",JSONObject().apply{put("addressId",addr);put("paymentMethod",payment);put("items",items)},token)
                Handler(Looper.getMainLooper()).post{loading=false;if(r.ok)onDone()else msg=r.message}
            }
        },enabled=!loading&&(selectedId.isNotBlank()||(recipient.isNotBlank()&&street.isNotBlank()&&number.isNotBlank()&&neighborhood.isNotBlank()&&city.isNotBlank()&&state.isNotBlank()&&zip.isNotBlank()))){
            Text(if(loading)"Processando..." else "Confirmar pedido")
        }},
        dismissButton={TextButton(onClick=onCancel){Text("Voltar")}})
}

@Composable fun OrdersTab(token:String){
    var orders by remember{mutableStateOf(listOf<Order>())};var loading by remember{mutableStateOf(true)};var selected by remember{mutableStateOf<Order?>(null)}
    val ex=remember{Executors.newSingleThreadExecutor()}
    fun load(){loading=true;ex.execute{val r=call("GET","/orders",token=token);val a=r.json.optJSONArray("orders")?:JSONArray();val l=mutableListOf<Order>();for(i in 0 until a.length()){val o=a.getJSONObject(i);val ia=o.optJSONArray("items")?:JSONArray();val names=mutableListOf<String>();for(j in 0 until ia.length())names.add(ia.getJSONObject(j).optString("product_name"));l.add(Order(o.optString("id"),o.optString("status"),o.optInt("subtotal_cents"),o.optInt("shipping_cents"),o.optInt("total_cents"),o.optString("payment_method"),o.optString("created_at"),o.optString("updated_at"),o.optString("tracking_code"),o.optString("courier_name"),names))};Handler(Looper.getMainLooper()).post{orders=l;loading=false}}}
    DisposableEffect(Unit){load();onDispose{ex.shutdownNow()}}
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){
        Text("Meus pedidos",color=Navy,fontSize=23.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=8.dp))
        if(loading)CircularProgressIndicator(Modifier.padding(25.dp))
        else if(orders.isEmpty())Text("Você ainda não fez nenhum pedido.",color=Color.Gray,modifier=Modifier.padding(top=25.dp))
        LazyColumn(Modifier.fillMaxSize().padding(top=10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            items(items=orders){o->Card(Modifier.fillMaxWidth().clickable{selected=o},shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(16.dp)){
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Pedido #${o.id.take(8)}",color=Navy,fontWeight=FontWeight.Bold);Text(statusLabel(o.status),color=Blue,fontWeight=FontWeight.Bold)}
                Text(o.items.joinToString(" • "),color=Color.Gray,fontSize=12.sp,modifier=Modifier.padding(top=6.dp))
                Text(money(o.total),color=Blue,fontSize=20.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=8.dp))
                Text("Pagamento: ${o.payment}",color=Color.Gray,fontSize=12.sp)
                if(o.trackingCode.isNotBlank())Text("Rastreamento: ${o.trackingCode}",color=Color.Gray,fontSize=12.sp)
            }}}
        }
    }
    selected?.let{OrderTrackingDialog(it,{selected=null})}
}
fun statusLabel(s:String)=when(s){"PENDING"->"Aguardando pagamento";"PAID"->"Pago";"PROCESSING"->"Preparando";"SHIPPED"->"Enviado";"DELIVERED"->"Entregue";else->s}


@Composable fun OrderTrackingDialog(order:Order,onClose:()->Unit){
    AlertDialog(onDismissRequest=onClose,title={Text("Acompanhar pedido")},text={Column{
        Text("Pedido #${order.id.take(8)}",color=Navy,fontWeight=FontWeight.Bold)
        Text(statusLabel(order.status),color=Blue,fontSize=20.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=8.dp))
        Text("Linha do pedido",color=Navy,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=14.dp))
        val steps=listOf("PENDING" to "Pedido recebido","PAID" to "Pagamento confirmado","PROCESSING" to "Em preparação","SHIPPED" to "Saiu para entrega","DELIVERED" to "Entregue")
        val rank=steps.indexOfFirst{it.first==order.status}.let{if(it<0)0 else it}
        steps.forEachIndexed{i,step->
            Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.padding(vertical=3.dp)){
                Icon(if(i<=rank)Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,null,tint=if(i<=rank)Blue else Color.LightGray,modifier=Modifier.size(20.dp))
                Text(step.second,color=if(i<=rank)Navy else Color.Gray,modifier=Modifier.padding(start=8.dp))
            }
        }
        if(order.trackingCode.isNotBlank())Text("Código: ${order.trackingCode}",color=Color.Gray,modifier=Modifier.padding(top=10.dp))
        if(order.courierName.isNotBlank())Text("Entregador: ${order.courierName}",color=Color.Gray)
        Text("Total: ${money(order.total)}",color=Blue,fontSize=20.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=10.dp))
    }},confirmButton={Button(onClick=onClose){Text("Fechar")}})
}

@Composable fun AccountTab(name:String,token:String,logout:()->Unit){
    var showStore by remember{mutableStateOf(false)}
    var showProduct by remember{mutableStateOf(false)}
    var showSellerOrders by remember{mutableStateOf(false)}
    var stores by remember{mutableStateOf(listOf<Store>())}
    var selectedStoreId by remember{mutableStateOf("")}
    var msg by remember{mutableStateOf("")}
    val ex=remember{Executors.newSingleThreadExecutor()}
    fun load(){
        ex.execute{
            val r=call("GET","/my/stores",token=token)
            val a=r.json.optJSONArray("stores")?:JSONArray();val l=mutableListOf<Store>()
            for(i in 0 until a.length()){val o=a.getJSONObject(i);l.add(Store(o.optString("id"),o.optString("name"),o.optString("description"),o.optInt("product_count")))}
            Handler(Looper.getMainLooper()).post{stores=l;if(selectedStoreId.isBlank()&&l.isNotEmpty())selectedStoreId=l.first().id}
        }
    }
    DisposableEffect(Unit){load();onDispose{ex.shutdownNow()}}
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp)){
        Text("Minha conta",color=Navy,fontSize=23.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=8.dp))
        Card(Modifier.fillMaxWidth().padding(top=12.dp),shape=RoundedCornerShape(20.dp)){
            Column(Modifier.padding(18.dp)){
                Text(name,color=Navy,fontSize=20.sp,fontWeight=FontWeight.Bold)
                Text("Cliente e vendedor",color=Color.Gray)
                Text("API conectada ✓",color=Color(0xFF159447),fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=12.dp))
            }
        }
        Text("Minhas lojas",color=Navy,fontSize=18.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=18.dp))
        if(stores.isEmpty())Text("Você ainda não possui uma loja.",color=Color.Gray,modifier=Modifier.padding(top=6.dp))
        stores.forEach{s->
            Card(Modifier.fillMaxWidth().padding(top=8.dp),shape=RoundedCornerShape(14.dp)){
                Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                    Icon(Icons.Default.Storefront,null,tint=Blue)
                    Column(Modifier.weight(1f).padding(start=10.dp)){Text(s.name,color=Navy,fontWeight=FontWeight.Bold);Text("${s.productCount} produto(s)",color=Color.Gray,fontSize=12.sp)}
                    RadioButton(selected=selectedStoreId==s.id,onClick={selectedStoreId=s.id})
                }
            }
        }
        Button(onClick={showStore=true},modifier=Modifier.fillMaxWidth().padding(top=12.dp)){
            Icon(Icons.Default.Storefront,null);Spacer(Modifier.width(6.dp));Text("Criar minha loja")
        }
        Button(onClick={if(selectedStoreId.isNotBlank())showProduct=true else msg="Crie uma loja primeiro."},
            modifier=Modifier.fillMaxWidth().padding(top=7.dp),enabled=stores.isNotEmpty()){
            Icon(Icons.Default.AddBox,null);Spacer(Modifier.width(6.dp));Text("Cadastrar produto")
        }
        Button(onClick={showSellerOrders=true},modifier=Modifier.fillMaxWidth().padding(top=7.dp),enabled=stores.isNotEmpty()){
            Icon(Icons.Default.LocalShipping,null);Spacer(Modifier.width(6.dp));Text("Pedidos da minha loja")
        }
        if(msg.isNotBlank())Text(msg,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(top=6.dp))
        OutlinedButton(onClick=logout,modifier=Modifier.fillMaxWidth().padding(top=8.dp)){
            Icon(Icons.Default.Logout,null);Spacer(Modifier.width(6.dp));Text("Sair")
        }
    }
    if(showStore)StoreDialog(token,{showStore=false;load()},{showStore=false})
    if(showProduct)SellerProductDialog(token,selectedStoreId,{showProduct=false;load()},{showProduct=false})
    if(showSellerOrders)SellerOrdersDialog(token,{showSellerOrders=false})
}


@Composable fun SellerOrdersDialog(token:String,onClose:()->Unit){
    var orders by remember{mutableStateOf(listOf<JSONObject>())};var loading by remember{mutableStateOf(true)};var msg by remember{mutableStateOf("")}
    val ex=remember{Executors.newSingleThreadExecutor()}
    fun load(){ex.execute{val r=call("GET","/seller/orders",token=token);val a=r.json.optJSONArray("orders")?:JSONArray();val l=mutableListOf<JSONObject>();for(i in 0 until a.length())l.add(a.getJSONObject(i));Handler(Looper.getMainLooper()).post{loading=false;if(r.ok)orders=l else msg=r.message}}}
    LaunchedEffect(Unit){load()};DisposableEffect(Unit){onDispose{ex.shutdownNow()}}
    AlertDialog(onDismissRequest=onClose,title={Text("Pedidos da minha loja")},text={
        Column{if(loading)CircularProgressIndicator() else if(orders.isEmpty())Text("Nenhum pedido para suas lojas.",color=Color.Gray)
            LazyColumn(Modifier.heightIn(max=420.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){items(items=orders){o->
                Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp)){Column(Modifier.padding(12.dp)){
                    Text("#${o.optString("id").take(8)} — ${o.optString("customer_name")}",color=Navy,fontWeight=FontWeight.Bold)
                    Text("${o.optString("recipient")} • ${o.optString("city")}/${o.optString("state")}",color=Color.Gray,fontSize=12.sp)
                    Text(money(o.optInt("total_cents")),color=Blue,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=4.dp))
                    Text(statusLabel(o.optString("status")),color=Blue,fontSize=12.sp)
                    Row(horizontalArrangement=Arrangement.spacedBy(5.dp),modifier=Modifier.padding(top=7.dp)){
                        listOf("PROCESSING" to "Preparando","SHIPPED" to "Enviado","DELIVERED" to "Entregue").forEach{(st,label)->
                            TextButton(onClick={ex.execute{call("PUT","/seller/orders/${o.optString("id")}",JSONObject().apply{put("status",st)},token);load()}}){Text(label,fontSize=11.sp)}
                        }
                    }
                }}
            }}
            if(msg.isNotBlank())Text(msg,color=MaterialTheme.colorScheme.error)
        }},confirmButton={TextButton(onClick=onClose){Text("Fechar")}})
}

@Composable fun StoreDialog(token:String,onDone:()->Unit,onCancel:()->Unit){
    var n by remember{mutableStateOf("")};var d by remember{mutableStateOf("")};var msg by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest=onCancel,title={Text("Criar loja")},text={Column{
        OutlinedTextField(n,{n=it},label={Text("Nome da loja")},singleLine=true)
        Spacer(Modifier.height(8.dp));OutlinedTextField(d,{d=it},label={Text("Descrição")},singleLine=true)
        if(msg.isNotBlank())Text(msg,color=MaterialTheme.colorScheme.error)
    }},confirmButton={Button(onClick={
        val ex=Executors.newSingleThreadExecutor()
        ex.execute{val r=call("POST","/stores",JSONObject().apply{put("name",n.trim());put("description",d.trim())},token)
            Handler(Looper.getMainLooper()).post{if(r.ok)onDone()else msg=r.message};ex.shutdown()}
    },enabled=n.trim().length>=2){Text("Criar")}},dismissButton={TextButton(onClick=onCancel){Text("Cancelar")}})
}

@Composable fun SellerProductDialog(token:String,storeId:String,onDone:()->Unit,onCancel:()->Unit){
    var name by remember{mutableStateOf("")};var desc by remember{mutableStateOf("")}
    var price by remember{mutableStateOf("")};var stock by remember{mutableStateOf("")};var image by remember{mutableStateOf("")}
    var categories by remember{mutableStateOf(listOf<Category>())};var categoryId by remember{mutableStateOf("")}
    var msg by remember{mutableStateOf("")};var loading by remember{mutableStateOf(false)}
    val ex=remember{Executors.newSingleThreadExecutor()}
    LaunchedEffect(Unit){
        ex.execute{
            val r=call("GET","/categories",token=token);val a=r.json.optJSONArray("categories")?:JSONArray();val l=mutableListOf<Category>()
            for(i in 0 until a.length()){val o=a.getJSONObject(i);l.add(Category(o.optString("id"),o.optString("name")))}
            Handler(Looper.getMainLooper()).post{categories=l;if(l.isNotEmpty())categoryId=l.first().id}
        }
    }
    DisposableEffect(Unit){onDispose{ex.shutdownNow()}}
    AlertDialog(onDismissRequest=onCancel,title={Text("Cadastrar produto")},text={
        Column{
            OutlinedTextField(name,{name=it},label={Text("Nome do produto")},singleLine=true)
            OutlinedTextField(desc,{desc=it},label={Text("Descrição")},singleLine=true)
            OutlinedTextField(price,{price=it},label={Text("Preço em centavos")},singleLine=true)
            OutlinedTextField(stock,{stock=it},label={Text("Estoque")},singleLine=true)
            OutlinedTextField(image,{image=it},label={Text("URL da imagem (opcional)")},singleLine=true)
            if(categories.isNotEmpty()){
                Text("Categoria",color=Navy,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=8.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){categories.take(4).forEach{c->FilterChip(selected=categoryId==c.id,onClick={categoryId=c.id},label={Text(c.name)})}}
            }
            if(msg.isNotBlank())Text(msg,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(top=6.dp))
        }},
        confirmButton={Button(onClick={
            loading=true;msg=""
            ex.execute{
                val r=call("POST","/products",JSONObject().apply{
                    put("storeId",storeId);put("name",name.trim());put("description",desc.trim())
                    put("priceCents",price.toIntOrNull()?:0);put("stock",stock.toIntOrNull()?:0)
                    put("categoryId",categoryId);put("imageUrl",image.trim())
                },token)
                Handler(Looper.getMainLooper()).post{loading=false;if(r.ok)onDone()else msg=r.message}
            }
        },enabled=!loading&&name.isNotBlank()&&(price.toIntOrNull()?:0)>0&&(stock.toIntOrNull()?:-1)>=0){
            Text(if(loading)"Salvando..." else "Cadastrar")
        }},
        dismissButton={TextButton(onClick=onCancel){Text("Cancelar")}})
}
