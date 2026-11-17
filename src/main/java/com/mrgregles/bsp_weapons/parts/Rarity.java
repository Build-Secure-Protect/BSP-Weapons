package com.mrgregles.bsp_weapons.parts;

import java.util.Locale;

/** The roll (owner decisions D20, D29): Normal, Rare, Epic, S. Multipliers come from the config. */
public enum Rarity {
    NORMAL, RARE, EPIC, S;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Rarity byId(String id) {
        for (Rarity r : values()) if (r.name().equalsIgnoreCase(id)) return r;
        return null;
    }

    public static Rarity byOrdinal(int i) {
        return i < 0 || i >= values().length ? NORMAL : values()[i];
    }
}
