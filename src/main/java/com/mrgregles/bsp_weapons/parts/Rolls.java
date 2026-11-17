package com.mrgregles.bsp_weapons.parts;

import net.minecraft.util.RandomSource;

import javax.annotation.Nullable;

/** The rarity roll (D20, D21, D30): no catalyst means Normal; a catalyst weights the four outcomes by its odds. */
public final class Rolls {
    private Rolls() {
    }

    public static Rarity roll(RandomSource random, @Nullable Catalyst catalyst) {
        if (catalyst == null) return Rarity.NORMAL;
        int total = 0;
        for (Rarity r : Rarity.values()) total += catalyst.weight(r);
        int pick = random.nextInt(total);
        for (Rarity r : Rarity.values()) {
            pick -= catalyst.weight(r);
            if (pick < 0) return r;
        }
        return Rarity.NORMAL;
    }
}
