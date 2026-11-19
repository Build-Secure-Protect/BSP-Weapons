package com.mrgregles.bsp_weapons.parts;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * A part's shape and behaviour (D6): a Focused barrel, an Energy cell. {@code classes} empty means any pattern. For a cell,
 * {@code ammo} is what it holds (D10): {@code energy} (Forge Energy, charged in any FE charger) or {@code rounds}.
 * {@code cost} is how many ingots or gems of the chosen material the Weapon Fabricator takes for one part (D100).
 */
public record PartType(ResourceLocation id, PartKind kind, Modifiers modifiers, Set<ResourceLocation> classes, String ammo, int cost) {
    public static final String ENERGY = "energy", ROUNDS = "rounds";

    public PartType {
        if (cost < 1 || cost > Need.MAX_COUNT) throw new IllegalArgumentException(id + ": cost must be 1 to " + Need.MAX_COUNT + ", not " + cost);
    }

    public PartType(ResourceLocation id, PartKind kind, Modifiers modifiers, Set<ResourceLocation> classes) {
        this(id, kind, modifiers, classes, ENERGY, defaultCost(kind));
    }

    public PartType(ResourceLocation id, PartKind kind, Modifiers modifiers, Set<ResourceLocation> classes, String ammo) {
        this(id, kind, modifiers, classes, ammo, defaultCost(kind));
    }

    /** The cost a part type's JSON leaves unsaid (D100): barrels 3, cells 2, grips 2, optics 1. */
    public static int defaultCost(PartKind kind) {
        return switch (kind) {
            case BARREL -> 3;
            case OPTIC -> 1;
            default -> 2;
        };
    }

    public boolean holdsEnergy() {
        return !ROUNDS.equals(ammo);
    }

    public boolean allowedOn(ResourceLocation pattern) {
        return classes.isEmpty() || classes.contains(pattern);
    }
}
