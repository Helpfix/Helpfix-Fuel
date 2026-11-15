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
        String v=prefs(c).getString("accent","red");
        if("blue".equals(v))return Color.rgb(46,202,255);
        if("green".equals(v))return Color.rgb(84,225,155);
        return Color.rgb(255,65,78);
    }
    public static void setAccent(Context c,String name){prefs(c).edit().putString("accent",name).apply();}
    private FuelStore(){}
}
