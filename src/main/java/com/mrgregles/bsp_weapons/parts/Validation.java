package com.mrgregles.bsp_weapons.parts;

import com.mrgregles.bsp_weapons.WeaponsConfig;
import com.mrgregles.bsp_weapons.data.WeaponData;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Whether a spec is a weapon the server would make: the pattern exists, every slot it asks for is filled with a part of
 * that kind and nothing else, every type and material exists and is allowed there, the core tier is real, and the
 * chips fit their slots and this weapon. Each problem is a plain sentence; an empty list means valid.
 */
public final class Validation {
    private Validation() {
    }

    public static List<String> problems(WeaponSpec spec) {
        List<String> out = new ArrayList<>();
        Pattern pattern = WeaponData.pattern(spec.pattern());
        if (pattern == null) {
            out.add("No weapon pattern '" + spec.pattern() + "'.");
            return out;
        }
        if (spec.coreTier() < 1 || spec.coreTier() > WeaponsConfig.tiers()) out.add("Core tier " + spec.coreTier() + " does not exist (1 to " + WeaponsConfig.tiers() + ").");
        Set<PartKind> seen = EnumSet.noneOf(PartKind.class);
        for (PartSpec part : spec.parts()) {
            if (part.kind() == PartKind.CORE) { out.add("The Core is not a listed part; it is the weapon's tier."); continue; }
            if (!seen.add(part.kind())) out.add("Two parts fill the " + part.kind().id() + " slot.");
            if (!pattern.slots().contains(part.kind())) out.add("A " + pattern.id().getPath().replace('_', ' ') + " has no " + part.kind().id() + " slot.");
            PartType type = WeaponData.partType(part.type());
            if (type == null) out.add("No part type '" + part.type() + "'.");
            else {
                if (type.kind() != part.kind()) out.add("'" + part.type() + "' is a " + type.kind().id() + ", not a " + part.kind().id() + ".");
                if (!type.allowedOn(pattern.id())) out.add("'" + part.type() + "' does not fit a " + pattern.id().getPath().replace('_', ' ') + ".");
            }
            WeaponMaterial material = WeaponData.material(part.material());
            if (material == null) out.add("No material '" + part.material() + "'.");
            else if (!material.allows(part.kind())) out.add("A " + part.kind().id() + " cannot be made of " + material.id().getPath().replace('_', ' ') + ".");
        }
        for (PartKind slot : pattern.slots()) if (!seen.contains(slot)) out.add("The " + slot.id() + " slot is empty.");
        int slots = SheetCalculator.augmentSlots(spec);
        if (spec.augments().size() > slots) out.add("Too many chips: " + spec.augments().size() + " for " + slots + " slot" + (slots == 1 ? "" : "s") + ".");
        Set<ResourceLocation> chips = new HashSet<>();
        for (ResourceLocation id : spec.augments()) {
            Augment a = WeaponData.augment(id);
            if (a == null) { out.add("No augment '" + id + "'."); continue; }
            if (!chips.add(id)) out.add("The chip '" + id.getPath().replace('_', ' ') + "' is fitted twice.");
            if (!a.allowedOn(pattern)) out.add("The chip '" + id.getPath().replace('_', ' ') + "' does not fit a " + pattern.id().getPath().replace('_', ' ') + ".");
        }
        return out;
    }

    public static boolean isValid(WeaponSpec spec) {
        return problems(spec).isEmpty();
    }
}
