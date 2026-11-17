package com.mrgregles.bsp_weapons.parts;

import java.util.Locale;

/**
 * The numbers on a weapon's sheet. {@code lowerIsBetter} says which way an improvement goes when a multiplier (a core
 * tier, a rarity) is applied: a Tier V core makes a charge time shorter, not longer.
 */
public enum Stat {
    DAMAGE(false), PIERCING(false), RANGE(false), FIRE_RATE(false), CHARGE_TIME(true), CAPACITY(false), RECHARGE(true),
    SPREAD(true), ZOOM(false), HEAT_CAP(false), BLOCK_DAMAGE(false), RICOCHETS(false), DURABILITY(false);

    public final boolean lowerIsBetter;

    Stat(boolean lowerIsBetter) {
        this.lowerIsBetter = lowerIsBetter;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The stat with this id (upper or lower case), or null. */
    public static Stat byId(String id) {
        for (Stat s : values()) if (s.name().equalsIgnoreCase(id)) return s;
        return null;
    }
}
