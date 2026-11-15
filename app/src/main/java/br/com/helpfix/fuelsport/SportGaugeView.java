package br.com.helpfix.fuelsport;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;
import java.util.Locale;

/**
 * Painel automotivo original, desenhado diretamente no Canvas Android.
 * Inspirado no formato da referencia: dois relogios analogicos, azul-ciano e preto.
 * Nenhum dado de combustivel vem de sensor/OBD: litros e autonomia sao estimativas.
 */
public final class SportGaugeView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private static final int WHITE = 0xffeaf8ff;
    private static final int MUTED = 0xff7896ae;
    private int accent = 0xff00d5ff;
    private float speed = 0f, liters = 15f, capacity = 46f, autonomy = 210f, distance = 0f;
    private static final Locale BR = Locale.forLanguageTag("pt-BR");
    private double odometer = Double.NaN;
    private boolean active = false, compact = false;
    private String radarBanner = "";
    private final Typeface bold = Typeface.create("sans-serif-condensed", Typeface.BOLD);
    private final Typeface normal = Typeface.create("sans-serif", Typeface.NORMAL);

    public SportGaugeView(Context c) { super(c); setLayerType(View.LAYER_TYPE_SOFTWARE, null); }
    public void setCompact(boolean value) { compact = value; invalidate(); }
    public void setAccent(int value) { accent = value; invalidate(); }
    public void setRadarBanner(String value) {
        if (value.equals(radarBanner)) return;
        radarBanner = value;
        invalidate();
    }
    public void setMetrics(float velocity, float remaining, float range, float tank,
            float driven, boolean tripActive, double odometerKm) {
        odometer = odometerKm;
        speed = clamp(velocity, 0f, 240f);
        liters = Math.max(0f, remaining);
        capacity = Math.max(1f, tank);
        autonomy = Math.max(0f, range);
        distance = Math.max(0f, driven);
        active = tripActive;
        invalidate();
    }
    private static float clamp(float x, float a, float b) {return Math.max(a, Math.min(b,x));}
    private void fill(int color){ p.reset();p.setAntiAlias(true);p.setSubpixelText(true);p.setColor(color);p.setStyle(Paint.Style.FILL); }
    private void stroke(int color,float width){ fill(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width); }
    private void text(Canvas c,String t,float x,float y,float size,int color,boolean isBold){
        fill(color);p.setTypeface(isBold?bold:normal);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(size);
        c.drawText(t,x,y,p);
    }
    private void line(Canvas c,float x1,float y1,float x2,float y2,int color,float width){
        stroke(color,width);c.drawLine(x1,y1,x2,y2,p);
    }
    private float rad(float degrees){return (float)Math.toRadians(degrees);}
    private int alpha(int color,int a){return (color & 0x00ffffff)|((a & 255)<<24);}
    private void border(Canvas c,RectF r,int color,float width,float radius){stroke(color,width);c.drawRoundRect(r,radius,radius,p);}

    @Override protected void onDraw(Canvas screen){
        super.onDraw(screen);
        if(getWidth() == 0 || getHeight() == 0) return;
        final float height = 560f;
        float scale = compact ? Math.min(getWidth() / 1000f, getHeight() / height)
                : getHeight() / height;
        float width = compact ? 1000f : getWidth() / scale;
        screen.drawColor(0xff040911);
        screen.save();
        screen.translate((getWidth() - width * scale) / 2, (getHeight() - height * scale) / 2);
        screen.scale(scale, scale);
        Canvas c = screen;
        float middle = width / 2;
        float centerWidth = width * .21f;
        float leftDial = (width - centerWidth) / 4;
        float rightDial = width - leftDial;
        float radius = Math.min(184f, leftDial - 13f);

        fill(0xff07101b);
        p.setShader(new LinearGradient(0, 0, 0, height, 0xff101b28, 0xff01050b, Shader.TileMode.CLAMP));
        c.drawRoundRect(2, 2, width - 2, height - 2, 28, 28, p);
        p.setShader(null);
        border(c, new RectF(4, 4, width - 4, height - 4), 0xff415b70, 3, 26);
        line(c, 14, 77, width - 14, 77, alpha(accent, 68), 1.3f);

        float menuWidth = compact ? 0 : 130 * getResources().getDisplayMetrics().density / scale;
        float headerWidth = Math.max(24, width - 2 * menuWidth);
        fittedText(c, "HELPFIX FUEL", middle, 37, 25, headerWidth, WHITE);
        if (active && !radarBanner.isEmpty()) {
            float halfBanner = Math.min(230, Math.min(width * .28f, headerWidth / 2));
            fill(0xff4d300b);
            c.drawRoundRect(middle - halfBanner, 46, middle + halfBanner, 70, 8, 8, p);
            fittedText(c, radarBanner, middle, 63, 17, halfBanner * 2 - 16, 0xffffd68f);
        } else {
            fittedText(c, "PAINEL AUTOMOTIVO  •  GPS", middle, 62, 13, headerWidth, 0xff69b3d4);
        }

        fittedText(c, "VELOCIDADE GPS", leftDial, 97, 18, leftDial * 1.8f, WHITE);
        fittedText(c, "COMBUSTÍVEL ESTIMADO", rightDial, 97, 18, leftDial * 1.8f, WHITE);
        if (radius > 12) {
            dialAt(c, leftDial, 299, radius, 240f, speed, true);
            dialAt(c, rightDial, 299, radius, 100f, clamp(liters / capacity * 100f, 0f, 100f), false);
        }

        c.save();
        float centerScale = Math.min(1.35f, (centerWidth - 8) / 168f);
        if (centerScale > 0) {
            c.translate(middle, 308);
            c.scale(centerScale, centerScale);
            c.translate(-500, -316);
            drawCenter(c);
        }
        c.restore();

        fill(0xff091927);
        c.drawRoundRect(12, 500, width - 12, 550, 12, 12, p);
        border(c, new RectF(12, 500, width - 12, 550), 0xff1b445d, 1, 12);
        float column = (width - 24) / 4;
        metric(c, 12 + column * .5f, column, "VIAGEM", active ? "ATIVA" : "PARADA",
                active ? 0xff5fe9b9 : 0xffafc0cf);
        metric(c, 12 + column * 1.5f, column, "DISTÂNCIA GPS", String.format(BR, "%,.1f km", distance), WHITE);
        String km = Double.isFinite(odometer) ? String.format(BR, "%,.1f km", odometer) : "—";
        metric(c, 12 + column * 2.5f, column, "ODÔMETRO", km, WHITE);
        metric(c, 12 + column * 3.5f, column, "COMBUSTÍVEL EST.", String.format(BR, "%.1f L", liters), WHITE);
        c.restore();
    }

    private void dialAt(Canvas c, float x, float y, float radius, float max, float value, boolean velocity) {
        c.save();
        c.translate(x, y);
        c.scale(radius / 177, radius / 177);
        drawDial(c, 0, 0, 177, max, value, velocity);
        c.restore();
    }

    private void metric(Canvas c, float x, float width, String label, String value, int color) {
        fittedText(c, label, x, 518, 13, width - 12, MUTED);
        fittedText(c, value, x, 540, 21, width - 12, color);
    }

    private void fittedText(Canvas c, String value, float x, float y, float size, float maxWidth, int color) {
        if (maxWidth <= 0) return;
        p.setTypeface(bold);
        p.setTextSize(size);
        float measured = p.measureText(value);
        if (measured > maxWidth) size *= maxWidth / measured;
        text(c, value, x, y, size, color, true);
    }

    private void drawDial(Canvas c,float cx,float cy,float r,float max,float value,boolean velocity){
        fill(0xff00040c);p.setShadowLayer(23,0,0,alpha(accent,60));c.drawCircle(cx,cy,r+12,p);p.clearShadowLayer();
        fill(0xff0d2b40);c.drawCircle(cx,cy,r+10,p);
        stroke(alpha(accent,195),3f);c.drawCircle(cx,cy,r+4,p);
        stroke(0xff05314a,6f);c.drawCircle(cx,cy,r-9,p);
        fill(0xff06101b);
        p.setShader(new RadialGradient(cx-24,cy-43,r*1.35f,new int[]{0xff11344b,0xff030a14,0xff000309},null,Shader.TileMode.CLAMP));
        c.drawCircle(cx,cy,r-15,p);p.setShader(null);
        stroke(alpha(accent,65),1.1f);c.drawCircle(cx,cy,r-27,p);

        final float start=135f,sweep=270f;
        RectF arc=new RectF(cx-r+21,cy-r+21,cx+r-21,cy+r-21);
        stroke(0xff17354a,7f);c.drawArc(arc,start,sweep,false,p);
        stroke(alpha(accent,175),4f);p.setStrokeCap(Paint.Cap.ROUND);
        c.drawArc(arc,start,sweep*clamp(value/max,0,1),false,p);p.setStrokeCap(Paint.Cap.BUTT);
        int steps=velocity?48:40;
        for(int i=0;i<=steps;i++) {
            float f=(float)i/steps, a=rad(start+f*sweep);
            boolean major=velocity?(i%8==0):(i%10==0);
            boolean medium=velocity?(i%4==0):(i%5==0);
            float inner=r-(major?47:(medium?35:30));
            float outer=r-24;
            int col=i<=steps*(value/max) ? (major?WHITE:accent) : (major?0xffaec1ce:0xff4784a2);
            line(c,cx+(float)Math.cos(a)*inner,cy+(float)Math.sin(a)*inner,
                   cx+(float)Math.cos(a)*outer,cy+(float)Math.sin(a)*outer,col,major?3f:1.5f);
            if(major){
                String label=velocity?String.valueOf(i*5):String.valueOf((int)(i*2.5f));
                float labelR=r-68;
                text(c,label,cx+(float)Math.cos(a)*labelR,
                        cy+(float)Math.sin(a)*labelR+6,velocity?20:19,WHITE,true);
            }
        }
        float angle=rad(start+sweep*clamp(value/max,0,1));
        float tip=r-75;
        float tx=cx+(float)Math.cos(angle)*tip;
        float ty=cy+(float)Math.sin(angle)*tip;
        float bx=cx-(float)Math.cos(angle)*30;
        float by=cy-(float)Math.sin(angle)*30;
        // Ponteiro com corpo iluminado e pino central como num painel real.
        p.setShadowLayer(9,0,0,accent);
        line(c,bx,by,tx,ty,alpha(accent,220),10f);
        p.clearShadowLayer();
        line(c,bx,by,tx,ty,0xffd6fbff,3f);
        fill(0xff0e3550);c.drawCircle(cx,cy,23,p);
        fill(accent);c.drawCircle(cx,cy,13,p);
        fill(WHITE);c.drawCircle(cx,cy,6,p);
        fill(0xff092c42);c.drawCircle(cx,cy,2.4f,p);
        if(velocity){
            text(c,"km/h",cx,cy+99,22,0xff74c7e5,true);
            text(c,String.format(Locale.getDefault(),"%.0f",speed),cx,cy+126,39,WHITE,true);
        } else {
            text(c,"TANQUE",cx,cy+94,17,0xff74c7e5,true);
            text(c,String.format(Locale.getDefault(),"%.0f%%",clamp(liters/capacity*100f,0f,100f)),cx,cy+126,39,WHITE,true);
        }
    }

    private void drawCenter(Canvas c){
        // Placa central escura encaixada entre os dois instrumentos.
        Path plate=new Path();
        plate.moveTo(439,223);plate.lineTo(561,223);
        plate.lineTo(584,254);plate.lineTo(584,382);
        plate.lineTo(560,412);plate.lineTo(440,412);
        plate.lineTo(416,382);plate.lineTo(416,254);plate.close();
        fill(0xff071320);c.drawPath(plate,p);
        stroke(alpha(accent,180),2f);c.drawPath(plate,p);
        stroke(alpha(accent,90),2f);c.drawLine(439,239,561,239,p);
        text(c,"AUTONOMIA",500,278,18,0xff9fcee3,true);
        text(c,"ESTIMADA",500,298,12,MUTED,false);
        String value=String.format(Locale.getDefault(),"%.0f",autonomy);
        float size=value.length()>4?52:(value.length()>3?61:76);
        text(c,value,500,366,size,WHITE,true);
        text(c,"KM RESTANTES",500,390,15,accent,true);
        text(c,"●",478,416,13,active?0xff42df9e:0xff9baabc,true);
        text(c,active?"GPS ATIVO":"INICIAR VIAGEM",523,416,12,active?0xff9edccf:MUTED,false);
    }
}
