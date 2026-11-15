package br.com.helpfix.fuelsport;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.net.Uri;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class RadarSettings {
    static final String ENABLED = "radar_enabled";
    static final String VOICE = "radar_voice";
    static final String DISTANCE = "radar_distance";
    static final String DATABASE = "radar_database";
    private static final String DOWNLOADED = "radar_downloaded_at";
    // Área fixa: Porto Alegre e parte da Região Metropolitana.
    private static final String QUERY =
            "[out:json][timeout:20];node[highway=speed_camera](-30.30,-51.33,-29.80,-50.92);out;";
    private static boolean downloading;

    static void show(Activity activity) {
        SharedPreferences prefs = FuelStore.prefs(activity);
        LinearLayout body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(18 * activity.getResources().getDisplayMetrics().density);
        body.setPadding(pad, 0, pad, pad);
        CheckBox enabled = option(activity, body, "Avisar durante a viagem GPS", prefs.getBoolean(ENABLED, false));
        CheckBox voice = option(activity, body, "Ler aviso em voz alta", prefs.getBoolean(VOICE, true));
        TextView caption = new TextView(activity);
        caption.setText("Distância do aviso");
        body.addView(caption);
        RadioGroup distances = new RadioGroup(activity);
        distances.setOrientation(RadioGroup.HORIZONTAL);
        int selected = prefs.getInt(DISTANCE, 400);
        for (int meters : new int[]{250, 400, 600}) {
            RadioButton option = new RadioButton(activity);
            option.setId(meters);
            option.setText(meters + " m");
            distances.addView(option);
        }
        distances.check(selected);
        body.addView(distances);
        TextView state = new TextView(activity);
        body.addView(state);
        inform(state, prefs);
        Button download = new Button(activity);
        download.setText("Baixar ou atualizar base local");
        body.addView(download);
        download.setOnClickListener(v -> refresh(activity, state, download));
        TextView coverage = new TextView(activity);
        coverage.setText("Radares fixos mapeados em Porto Alegre e parte da Região Metropolitana. Após o download, os avisos funcionam offline. Uma via paralela pode gerar aviso de proximidade.");
        body.addView(coverage);
        Button attribution = new Button(activity);
        attribution.setText("© Colaboradores do OpenStreetMap · ODbL");
        body.addView(attribution);
        attribution.setOnClickListener(v -> {
            try {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openstreetmap.org/copyright")));
            } catch (android.content.ActivityNotFoundException ignored) {}
        });
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(body);
        new AlertDialog.Builder(activity).setTitle("Avisos de radar").setView(scroll)
                .setNegativeButton("Cancelar", null).setPositiveButton("Salvar", (dialog, which) ->
                    prefs.edit().putBoolean(ENABLED, enabled.isChecked()).putBoolean(VOICE, voice.isChecked())
                        .putInt(DISTANCE, distances.getCheckedRadioButtonId()).apply()).show();
    }

    private static CheckBox option(Activity activity, LinearLayout body, String title, boolean checked) {
        CheckBox option = new CheckBox(activity);
        option.setText(title);
        option.setChecked(checked);
        body.addView(option);
        return option;
    }

    private static void inform(TextView state, SharedPreferences prefs) {
        int count = read(prefs.getString(DATABASE, "")).size();
        long date = prefs.getLong(DOWNLOADED, 0);
        state.setText(count == 0 ? "Base local ainda não baixada." : String.format(
                Locale.forLanguageTag("pt-BR"), "%d radares fixos · Atualizada em %td/%<tm/%<tY", count, date));
    }

    private static void refresh(Activity activity, TextView state, Button download) {
        if (downloading) {
            state.setText("Já existe uma atualização em andamento.");
            return;
        }
        downloading = true;
        download.setEnabled(false);
        state.setText("Baixando radares fixos…");
        Context context = activity.getApplicationContext();
        new Thread(() -> {
            boolean success = false;
            try {
                String database = fetch();
                success = FuelStore.prefs(context).edit().putString(DATABASE, database)
                        .putLong(DOWNLOADED, System.currentTimeMillis()).commit();
            } catch (IOException | JSONException ignored) {
            }
            boolean updated = success;
            activity.runOnUiThread(() -> {
                downloading = false;
                if (activity.isDestroyed()) return;
                download.setEnabled(true);
                if (updated) inform(state, FuelStore.prefs(context));
                else state.setText("A atualização falhou. O app continua usando a última base disponível.");
            });
        }, "helpfix-radar-download").start();
    }

    private static String fetch() throws IOException, JSONException {
        URL url = new URL("https://overpass-api.de/api/interpreter?data=" + Uri.encode(QUERY));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "HelpfixFuel/3.3.3 (manual OpenStreetMap radar download)");
            if (connection.getResponseCode() != 200) throw new IOException("Resposta inválida");
            try (var stream = connection.getInputStream()) {
                byte[] bytes = new byte[1_000_000];
                int size = 0;
                int read;
                while (size < bytes.length && (read = stream.read(bytes, size, bytes.length - size)) != -1) {
                    size += read;
                }
                if (size == bytes.length) throw new IOException("Base maior que o esperado");
                String raw = new String(bytes, 0, size, java.nio.charset.StandardCharsets.UTF_8);
                JSONObject response = new JSONObject(raw);
                if (response.has("remark")) throw new IOException("Consulta incompleta");
                String elements = response.getJSONArray("elements").toString();
                if (read(elements).isEmpty()) throw new IOException("Consulta sem resultados");
                return elements;
            }
        } finally {
            connection.disconnect();
        }
    }

    static List<RadarMatch.Point> read(String database) {
        List<RadarMatch.Point> points = new ArrayList<>();
        if (database.isEmpty()) return points;
        try {
            JSONArray entries = new JSONArray(database);
            for (int i = 0; i < entries.length(); i++) {
                JSONObject node = entries.optJSONObject(i);
                if (node == null || !"node".equals(node.optString("type"))) continue;
                JSONObject tags = node.optJSONObject("tags");
                if (tags == null || !"speed_camera".equals(tags.optString("highway"))) continue;
                long id = node.optLong("id", 0);
                double lat = node.optDouble("lat"), lon = node.optDouble("lon");
                if (id <= 0 || !Double.isFinite(lat) || !Double.isFinite(lon)
                        || lat < -30.30 || lat > -29.80 || lon < -51.33 || lon > -50.92) continue;
                String speed = tags.optString("maxspeed").trim().replaceFirst("\\s*km/h$", "");
                int limit = speed.matches("[0-9]{1,3}") ? Integer.parseInt(speed) : 0;
                points.add(new RadarMatch.Point(id, lat, lon, limit <= 150 ? limit : 0));
            }
        } catch (JSONException ignored) {}
        return points;
    }

    private RadarSettings() {}
}
