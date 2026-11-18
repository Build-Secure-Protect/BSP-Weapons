package com.mrgregles.bsp_weapons.weapon;

import com.mrgregles.bsp_weapons.BspWeapons;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * How a weapon's hit becomes damage (D11, D23). The laser damage type bypasses vanilla armour, and this class applies the
 * armour itself to the share the weapon does not pierce, with vanilla's own formula (armour and toughness), so a 50 %
 * piercing weapon has half its hit ignore armour. Protection enchantments, the Resistance effect and BSP-Core's totem
 * Resistance still see the whole hit, since none of those is armour. The shooter is the source's entity, so a kill is
 * the player's (BSP-RankUp counts it) and the death message names them.
 */
public final class Damage {
    public static final ResourceKey<DamageType> LASER = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(BspWeapons.MODID, "laser"));

    private Damage() {
    }

    /** The amount to deal through an armour-bypassing type: the pierced share whole, the rest after the target's armour. */
    public static float amount(double damage, double piercingPercent, double armour, double toughness) {
        double p = Math.max(0, Math.min(1, piercingPercent / 100.0));
        double pierced = damage * p, rest = damage * (1 - p);
        double restAfter = CombatRules.getDamageAfterAbsorb((float) rest, (float) armour, (float) toughness);
        return (float) (pierced + restAfter);
    }

    public static float amountFor(LivingEntity target, double damage, double piercingPercent) {
        return amount(damage, piercingPercent, target.getArmorValue(), target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
    }

    public static DamageSource laser(Level level, @Nullable Entity bolt, @Nullable Entity shooter) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(LASER), bolt, shooter);
    }
}
