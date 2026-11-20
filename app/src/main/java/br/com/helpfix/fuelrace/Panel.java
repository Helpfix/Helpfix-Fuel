package br.com.helpfix.fuelrace;

import android.Manifest;
import android.app.Activity;
import android.os.*;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.*;
import android.view.*;
import android.webkit.*;
import org.json.JSONObject;

public class Panel extends Activity implements LocationListener {
    WebView view;
    LocationManager manager;
    Location previous;
    boolean ready, active;
    final Handler timer = new Handler();
    final Runnable stale = () -> status("Aguardando sinal GPS");
    public void onCreate(Bundle saved) {
        super.onCreate(saved);
        manager = (LocationManager)getSystemService(LOCATION_SERVICE);
        view = new WebView(this);
        view.getSettings().setJavaScriptEnabled(true);
        view.getSettings().setDomStorageEnabled(true);
        view.getSettings().setAllowContentAccess(false);
        view.setWebViewClient(new WebViewClient(){
            public boolean shouldOverrideUrlLoading(WebView v,String url){return true;}
            public void onPageFinished(WebView v,String url){ready=true;start();}
        });
        view.addJavascriptInterface(new Object(){
            @JavascriptInterface public void map(double lat,double lon){
                if(!(Math.abs(lat)<=90 && Math.abs(lon)<=180))return;
                runOnUiThread(() -> {
                    android.net.Uri url=android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query="+lat+","+lon);
                    try { startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,url)); }
                    catch(android.content.ActivityNotFoundException e) {
                        android.widget.Toast.makeText(Panel.this,"Abra as coordenadas exibidas em um app de mapas.",android.widget.Toast.LENGTH_LONG).show();
                    }
                });
            }
            @JavascriptInterface public void gps(){runOnUiThread(() -> {
                if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},7);
                else start();
            });}
        },"Car");
        setContentView(view);
        view.loadUrl("file:///android_asset/index.html");
        bars();
    }
    void bars(){
        boolean wide=getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        getWindow().getDecorView().setSystemUiVisibility(wide?5894:0);
        if(wide)getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);bars();}
    public void onWindowFocusChanged(boolean b){super.onWindowFocusChanged(b);if(b)bars();}
    void status(String label){previous=null;if(ready)view.evaluateJavascript("setSignal("+JSONObject.quote(label)+")",null);}
    void start(){
        if(!ready||!active)return;
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){status("Ativar GPS preciso");return;}
        try{
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,this);
            status(manager.isProviderEnabled(LocationManager.GPS_PROVIDER)?"Buscando sinal GPS":"GPS desligado");
        }catch(RuntimeException e){status("GPS indisponível");}
    }
    public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);start();}
    protected void onResume(){super.onResume();active=true;start();}
    protected void onPause(){active=false;timer.removeCallbacks(stale);manager.removeUpdates(this);status("GPS pausado");super.onPause();}
    public void onProviderDisabled(String p){status("GPS desligado");}
    public void onProviderEnabled(String p){status("Buscando sinal GPS");}
    public void onStatusChanged(String p,int s,Bundle b){}
    public void onLocationChanged(Location loc){
        if(!active)return;
        if(!loc.hasAccuracy()||loc.getAccuracy()>35||SystemClock.elapsedRealtimeNanos()-loc.getElapsedRealtimeNanos()>8000000000L){status("Sinal GPS fraco");return;}
        double speed=loc.hasSpeed()?loc.getSpeed()*3.6:-1, distance=0;
        if(previous!=null){
            double dt=(loc.getElapsedRealtimeNanos()-previous.getElapsedRealtimeNanos())/1e9;
            double m=previous.distanceTo(loc);
            if(dt>0 && dt<=8 && m/dt<90){
                if(speed<0)speed=m>Math.max(3,loc.getAccuracy())?m/dt*3.6:0;
                if(speed>=3)distance=m/1000;
            }
        }
        previous=loc;
        timer.removeCallbacks(stale);timer.postDelayed(stale,8500);
        view.evaluateJavascript("fix("+Math.round(speed)+","+distance+","+loc.getLatitude()+","+loc.getLongitude()+","+loc.getAccuracy()+")",null);
    }
    public void onBackPressed(){view.evaluateJavascript("closeForm()",v->{if(!"true".equals(v))finish();});}
}
