package com.thanpisitkub.mod.fx;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Colour over lifetime. The exported definitions store a colour as
 * {@code [alpha, r, g, b]}; gradient stops are {@code [time, alpha, r, g, b]}.
 */
public final class Tint {
    private final double[][] stops; // each: t, a, r, g, b
    private final double[] constant; // a, r, g, b

    private Tint(double[][] stops, double[] constant) {
        this.stops = stops;
        this.constant = constant;
    }

    public static Tint white() {
        return new Tint(null, new double[]{1, 1, 1, 1});
    }

    public static Tint fromJson(JsonObject o) {
        if (o == null) return white();
        String mode = o.has("mode") ? o.get("mode").getAsString() : "const";
        if (mode.equalsIgnoreCase("grad") && o.has("stops")) {
            List<double[]> list = new ArrayList<>();
            for (JsonElement e : o.getAsJsonArray("stops")) {
                JsonArray a = e.getAsJsonArray();
                if (a.size() < 5) continue;
                list.add(new double[]{a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble(),
                        a.get(3).getAsDouble(), a.get(4).getAsDouble()});
            }
            if (!list.isEmpty()) {
                list.sort((x, y) -> Double.compare(x[0], y[0]));
                return new Tint(list.toArray(new double[0][]), null);
            }
            return white();
        }
        double[] c = {1, 1, 1, 1};
        if (o.has("rgba")) {
            JsonArray a = o.getAsJsonArray("rgba");
            for (int i = 0; i < 4 && i < a.size(); i++) c[i] = a.get(i).getAsDouble();
        }
        return new Tint(null, c);
    }

    /** Fills {@code out} with r, g, b, a for the given normalised age (0..1). */
    public void eval(double t, float[] out) {
        double a, r, g, b;
        if (stops == null) {
            a = constant[0];
            r = constant[1];
            g = constant[2];
            b = constant[3];
        } else if (t <= stops[0][0]) {
            a = stops[0][1];
            r = stops[0][2];
            g = stops[0][3];
            b = stops[0][4];
        } else if (t >= stops[stops.length - 1][0]) {
            double[] s = stops[stops.length - 1];
            a = s[1];
            r = s[2];
            g = s[3];
            b = s[4];
        } else {
            int i = 0;
            while (i < stops.length - 2 && t > stops[i + 1][0]) i++;
            double[] s0 = stops[i], s1 = stops[i + 1];
            double span = s1[0] - s0[0];
            double u = span <= 1e-9 ? 0 : (t - s0[0]) / span;
            a = s0[1] + (s1[1] - s0[1]) * u;
            r = s0[2] + (s1[2] - s0[2]) * u;
            g = s0[3] + (s1[3] - s0[3]) * u;
            b = s0[4] + (s1[4] - s0[4]) * u;
        }
        out[0] = (float) clamp01(r);
        out[1] = (float) clamp01(g);
        out[2] = (float) clamp01(b);
        out[3] = (float) clamp01(a);
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : Math.min(v, 1);
    }
}
