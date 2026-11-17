package com.mrgregles.bsp_weapons.parts;

/** How a weapon class delivers its hit; augments and types may be limited to a kind. */
public enum WeaponKind {
    ENERGY, PROJECTILE, THROWN;

    public static WeaponKind byId(String id) {
        for (WeaponKind k : values()) if (k.name().equalsIgnoreCase(id)) return k;
        return null;
    }
}
