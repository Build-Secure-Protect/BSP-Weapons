package com.mrgregles.bsp_weapons.gametest;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.WeaponsConfig;
import com.mrgregles.bsp_weapons.compat.RegionGuard;
import com.mrgregles.bsp_weapons.item.WeaponItem;
import com.mrgregles.bsp_weapons.parts.PartKind;
import com.mrgregles.bsp_weapons.parts.PartSpec;
import com.mrgregles.bsp_weapons.parts.Rarity;
import com.mrgregles.bsp_weapons.parts.Sheet;
import com.mrgregles.bsp_weapons.parts.Stat;
import com.mrgregles.bsp_weapons.parts.WeaponSpec;
import com.mrgregles.bsp_weapons.registry.ModItems;
import com.mrgregles.bsp_weapons.weapon.Damage;
import com.mrgregles.bsp_weapons.weapon.Firing;
import com.mrgregles.bsp_weapons.weapon.LaserBoltEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Firing (WP-4; D11, D23, D28, D45, D64, D65): the shot's rules on the weapon's tag, the lance, its hit, piercing, range and the Region Guard hook. */
@GameTestHolder(BspWeapons.MODID)
@PrefixGameTestTemplate(false)
public class FiringGameTests {
    private static final String EMPTY = SetupGameTests.EMPTY;

    private static ResourceLocation id(String path) {
        return new ResourceLocation(BspWeapons.MODID, path);
    }

    private static ItemStack pistol(int tier) {
        return pistol(tier, "energy_cell");
    }

    private static ItemStack pistol(int tier, String cell) {
        return pistol(tier, cell, List.of());
    }

    private static ItemStack pistol(int tier, String cell, List<com.mrgregles.bsp_weapons.parts.FittedChip> chips) {
        WeaponSpec spec = new WeaponSpec(id("laser_pistol"), tier, List.of(
                new PartSpec(PartKind.BARREL, id("focused_barrel"), id("tetrium"), Rarity.NORMAL),
                new PartSpec(PartKind.CELL, id(cell), id("steel"), Rarity.NORMAL),
                new PartSpec(PartKind.GRIP, id("standard_grip"), id("magnatite"), Rarity.NORMAL),
                new PartSpec(PartKind.OPTIC, id("iron_sight"), id("tetrium"), Rarity.NORMAL)), Rarity.NORMAL, chips);
        return ModItems.weapon(id("laser_pistol")).of(spec);
    }

    /** A mock player standing at the test's spot, looking along +x. */
    private static ServerPlayer shooter(GameTestHelper h, BlockPos at) {
        ServerPlayer p = TestPlayers.mockServerPlayer(h);
        Vec3 pos = h.absoluteVec(Vec3.atBottomCenterOf(at));
        p.moveTo(pos.x, pos.y, pos.z, -90f, 0f); // yaw -90 looks along +x
        return p;
    }

    /** The lances inside this test's own box (never a neighbour's: tests of one batch run side by side). */
    private static List<LaserBoltEntity> bolts(GameTestHelper h) {
        return h.getLevel().getEntitiesOfClass(LaserBoltEntity.class, new AABB(h.absolutePos(new BlockPos(0, 0, 0)), h.absolutePos(new BlockPos(24, 10, 24))));
    }

    /** Removes only the lances this shooter fired. */
    private static void clear(GameTestHelper h, ServerPlayer shooter) {
        for (LaserBoltEntity b : bolts(h)) if (b.getOwner() == shooter) b.discard();
    }

