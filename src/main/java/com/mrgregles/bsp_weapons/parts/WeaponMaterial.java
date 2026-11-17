package com.mrgregles.bsp_weapons.parts;

import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * A material a part can be made of (D18, D33 to D36): keyed to an item tag such as {@code forge:ingots/tetrium}, with a
 * band (1 to 5) that supplies default modifiers, a colour for the tint, the kinds it may be made into, its own modifiers
 * per kind (added on top of the band's), and at most one trait id.
 */
public record WeaponMaterial(ResourceLocation id, ResourceLocation tag, int band, int colour, Set<PartKind> kinds,
                             Map<PartKind, Modifiers> modifiers, String trait) {

    public WeaponMaterial {
        if (band < 1 || band > 5) throw new IllegalArgumentException("band must be 1 to 5: " + band);
        EnumMap<PartKind, Modifiers> copy = new EnumMap<>(PartKind.class); // an EnumMap copy of an empty plain map throws
        copy.putAll(modifiers);
        modifiers = copy;
    }

    /** The material's own modifiers for a kind (without the band's defaults); never null. */
    public Modifiers own(PartKind kind) {
        Modifiers m = modifiers.get(kind);
        return m == null ? Modifiers.NONE : m;
    }

    public boolean allows(PartKind kind) {
        return kinds.contains(kind);
    }
}
