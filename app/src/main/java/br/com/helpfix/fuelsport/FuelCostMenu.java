package br.com.helpfix.fuelsport;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class FuelCostMenu {
    static final String TITLE = "5. Quanto custa rodar com o Celta?";
    private static final Locale BR = Locale.forLanguageTag("pt-BR");
    private static final int WHITE = 0xFFF1F9FF;
    private static final int MUTED = 0xFFB5CDDC;
    private static final int CYAN = 0xFF77DFFF;
    private static final String FILLS_KEY = "cost_fillups";

    private final Activity activity;

    FuelCostMenu(Activity activity) {
        this.activity = activity;
    }

    void show() {
        List<FuelCosts.Fill> fills;
        try {
            fills = loadFills();
        } catch (JSONException | IllegalArgumentException e) {
            new AlertDialog.Builder(activity).setTitle(TITLE)
                    .setMessage("Não foi possível abrir os registros de abastecimento.")
                    .setPositiveButton("Fechar", null).show();
            return;
        }

        LinearLayout body = body();
        FuelCosts.Measurement measurement = FuelCosts.measure(fills);
        double efficiency = measurement.available()
                ? measurement.kmPerLiter() : FuelStore.get(activity, "km_per_l", 14f);
        FuelCosts.Fill latest = fills.isEmpty() ? null : fills.get(fills.size() - 1);

        heading(body, "Consumo e custo");
        if (measurement.available()) {
            text(body, format("%.2f km/L medidos · R$ %.3f/km no histórico",
                    efficiency, measurement.costPerKm()), 17, WHITE);
            text(body, format("Medição em %.1f km entre abastecimentos com tanque cheio.",
                    measurement.distance()), 13, MUTED);
        } else {
            text(body, format("%.2f km/L · Consumo médio configurado", efficiency), 17, WHITE);
            text(body, "Para medir o consumo real, registre dois tanques cheios e todos os abastecimentos entre eles.", 13, MUTED);
        }

        heading(body, "Simular uma entrega ou viagem");
        EditText price = field(body, "Preço da gasolina (R$/L)",
                latest == null ? "" : format("%.3f", latest.pricePerLiter()), "Ex. 6,00");
        TextView costPerKm = text(body, "", 16, CYAN);
        EditText distance = field(body, "Distância total (km)", "", "Inclua a volta, se houver");
        EditText revenue = field(body, "Valor da entrega (R$) · opcional", "", "Ex. 30,00");
        TextView result = text(body, "", 17, WHITE);
        result.setPadding(0, dp(9), 0, 0);
        Runnable refresh = () -> updateSimulation(efficiency, price, distance, revenue, costPerKm, result);
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                refresh.run();
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        price.addTextChangedListener(watcher);
        distance.addTextChangedListener(watcher);
        revenue.addTextChangedListener(watcher);
        refresh.run();

        heading(body, "Abastecimentos recentes");
        TextView description = text(body, "Registre a quilometragem do odômetro, os litros e o total pago. O saldo do painel é ajustado pelo menu Combustível e consumo.", 13, MUTED);
        description.setPadding(0, 0, 0, dp(4));
        if (latest != null) {
            for (int i = fills.size() - 1; i >= Math.max(0, fills.size() - 5); i--) {
                FuelCosts.Fill fill = fills.get(i);
                text(body, format("%,.1f km · %.2f L · R$ %.2f%s",
                        fill.odometer(), fill.liters(), fill.paid(),
                        fill.fullTank() ? " · Tanque cheio" : ""), 14, WHITE);
            }
        } else {
            text(body, "Nenhum abastecimento registrado.", 14, MUTED);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity).setTitle(TITLE)
                .setView(scroll(body)).setNegativeButton("Fechar", null)
                .setPositiveButton("Registrar", (dialog, which) -> editFill(fills, null));
        if (latest != null) builder.setNeutralButton("Editar último",
                (dialog, which) -> editFill(fills, latest));
        builder.show();
    }

    private void updateSimulation(double efficiency, EditText priceInput, EditText distanceInput,
            EditText revenueInput, TextView costPerKm, TextView result) {
        result.setText("");
        if (priceInput.length() == 0) {
            costPerKm.setText("Informe o preço por litro para calcular o custo.");
            return;
        }
        double price = read(priceInput);
        if (!(price > 0 && Double.isFinite(price / efficiency))) {
            costPerKm.setText("Confira o preço por litro.");
            return;
        }
        costPerKm.setText(format("Custo estimado de gasolina · R$ %.3f/km", price / efficiency));
        if (distanceInput.length() == 0) return;
        double distance = read(distanceInput);
        if (!(distance > 0 && Double.isFinite(distance / efficiency * price))) {
            result.setText("Confira a distância total.");
            return;
        }
        double liters = FuelCosts.litersFor(distance, efficiency);
        double tripCost = liters * price;
        String resultText = format("%.2f L · R$ %.2f de gasolina", liters, tripCost);
        if (revenueInput.length() > 0) {
            double revenue = read(revenueInput);
            if (revenue >= 0) {
                double balance = revenue - tripCost;
                resultText += balance >= 0
                        ? format("\nSobra após gasolina · R$ %.2f", balance)
                        : format("\nFalta para cobrir a gasolina · R$ %.2f", -balance);
            } else {
                resultText += "\nConfira o valor da entrega.";
            }
        }
        result.setText(resultText);
    }

    private void editFill(List<FuelCosts.Fill> fills, FuelCosts.Fill current) {
        boolean editing = current != null;
        LinearLayout body = body();
        EditText odometer = field(body, "Odômetro do carro (km)",
                editing ? format("%.1f", current.odometer()) : "", "Sem separador de milhar");
        EditText liters = field(body, "Litros abastecidos",
                editing ? format("%.3f", current.liters()) : "", "Ex. 15,285");
        EditText paid = field(body, "Valor pago (R$)",
                editing ? format("%.2f", current.paid()) : "", "Ex. 100,00");
        CheckBox full = new CheckBox(activity);
        full.setText("Completei o tanque");
        full.setTextColor(WHITE);
        full.setChecked(editing && current.fullTank());
        body.addView(full);
        TextView error = text(body, "", 14, 0xFFFF99A0);
        ScrollView formScroll = scroll(body);
        AlertDialog form = new AlertDialog.Builder(activity)
                .setTitle(editing ? "Editar último abastecimento" : "Registrar abastecimento")
                .setView(formScroll).setNegativeButton("Voltar", null)
                .setPositiveButton("Salvar", null).create();
        form.setOnCancelListener(dialog -> show());
        form.setOnShowListener(dialog -> {
            form.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(view -> {
                form.dismiss();
                show();
            });
            form.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                try {
                    FuelCosts.Fill fill = new FuelCosts.Fill(read(odometer), read(liters),
                            read(paid), full.isChecked());
                    saveFills(FuelCosts.saveFill(fills, fill, editing));
                } catch (IllegalArgumentException e) {
                    error.setText(e.getMessage());
                    formScroll.post(() -> formScroll.smoothScrollTo(0, error.getBottom()));
                    return;
                } catch (JSONException e) {
                    error.setText("Não foi possível salvar o abastecimento.");
                    formScroll.post(() -> formScroll.smoothScrollTo(0, error.getBottom()));
                    return;
                }
                form.dismiss();
                show();
            });
        });
        form.show();
    }

    private List<FuelCosts.Fill> loadFills() throws JSONException {
        JSONArray entries = new JSONArray(FuelStore.prefs(activity).getString(FILLS_KEY, "[]"));
        List<FuelCosts.Fill> fills = new ArrayList<>();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);
            fills.add(new FuelCosts.Fill(entry.getDouble("odometer"), entry.getDouble("liters"),
                    entry.getDouble("paid"), entry.getBoolean("fullTank")));
        }
        return fills;
    }

    private void saveFills(List<FuelCosts.Fill> fills) throws JSONException {
        JSONArray entries = new JSONArray();
        for (FuelCosts.Fill fill : fills) {
            JSONObject entry = new JSONObject();
            entry.put("odometer", fill.odometer());
            entry.put("liters", fill.liters());
            entry.put("paid", fill.paid());
            entry.put("fullTank", fill.fullTank());
            entries.put(entry);
        }
        FuelStore.prefs(activity).edit().putString(FILLS_KEY, entries.toString()).apply();
    }

    private LinearLayout body() {
        LinearLayout body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setBackgroundColor(0xFF101E2A);
        body.setPadding(dp(20), 0, dp(20), dp(16));
        body.setFocusableInTouchMode(true);
        body.requestFocus();
        return body;
    }

    private ScrollView scroll(LinearLayout body) {
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(body, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return scroll;
    }

    private void heading(LinearLayout body, String title) {
        TextView heading = text(body, title, 16, CYAN);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        heading.setPadding(0, dp(16), 0, dp(4));
    }

    private TextView text(LinearLayout body, String value, int size, int color) {
        TextView text = new TextView(activity);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        body.addView(text);
        return text;
    }

    private EditText field(LinearLayout body, String caption, String value, String example) {
        EditText input = new EditText(activity);
        input.setId(android.view.View.generateViewId());
        TextView title = text(body, caption, 14, MUTED);
        title.setPadding(0, dp(8), 0, dp(3));
        title.setLabelFor(input.getId());
        input.setSingleLine();
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        input.setText(value);
        input.setHint(example);
        input.setTextSize(17);
        input.setSelectAllOnFocus(true);
        input.setTextColor(WHITE);
        input.setHintTextColor(MUTED);
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xFF192F40);
        background.setStroke(dp(1), 0xFF3C6A84);
        background.setCornerRadius(dp(8));
        input.setBackground(background);
        input.setPadding(dp(12), 0, dp(12), 0);
        body.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        return input;
    }

    private double read(EditText input) {
        try {
            return FuelCosts.parseNumber(input.getText());
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private static String format(String pattern, Object... values) {
        return String.format(BR, pattern, values);
    }

    private int dp(float value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
