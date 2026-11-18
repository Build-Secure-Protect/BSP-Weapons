package com.mrgregles.bsp_weapons.parts;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.EnumMap;
import java.util.Map;

/**
 * Changes a part, material band, type or augment makes to a sheet: a flat amount and a percentage per stat, applied as
 * {@code (value + flat) * (1 + percent / 100)}. JSON: {@code {"DAMAGE": {"flat": 1, "percent": 10}, "RANGE": {"percent": 20}}}.
 */
public final class Modifiers {
    public static final Modifiers NONE = new Modifiers();
    private final EnumMap<Stat, double[]> changes = new EnumMap<>(Stat.class); // [flat, percent]

    public Modifiers() {
    }

    public Modifiers flat(Stat s, double v) {
        changes.computeIfAbsent(s, k -> new double[2])[0] += v;
        return this;
    }

    public Modifiers percent(Stat s, double v) {
        changes.computeIfAbsent(s, k -> new double[2])[1] += v;
        return this;
    }

    public double flatOf(Stat s) {
        double[] c = changes.get(s);
        return c == null ? 0 : c[0];
    }

    public double percentOf(Stat s) {
        double[] c = changes.get(s);
        return c == null ? 0 : c[1];
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }

    public Map<Stat, double[]> changes() {
        return changes;
    }

    /** A copy with the other's changes added. */
    public Modifiers plus(Modifiers other) {
        Modifiers m = new Modifiers();
        for (Map.Entry<Stat, double[]> e : changes.entrySet()) m.flat(e.getKey(), e.getValue()[0]).percent(e.getKey(), e.getValue()[1]);
        for (Map.Entry<Stat, double[]> e : other.changes.entrySet()) m.flat(e.getKey(), e.getValue()[0]).percent(e.getKey(), e.getValue()[1]);
        return m;
    }

    /** Applies the changes to the sheet in place and returns it. */
    public Sheet apply(Sheet sheet) {
        for (Map.Entry<Stat, double[]> e : changes.entrySet()) {
            double v = sheet.get(e.getKey());
            sheet.set(e.getKey(), (v + e.getValue()[0]) * (1 + e.getValue()[1] / 100.0));
        }
        return sheet;
    }

    /** Reads {@code {"STAT": {"flat": x, "percent": y}, ...}}; unknown stats are an error, so a typo never passes quietly. */
    public static Modifiers fromJson(JsonObject json) {
        Modifiers m = new Modifiers();
        if (json == null) return m;
        for (Map.Entry<String, JsonElement> e : json.entrySet()) {
            Stat s = Stat.byId(e.getKey());
            if (s == null) throw new IllegalArgumentException("unknown stat '" + e.getKey() + "'");
            JsonObject o = e.getValue().getAsJsonObject();
            if (o.has("flat")) m.flat(s, finite(o.get("flat").getAsDouble(), e.getKey()));
            if (o.has("percent")) m.percent(s, finite(o.get("percent").getAsDouble(), e.getKey()));
        }
        return m;
    }

    static double finite(double v, String where) {
        if (Double.isNaN(v) || Double.isInfinite(v)) throw new IllegalArgumentException("'" + where + "' is not a number");
        return v;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("{");
        for (Map.Entry<Stat, double[]> e : changes.entrySet()) b.append(e.getKey().id()).append(":+").append(e.getValue()[0]).append("/").append(e.getValue()[1]).append("% ");
        return b.append("}").toString();
    }
}