    @GameTest(template = EMPTY, batch = "firing")
    public void a_shot_spends_charge_adds_heat_and_wear_and_the_next_waits_for_the_cooldown(GameTestHelper h) {
        ItemStack gun = pistol(2);
        ServerPlayer p = shooter(h, new BlockPos(2, 2, 2));
        Sheet sheet = Firing.sheetOf(gun);
        int capacity = (int) Math.round(sheet.get(Stat.CAPACITY));
        h.assertTrue(Firing.shots(gun) == capacity, "a new weapon is full: " + Firing.shots(gun));
        h.assertTrue(gun.getMaxDamage() == Math.round(sheet.get(Stat.DURABILITY)), "durability is the sheet's: " + gun.getMaxDamage());
        Firing.Refusal r = Firing.tryFire(h.getLevel(), p, gun);
        h.assertTrue(r == Firing.Refusal.NONE, "the first shot fires: " + r);
        h.assertTrue(Firing.shots(gun) == capacity - 1, "one shot spent");
        h.assertTrue(Firing.heat(gun, h.getLevel().getGameTime()) >= WeaponsConfig.SHOT_HEAT.get() - 1e-6, "heat added");
        h.assertTrue(gun.getDamageValue() == WeaponsConfig.WEAR_PER_SHOT.get(), "one point of wear");
        h.assertTrue(Firing.tryFire(h.getLevel(), p, gun) == Firing.Refusal.COOLDOWN, "the second shot waits for the cooldown");
        List<LaserBoltEntity> mine = bolts(h).stream().filter(b -> b.getOwner() == p).toList(); // other tests' lances may cross this box
        h.assertTrue(mine.size() == 1, "one lance of this shooter's: " + mine.size());
        h.assertTrue(mine.get(0).getDeltaMovement().x > 2, "the lance flies along the look at the config's speed");
        clear(h, p);
        // an invalid weapon never fires
        ItemStack bare = new ItemStack(ModItems.weapon(id("laser_pistol")));
        h.assertTrue(Firing.tryFire(h.getLevel(), p, bare) == Firing.Refusal.INVALID, "no spec, no shot");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "firing")
    public void overheating_locks_the_weapon_and_costs_durability_and_an_empty_one_reloads(GameTestHelper h) {
        ItemStack gun = pistol(1, "magazine"); // a magazine reloads from itself for now (rounds come with WP-5b); an energy cell must be charged
        ServerPlayer p = shooter(h, new BlockPos(2, 2, 6));
        Sheet sheet = Firing.sheetOf(gun);
        double cap = sheet.get(Stat.HEAT_CAP);
        CompoundTag t = gun.getOrCreateTag();
        // heat sitting just under the cap: the next shot overheats
        t.putDouble(Firing.HEAT, cap - 1);
        t.putLong(Firing.HEAT_AT, h.getLevel().getGameTime());
        int wearBefore = gun.getDamageValue();
        h.assertTrue(Firing.tryFire(h.getLevel(), p, gun) == Firing.Refusal.NONE, "the hot shot still fires");
        h.assertTrue(Firing.locked(gun, h.getLevel().getGameTime()), "and locks the weapon");
        h.assertTrue(gun.getDamageValue() == wearBefore + WeaponsConfig.WEAR_PER_SHOT.get() + WeaponsConfig.OVERHEAT_DURABILITY.get(), "the overheat costs durability: " + gun.getDamageValue());
        t.putLong(Firing.LAST_SHOT, 0);
        h.assertTrue(Firing.tryFire(h.getLevel(), p, gun) == Firing.Refusal.LOCKED, "locked weapons do not fire");
        t.putLong(Firing.LOCKED_UNTIL, 0);
        t.putDouble(Firing.HEAT, 0);
        // empty: the reload starts (the player carries rounds, D70) and lasts the sheet's Recharge
        p.getInventory().add(new ItemStack(ModItems.ENERGY_ROUND.get(), 64));
        t.putInt(Firing.CHARGE, 0);
        h.assertTrue(Firing.tryFire(h.getLevel(), p, gun) == Firing.Refusal.EMPTY, "an empty weapon starts reloading");
        h.assertTrue(Firing.reloading(gun, h.getLevel().getGameTime()), "and is reloading");
        h.assertTrue(t.getLong(Firing.RELOAD_UNTIL) - h.getLevel().getGameTime() == Math.round(sheet.get(Stat.RECHARGE)), "for the sheet's Recharge ticks");
        h.assertTrue(Firing.tryFire(h.getLevel(), p, gun) == Firing.Refusal.RELOADING, "no shot while reloading");
        t.putLong(Firing.RELOAD_UNTIL, h.getLevel().getGameTime() - 1);
        h.assertTrue(Firing.tryFire(h.getLevel(), p, gun) == Firing.Refusal.NONE && Firing.charge(gun) == Math.round(sheet.get(Stat.CAPACITY)) - 1, "full again after the reload, and fired");
        clear(h, p);
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "firing_hit", timeoutTicks = 200)
    public void the_lance_hurts_what_it_hits_and_stops_at_a_block(GameTestHelper h) {
        // a floor to stand on, the shooter at x 2, a zombie at x 8 in the line of fire, a wall at x 12 and a second zombie behind it
        for (int x = 1; x <= 20; x++) for (int z = 2; z <= 6; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        for (int y = 2; y <= 5; y++) for (int z = 2; z <= 6; z++) h.setBlock(new BlockPos(12, y, z), Blocks.STONE);
        ServerPlayer p = shooter(h, new BlockPos(2, 2, 4));
        Zombie near = h.spawn(EntityType.ZOMBIE, new BlockPos(8, 2, 4));
        Zombie far = h.spawn(EntityType.ZOMBIE, new BlockPos(16, 2, 4));
        near.setNoAi(true); far.setNoAi(true);
        float nearHealth = near.getHealth(), farHealth = far.getHealth();
        ItemStack gun = pistol(2);
        h.assertTrue(Firing.tryFire(h.getLevel(), p, gun) == Firing.Refusal.NONE, "fired");
        h.succeedWhen(() -> {
            h.assertTrue(near.getHealth() < nearHealth, "the near zombie was hurt: " + near.getHealth() + " of " + nearHealth);
            h.assertTrue(far.getHealth() == farHealth, "the wall keeps the far zombie safe");
            h.assertTrue(bolts(h).stream().noneMatch(b -> b.getOwner() == p), "the lance is gone after the hit");
        });
    }

    @GameTest(template = EMPTY, batch = "firing_push", timeoutTicks = 200)
    public void a_knockback_chip_throws_the_target_back_along_the_shot(GameTestHelper h) {
        // two lanes on one floor: a plain pistol at z 4, a Knockback pistol at z 8; the zombies keep their AI so they can be moved, but cannot walk
        for (int x = 1; x <= 22; x++) for (int z = 2; z <= 10; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        ServerPlayer plainShooter = shooter(h, new BlockPos(2, 2, 4)), chipShooter = shooter(h, new BlockPos(2, 2, 8));
        Zombie plain = h.spawn(EntityType.ZOMBIE, new BlockPos(8, 2, 4)), pushed = h.spawn(EntityType.ZOMBIE, new BlockPos(8, 2, 8));
        for (Zombie z : List.of(plain, pushed)) z.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 400, 10, false, false));
        double plainStart = plain.getX(), pushedStart = pushed.getX();
        ItemStack gun = pistol(2), chipped = pistol(3, "energy_cell", List.of(new com.mrgregles.bsp_weapons.parts.FittedChip(id("knockback"), Rarity.NORMAL, 10)));
        h.assertTrue(Firing.pushOf(WeaponItem.specOf(gun)) == 0 && Math.abs(Firing.pushOf(WeaponItem.specOf(chipped)) - WeaponsConfig.PUSH_SCALE.get()) < 1e-9, "the chip gives a block of push, scaled by the config");
        h.assertTrue(Firing.tryFire(h.getLevel(), plainShooter, gun) == Firing.Refusal.NONE && Firing.tryFire(h.getLevel(), chipShooter, chipped) == Firing.Refusal.NONE, "both fired");
        h.runAfterDelay(60, () -> {
            double plainMoved = plain.getX() - plainStart, pushedMoved = pushed.getX() - pushedStart;
            h.assertTrue(plain.getHealth() < plain.getMaxHealth() && pushed.getHealth() < pushed.getMaxHealth(), "both zombies were hurt");
            h.assertTrue(pushedMoved > 0.5, "the Knockback zombie was thrown back along the shot: " + pushedMoved);
            h.assertTrue(pushedMoved > plainMoved + 0.3, "and further than the plain hit moved its zombie: " + pushedMoved + " vs " + plainMoved);
            h.succeed();
        });
    }

