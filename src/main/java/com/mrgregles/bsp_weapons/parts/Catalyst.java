package com.mrgregles.bsp_weapons.parts;

import net.minecraft.resources.ResourceLocation;

/**
 * A roll catalyst (D21, D30): lapis plus one ingredient; {@code odds} are the weights of Normal, Rare, Epic and S. The
 * ingredient is an item tag or item id as written in the JSON; the recipe task reads it.
 */
public record Catalyst(ResourceLocation id, String ingredient, int[] odds, int colour) {

    public Catalyst {
        if (odds.length != Rarity.values().length) throw new IllegalArgumentException(id + ": odds need " + Rarity.values().length + " numbers");
        long sum = 0;
        for (int o : odds) {
            if (o < 0) throw new IllegalArgumentException(id + ": odds cannot be negative");
            sum += o;
        }
        if (sum == 0) throw new IllegalArgumentException(id + ": odds are all zero");
        if (sum > 1_000_000) throw new IllegalArgumentException(id + ": odds add up past a million");
        odds = odds.clone();
    }

    public int weight(Rarity r) {
        return odds[r.ordinal()];
    }
}
