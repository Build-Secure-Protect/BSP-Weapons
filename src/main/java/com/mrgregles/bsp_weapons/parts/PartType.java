package com.mrgregles.bsp_weapons.parts;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/** A part's shape and behaviour (D6): a Focused barrel, an Energy cell. {@code classes} empty means any pattern. */
public record PartType(ResourceLocation id, PartKind kind, Modifiers modifiers, Set<ResourceLocation> classes) {

    public boolean allowedOn(ResourceLocation pattern) {
        return classes.isEmpty() || classes.contains(pattern);
    }
}
