package com.mrgregles.bsp_weapons.parts;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * A part's shape and behaviour (D6): a Focused barrel, an Energy cell. {@code classes} empty means any pattern. For a cell,
 * {@code ammo} is what it holds (D10): {@code energy} (Forge Energy, charged in any FE charger) or {@code rounds}.
 */
public record PartType(ResourceLocation id, PartKind kind, Modifiers modifiers, Set<ResourceLocation> classes, String ammo) {
    public static final String ENERGY = "energy", ROUNDS = "rounds";

    public PartType(ResourceLocation id, PartKind kind, Modifiers modifiers, Set<ResourceLocation> classes) {
        this(id, kind, modifiers, classes, ENERGY);
    }

    public boolean holdsEnergy() {
        return !ROUNDS.equals(ammo);
    }

    public boolean allowedOn(ResourceLocation pattern) {
        return classes.isEmpty() || classes.contains(pattern);
    }
}
