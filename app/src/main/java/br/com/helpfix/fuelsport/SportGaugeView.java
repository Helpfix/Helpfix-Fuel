package br.com.helpfix.fuelsport;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;
import java.util.Locale;

/** Three car-style analog dials, optimized for a floating map overlay. */
public class SportGaugeView extends View {
    private final Paint p = new Paint(3);
    private int accent = Color.rgb(255, 65, 78);
    private float speedKmh = 0f;
    private float fuelLiters = 15f;
    private float rangeKm = 210f;
    private float capacity = 46f;
    private boolean compact = false;
    public SportGaugeView(Context ctx) { super(ctx); setLayerType(View.LAYER_TYPE_SOFTWARE, null); }
    public void setMetrics(float speed, float fuel, float range, float tank) {
        speedKmh=Math.max(0,speed); fuelLiters=Math.max(0,fuel); rangeKm=Math.max(0,range);
        capacity=Math.max(1,tank); invalidate();
    }
    public void setAccent(int color) { accent=color; invalidate(); }
    public void setCompact(boolean compact) { this.compact=compact; invalidate(); }

    private void color(int c) { p.setColor(c); p.setStyle(Paint.Style.FILL); p.setStrokeWidth(1); p.clearShadowLayer(); }
    private void text(Canvas c, String s, float x, float y, float size, int col, boolean bold) {
        color(col); p.setTypeface(bold ? android.graphics.Typeface.create("sans-serif-condensed",android.graphics.Typeface.BOLD)
                   : android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL));
        p.setTextSize(size); p.setTextAlign(Paint.Align.CENTER); c.drawText(s,x,y,p);
    }
    @Override protected void onDraw(Canvas actual) {
        super.onDraw(actual);
        float w=getWidth(), h=getHeight();
        if(w<=0||h<=0)return;
        float baseW=compact?325:365, baseH=compact?158:213;
        float scale=Math.min(w/baseW,h/baseH);
        actual.save();
        actual.translate((w-baseW*scale)/2f,(h-baseH*scale)/2f);
        actual.scale(scale,scale);
        Canvas c=actual;
        color(Color.rgb(10,14,21));
        p.setShadowLayer(8f,0,4,0x99000000);
        c.drawRoundRect(new RectF(3,3,baseW-3,baseH-3),18,18,p);p.clearShadowLayer();
        color(0xff27303d);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.6f);
        c.drawRoundRect(new RectF(3,3,baseW-3,baseH-3),18,18,p);
        color(accent);p.setStyle(Paint.Style.FILL);
        c.drawRoundRect(new RectF(14,10,74,13),2,2,p);
        text(c,"HELPFIX  /  SPORT",baseW/2,compact?24:28,compact?11:13,0xffd9e0e8,true);
        text(c,"GPS • PAINEL ESPORTIVO",baseW/2,compact?36:44,compact?7.5f:9,0xff7e8c9d,false);
        float cy=compact?96:123;
        float rMain=compact?46:65;
        float rSide=compact?31:44;
        gauge(c,baseW/2,cy,rMain,0,240,260f,speedKmh,"VELOCIDADE",String.format(Locale.US,"%.0f",speedKmh),"km/h",true);
        gauge(c,compact?53:60,cy+6,rSide,0,capacity,fuelLiters,"COMB.",String.format(Locale.US,"%.1f",fuelLiters),"L",false);
        gauge(c,baseW-(compact?53:60),cy+6,rSide,0,Math.max(100,capacity*24),rangeKm,"AUTON.",String.format(Locale.US,"%.0f",rangeKm),"km",false);
        if(!compact)text(c,"ESTIMATIVAS: abastecimento e consumo informados",baseW/2,201,9,0xff8996a4,false);
        c.restore();
    }
    private void gauge(Canvas c,float cx,float cy,float r,float min,float max,float value,
                       String title,String big,String unit,boolean isMain) {
        color(0xff171f2b);c.drawCircle(cx,cy,r+4,p);
        color(0xff263241);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(isMain?9:5);
        c.drawArc(new RectF(cx-r+5,cy-r+5,cx+r-5,cy+r-5),140,260,false,p);
        color(accent);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(isMain?7:4);p.setStrokeCap(Paint.Cap.ROUND);
        float frac=Math.max(0,Math.min(1,(value-min)/Math.max(0.01f,max-min)));
        c.drawArc(new RectF(cx-r+5,cy-r+5,cx+r-5,cy+r-5),140,260*frac,false,p);
        p.setStrokeCap(Paint.Cap.BUTT);
        for(int i=0;i<=10;i++) {
            float rad=(float)Math.toRadians(140+i*26);
            float inner=r-(isMain?14:10), outer=r-(isMain?6:6);
            color(i<=frac*10 ? 0xffc7d1de : 0xff49586a);
            p.setStrokeWidth(i%2==0?2:1);
            c.drawLine(cx+(float)Math.cos(rad)*inner,cy+(float)Math.sin(rad)*inner,
                       cx+(float)Math.cos(rad)*outer,cy+(float)Math.sin(rad)*outer,p);
        }
        float theta=(float)Math.toRadians(140+frac*260);
        float len=r*(isMain?0.70f:0.62f);
        color(0xfff8fafc);p.setStrokeWidth(isMain?2.5f:1.8f);
        c.drawLine(cx,cy,cx+(float)Math.cos(theta)*len,cy+(float)Math.sin(theta)*len,p);
        color(accent); c.drawCircle(cx,cy,isMain?4:3,p);
        float titleY=cy-r*(isMain?0.35f:0.32f);
        text(c,title,cx,titleY,isMain?8.5f:7,0xffa8b5c4,true);
        text(c,big,cx,cy+(isMain?19:13),isMain?33:(big.length()>4?17:19),0xfff6f8fb,true);
        text(c,unit,cx,cy+(isMain?33:24),isMain?10:8,0xff9eabba,false);
    }
}
