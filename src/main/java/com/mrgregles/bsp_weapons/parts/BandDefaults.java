package com.mrgregles.bsp_weapons.parts;

import java.util.EnumMap;
import java.util.Map;

/** What a material of a band gives a part of each kind when the material lists nothing of its own (D36). */
public record BandDefaults(int band, Map<PartKind, Modifiers> byKind, Modifiers any) {

    public BandDefaults {
        EnumMap<PartKind, Modifiers> copy = new EnumMap<>(PartKind.class); // an EnumMap copy of an empty plain map throws
        copy.putAll(byKind);
        byKind = copy;
    }

    /** The band's modifiers for a kind: the kind's own plus the ones for any kind. */
    public Modifiers forKind(PartKind kind) {
        Modifiers m = byKind.get(kind);
        return (m == null ? Modifiers.NONE : m).plus(any);
    }
}
