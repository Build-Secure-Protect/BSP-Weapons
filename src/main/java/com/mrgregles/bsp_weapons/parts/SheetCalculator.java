package com.mrgregles.bsp_weapons.parts;

import com.mrgregles.bsp_weapons.WeaponsConfig;
import com.mrgregles.bsp_weapons.data.WeaponData;

/**
 * Works a weapon's sheet out from its spec, the layers in order (D7, D8, D9, D20, D29): the pattern's base sheet,
 * improved by the core tier; each part's type and material modifiers (the band's defaults plus the material's own),
 * each part's change scaled by its own roll; the weapon's roll; the chips, each scaled by its own roll (D81); then the
 * caps. Server side, every time it is
 * needed; nothing is read back from the item but the spec.
 */
public final class SheetCalculator {
    private SheetCalculator() {
    }

    /** The sheet; a spec that does not validate still gets a sheet from what can be read, so callers validate first. */
    public static Sheet compute(WeaponSpec spec) {
        Pattern pattern = WeaponData.pattern(spec.pattern());
        Sheet sheet = pattern == null ? new Sheet() : new Sheet(pattern.base());
        sheet.improve(WeaponsConfig.tierMultiplier(spec.coreTier()));
        for (PartSpec part : spec.parts()) {
            Modifiers m = partModifiers(part);
            if (m.isEmpty()) continue;
            Sheet after = m.apply(new Sheet(sheet));
            sheet.blendTowards(after, WeaponsConfig.rarityPartMultiplier(part.rarity()));
        }
        sheet.improve(WeaponsConfig.rarityWeaponMultiplier(spec.roll()));
        for (FittedChip chip : spec.augments()) {
            Augment a = WeaponData.augment(chip.augment());
            if (a == null || a.modifiers().isEmpty()) continue;
            Sheet after = a.modifiers().apply(new Sheet(sheet));
            sheet.blendTowards(after, WeaponsConfig.rarityPartMultiplier(chip.rarity()));
        }
        return clamp(sheet);
    }

    /** What one fitted part changes: its type's modifiers plus its material's for that kind (band defaults and own). */
    public static Modifiers partModifiers(PartSpec part) {
        Modifiers m = Modifiers.NONE;
        PartType type = WeaponData.partType(part.type());
        if (type != null) m = m.plus(type.modifiers());
        WeaponMaterial material = WeaponData.material(part.material());
        if (material != null) m = m.plus(WeaponData.band(material.band()).forKind(part.kind())).plus(material.own(part.kind()));
        return m;
    }

    /** No stat below zero; piercing and range under their caps. */
    public static Sheet clamp(Sheet sheet) {
        for (Stat s : Stat.values()) if (sheet.has(s) && sheet.get(s) < 0) sheet.set(s, 0);
        if (sheet.has(Stat.PIERCING)) sheet.set(Stat.PIERCING, Math.min(WeaponsConfig.PIERCING_CAP.get(), sheet.get(Stat.PIERCING)));
        if (sheet.has(Stat.RANGE)) sheet.set(Stat.RANGE, Math.min(WeaponsConfig.RANGE_CAP.get(), sheet.get(Stat.RANGE)));
        return sheet;
    }

    /** How many chips the weapon may carry: its core tier's slots, plus one for an S roll. */
    public static int augmentSlots(WeaponSpec spec) {
        return WeaponsConfig.tierAugmentSlots(spec.coreTier()) + (spec.roll() == Rarity.S && WeaponsConfig.S_EXTRA_AUGMENT_SLOT.get() ? 1 : 0);
    }
}
