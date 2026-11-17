package com.mrgregles.bsp_weapons.parts;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** A weapon class (D5, D17): the slots it needs besides the Core, how it hits, and its base sheet. */
public record Pattern(ResourceLocation id, WeaponKind kind, List<PartKind> slots, Sheet base) {

    public Pattern {
        if (slots.contains(PartKind.CORE)) throw new IllegalArgumentException(id + ": the Core is every pattern's first slot and is not listed");
        for (PartKind k : slots) if (slots.indexOf(k) != slots.lastIndexOf(k)) throw new IllegalArgumentException(id + ": slot " + k + " listed twice");
        slots = List.copyOf(slots);
    }
}
