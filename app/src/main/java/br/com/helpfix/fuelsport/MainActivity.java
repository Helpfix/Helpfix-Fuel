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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class MainActivity extends Activity {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private SportGaugeView gauge;
    private TextView metrics;
    private EditText litersInput,consumptionInput,capacityInput;
    private boolean pendingOverlay=false;
    private boolean pendingStart=false;
    private static final int GPS_CODE=121;
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int fill,int border,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);
        d.setCornerRadius(dp(radius));if(border!=0)d.setStroke(dp(1),border);return d;}
    private TextView txt(String text,int size,int color){TextView v=new TextView(this);v.setText(text);v.setTextColor(color);v.setTextSize(size);return v;}
    private void gap(LinearLayout l,int dp){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(dp)));}
    private Button btn(String text,int fill){Button b=new Button(this);b.setText(text);b.setTextColor(Color.WHITE);
        b.setAllCaps(false);b.setTextSize(14);b.setBackground(bg(fill,0,12));return b;}
    private EditText number(String title,float initial){EditText t=new EditText(this);t.setSingleLine(true);t.setTextColor(Color.WHITE);
        t.setTextSize(18);t.setHintTextColor(0xff8998a8);t.setHint(title);
        t.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        t.setText(String.format(Locale.US,"%.1f",initial));t.setBackground(bg(0xff1a2430,0xff3b4859,10));
        t.setPadding(dp(10),dp(10),dp(10),dp(10));return t;}
    private void label(LinearLayout l,String value){TextView v=txt(value,12,0xffa9b9ca);l.addView(v);gap(l,6);}
    @Override public void onCreate(Bundle b){super.onCreate(b);
        getWindow().setStatusBarColor(0xff0d131c);getWindow().setNavigationBarColor(0xff0d131c);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(0xff0c111a);
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(18),dp(22),dp(18),dp(25));
        scroll.addView(panel);setContentView(scroll);
        TextView title=txt("HELPFIX FUEL",28,Color.WHITE);title.setTypeface(null,1);panel.addView(title);
        TextView subtitle=txt("SPORT EDITION  •  v3.2.3",12,0xffff6773);panel.addView(subtitle);gap(panel,15);
        gauge=new SportGaugeView(this);panel.addView(gauge,new LinearLayout.LayoutParams(-1,dp(232)));
        gap(panel,10);metrics=txt("",13,0xffa7b7c8);metrics.setGravity(Gravity.CENTER);panel.addView(metrics);
        gap(panel,20);
        label(panel,"CONFIGURAÇÃO DO COMBUSTÍVEL (ESTIMADA)");
        litersInput=number("Litros no tanque",FuelStore.get(this,"liters",15));panel.addView(litersInput);
        gap(panel,9);
        consumptionInput=number("Consumo em km/L",FuelStore.get(this,"km_per_l",14));panel.addView(consumptionInput);
        gap(panel,9);
        capacityInput=number("Capacidade do tanque em L",FuelStore.get(this,"capacity",46));panel.addView(capacityInput);
        gap(panel,12);
        Button save=btn("Salvar combustível e consumo",0xff2b4054);panel.addView(save);
        save.setOnClickListener(v->saveFuel());gap(panel,12);
        Button style=btn("🎨  Trocar estilo dos relógios",0xff364252);panel.addView(style);
        style.setOnClickListener(v->chooseStyle());gap(panel,12);
        Button start=btn("▶  Iniciar viagem por GPS",0xffa92e3c);panel.addView(start);
        start.setOnClickListener(v->startTrip());gap(panel,9);
        Button overlay=btn("▣  Mostrar painel flutuante",0xff244f65);panel.addView(overlay);
        overlay.setOnClickListener(v->showOverlay());gap(panel,9);
        Button hide=btn("—  Ocultar painel flutuante",0xff26313f);panel.addView(hide);
        hide.setOnClickListener(v->send(TrackingService.HIDE));gap(panel,9);
        Button stop=btn("■  Encerrar viagem",0xff34252c);panel.addView(stop);
        stop.setOnClickListener(v->{send(TrackingService.STOP);Toast.makeText(this,"Viagem encerrada",Toast.LENGTH_SHORT).show();});
        gap(panel,18);
        TextView foot=txt("Velocidade: GPS do celular. Combustível e autonomia: estimativas por litros informados e distância medida. Teste inicialmente com o carro parado.",12,0xff8898a8);
        panel.addView(foot);
        update();
    }
    private void saveFuel(){
        try{
            float liters=Float.parseFloat(litersInput.getText().toString().replace(',','.'));
            float consumption=Float.parseFloat(consumptionInput.getText().toString().replace(',','.'));
            float capacity=Float.parseFloat(capacityInput.getText().toString().replace(',','.'));
            if(!Float.isFinite(liters)||!Float.isFinite(consumption)||!Float.isFinite(capacity)||
              liters<0||consumption<=0||capacity<=0||liters>capacity||capacity>300||consumption>100)throw new NumberFormatException();
            FuelStore.prefs(this).edit().putFloat("liters",liters).putFloat("km_per_l",consumption)
                .putFloat("capacity",capacity).apply();
            if(FuelStore.active(this))send(TrackingService.REFRESH);
            update();Toast.makeText(this,"Parâmetros salvos",Toast.LENGTH_SHORT).show();
        }catch(NumberFormatException e){Toast.makeText(this,"Confira litros, consumo e capacidade",Toast.LENGTH_LONG).show();}
    }
    private void chooseStyle(){
        final String[] names={"Racing vermelho","GT azul","Sport verde"};
        final String[] ids={"red","blue","green"};
        new AlertDialog.Builder(this).setTitle("Estilo dos mostradores")
            .setSingleChoiceItems(names,-1,(dialog,which)->{FuelStore.setAccent(this,ids[which]);
                update();if(FuelStore.active(this))send(TrackingService.REFRESH);dialog.dismiss();})
            .setNegativeButton("Cancelar",null).show();
    }
    private boolean hasGps(){return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED ||
        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
    private void askGps(){
        if(Build.VERSION.SDK_INT>=33)requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.POST_NOTIFICATIONS},GPS_CODE);
        else requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},GPS_CODE);
    }
    private void startTrip(){saveFuel();if(!hasGps()){pendingStart=true;askGps();return;}
        send(TrackingService.START);Toast.makeText(this,"GPS de viagem iniciado",Toast.LENGTH_SHORT).show();}
    private void showOverlay(){
        if(!hasGps()){pendingOverlay=true;askGps();return;}
        if(Build.VERSION.SDK_INT>=23&&!Settings.canDrawOverlays(this)){
            pendingOverlay=true;Intent i=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:"+getPackageName()));startActivity(i);return;
        }
        if(!FuelStore.active(this))send(TrackingService.START);
        send(TrackingService.SHOW);
    }
    private void send(String a){Intent i=new Intent(this,TrackingService.class);i.setAction(a);
        if(a.equals(TrackingService.START)||a.equals(TrackingService.SHOW))startForegroundService(i);
        else startService(i);
    }
    @Override public void onRequestPermissionsResult(int code,String[] names,int[] results){
        super.onRequestPermissionsResult(code,names,results);
        if(code==GPS_CODE&&hasGps()){
            if(pendingStart){pendingStart=false;startTrip();}
            if(pendingOverlay){pendingOverlay=false;showOverlay();}
        }
    }
    @Override protected void onResume(){super.onResume();
        if(pendingOverlay&&hasGps()&&Build.VERSION.SDK_INT>=23&&Settings.canDrawOverlays(this)){
            pendingOverlay=false;showOverlay();}
        handler.post(uiTick);
    }
    @Override protected void onPause(){handler.removeCallbacks(uiTick);super.onPause();}
    private final Runnable uiTick=new Runnable(){@Override public void run(){update();handler.postDelayed(this,1000);}};
    private void update(){if(gauge==null)return;
        float liters=FuelStore.get(this,"liters",15f), kmPerLiter=FuelStore.get(this,"km_per_l",14f);
        float speed=FuelStore.get(this,"speed",0), distance=FuelStore.get(this,"distance",0), tank=FuelStore.get(this,"capacity",46);
        gauge.setAccent(FuelStore.accent(this));gauge.setMetrics(speed,liters,liters*kmPerLiter,tank);
        metrics.setText(String.format(Locale.getDefault(),"%s  •  %.1f km percorridos  •  %.0f km estimados",
            FuelStore.active(this)?"VIAGEM ATIVA":"VIAGEM PARADA",distance,liters*kmPerLiter));
    }
}
