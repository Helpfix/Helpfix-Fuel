package br.com.helpfix.fuelsport;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

public final class FuelStore {
    public static final String PREFS = "fuel_sport_prefs";
    public static SharedPreferences prefs(Context context){return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    public static float get(Context c,String k,float fallback){return prefs(c).getFloat(k,fallback);}
    public static boolean active(Context c){return prefs(c).getBoolean("active",false);}
    public static int accent(Context c){
        String v=prefs(c).getString("accent","blue");
        if("blue".equals(v))return Color.rgb(46,202,255);
        if("amber".equals(v))return Color.rgb(255,188,62);
        return Color.rgb(255,79,96);
    }
    public static void setAccent(Context c,String name){prefs(c).edit().putString("accent",name).apply();}
    private FuelStore(){}
}
