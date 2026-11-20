const initial=()=>({odo:null,rate:14,trip:0,used:0,fuel:null,partial:false,gps:0});
function project(s,d){
  const burned=d/s.rate;
  return {...s,trip:s.trip+d,used:s.used+burned,fuel:s.fuel===null?null:Math.max(0,s.fuel-burned)};
}
function record(s,km,mode,liters,rate){
  if(!Number.isFinite(km)||km<0||(s.odo!==null&&km<s.odo))throw Error('Informe um odômetro válido, igual ou superior à última leitura.');
  if(!Number.isFinite(rate)||rate<=0)throw Error('Informe um consumo médio maior que zero.');
  if(['add','total'].includes(mode)&&(!Number.isFinite(liters)||liters<0||(mode==='add'&&liters===0)))throw Error('Confira a quantidade de litros.');
  const n=project(s,s.odo===null?0:km-s.odo);
  if(mode==='total'||mode==='unknown'){
    n.trip=0;n.used=0;n.partial=false;n.fuel=mode==='total'?liters:null;
  }else if(mode==='add'){
    n.partial=n.fuel===null||n.partial;n.fuel=(n.fuel||0)+liters;
  }
  return {...n,odo:km,rate,gps:0};
}
