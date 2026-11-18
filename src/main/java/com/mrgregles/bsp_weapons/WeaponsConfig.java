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
    public static final ForgeConfigSpec.DoubleValue SHOT_HEAT, HEAT_COOL_SECONDS, BOLT_SPEED, PUSH_SCALE;
    public static final ForgeConfigSpec.IntValue OVERHEAT_LOCK_TICKS, OVERHEAT_DURABILITY, WEAR_PER_SHOT, ENERGY_PER_SHOT;
    public static final ForgeConfigSpec.BooleanValue ASSEMBLED_FULL;
    public static final ForgeConfigSpec.IntValue ENHANCER_CAPACITY, ENHANCER_MAX_RECEIVE, FIT_CHARGE, UPGRADE_CHARGE, REPAIR_CHARGE;
    public static final ForgeConfigSpec.DoubleValue REPAIR_SHARE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("sheet");
        TIER_MULTIPLIERS = b.comment("Core tiers I to V: how much the whole sheet improves (lower-is-better stats shrink by the same factor).")
                .defineList("tierMultipliers", List.of(1.0, 1.25, 1.55, 1.9, 2.3), o -> o instanceof Number d && d.doubleValue() > 0);
        TIER_AUGMENT_SLOTS = b.comment("Core tiers I to V: how many augment chips the weapon takes.")
                .defineList("tierAugmentSlots", List.of(1, 1, 2, 2, 3), o -> o instanceof Number i && i.intValue() >= 0 && i.intValue() <= 8);
        RARITY_WEAPON_MULTIPLIERS = b.comment("The weapon's roll, Normal / Rare / Epic / S: how much the finished sheet improves.")
                .defineList("rarityWeaponMultipliers", List.of(1.0, 1.10, 1.20, 1.35), o -> o instanceof Number d && d.doubleValue() > 0);
        RARITY_PART_MULTIPLIERS = b.comment("A part's roll, Normal / Rare / Epic / S: how much of that part's own contribution is kept (1.15 = 115 %).")
                .defineList("rarityPartMultipliers", List.of(1.0, 1.05, 1.10, 1.15), o -> o instanceof Number d && d.doubleValue() > 0);
        S_EXTRA_AUGMENT_SLOT = b.comment("An S-tier weapon gets one augment slot more than its core tier gives.").define("sExtraAugmentSlot", true);
        PIERCING_CAP = b.comment("Armour piercing never goes above this percentage, whatever the parts and chips add.").defineInRange("piercingCap", 75.0, 0.0, 100.0);
        RANGE_CAP = b.comment("No weapon reaches further than this many blocks.").defineInRange("rangeCap", 160.0, 1.0, 1000.0);
        b.pop();
        b.push("firing");
        SHOT_HEAT = b.comment("Heat one shot adds, against the weapon's heat capacity.").defineInRange("shotHeat", 12.0, 0.0, 1000.0);
        HEAT_COOL_SECONDS = b.comment("Seconds a weapon takes to cool from its heat capacity to nothing.").defineInRange("heatCoolSeconds", 3.0, 0.1, 600.0);
        OVERHEAT_LOCK_TICKS = b.comment("Ticks a weapon is locked after overheating.").defineInRange("overheatLockTicks", 30, 1, 1200);
        OVERHEAT_DURABILITY = b.comment("Durability an overheat costs, on top of the wear per shot.").defineInRange("overheatDurability", 5, 0, 1000);
        WEAR_PER_SHOT = b.comment("Durability each shot costs (Unbreaking applies).").defineInRange("wearPerShot", 1, 0, 100);
        BOLT_SPEED = b.comment("Blocks a tick an energy weapon's lance travels (D65).").defineInRange("boltSpeed", 2.4, 0.1, 10.0);
        PUSH_SCALE = b.comment("Multiplies the blocks a chip's push throws the target back (D74; the Knockback chip's data says one block).").defineInRange("pushScale", 1.0, 0.0, 10.0);
        ENERGY_PER_SHOT = b.comment("Forge Energy one shot costs a weapon with an energy cell; its store is Capacity shots' worth (D10).").defineInRange("energyPerShot", 250, 1, 1000000);
        ASSEMBLED_FULL = b.comment("A weapon leaves the assembler with a full charge (true) or empty, to be charged first (false).").define("assembledFull", true);
        b.pop();
        b.push("enhancer");
        ENHANCER_CAPACITY = b.comment("Forge Energy the Weapon Enhancer stores (D75).").defineInRange("enhancerCapacity", 20000, 1, 100000000);
        ENHANCER_MAX_RECEIVE = b.comment("Forge Energy the Weapon Enhancer takes a tick from a cable or charger.").defineInRange("enhancerMaxReceive", 1000, 1, 100000000);
        FIT_CHARGE = b.comment("Forge Energy a fit or a swap burns (D75).").defineInRange("fitCharge", 400, 0, 100000000);
        UPGRADE_CHARGE = b.comment("Forge Energy a Core upgrade burns (D75, D76).").defineInRange("upgradeCharge", 2000, 0, 100000000);
        REPAIR_CHARGE = b.comment("Forge Energy a repair burns (D75, D77).").defineInRange("repairCharge", 1000, 0, 100000000);
        REPAIR_SHARE = b.comment("The share of a weapon's durability one ingot or gem restores (D77).").defineInRange("repairShare", 0.25, 0.01, 1.0);
        b.pop();
        SPEC = b.build();
    }

    private WeaponsConfig() {
    }

    private static final List<Double> TIERS_DEFAULT = List.of(1.0, 1.25, 1.55, 1.9, 2.3), WEAPON_ROLL_DEFAULT = List.of(1.0, 1.10, 1.20, 1.35), PART_ROLL_DEFAULT = List.of(1.0, 1.05, 1.10, 1.15);
    private static final List<Integer> SLOTS_DEFAULT = List.of(1, 1, 2, 2, 3);

    /** A list a config edit emptied falls back to its default, so a bad edit never throws on every tooltip and shot. */
    private static <T> List<? extends T> listOr(List<? extends T> l, List<T> fallback) {
        return l == null || l.isEmpty() ? fallback : l;
    }

    public static double tierMultiplier(int tier) {
        List<? extends Double> l = listOr(TIER_MULTIPLIERS.get(), TIERS_DEFAULT);
        return l.get(Math.max(0, Math.min(l.size() - 1, tier - 1)));
    }

    public static int tierAugmentSlots(int tier) {
        List<? extends Integer> l = listOr(TIER_AUGMENT_SLOTS.get(), SLOTS_DEFAULT);
        return l.get(Math.max(0, Math.min(l.size() - 1, tier - 1)));
    }

    public static double rarityWeaponMultiplier(com.mrgregles.bsp_weapons.parts.Rarity r) {
        List<? extends Double> l = listOr(RARITY_WEAPON_MULTIPLIERS.get(), WEAPON_ROLL_DEFAULT);
        return l.get(Math.min(l.size() - 1, r.ordinal()));
    }

    public static double rarityPartMultiplier(com.mrgregles.bsp_weapons.parts.Rarity r) {
        List<? extends Double> l = listOr(RARITY_PART_MULTIPLIERS.get(), PART_ROLL_DEFAULT);
        return l.get(Math.min(l.size() - 1, r.ordinal()));
    }

    public static int tiers() {
        return listOr(TIER_MULTIPLIERS.get(), TIERS_DEFAULT).size();
    }
}
