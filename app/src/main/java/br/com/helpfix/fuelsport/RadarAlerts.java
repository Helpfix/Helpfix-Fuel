package br.com.helpfix.fuelsport;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class RadarAlerts {
    private final SharedPreferences prefs;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Set<Long> warned = new HashSet<>();
    private String database = "";
    private List<RadarMatch.Point> points = java.util.Collections.emptyList();
    private TextToSpeech voice;
    private boolean voiceReady;
    private long lastVoiceAt;
    private String banner = "";

    RadarAlerts(Context context) {
        prefs = FuelStore.prefs(context);
        if (prefs.getBoolean(RadarSettings.ENABLED, false)
                && prefs.getBoolean(RadarSettings.VOICE, true)) {
            prepareVoice(context);
        }
    }

    private void prepareVoice(Context context) {
        if (voice != null) return;
        voice = new TextToSpeech(context, status -> main.post(() -> {
            if (status == TextToSpeech.SUCCESS && voice != null) {
                voiceReady = voice.setLanguage(Locale.forLanguageTag("pt-BR")) >= 0;
            }
        }));
    }

    String update(Context context, Location fix, Location previous, float speed) {
        if (!prefs.getBoolean(RadarSettings.ENABLED, false)) {
            warned.clear();
            banner = "";
            close();
            return banner;
        }
        if (prefs.getBoolean(RadarSettings.VOICE, true)) prepareVoice(context);
        else close();

        String saved = prefs.getString(RadarSettings.DATABASE, "");
        if (!database.equals(saved)) {
            database = saved;
            points = RadarSettings.read(saved);
            warned.clear();
        }
        if (!fix.hasAccuracy() || fix.getAccuracy() > 35
                || SystemClock.elapsedRealtimeNanos() - fix.getElapsedRealtimeNanos() > 8_000_000_000L) {
            return "";
        }
        if (!(speed >= 5)) return "";
        double bearing;
        if (fix.hasBearing()) {
            bearing = fix.getBearing();
        } else if (previous != null && previous.hasAccuracy() && previous.getAccuracy() <= 35
                && fix.getElapsedRealtimeNanos() > previous.getElapsedRealtimeNanos()
                && fix.getElapsedRealtimeNanos() - previous.getElapsedRealtimeNanos() <= 12_000_000_000L
                && previous.distanceTo(fix) >= 10) {
            bearing = previous.bearingTo(fix);
        } else {
            return "";
        }

        int radius = prefs.getInt(RadarSettings.DISTANCE, 400);
        RadarMatch.Point nearest = null;
        double closest = radius;
        for (RadarMatch.Point point : points) {
            double distance = RadarMatch.distance(fix.getLatitude(), fix.getLongitude(), point);
            if (distance > radius + 150) warned.remove(point.id());
            if (distance < closest && RadarMatch.ahead(fix.getLatitude(), fix.getLongitude(), bearing, point)) {
                nearest = point;
                closest = distance;
            }
        }
        banner = "";
        if (nearest != null) {
            long meters = Math.max(10, Math.round(closest / 10) * 10);
            int limit = nearest.limit();
            banner = "Radar fixo a " + meters + " m" + (limit > 0 ? " · Limite " + limit + " km/h" : "");
            long now = SystemClock.elapsedRealtime();
            if (!warned.contains(nearest.id()) && voiceReady
                    && (lastVoiceAt == 0 || now - lastVoiceAt >= 8_000)) {
                String speech = "Radar fixo à frente, a " + meters + " metros.";
                if (limit > 0) speech += " Limite de " + limit + " quilômetros por hora.";
                if (voice.speak(speech, TextToSpeech.QUEUE_FLUSH, null, "radar-" + nearest.id())
                        == TextToSpeech.SUCCESS) {
                    warned.add(nearest.id());
                    lastVoiceAt = now;
                }
            }
        }
        return banner;
    }

    void close() {
        main.removeCallbacksAndMessages(null);
        if (voice != null) {
            voice.stop();
            voice.shutdown();
            voice = null;
        }
        voiceReady = false;
    }
}
