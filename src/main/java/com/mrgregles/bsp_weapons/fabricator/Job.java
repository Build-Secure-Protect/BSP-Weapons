package com.mrgregles.bsp_weapons.fabricator;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * One row of the Weapon Fabricator's catalogue (D109, R27): a part type (any allowed material), an augment chip, or a
 * fabricating recipe (an engram, a Core, a catalyst, rounds). The tab is where the Catalogue lists it.
 */
public record Job(Kind kind, ResourceLocation id) {
    public enum Kind {
        PART, CHIP, RECIPE;

        public static Kind byId(String id) {
            for (Kind k : values()) if (k.name().equalsIgnoreCase(id)) return k;
            return null;
        }
    }

    public enum Tab {
        PARTS, CHIPS, ENGRAM, MORE;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static Job part(ResourceLocation partType) {
        return new Job(Kind.PART, partType);
    }

    public static Job chip(ResourceLocation augment) {
        return new Job(Kind.CHIP, augment);
    }

    public static Job recipe(ResourceLocation recipe) {
        return new Job(Kind.RECIPE, recipe);
    }

    /** Parts and chips roll a rarity with a catalyst (D100, D101); the fixed recipes do not (D121). */
    public boolean rolls() {
        return kind != Kind.RECIPE;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("Kind", kind.name().toLowerCase(Locale.ROOT));
        t.putString("Id", id.toString());
        return t;
    }

    /** The job a tag names, or null for a tag that names none. */
    @Nullable
    public static Job load(@Nullable CompoundTag t) {
        if (t == null) return null;
        Kind k = Kind.byId(t.getString("Kind"));
        ResourceLocation id = ResourceLocation.tryParse(t.getString("Id"));
        return k == null || id == null ? null : new Job(k, id);
    }
}
