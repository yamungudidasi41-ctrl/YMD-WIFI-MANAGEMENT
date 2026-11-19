const $ = id => document.getElementById(id);
const money = n => 'TSh ' + Number(n || 0).toLocaleString('en-TZ');
async function api(path, options={}) {
  const response = await fetch('/api' + path, {
    ...options,
    headers: {'Content-Type':'application/json', ...(options.headers || {})}
  });
  if (response.status === 401) { showLogin(); throw new Error('Please sign in again.'); }
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) throw new Error(data?.error || 'Request failed');
  return data;
}
function message(text, error=false) {
  $('appMessage').textContent = text;
  $('appMessage').className = 'message' + (error ? ' error' : '');
}
function showLogin() { $('loginView').classList.remove('hidden'); $('appView').classList.add('hidden'); }
function showApp() { $('loginView').classList.add('hidden'); $('appView').classList.remove('hidden'); loadDashboard(); }
$('loginForm').addEventListener('submit', async e => {
  e.preventDefault();
  $('loginMessage').textContent = 'Signing in...';
  try {
    await api('/auth/login', {method:'POST', body:JSON.stringify({username:$('username').value,password:$('password').value})});
    $('password').value=''; $('loginMessage').textContent=''; showApp();
  } catch(err) { $('loginMessage').textContent=err.message; }
});
$('logoutBtn').onclick = async () => { try { await api('/auth/logout',{method:'POST'}); } finally { showLogin(); } };
document.querySelectorAll('.nav').forEach(btn => btn.onclick=()=>openPage(btn.dataset.page));
function openPage(name) {
  document.querySelectorAll('.page').forEach(p=>p.classList.add('hidden'));
  $(name+'Page').classList.remove('hidden');
  document.querySelectorAll('.nav').forEach(n=>n.classList.toggle('active',n.dataset.page===name));
  $('pageTitle').textContent=name[0].toUpperCase()+name.slice(1);
  ({dashboard:loadDashboard,customers:loadCustomers,packages:loadPackages,subscriptions:loadSubscriptions,payments:loadPayments})[name]();
}
async function loadDashboard() {
  try {
    const d=await api('/dashboard');
    $('statCustomers').textContent=d.customers;
    $('statPackages').textContent=d.packages;
    $('statActive').textContent=d.activeSubscriptions;
    $('statExpired').textContent=d.expiredSubscriptions;
    $('statRevenue').textContent=money(d.totalRevenueTsh);
  } catch(e){message(e.message,true);}
}
$('showCustomerForm').onclick=()=>{$('customerForm').classList.remove('hidden');$('customerForm').reset();$('customerId').value='';};
$('cancelCustomer').onclick=()=>$('customerForm').classList.add('hidden');
$('customerForm').onsubmit=async e=>{
  e.preventDefault();
  const id=$('customerId').value;
  const body={fullName:$('fullName').value,phone:$('phone').value,address:$('address').value};
  try { await api('/customers'+(id?'/'+id:''),{method:id?'PUT':'POST',body:JSON.stringify(body)}); $('customerForm').classList.add('hidden');message('Customer saved.');loadCustomers(); }
  catch(err){message(err.message,true);}
};
async function loadCustomers(){
  try{
    const rows=await api('/customers');
    $('customersTable').innerHTML=rows.map(c=>`<tr><td>${c.id}</td><td>${esc(c.fullName)}</td><td>${esc(c.phone)}</td><td>${esc(c.address||'—')}</td><td><button class="secondary" onclick="editCustomer(${c.id})">Edit</button><button class="danger" onclick="deleteCustomer(${c.id})">Delete</button></td></tr>`).join('')||'<tr><td colspan="5">No customers registered.</td></tr>';
    fillCustomerSelects(rows);
  }catch(e){message(e.message,true);}
}
window.editCustomer=async id=>{
  const c=(await api('/customers')).find(x=>x.id===id); if(!c)return;
  $('customerId').value=c.id;$('fullName').value=c.fullName;$('phone').value=c.phone;$('address').value=c.address||'';
  $('customerForm').classList.remove('hidden');window.scrollTo({top:0,behavior:'smooth'});
};
window.deleteCustomer=async id=>{
  if(!confirm('Delete this customer? This is not allowed if related subscriptions/payments exist.'))return;
  try{await api('/customers/'+id,{method:'DELETE'});message('Customer deleted.');loadCustomers();}catch(e){message(e.message,true);}
};
function fillCustomerSelects(rows){
  ['subCustomer','paymentCustomer'].forEach(id=>$(id).innerHTML=rows.map(c=>`<option value="${c.id}">${esc(c.fullName)} (${esc(c.phone)})</option>`).join(''));
}
$('showPackageForm').onclick=()=>$('packageForm').classList.toggle('hidden');
$('packageForm').onsubmit=async e=>{
  e.preventDefault();
  const body={name:$('packageName').value,durationDays:Number($('durationDays').value),priceTsh:Number($('priceTsh').value),description:$('packageDescription').value,active:true};
  try{await api('/packages',{method:'POST',body:JSON.stringify(body)});$('packageForm').reset();$('packageForm').classList.add('hidden');message('Package saved.');loadPackages();}catch(err){message(err.message,true);}
};
async function loadPackages(){
  try{
    const rows=await api('/packages');
    $('packagesList').innerHTML=rows.map(p=>`<article class="package-card"><h4>${esc(p.name)}</h4><strong>${money(p.priceTsh)}</strong><p>${p.durationDays} days</p><small>${esc(p.description||'')}</small></article>`).join('')||'<p>No packages yet.</p>';
    $('subPackage').innerHTML=rows.filter(p=>p.active).map(p=>`<option value="${p.id}">${esc(p.name)} — ${money(p.priceTsh)}</option>`).join('');
  }catch(e){message(e.message,true);}
}
$('showSubscriptionForm').onclick=async()=>{$('subscriptionForm').classList.toggle('hidden');await loadCustomers();await loadPackages();};
$('startDate').value=new Date().toISOString().slice(0,10);
$('subscriptionForm').onsubmit=async e=>{
  e.preventDefault();
  try{await api('/subscriptions',{method:'POST',body:JSON.stringify({customerId:$('subCustomer').value,packageId:$('subPackage').value,startDate:$('startDate').value})});$('subscriptionForm').classList.add('hidden');message('Subscription created.');loadSubscriptions();}catch(err){message(err.message,true);}
};
async function loadSubscriptions(){
  try{
    const rows=await api('/subscriptions');
    $('subscriptionsTable').innerHTML=rows.map(s=>`<tr><td>${esc(s.customer.fullName)}</td><td>${esc(s.wifiPackage.name)}</td><td>${s.startDate}</td><td>${s.expiryDate}</td><td><span class="status">${new Date(s.expiryDate+'T23:59:59')>=new Date()?'ACTIVE':'EXPIRED'}</span></td></tr>`).join('')||'<tr><td colspan="5">No subscriptions yet.</td></tr>';
  }catch(e){message(e.message,true);}
}
$('showPaymentForm').onclick=async()=>{$('paymentForm').classList.toggle('hidden');await loadCustomers();};
$('paymentForm').onsubmit=async e=>{
  e.preventDefault();
  const body={customerId:$('paymentCustomer').value,amountTsh:Number($('paymentAmount').value),method:$('paymentMethod').value,reference:$('paymentReference').value};
  try{await api('/payments',{method:'POST',body:JSON.stringify(body)});$('paymentForm').reset();$('paymentForm').classList.add('hidden');message('Payment recorded.');loadPayments();}catch(err){message(err.message,true);}
};
async function loadPayments(){
  try{
    const rows=await api('/payments');
    $('paymentsTable').innerHTML=rows.map(p=>`<tr><td>${p.paidAt?.replace('T',' ')||''}</td><td>${esc(p.customer.fullName)}</td><td>${money(p.amountTsh)}</td><td>${esc(p.method||'')}</td><td>${esc(p.reference||'—')}</td></tr>`).join('')||'<tr><td colspan="5">No payments recorded.</td></tr>';
  }catch(e){message(e.message,true);}
}
function esc(value){return String(value??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
(async()=>{try{await api('/auth/me');showApp();}catch(e){showLogin();}})();
