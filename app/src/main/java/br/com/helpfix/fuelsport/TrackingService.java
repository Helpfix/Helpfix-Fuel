package br.com.helpfix.fuelsport;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.drawable.GradientDrawable;


/** GPS trip tracking and movable Android overlay. Starts only from visible Activity. */
public class TrackingService extends Service implements LocationListener {
    public static final String START="br.com.helpfix.fuelsport.START";
    public static final String STOP="br.com.helpfix.fuelsport.STOP";
    public static final String SHOW="br.com.helpfix.fuelsport.SHOW";
    public static final String HIDE="br.com.helpfix.fuelsport.HIDE";
    public static final String REFRESH="br.com.helpfix.fuelsport.REFRESH";
    private static final String CHANNEL="helpfix_trip";
    private static final int NOTIFICATION_ID=73;
    private LocationManager lm;
    private Location previous;
    static final String START_RESULT = "trip_start_result";
    static final String OVERLAY_REQUESTED = "trip_overlay_requested";
    static final int STARTED = 1;
    static final int NO_LOCATION = 2;
    static final int START_FAILED = 3;
    static final int OVERLAY_FAILED = 4;
    private static boolean running;

    static boolean isRunning() {
        return running;
    }

    private WindowManager windowManager;
    private FrameLayout floating;
    private SportGaugeView dashboard;
    private WindowManager.LayoutParams params;
    private boolean mini=false;
    private int startX,startY;
    private float touchX,touchY;

    @Override public IBinder onBind(Intent i){return null;}
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (STOP.equals(action)) {
            endTrip();
            return START_NOT_STICKY;
        }
        if (HIDE.equals(action) || REFRESH.equals(action)) {
            if (running) {
                if (HIDE.equals(action)) removeOverlay();
                else updateOverlay();
            } else {
                stopSelf();
            }
            return START_NOT_STICKY;
        }
        if (!START.equals(action) && !SHOW.equals(action)) {
            if (!running) stopSelf();
            return START_NOT_STICKY;
        }

