package com.mrgregles.bsp_weapons.weapon;

import com.mrgregles.bsp_weapons.WeaponsConfig;
import com.mrgregles.bsp_weapons.item.CoreItem;
import com.mrgregles.bsp_weapons.item.WeaponItem;
import com.mrgregles.bsp_weapons.parts.Sheet;
import com.mrgregles.bsp_weapons.parts.SheetCalculator;
import com.mrgregles.bsp_weapons.parts.Stat;
import com.mrgregles.bsp_weapons.parts.Validation;
import com.mrgregles.bsp_weapons.parts.WeaponSpec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The server's side of a shot (D28, D45, D65): the weapon's charge, heat, lock and reload live in its tag and are read
 * and written only here. A shot needs the weapon valid, not locked, not reloading, off cooldown and with charge; it
 * spawns the lance, adds heat, spends charge and wear, and locks the weapon with extra wear when the heat reaches the
 * capacity. An empty weapon starts its reload (the sheet's Recharge in ticks) and is full when it ends.
 */
public final class Firing {
    public static final String CHARGE = "Charge", HEAT = "Heat", HEAT_AT = "HeatAt", LAST_SHOT = "LastShot", LOCKED_UNTIL = "LockedUntil", RELOAD_UNTIL = "ReloadUntil";

    private Firing() {
    }

    public enum Refusal { NONE, INVALID, LOCKED, RELOADING, COOLDOWN, EMPTY, BROKEN }

    /** Shots left: for an energy cell, whole shots' worth of stored FE; for a magazine, the rounds in it. */
    public static int shots(ItemStack stack) {
        return WeaponEnergy.holdsEnergy(stack) ? WeaponEnergy.stored(stack) / Math.max(1, WeaponEnergy.perShot()) : charge(stack);
    }

    /** Heat as of now: what was recorded, cooled by the time since. */
    public static double heat(ItemStack stack, long now) {
        CompoundTag t = stack.getTag();
        if (t == null || !t.contains(HEAT)) return 0;
        double heat = t.getDouble(HEAT);
        long since = Math.max(0, now - t.getLong(HEAT_AT));
        Sheet sheet = sheetOf(stack);
        double cap = sheet == null ? 50 : Math.max(1, sheet.get(Stat.HEAT_CAP));
        double perTick = cap / (WeaponsConfig.HEAT_COOL_SECONDS.get() * 20);
        return Math.max(0, heat - since * perTick);
    }

    public static int charge(ItemStack stack) {
        CompoundTag t = stack.getTag();
        Sheet sheet = sheetOf(stack);
        int cap = sheet == null ? 0 : (int) Math.round(sheet.get(Stat.CAPACITY));
        return t == null || !t.contains(CHARGE) ? 0 : Math.max(0, Math.min(cap, t.getInt(CHARGE)));
    }

    /**
     * The stamps on a weapon are game-time ticks, and a weapon carried to another server (BSP-InventorySync copies the
     * tag) meets a clock that may be far ahead or behind. A stamp in the future is pulled back to now, so a lock, a
     * reload or a cooldown lasts at most its own window from the moment the weapon arrives, never hours.
     */
    static void normalise(CompoundTag t, long now) {
        for (String key : new String[]{LAST_SHOT, HEAT_AT}) if (t.contains(key) && t.getLong(key) > now) t.putLong(key, now);
        if (t.contains(LOCKED_UNTIL) && t.getLong(LOCKED_UNTIL) > now + WeaponsConfig.OVERHEAT_LOCK_TICKS.get()) t.putLong(LOCKED_UNTIL, now + WeaponsConfig.OVERHEAT_LOCK_TICKS.get());
        if (t.contains(RELOAD_UNTIL) && t.getLong(RELOAD_UNTIL) > now + 20 * 60) t.putLong(RELOAD_UNTIL, now + 20 * 60);
    }

    public static boolean locked(ItemStack stack, long now) {
        CompoundTag t = stack.getTag();
        return t != null && t.getLong(LOCKED_UNTIL) > now;
    }

    public static boolean reloading(ItemStack stack, long now) {
        CompoundTag t = stack.getTag();
        return t != null && t.getLong(RELOAD_UNTIL) > now;
    }

    /** Settles a reload that has ended: a player's magazine takes up to Capacity rounds from their inventory (D70); anyone else's is simply full. */
    public static void settle(ItemStack stack, long now, @Nullable LivingEntity holder) {
        CompoundTag t = stack.getTag();
        if (t == null || !t.contains(RELOAD_UNTIL)) return;
        if (t.getLong(RELOAD_UNTIL) <= now && t.getLong(RELOAD_UNTIL) > 0) {
            Sheet sheet = sheetOf(stack);
            int capacity = sheet == null ? 0 : (int) Math.round(sheet.get(Stat.CAPACITY));
            t.putInt(CHARGE, holder instanceof net.minecraft.world.entity.player.Player player ? takeRounds(player, capacity) : capacity);
            t.remove(RELOAD_UNTIL);
        }
    }

    public static void settle(ItemStack stack, long now) {
        settle(stack, now, null);
    }

    /** Rounds in a player's inventory. */
    public static int roundsCarried(net.minecraft.world.entity.player.Player player) {
        int n = 0;
        for (ItemStack s : player.getInventory().items) if (s.is(com.mrgregles.bsp_weapons.registry.ModItems.ENERGY_ROUND.get())) n += s.getCount();
        return n;
    }

    /** Removes up to {@code wanted} rounds from the inventory and returns how many were taken. */
    static int takeRounds(net.minecraft.world.entity.player.Player player, int wanted) {
        int taken = 0;
        for (ItemStack s : player.getInventory().items) {
            if (taken >= wanted) break;
            if (!s.is(com.mrgregles.bsp_weapons.registry.ModItems.ENERGY_ROUND.get())) continue;
            int n = Math.min(wanted - taken, s.getCount());
            s.shrink(n);
            taken += n;
        }
        return taken;
    }

    public static int cooldownTicks(Sheet sheet) {
        double rate = Math.max(0.1, sheet.get(Stat.FIRE_RATE));
        return Math.max(1, (int) Math.round(20.0 / rate));
    }

    @Nullable
    public static Sheet sheetOf(ItemStack stack) {
        WeaponSpec spec = WeaponItem.specOf(stack);
        return spec == null || !Validation.isValid(spec) ? null : SheetCalculator.compute(spec);
    }

    public static Refusal check(ItemStack stack, long now) {
        return check(stack, now, null);
    }

    /** Why the weapon will not fire now, or NONE. Also starts the reload of an empty magazine when its holder has rounds. */
    public static Refusal check(ItemStack stack, long now, @Nullable LivingEntity holder) {
        if (stack.getTag() != null) normalise(stack.getTag(), now);
        settle(stack, now, holder);
        Sheet sheet = sheetOf(stack);
        if (sheet == null) return Refusal.INVALID;
        if (stack.getTag() != null && stack.getTag().getBoolean("Unbreakable")) return Refusal.INVALID; // a forged tag: no weapon of ours is unbreakable
        if (stack.getMaxDamage() > 0 && stack.getDamageValue() >= stack.getMaxDamage() - 1) return Refusal.BROKEN;
        if (locked(stack, now)) return Refusal.LOCKED;
        if (reloading(stack, now)) return Refusal.RELOADING;
        CompoundTag t = stack.getOrCreateTag();
        if (t.contains(LAST_SHOT) && now - t.getLong(LAST_SHOT) < cooldownTicks(sheet)) return Refusal.COOLDOWN;
        if (WeaponEnergy.holdsEnergy(stack)) {
            if (WeaponEnergy.stored(stack) < WeaponEnergy.perShot()) return Refusal.EMPTY; // charge it: an energy cell never refills on its own
            return Refusal.NONE;
        }
        if (charge(stack) <= 0) {
            boolean hasRounds = !(holder instanceof net.minecraft.world.entity.player.Player player) || roundsCarried(player) > 0;
            if (hasRounds) t.putLong(RELOAD_UNTIL, now + Math.max(1, (long) Math.round(sheet.get(Stat.RECHARGE))));
            return Refusal.EMPTY;
        }
        return Refusal.NONE;
    }

    /** Fires if it can: spawns the lance along the shooter's look, books the shot on the weapon. Returns the refusal, NONE on a shot. */
    /** The blocks the weapon's chips throw a target back (D74): each chip's push, scaled by its own roll (D81) and the config. */
    public static double pushOf(WeaponSpec spec) {
        double push = 0;
        for (com.mrgregles.bsp_weapons.parts.FittedChip c : spec.augments()) {
            com.mrgregles.bsp_weapons.parts.Augment a = com.mrgregles.bsp_weapons.data.WeaponData.augment(c.augment());
            if (a != null) push += a.push() * WeaponsConfig.rarityPartMultiplier(c.rarity());
        }
        return push * WeaponsConfig.PUSH_SCALE.get();
    }

    public static Refusal tryFire(ServerLevel level, LivingEntity shooter, ItemStack stack) {
        long now = level.getGameTime();
        Refusal why = check(stack, now, shooter);
        if (why != Refusal.NONE) return why;
        Sheet sheet = sheetOf(stack);
        CompoundTag t = stack.getOrCreateTag();
        WeaponSpec spec = WeaponItem.specOf(stack);
        int colour = CoreItem.TIER_COLOURS[Math.max(1, Math.min(CoreItem.TIER_COLOURS.length, spec.coreTier())) - 1];
        LaserBoltEntity bolt = new LaserBoltEntity(level, shooter, sheet.get(Stat.DAMAGE), sheet.get(Stat.PIERCING), sheet.get(Stat.RANGE), colour);
        Vec3 look = shooter.getLookAngle();
        double spread = Math.toRadians(sheet.get(Stat.SPREAD));
        if (spread > 0) {
            look = look.add((level.random.nextDouble() - 0.5) * spread, (level.random.nextDouble() - 0.5) * spread, (level.random.nextDouble() - 0.5) * spread).normalize();
        }
        bolt.push(pushOf(spec));
        bolt.fly(look.scale(WeaponsConfig.BOLT_SPEED.get()));
        level.addFreshEntity(bolt);
        t.putLong(LAST_SHOT, now);
        if (WeaponEnergy.holdsEnergy(stack)) WeaponEnergy.setStored(stack, WeaponEnergy.stored(stack) - WeaponEnergy.perShot());
        else t.putInt(CHARGE, charge(stack) - 1);
        double heat = heat(stack, now) + WeaponsConfig.SHOT_HEAT.get();
        double cap = Math.max(1, sheet.get(Stat.HEAT_CAP));
        if (heat >= cap) {
            heat = cap;
            t.putLong(LOCKED_UNTIL, now + WeaponsConfig.OVERHEAT_LOCK_TICKS.get());
            wear(stack, shooter, WeaponsConfig.OVERHEAT_DURABILITY.get());
        }
        t.putDouble(HEAT, heat);
        t.putLong(HEAT_AT, now);
        wear(stack, shooter, WeaponsConfig.WEAR_PER_SHOT.get());
        return Refusal.NONE;
    }

    /** Wear never breaks the weapon: it stops at its last point, where {@link #check} refuses to fire (BROKEN). */
    private static void wear(ItemStack stack, LivingEntity shooter, int amount) {
        if (amount <= 0 || stack.getMaxDamage() <= 0) return;
        int room = stack.getMaxDamage() - 1 - stack.getDamageValue();
        if (room <= 0) return;
        stack.hurtAndBreak(Math.min(amount, room), shooter, s -> s.broadcastBreakEvent(EquipmentSlot.MAINHAND));
    }
}
