package com.mrgregles.bsp_weapons;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/** The server config: the multipliers of the layers (D7, D22, D29) and the caps. Numbers the owner tunes. */
public final class WeaponsConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<List<? extends Double>> TIER_MULTIPLIERS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> TIER_AUGMENT_SLOTS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends Double>> RARITY_WEAPON_MULTIPLIERS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends Double>> RARITY_PART_MULTIPLIERS;
    public static final ForgeConfigSpec.BooleanValue S_EXTRA_AUGMENT_SLOT;
    public static final ForgeConfigSpec.DoubleValue PIERCING_CAP;
    public static final ForgeConfigSpec.DoubleValue RANGE_CAP;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("sheet");
        TIER_MULTIPLIERS = b.comment("Core tiers I to V: how much the whole sheet improves (lower-is-better stats shrink by the same factor).")
                .defineList("tierMultipliers", List.of(1.0, 1.25, 1.55, 1.9, 2.3), o -> o instanceof Double d && d > 0);
        TIER_AUGMENT_SLOTS = b.comment("Core tiers I to V: how many augment chips the weapon takes.")
                .defineList("tierAugmentSlots", List.of(1, 1, 2, 2, 3), o -> o instanceof Integer i && i >= 0 && i <= 8);
        RARITY_WEAPON_MULTIPLIERS = b.comment("The weapon's roll, Normal / Rare / Epic / S: how much the finished sheet improves.")
                .defineList("rarityWeaponMultipliers", List.of(1.0, 1.10, 1.20, 1.35), o -> o instanceof Double d && d > 0);
        RARITY_PART_MULTIPLIERS = b.comment("A part's roll, Normal / Rare / Epic / S: how much of that part's own contribution is kept (1.15 = 115 %).")
                .defineList("rarityPartMultipliers", List.of(1.0, 1.05, 1.10, 1.15), o -> o instanceof Double d && d > 0);
        S_EXTRA_AUGMENT_SLOT = b.comment("An S-tier weapon gets one augment slot more than its core tier gives.").define("sExtraAugmentSlot", true);
        PIERCING_CAP = b.comment("Armour piercing never goes above this percentage, whatever the parts and chips add.").defineInRange("piercingCap", 75.0, 0.0, 100.0);
        RANGE_CAP = b.comment("No weapon reaches further than this many blocks.").defineInRange("rangeCap", 160.0, 1.0, 1000.0);
        b.pop();
        SPEC = b.build();
    }

    private WeaponsConfig() {
    }

    public static double tierMultiplier(int tier) {
        List<? extends Double> l = TIER_MULTIPLIERS.get();
        return l.get(Math.max(0, Math.min(l.size() - 1, tier - 1)));
    }

    public static int tierAugmentSlots(int tier) {
        List<? extends Integer> l = TIER_AUGMENT_SLOTS.get();
        return l.get(Math.max(0, Math.min(l.size() - 1, tier - 1)));
    }

    public static double rarityWeaponMultiplier(com.mrgregles.bsp_weapons.parts.Rarity r) {
        List<? extends Double> l = RARITY_WEAPON_MULTIPLIERS.get();
        return l.get(Math.min(l.size() - 1, r.ordinal()));
    }

    public static double rarityPartMultiplier(com.mrgregles.bsp_weapons.parts.Rarity r) {
        List<? extends Double> l = RARITY_PART_MULTIPLIERS.get();
        return l.get(Math.min(l.size() - 1, r.ordinal()));
    }

    public static int tiers() {
        return TIER_MULTIPLIERS.get().size();
    }
}
