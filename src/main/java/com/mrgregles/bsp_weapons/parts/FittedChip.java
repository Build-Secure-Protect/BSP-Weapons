package com.mrgregles.bsp_weapons.parts;

import com.mrgregles.bsp_weapons.BspWeapons;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * A chip as a weapon remembers it (D72, D81, D83): which augment, the chip's own roll (which scales its effect by the
 * part multipliers) and the points it has left. Tag: {@code {Id, Rarity, Durability}}. Nothing else is trusted: the
 * effect itself always comes from the augment's data.
 */
public record FittedChip(ResourceLocation augment, Rarity rarity, int durability) {
    /** The most points a tag may claim; the data's own durability (about ten) is the real ceiling the machine applies. */
    public static final int MAX_DURABILITY = 255;

    public FittedChip {
        durability = Math.max(0, Math.min(MAX_DURABILITY, durability));
    }

    public CompoundTag toTag() {
        CompoundTag t = new CompoundTag();
        t.putString("Id", augment.toString());
        t.putString("Rarity", rarity.id());
        t.putInt("Durability", durability);
        return t;
    }

    /** Reads a chip; junk becomes an unknown augment, a Normal roll or no points, all of which validation or the machine refuse. */
    public static FittedChip fromTag(CompoundTag t) {
        ResourceLocation id = ResourceLocation.tryParse(t.getString("Id"));
        Rarity r = Rarity.byId(t.getString("Rarity"));
        return new FittedChip(id == null ? new ResourceLocation(BspWeapons.MODID, "unknown") : id, r == null ? Rarity.NORMAL : r, t.getInt("Durability"));
    }

    /** The chip after one point is spent. */
    public FittedChip worn() {
        return new FittedChip(augment, rarity, durability - 1);
    }

    public boolean burntOut() {
        return durability <= 0;
    }
}
