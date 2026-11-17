package com.mrgregles.bsp_weapons.parts;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/** A chip fitted at the enhancement machine (D9, D25): the last layer. Empty {@code kinds} or {@code classes} means any. */
public record Augment(ResourceLocation id, Modifiers modifiers, Set<WeaponKind> kinds, Set<ResourceLocation> classes) {

    public boolean allowedOn(Pattern pattern) {
        return (kinds.isEmpty() || kinds.contains(pattern.kind())) && (classes.isEmpty() || classes.contains(pattern.id()));
    }
}
