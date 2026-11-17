package com.mrgregles.bsp_weapons.parts;

import java.util.Locale;

/** The slots a pattern can ask for. The Core is special: it carries a tier, not a type and material. */
public enum PartKind {
    CORE, BARREL, CELL, GRIP, OPTIC;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static PartKind byId(String id) {
        for (PartKind k : values()) if (k.name().equalsIgnoreCase(id)) return k;
        return null;
    }
}