        ResultReceiver result = intent.getParcelableExtra(START_RESULT);
        int status;
        try {
            if (!running) {
                makeForeground();
                if (startLocation()) {
                    FuelStore.prefs(this).edit().putFloat("speed", 0).apply();
                    running = true;
                }
            }
            status = running ? STARTED : NO_LOCATION;
        } catch (SecurityException | IllegalStateException | IllegalArgumentException e) {
            android.util.Log.w("HelpfixFuel", "Falha ao iniciar a localização", e);
            status = START_FAILED;
        }
        if (status == STARTED) {
            if (SHOW.equals(action) || intent.getBooleanExtra(OVERLAY_REQUESTED, false)) {
                try {
                    addOverlay();
                } catch (SecurityException | IllegalStateException | IllegalArgumentException e) {
                    android.util.Log.w("HelpfixFuel", "Falha ao abrir painel flutuante", e);
                    removeOverlay();
                }
                if (floating == null) status = OVERLAY_FAILED;
            } else if (intent.hasExtra(OVERLAY_REQUESTED)) {
                removeOverlay();
            }
        } else {
            endTrip();
        }
        if (result != null) result.send(status, null);
        return START_NOT_STICKY;
    }

    private void endTrip() {
        running = false;
        previous = null;
        FuelStore.prefs(this).edit().putFloat("speed", 0).apply();
        stopLocation();
        removeOverlay();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void makeForeground(){
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26){NotificationChannel channel=new NotificationChannel(CHANNEL,"Helpfix Fuel Sport - Viagem",NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("GPS e painel flutuante ativos durante a viagem");nm.createNotificationChannel(channel);}
        Intent open=new Intent(this,MainActivity.class);
        PendingIntent pi=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification n;
        if(Build.VERSION.SDK_INT>=26){n=new Notification.Builder(this,CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_compass).setContentTitle("Helpfix Fuel Sport")
            .setContentText("Painel de viagem em execução").setOngoing(true).setContentIntent(pi).build();}
        else{n=new Notification.Builder(this).setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Helpfix Fuel Sport").setContentText("Viagem em execução").setContentIntent(pi).build();}
        if(Build.VERSION.SDK_INT>=29) startForeground(NOTIFICATION_ID,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        else startForeground(NOTIFICATION_ID,n);
    }
    private boolean startLocation() {
        lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (lm == null || (Build.VERSION.SDK_INT >= 28 && !lm.isLocationEnabled())) return false;
        boolean precise = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean approximate = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean registered = false;
        if (precise && lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, this);
            registered = true;
        }
        if ((precise || approximate) && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 5f, this);
            registered = true;
        }
        return registered;
    }

    private void stopLocation() {
        if (lm != null) {
            try {
                lm.removeUpdates(this);
            } catch (SecurityException e) {
                android.util.Log.w("HelpfixFuel", "Permissão de localização revogada", e);
            }
            lm = null;
        }
    }

    @Override public void onLocationChanged(Location loc){
        if(!running || loc==null)return;
        if(loc.hasAccuracy() && loc.getAccuracy()>100f)return;
        float speed=loc.hasSpeed()?Math.max(0,loc.getSpeed()*3.6f):0;
        float kmStep=0;
        if(previous!=null){
            long ms=loc.getElapsedRealtimeNanos()/1000000L-previous.getElapsedRealtimeNanos()/1000000L;
            if(ms>0 && ms<120000){
                float distance=previous.distanceTo(loc);
                float inferred=distance*3600f/ms;
                if(inferred<220f && distance>2 && (loc.getAccuracy()<40 || !loc.hasAccuracy())){
                    kmStep=distance/1000f;
                    if(!loc.hasSpeed())speed=inferred;
                }
            }
        }
        previous=loc;
        float consumption=Math.max(1,FuelStore.get(this,"km_per_l",14f));
        float fuel=Math.max(0,FuelStore.get(this,"liters",15f)-kmStep/consumption);
        float driven=FuelStore.get(this,"distance",0f)+kmStep;
        FuelStore.prefs(this).edit().putFloat("liters",fuel).putFloat("distance",driven)
            .putFloat("speed",Math.min(speed,240)).apply();
        updateOverlay();
    }
    @Override public void onProviderEnabled(String p){}
    @Override public void onProviderDisabled(String p){}
    @Override public void onStatusChanged(String p,int s,android.os.Bundle b){}

    private TextView button(String s){
        TextView b=new TextView(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(17);
        b.setGravity(Gravity.CENTER);b.setWidth(dp(33));b.setHeight(dp(32));
        return b;
    }
    private GradientDrawable bg(){GradientDrawable d=new GradientDrawable();d.setColor(0xf018202b);d.setCornerRadius(dp(11));d.setStroke(dp(1),0xff465063);return d;}
    private int dp(float px){return (int)(px*getResources().getDisplayMetrics().density+.5f);}
    private void addOverlay(){
        if(floating!=null){updateOverlay();return;}
        if(Build.VERSION.SDK_INT<23 || !Settings.canDrawOverlays(this))return;
        windowManager=(WindowManager)getSystemService(WINDOW_SERVICE);
        if (windowManager == null) return;
        mini = false;
        floating=new FrameLayout(this); floating.setBackground(bg());floating.setClipChildren(false);
        LinearLayout stack=new LinearLayout(this);stack.setOrientation(LinearLayout.VERTICAL);
        floating.addView(stack,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout bar=new LinearLayout(this);bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(10),0,dp(3),0);
        TextView move=new TextView(this);move.setText("≡  HELPFIX FUEL  •  ARRASTE");
        move.setTextColor(0xffced9e6);move.setTextSize(9);move.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(move,new LinearLayout.LayoutParams(0,dp(33),1));
        TextView shrink=button("−"),close=button("×");
        bar.addView(shrink);bar.addView(close);stack.addView(bar);
        android.util.DisplayMetrics display = getResources().getDisplayMetrics();
        int barHeight = dp(33);
        int maxWidthByHeight = Math.round((display.heightPixels - barHeight - dp(16)) / .56f);
        int widthPx = Math.min(dp(480), Math.min(display.widthPixels - dp(16), maxWidthByHeight));
        if (widthPx <= 0) {
            removeOverlay();
            return;
        }
        int dashboardH = Math.round(widthPx * .56f);
        dashboard=new SportGaugeView(this);dashboard.setCompact(true);
        stack.addView(dashboard,new LinearLayout.LayoutParams(-1,dashboardH));
        params=new WindowManager.LayoutParams(widthPx,dashboardH+barHeight,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.START;
        params.x = clampPosition(FuelStore.prefs(this).getInt("window_x", 20), display.widthPixels - widthPx);
        params.y = clampPosition(FuelStore.prefs(this).getInt("window_y", 120), display.heightPixels - params.height);
        move.setOnTouchListener((v,event)->{
            switch(event.getActionMasked()){
                case MotionEvent.ACTION_DOWN:touchX=event.getRawX();touchY=event.getRawY();startX=params.x;startY=params.y;return true;
                case MotionEvent.ACTION_MOVE:
                    params.x = clampPosition(startX + Math.round(event.getRawX() - touchX),
                            getResources().getDisplayMetrics().widthPixels - params.width);
                    params.y = clampPosition(startY + Math.round(event.getRawY() - touchY),
                            getResources().getDisplayMetrics().heightPixels - params.height);
                    try{windowManager.updateViewLayout(floating,params);}catch(Exception ignored){}return true;
                case MotionEvent.ACTION_UP:
                    FuelStore.prefs(this).edit().putInt("window_x",params.x).putInt("window_y",params.y).apply();return true;
            }return false;
        });
        shrink.setOnClickListener(v->{mini=!mini;dashboard.setVisibility(mini?View.GONE:View.VISIBLE);
            params.height = mini ? barHeight : dashboardH + barHeight;
            params.y = clampPosition(params.y, getResources().getDisplayMetrics().heightPixels - params.height);
            try{windowManager.updateViewLayout(floating,params);}catch(Exception ignored){}
            shrink.setText(mini?"+":"−");});
        close.setOnClickListener(v->removeOverlay());
        try{windowManager.addView(floating,params);}catch(Exception e){floating=null;dashboard=null;return;}
        updateOverlay();
    }
    private int clampPosition(int value, int max) {
        return Math.max(0, Math.min(max, value));
    }
    private void updateOverlay(){
        if(dashboard==null)return;
        float fuel=FuelStore.get(this,"liters",15), kmPerLiter=FuelStore.get(this,"km_per_l",14), tank=FuelStore.get(this,"capacity",46);
        dashboard.setAccent(FuelStore.accent(this));
        dashboard.setMetrics(FuelStore.get(this,"speed",0),fuel,fuel*kmPerLiter,tank,
            FuelStore.get(this,"distance",0),FuelStore.active(this),FuelStore.odometer(this));
    }
    private void removeOverlay(){if(floating!=null){try{windowManager.removeView(floating);}catch(Exception ignored){}
        floating=null;dashboard=null;}}
    @Override public void onDestroy(){
        running = false;
        FuelStore.prefs(this).edit().putFloat("speed", 0).apply();
        stopLocation();
        removeOverlay();
        super.onDestroy();
    }
}
