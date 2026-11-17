package com.mrgregles.bsp_weapons.parts;

import java.util.EnumMap;
import java.util.Map;

/** A weapon's numbers, one per {@link Stat}; a stat not set reads 0. Mutable while a sheet is being worked out. */
public final class Sheet {
    private final EnumMap<Stat, Double> values = new EnumMap<>(Stat.class);

    public Sheet() {
    }

    public Sheet(Sheet other) {
        values.putAll(other.values);
    }

    public double get(Stat s) {
        Double v = values.get(s);
        return v == null ? 0 : v;
    }

    public Sheet set(Stat s, double v) {
        values.put(s, v);
        return this;
    }

    public boolean has(Stat s) {
        return values.containsKey(s);
    }

    public Map<Stat, Double> values() {
        return values;
    }

    /** Multiplies every stat the better way: higher-is-better stats grow, lower-is-better stats shrink. */
    public Sheet improve(double mult) {
        if (mult <= 0) throw new IllegalArgumentException("multiplier must be positive: " + mult);
        for (Stat s : Stat.values()) {
            if (!values.containsKey(s)) continue;
            values.put(s, s.lowerIsBetter ? get(s) / mult : get(s) * mult);
        }
        return this;
    }

    /** this + (other - this) * share, stat by stat: how much of a change to keep. */
    public Sheet blendTowards(Sheet other, double share) {
        for (Stat s : Stat.values()) {
            if (!values.containsKey(s) && !other.values.containsKey(s)) continue;
            values.put(s, get(s) + (other.get(s) - get(s)) * share);
        }
        return this;
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
