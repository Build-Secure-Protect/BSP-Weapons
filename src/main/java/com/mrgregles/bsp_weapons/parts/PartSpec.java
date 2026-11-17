package com.mrgregles.bsp_weapons.parts;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** One fitted part as the item remembers it: its kind, type, material and its own roll. Never the numbers. */
public record PartSpec(PartKind kind, ResourceLocation type, ResourceLocation material, Rarity rarity) {

    public CompoundTag toTag() {
        CompoundTag t = new CompoundTag();
        t.putString("Kind", kind.id());
        t.putString("Type", type.toString());
        t.putString("Material", material.toString());
        t.putString("Rarity", rarity.id());
        return t;
    }

    /** Reads a part; a malformed tag gives a part that validation will refuse rather than an exception. */
    public static PartSpec fromTag(CompoundTag t) {
        PartKind kind = PartKind.byId(t.getString("Kind"));
        ResourceLocation type = ResourceLocation.tryParse(t.getString("Type"));
        ResourceLocation material = ResourceLocation.tryParse(t.getString("Material"));
        Rarity rarity = Rarity.byId(t.getString("Rarity"));
        return new PartSpec(kind == null ? PartKind.BARREL : kind,
                type == null ? new ResourceLocation("bsp_weapons", "unknown") : type,
                material == null ? new ResourceLocation("bsp_weapons", "unknown") : material,
                rarity == null ? Rarity.NORMAL : rarity);
    }
}