    @GameTest(template = EMPTY, batch = "firing_range", timeoutTicks = 200)
    public void the_lance_fades_at_its_range(GameTestHelper h) {
        ServerPlayer p = shooter(h, new BlockPos(2, 5, 10));
        ItemStack gun = pistol(1);
        LaserBoltEntity bolt = new LaserBoltEntity(h.getLevel(), p, 4, 0, 3, 0x19D3B0); // a three-block range
        bolt.setDeltaMovement(new Vec3(WeaponsConfig.BOLT_SPEED.get(), 0, 0));
        h.getLevel().addFreshEntity(bolt);
        h.succeedWhen(() -> {
            h.assertTrue(bolt.isRemoved(), "the lance faded; travelled " + bolt.travelled());
            h.assertTrue(bolt.travelled() >= 3 && bolt.travelled() < 3 + WeaponsConfig.BOLT_SPEED.get() + 0.01, "it went its range and no further: " + bolt.travelled());
        });
    }

    @GameTest(template = EMPTY, batch = "firing")
    public void piercing_skips_a_share_of_the_armour_and_region_guard_is_asked_only_when_present(GameTestHelper h) {
        // 10 damage against armour 20: vanilla's formula takes most of it; 50 % piercing lets half through whole
        float none = Damage.amount(10, 0, 20, 0), half = Damage.amount(10, 50, 20, 0), all = Damage.amount(10, 100, 20, 0);
        h.assertTrue(none < half && half < all, "more piercing, more damage: " + none + " < " + half + " < " + all);
        h.assertTrue(Math.abs(all - 10) < 1e-5, "100 % piercing is the whole hit");
        h.assertTrue(Math.abs(half - (5 + Damage.amount(5, 0, 20, 0))) < 1e-5, "50 % is half whole and half through the armour");
        h.assertTrue(Math.abs(Damage.amount(10, 0, 0, 0) - 10) < 1e-5, "no armour, no loss");
        h.assertTrue(Damage.amount(10, 150, 20, 0) == all, "piercing never goes above 100 %");
        // the adapter stands aside without the mod (the GameTest server runs without Region Guard unless -PwithBsp)
        ServerPlayer p = shooter(h, new BlockPos(2, 2, 2));
        boolean allowed = RegionGuard.pvpAllowed(h.getLevel(), p.blockPosition(), p.blockPosition().east(5), p);
        h.assertTrue(RegionGuard.isLoaded() || allowed, "without Region Guard every shot is allowed");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "firing")
    public void a_weapon_takes_only_durability_enchantments(GameTestHelper h) {
        ItemStack gun = pistol(3);
        WeaponItem item = (WeaponItem) gun.getItem();
        h.assertTrue(item.isEnchantable(gun) && item.canApplyAtEnchantingTable(gun, net.minecraft.world.item.enchantment.Enchantments.UNBREAKING) && item.canApplyAtEnchantingTable(gun, net.minecraft.world.item.enchantment.Enchantments.MENDING), "Unbreaking and Mending apply (D31)");
        h.assertTrue(!item.canApplyAtEnchantingTable(gun, net.minecraft.world.item.enchantment.Enchantments.POWER_ARROWS) && !item.canApplyAtEnchantingTable(gun, net.minecraft.world.item.enchantment.Enchantments.SHARPNESS), "bow and sword enchantments do not");
        h.assertTrue(!item.isEnchantable(new ItemStack(item)), "a weapon with no spec is not enchantable");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "firing")
    public void audit_stamps_from_another_servers_clock_are_pulled_back_to_now(GameTestHelper h) {
        ItemStack gun = pistol(2);
        ServerPlayer p = shooter(h, new BlockPos(2, 2, 14));
        long now = h.getLevel().getGameTime();
        CompoundTag t = gun.getOrCreateTag();
        // a weapon that arrived from a server millions of ticks ahead
        t.putLong(Firing.LAST_SHOT, now + 5_000_000L);
        t.putLong(Firing.LOCKED_UNTIL, now + 5_000_000L);
        t.putLong(Firing.HEAT_AT, now + 5_000_000L);
        t.putDouble(Firing.HEAT, 1);
        Firing.Refusal r = Firing.check(gun, now, p);
        h.assertTrue(r == Firing.Refusal.LOCKED, "it is locked, not stuck on a cooldown from the future: " + r);
        h.assertTrue(t.getLong(Firing.LOCKED_UNTIL) <= now + WeaponsConfig.OVERHEAT_LOCK_TICKS.get(), "and the lock ends within its own window");
        h.assertTrue(t.getLong(Firing.LAST_SHOT) == now && t.getLong(Firing.HEAT_AT) == now, "the other stamps are now");
        // a weapon worn to its last point never breaks from overheating and refuses to fire
        ItemStack worn = pistol(1);
        worn.setDamageValue(worn.getMaxDamage() - 1);
        h.assertTrue(Firing.check(worn, now, p) == Firing.Refusal.BROKEN, "a worn-out weapon is BROKEN, not destroyed");
        h.succeed();
    }
}
