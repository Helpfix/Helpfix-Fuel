package br.com.helpfix.fuelsport;

import java.util.ArrayList;
import java.util.List;

final class FuelCosts {
    record Fill(double odometer, double liters, double paid, boolean fullTank) {
        Fill {
            if (!Double.isFinite(odometer) || odometer < 0
                    || !Double.isFinite(liters) || liters <= 0
                    || !Double.isFinite(paid) || paid <= 0
                    || !Double.isFinite(paid / liters)) {
                throw new IllegalArgumentException("Confira o odômetro, os litros e o valor pago.");
            }
        }

        double pricePerLiter() {
            return paid / liters;
        }
    }

    record Measurement(double distance, double liters, double paid) {
        boolean available() {
            return distance > 0 && liters > 0;
        }

        double kmPerLiter() {
            return distance / liters;
        }

        double costPerKm() {
            return paid / distance;
        }
    }

    static double parseNumber(CharSequence input) {
        String number = input.toString().trim().replace(',', '.');
        if (!number.matches("[0-9]+(?:\\.[0-9]+)?")) throw new NumberFormatException();
        double value = Double.parseDouble(number);
        if (!Double.isFinite(value)) throw new NumberFormatException();
        return value;
    }

    static List<Fill> saveFill(List<Fill> fills, Fill fill, boolean replaceLast) {
        List<Fill> updated = new ArrayList<>(fills);
        if (replaceLast) updated.remove(updated.size() - 1);
        if (!updated.isEmpty() && fill.odometer() <= updated.get(updated.size() - 1).odometer()) {
            throw new IllegalArgumentException("O odômetro precisa ser maior que o do registro anterior.");
        }
        updated.add(fill);
        return updated;
    }

    static Measurement measure(List<Fill> fills) {
        double distance = 0;
        double liters = 0;
        double paid = 0;
        double intervalLiters = 0;
        double intervalPaid = 0;
        Fill start = null;
        for (Fill fill : fills) {
            // O primeiro tanque cheio é a referência. O volume e o custo dele antecedem a medição.
            if (start != null) {
                intervalLiters += fill.liters();
                intervalPaid += fill.paid();
                if (fill.fullTank()) {
                    distance += fill.odometer() - start.odometer();
                    liters += intervalLiters;
                    paid += intervalPaid;
                    intervalLiters = 0;
                    intervalPaid = 0;
                }
            }
            if (fill.fullTank()) start = fill;
        }
        return new Measurement(distance, liters, paid);
    }

    static double litersFor(double distance, double kmPerLiter) {
        return distance / kmPerLiter;
    }

    private FuelCosts() {}
}
