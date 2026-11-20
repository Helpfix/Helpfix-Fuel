const el=id=>document.getElementById(id);
const key='helpfix-fuel-v1';
let state=JSON.parse(localStorage.getItem(key)||'null')||initial(),speed=-1;
const num=(n,p=1)=>n.toLocaleString('pt-BR',{maximumFractionDigits:p});
function save(){localStorage.setItem(key,JSON.stringify(state));}
function dial(id,value,max,steps,labels,accent,large){
 const c=el(id),w=c.clientWidth,h=c.clientHeight,d=window.devicePixelRatio||1;
 c.width=w*d;c.height=h*d;
 const g=c.getContext('2d');g.scale(d,d);
 const r=Math.min(w*.465,h*(large?.47:.55)),x=w/2,y=h*(large?.51:.61);
 const angle=v=>Math.PI*(.8+1.4*v/max);
 g.translate(x,y);
 g.beginPath();g.strokeStyle='#455250';g.lineWidth=large?5:3;g.arc(0,0,r,angle(0),angle(max));g.stroke();
 for(let i=0;i<=steps;i++){
   const a=angle(max*i/steps),big=i%(steps/labels)===0;
   const point=k=>[Math.cos(a)*k,Math.sin(a)*k];
   g.beginPath();g.moveTo(...point(r*.94));g.lineTo(...point(r*(big?.80:.88)));g.strokeStyle='#acbab8';g.lineWidth=big?2:1;g.stroke();
   if(big){g.fillStyle='#d9e1de';g.textAlign='center';g.textBaseline='middle';g.font='bold '+Math.max(8,Math.min(14,r*.14))+'px sans-serif';g.fillText(Math.round(max*i/steps),...point(r*.66));}
 }
 g.rotate(angle(Math.max(0,Math.min(max,value))));
 g.fillStyle=value<0?'#69716f':accent;g.beginPath();g.moveTo(r*.96,0);g.lineTo(r*.67,-r*.048);g.lineTo(-r*.12,-r*.035);g.lineTo(-r*.12,r*.035);g.lineTo(r*.67,r*.048);g.closePath();g.fill();
 g.beginPath();g.arc(0,0,r*.073,0,Math.PI*2);g.fill();
}
function draw(){
 dial('dial',speed,220,44,11,'#d3fe57',true);
 const km=state.odo===null?null:state.odo+state.gps;
 dial('odial',km===null?-1:(km%1)*1000,1000,20,4,'#ff995d',false);
 el('totalKm').textContent=km===null?'—':num(km,1);
 el('odial').setAttribute('aria-label','Metros percorridos no quilômetro atual, de zero a mil');
}
function render(){
 const s=project(state,state.gps),known=s.fuel!==null;
 el('range').textContent=known?num(Math.floor(s.fuel*s.rate+1e-8),0):'—';
 el('rlabel').textContent=s.partial?'Autonomia dos litros registrados':'Autonomia estimada';
 el('note').textContent=s.partial?'O saldo anterior ao abastecimento é desconhecido.':known?'Estimativa pela média de consumo.':'Informe o total no tanque ou registre um abastecimento.';
 el('trip').textContent=s.odo===null?'—':num(s.trip)+' km';
 el('used').textContent=s.odo===null?'—':num(s.used,2)+' L';
 el('balance').textContent=known?(s.partial?'≥ ':'')+num(s.fuel,2)+' L':'Desconhecido';
 el('eff').textContent=num(s.rate,2)+' km/L';
 draw();updateRide();
 el('odo').textContent=s.odo===null?'Odômetro inicial pendente':'Última leitura · '+num(s.odo)+' km';
}
let latest=null,pending=false,parkTimer,skipLeg=true;
function updateRide(){
 const t=state.ride;
 el('drive').textContent=t?.active?'Parar viagem':'Iniciar viagem';
 el('drive').classList.toggle('active',!!t?.active);
 el('rideInfo').textContent=t?(t.active?'Em viagem':'Última viagem')+' · '+num(t.km,2)+' km':'';
}
function toggleDrive(){
 if(state.ride?.active){state.ride.active=false;state.ride.end=Date.now();}
 else{state.ride={active:true,start:Date.now(),km:0};skipLeg=true;if(!latest&&window.Car)Car.gps();}
 save();updateRide();
}
function parkInfo(){
 const p=state.park;
 el('parkInfo').textContent=p?'Local salvo em '+new Date(p.time).toLocaleString('pt-BR')+'\n'+p.lat.toFixed(6)+', '+p.lon.toFixed(6)+' · precisão aproximada de '+Math.round(p.acc)+' m':'Nenhum local marcado.';
 el('map').hidden=!p;el('mark').textContent=p?'Atualizar local do carro':'Marcar onde parei';
}
function openParking(){el('parkSheet').hidden=false;el('parkMsg').textContent='';parkInfo();}
function capture(){
 if(!pending||!latest||Date.now()-latest.time>5000)return;
 pending=false;clearTimeout(parkTimer);state.park={...latest};save();parkInfo();el('parkMsg').textContent='Local do carro salvo neste aparelho.';
}
function markParking(){
 pending=true;clearTimeout(parkTimer);el('parkMsg').textContent='Aguardando uma posição GPS precisa…';capture();
 if(pending){if(window.Car)Car.gps();parkTimer=setTimeout(()=>{pending=false;el('parkMsg').textContent='Sem posição recente. Confira o GPS e a permissão de localização precisa e tente novamente.';},30000);}
}
function showMap(){if(state.park&&window.Car)Car.map(state.park.lat,state.park.lon);}
function setSignal(t){latest=null;skipLeg=true;speed=-1;el('signal').textContent=t;el('spd').textContent='—';draw();}
function fix(v,km,lat,lon,acc){
 speed=v;el('signal').textContent='● GPS ativo';el('spd').textContent=v<0?'—':v;
 if(Number.isFinite(lat)&&Number.isFinite(lon)&&acc<=35){latest={lat,lon,acc,time:Date.now()};capture();}
 if(state.ride?.active){
   if(!skipLeg&&km>0)state.ride.km+=km;
   if(!skipLeg&&state.odo!==null&&km>0)state.gps+=km;
   skipLeg=false;if(km>0)save();
 }
 render();
}
function openForm(){el('sheet').hidden=false;el('km').value=state.odo===null?'':String(state.odo).replace('.',',');el('rate').value=String(state.rate).replace('.',',');el('mode').value='keep';el('amount').value='';el('err').textContent='';fields();}
function closeForm(){if(!el('parkSheet').hidden){pending=false;clearTimeout(parkTimer);el('parkSheet').hidden=true;return true;}if(el('sheet').hidden)return false;el('sheet').hidden=true;return true;}
function fields(){const m=el('mode').value,show=['add','total'].includes(m);el('amountLabel').hidden=!show;el('amountLabel').style.display=show?'block':'none';el('amountTitle').textContent=m==='add'?'Litros adicionados':'Total atual no tanque (L)';}
el('mode').onchange=fields;
function decimal(id){const v=el(id).value.trim();if(!/^\d+(?:[,.]\d+)?$/.test(v))return NaN;return Number(v.replace(',','.'));}
el('form').onsubmit=e=>{e.preventDefault();try{state=record(state,decimal('km'),el('mode').value,decimal('amount'),decimal('rate'));save();render();closeForm();}catch(err){el('err').textContent=err.message;}};
window.addEventListener('resize',draw);render();draw();
