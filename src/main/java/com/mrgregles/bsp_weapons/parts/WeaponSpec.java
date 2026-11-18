package com.mrgregles.bsp_weapons.parts;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything a weapon item remembers (D5, D8, D20, D29, D83): its pattern, core tier, parts, the weapon's roll and its chips
 * (each with its own roll and points left).
 * The sheet is never stored: {@link SheetCalculator} works it out from this on the server each time, so a tag edited by a
 * client changes nothing.
 */
public record WeaponSpec(ResourceLocation pattern, int coreTier, List<PartSpec> parts, Rarity roll, List<FittedChip> augments) {
    public static final String TAG = "BspWeapon";
    public static final int MAX_PARTS = 16;
    public static final int MAX_AUGMENTS = 8;

    public WeaponSpec {
        parts = List.copyOf(parts);
        augments = List.copyOf(augments);
    }

    /** The fitted part of a kind, or null. */
    public PartSpec part(PartKind kind) {
        for (PartSpec p : parts) if (p.kind() == kind) return p;
        return null;
    }

    public CompoundTag toTag() {
        CompoundTag t = new CompoundTag();
        t.putString("Pattern", pattern.toString());
        t.putInt("Core", coreTier);
        ListTag ps = new ListTag();
        for (PartSpec p : parts) ps.add(p.toTag());
        t.put("Parts", ps);
        t.putString("Roll", roll.id());
        ListTag as = new ListTag();
        for (FittedChip a : augments) as.add(a.toTag());
        t.put("Chips", as);
        return t;
    }

    /** Reads a weapon; lists are capped so a forged tag cannot carry thousands of entries. Unknown ids are left for validation. */
    public static WeaponSpec fromTag(CompoundTag t) {
        ResourceLocation pattern = ResourceLocation.tryParse(t.getString("Pattern"));
        List<PartSpec> parts = new ArrayList<>();
        ListTag ps = t.getList("Parts", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(ps.size(), MAX_PARTS); i++) parts.add(PartSpec.fromTag(ps.getCompound(i)));
        List<FittedChip> augments = new ArrayList<>();
        ListTag as = t.getList("Chips", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(as.size(), MAX_AUGMENTS); i++) augments.add(FittedChip.fromTag(as.getCompound(i)));
        Rarity roll = Rarity.byId(t.getString("Roll"));
        return new WeaponSpec(pattern == null ? new ResourceLocation("bsp_weapons", "unknown") : pattern, t.getInt("Core"), parts,
                roll == null ? Rarity.NORMAL : roll, augments);
    }
}
