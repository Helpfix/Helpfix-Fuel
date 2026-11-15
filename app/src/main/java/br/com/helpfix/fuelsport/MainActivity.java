package br.com.helpfix.fuelsport;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

/**
 * Tela de viagem: somente painel automotivo. Configuracoes ficam no menu recolhido.
 * A interface gira para paisagem para privilegiar os dois mostradores.
 */
public final class MainActivity extends Activity {
    private static final int GPS_CODE=121;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SportGaugeView dashboard;
    private LinearLayout menuPane;
    private TextView menuHandle;
    private boolean menuOpen=false;
    private boolean pendingTrip=false,pendingOverlay=false,pendingSystemOverlay=false;
    private final int CYAN=0xff00d7ff;

    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable rounded(int fill,int outline,int radius){
        GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));
        if(outline!=0)g.setStroke(dp(1),outline);return g;
    }
    private TextView label(String t,int size,int color){
        TextView v=new TextView(this);v.setText(t);v.setTextSize(size);v.setTextColor(color);return v;
    }
    private Button action(String title,int color){
        Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextSize(14);
        b.setTextColor(Color.WHITE);b.setBackground(rounded(color,0xff345369,11));return b;
    }
    private void fullscreen(){
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);fullscreen();
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(0xff030910);setContentView(root);
        dashboard=new SportGaugeView(this);
        root.addView(dashboard,new FrameLayout.LayoutParams(-1,-1));

        // Painel de opcoes que so aparece quando se toca em MENU.
        menuPane=new LinearLayout(this);menuPane.setOrientation(LinearLayout.VERTICAL);
        menuPane.setBackground(rounded(0xf50b1723,0xff2b6c89,16));
        FrameLayout.LayoutParams paneParams=new FrameLayout.LayoutParams(
            Math.min(dp(495),getResources().getDisplayMetrics().widthPixels-dp(24)),
            FrameLayout.LayoutParams.MATCH_PARENT,Gravity.RIGHT|Gravity.TOP);
        paneParams.setMargins(0,dp(8),dp(8),dp(48));
        root.addView(menuPane,paneParams);
        menuPane.setVisibility(View.GONE);
        buildMenu();

        menuHandle=label("☰   MENU",14,0xffa4ecff);
        menuHandle.setGravity(Gravity.CENTER);
        menuHandle.setTypeface(null,1);
        menuHandle.setBackground(rounded(0xf5071c29,0xff3184a2,12));
        FrameLayout.LayoutParams handleParams=new FrameLayout.LayoutParams(dp(140),dp(37),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
        handleParams.bottomMargin=dp(7);
        root.addView(menuHandle,handleParams);
        menuHandle.setOnClickListener(v->toggleMenu());
        updateDashboard();
    }
    private void toggleMenu(){
        menuOpen=!menuOpen;
        menuPane.setVisibility(menuOpen?View.VISIBLE:View.GONE);
        menuHandle.setText(menuOpen?"✕   FECHAR MENU":"☰   MENU");
    }
    private void buildMenu(){
        TextView title=label("HELPFIX FUEL  /  CONTROLES",17,Color.WHITE);
        title.setTypeface(null,1);title.setPadding(dp(15),dp(13),dp(10),dp(6));menuPane.addView(title);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);
        menuPane.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(13),dp(2),dp(13),dp(12));scroll.addView(list);
        addAction(list,"▶  INICIAR VIAGEM (GPS)",0xff065573,()->startTrip());
        addAction(list,"▣  MOSTRAR SOBRE O WAZE / MAPS",0xff075167,()->showOverlay());
        addAction(list,"◻  OCULTAR PAINEL FLUTUANTE",0xff202f40,()->send(TrackingService.HIDE));
        addAction(list,"■  ENCERRAR VIAGEM",0xff552936,()->{
            if(FuelStore.active(this))send(TrackingService.STOP);
            FuelStore.prefs(this).edit().putBoolean("active",false).putFloat("speed",0).apply();
            updateDashboard();toast("Viagem encerrada");
        });
        addAction(list,FuelCostMenu.TITLE,0xff075167,()->new FuelCostMenu(this).show());
        addAction(list,"⛽  AJUSTAR COMBUSTÍVEL E CONSUMO",0xff1c435b,()->fuelDialog());
        addAction(list,"✦  COR DO PAINEL",0xff1d394e,()->styleDialog());
        TextView notice=label("Velocidade: GPS do celular.\nCombustível (%), litros e autonomia (km) são ESTIMADOS a partir do abastecimento, consumo médio e distância registrada. Sem leitura do tanque/OBD.\n\nEvite configurar enquanto dirige.",12,0xffa9bfd0);
        notice.setPadding(dp(8),dp(6),dp(8),dp(8));list.addView(notice);
    }
    private void addAction(LinearLayout list,String title,int color,Runnable job){
        Button b=action(title,color);list.addView(b,new LinearLayout.LayoutParams(-1,dp(46)));
        b.setOnClickListener(v->{job.run();if(menuOpen)toggleMenu();});
        View spacer=new View(this);list.addView(spacer,new LinearLayout.LayoutParams(1,dp(8)));
    }
    private void toast(String t){Toast.makeText(this,t,Toast.LENGTH_SHORT).show();}
    private EditText numeric(float value){
        EditText t=new EditText(this);
        t.setSingleLine(true);t.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        t.setText(String.format(Locale.US,"%.1f",value));t.setTextColor(Color.WHITE);
        t.setTextSize(17);t.setSelectAllOnFocus(true);
        t.setPadding(dp(9),dp(8),dp(9),dp(8));t.setBackground(rounded(0xff1c3546,0xff3b7f99,7));
        return t;
    }
    private void field(LinearLayout pane,String title,EditText input){
        TextView t=label(title,13,0xffc1d8e9);t.setPadding(0,dp(10),0,dp(3));pane.addView(t);
        pane.addView(input,new LinearLayout.LayoutParams(-1,dp(40)));
    }
    private void fuelDialog(){
        LinearLayout inputs=new LinearLayout(this);inputs.setOrientation(LinearLayout.VERTICAL);
        inputs.setPadding(dp(18),dp(5),dp(18),dp(8));inputs.setBackgroundColor(0xff112332);
        EditText fuel=numeric(FuelStore.get(this,"liters",15f));
        EditText tank=numeric(FuelStore.get(this,"capacity",46f));
        EditText mileage=numeric(FuelStore.get(this,"km_per_l",14f));
        field(inputs,"Litros estimados no tanque",fuel);
        field(inputs,"Capacidade total do tanque (litros)",tank);
        field(inputs,"Consumo médio (km/L)",mileage);
        AlertDialog dialog=new AlertDialog.Builder(this)
            .setTitle("Configurar combustível estimado")
            .setView(inputs)
            .setNegativeButton("Cancelar",null)
            .setPositiveButton("Salvar",null).create();
        dialog.setOnShowListener(unused->{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try {
                float l=Float.parseFloat(fuel.getText().toString().replace(',','.'));
                float c=Float.parseFloat(tank.getText().toString().replace(',','.'));
                float k=Float.parseFloat(mileage.getText().toString().replace(',','.'));
                if(!Float.isFinite(l)||!Float.isFinite(c)||!Float.isFinite(k)||
                    c<=0||c>300||l<0||l>c||k<=0||k>100)throw new NumberFormatException();
                FuelStore.prefs(this).edit().putFloat("liters",l).putFloat("capacity",c)
                    .putFloat("km_per_l",k).apply();
                if(FuelStore.active(this))send(TrackingService.REFRESH);
                updateDashboard();dialog.dismiss();toast("Abastecimento/consumo salvos");
            }catch(NumberFormatException e){toast("Verifique litros, capacidade e consumo");}
        });});
        dialog.show();
    }
    private void styleDialog(){
        String[] names={"Azul ciano (imagem de referência)","Vermelho Racing","Âmbar clássico"};
        String[] ids={"blue","red","amber"};
        int old=0;String id=FuelStore.prefs(this).getString("accent","blue");
        for(int i=0;i<ids.length;i++)if(ids[i].equals(id))old=i;
        new AlertDialog.Builder(this).setTitle("Iluminação do painel")
            .setSingleChoiceItems(names,old,(d,which)->{
                FuelStore.setAccent(this,ids[which]);
                updateDashboard();if(FuelStore.active(this))send(TrackingService.REFRESH);
                d.dismiss();
            }).setNegativeButton("Cancelar",null).show();
    }
    private boolean hasGps(){
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;
    }
    private void askGps(){
        if(Build.VERSION.SDK_INT>=33)requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
           Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.POST_NOTIFICATIONS},GPS_CODE);
        else requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
           Manifest.permission.ACCESS_COARSE_LOCATION},GPS_CODE);
    }
    private void startTrip(){
        if(!hasGps()){pendingTrip=true;askGps();return;}
        send(TrackingService.START);toast("Monitoramento GPS iniciado");
    }
    private void showOverlay(){
        if(!hasGps()){pendingOverlay=true;askGps();return;}
        if(Build.VERSION.SDK_INT>=23 && !Settings.canDrawOverlays(this)){
            pendingSystemOverlay=true;
            Intent permission=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:"+getPackageName()));
            startActivity(permission);return;
        }
        if(!FuelStore.active(this))send(TrackingService.START);
        send(TrackingService.SHOW);
        toast("Painel flutuante ativado");
    }
    private void send(String action){
        Intent service=new Intent(this,TrackingService.class);service.setAction(action);
        try{
            if(TrackingService.START.equals(action)||TrackingService.SHOW.equals(action))
                startForegroundService(service);
            else if(FuelStore.active(this))startService(service);
        }catch(Exception e){toast("Não foi possível iniciar o serviço. Confira as permissões.");}
    }
    @Override public void onRequestPermissionsResult(int code,String[] perms,int[] results){
        super.onRequestPermissionsResult(code,perms,results);
        if(code!=GPS_CODE)return;
        if(!hasGps()){pendingTrip=false;pendingOverlay=false;toast("Permita a localização para utilizar o GPS");return;}
        if(pendingTrip){pendingTrip=false;startTrip();}
        if(pendingOverlay){pendingOverlay=false;showOverlay();}
    }
    @Override protected void onResume(){
        super.onResume();fullscreen();
        if(pendingSystemOverlay && Settings.canDrawOverlays(this)){
            pendingSystemOverlay=false;
            handler.post(()->showOverlay());
        }
        handler.removeCallbacks(uiTick);
        handler.post(uiTick);
    }
    @Override protected void onPause(){handler.removeCallbacks(uiTick);super.onPause();}
    private final Runnable uiTick=new Runnable(){@Override public void run(){
        updateDashboard();handler.postDelayed(this,1000L);
    }};
    private void updateDashboard(){
        if(dashboard==null)return;
        float fuel=FuelStore.get(this,"liters",15f);
        float consumption=FuelStore.get(this,"km_per_l",14f);
        float capacity=FuelStore.get(this,"capacity",46f);
        dashboard.setAccent(FuelStore.accent(this));
        dashboard.setMetrics(FuelStore.get(this,"speed",0f),fuel,
            fuel*consumption,capacity,FuelStore.get(this,"distance",0f),FuelStore.active(this));
    }
}
