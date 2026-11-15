package br.com.helpfix.fuelsport;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

public final class FuelStore {
    public static final String PREFS = "fuel_sport_prefs";
    public static SharedPreferences prefs(Context context){return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    public static float get(Context c,String k,float fallback){return prefs(c).getFloat(k,fallback);}
    public static boolean active(Context c){return prefs(c).getBoolean("active",false);}
    private static final String ODOMETER_BASE = "odometer_base";
    private static final String ODOMETER_GPS_BASE = "odometer_gps_base";
    static final String INTEGRATED_FILL = "integrated_last_fill";

    static double odometer(Context context) {
        SharedPreferences p = prefs(context);
        if (!p.contains(ODOMETER_BASE)) return Double.NaN;
        double base = Double.longBitsToDouble(p.getLong(ODOMETER_BASE, 0));
        return base + Math.max(0, (double) p.getFloat("distance", 0)
                - p.getFloat(ODOMETER_GPS_BASE, 0));
    }

    static SharedPreferences.Editor setOdometer(Context context, SharedPreferences.Editor editor, double km) {
        if (!validOdometer(km)) throw new IllegalArgumentException("Confira a quilometragem do painel.");
        return editor.putLong(ODOMETER_BASE, Double.doubleToLongBits(km))
                .putFloat(ODOMETER_GPS_BASE, get(context, "distance", 0));
    }

    static boolean validOdometer(double km) {
        return km >= 0 && km <= 9_999_999;
    }
    public static int accent(Context c){
        String v=prefs(c).getString("accent","blue");
        if("blue".equals(v))return Color.rgb(46,202,255);
        if("amber".equals(v))return Color.rgb(255,188,62);
        return Color.rgb(255,79,96);
    }
    public static void setAccent(Context c,String name){prefs(c).edit().putString("accent",name).apply();}
    private FuelStore(){}
}
