package com.mrgregles.bsp_weapons.gametest;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.parts.PartKind;
import com.mrgregles.bsp_weapons.parts.PartSpec;
import com.mrgregles.bsp_weapons.parts.Rarity;
import com.mrgregles.bsp_weapons.parts.Sheet;
import com.mrgregles.bsp_weapons.parts.Stat;
import com.mrgregles.bsp_weapons.parts.WeaponSpec;
import com.mrgregles.bsp_weapons.registry.ModItems;
import com.mrgregles.bsp_weapons.weapon.Firing;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Magazines and rounds (WP-5b; D68 to D70): the reload takes rounds from the inventory, and nothing happens without them. */
@GameTestHolder(BspWeapons.MODID)
@PrefixGameTestTemplate(false)
public class RoundsGameTests {
    private static final String EMPTY = SetupGameTests.EMPTY;

    private static ResourceLocation id(String path) {
        return new ResourceLocation(BspWeapons.MODID, path);
    }

    private static ItemStack magazinePistol() {
        WeaponSpec spec = new WeaponSpec(id("laser_pistol"), 2, List.of(
                new PartSpec(PartKind.BARREL, id("focused_barrel"), id("tetrium"), Rarity.NORMAL),
                new PartSpec(PartKind.CELL, id("magazine"), id("steel"), Rarity.NORMAL),
                new PartSpec(PartKind.GRIP, id("standard_grip"), id("magnatite"), Rarity.NORMAL),
                new PartSpec(PartKind.OPTIC, id("iron_sight"), id("tetrium"), Rarity.NORMAL)), Rarity.NORMAL, List.of());
        return ModItems.weapon(id("laser_pistol")).of(spec);
    }

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer p = TestPlayers.mockServerPlayer(h);
        Vec3 pos = h.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(2, 2, 2)));
        p.moveTo(pos.x, pos.y, pos.z, -90f, 0f);
        return p;
    }

    @GameTest(template = EMPTY, batch = "rounds")
    public void an_empty_magazine_reloads_from_the_inventory_and_takes_what_there_is(GameTestHelper h) {
        ItemStack gun = magazinePistol();
        Sheet sheet = Firing.sheetOf(gun);
        int capacity = (int) Math.round(sheet.get(Stat.CAPACITY));
        ServerPlayer p = player(h);
        p.getInventory().add(new ItemStack(ModItems.ENERGY_ROUND.get(), 64));
        CompoundTag t = gun.getOrCreateTag();
        t.putInt(Firing.CHARGE, 0);
        long now = h.getLevel().getGameTime();
        h.assertTrue(Firing.check(gun, now, p) == Firing.Refusal.EMPTY && Firing.reloading(gun, now), "empty with rounds carried: the reload starts");
        t.putLong(Firing.RELOAD_UNTIL, now - 1);
        Firing.settle(gun, now, p);
        h.assertTrue(Firing.charge(gun) == capacity, "full after the reload: " + Firing.charge(gun));
        h.assertTrue(Firing.roundsCarried(p) == 64 - capacity, "Capacity rounds left the inventory: " + Firing.roundsCarried(p));
        // fewer rounds than the capacity: it takes what there is
        p.getInventory().clearContent();
        p.getInventory().add(new ItemStack(ModItems.ENERGY_ROUND.get(), 3));
        t.putInt(Firing.CHARGE, 0);
        t.putLong(Firing.RELOAD_UNTIL, now - 1);
        Firing.settle(gun, now, p);
        h.assertTrue(Firing.charge(gun) == 3 && Firing.roundsCarried(p) == 0, "three rounds loaded, none left");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "rounds")
    public void with_no_rounds_an_empty_magazine_neither_reloads_nor_fires_and_energy_weapons_ignore_rounds(GameTestHelper h) {
        ItemStack gun = magazinePistol();
        ServerPlayer p = player(h);
        p.getInventory().clearContent();
        gun.getOrCreateTag().putInt(Firing.CHARGE, 0);
        long now = h.getLevel().getGameTime();
        h.assertTrue(Firing.check(gun, now, p) == Firing.Refusal.EMPTY && !Firing.reloading(gun, now), "no rounds: no reload starts");
        h.assertTrue(Firing.tryFire(h.getLevel(), p, gun) == Firing.Refusal.EMPTY, "and no shot");
        // rounds arrive: the next attempt starts the reload
        p.getInventory().add(new ItemStack(ModItems.ENERGY_ROUND.get(), 10));
        h.assertTrue(Firing.check(gun, now, p) == Firing.Refusal.EMPTY && Firing.reloading(gun, now), "rounds in hand: the reload starts");
        // a shooter that is not a player (a turret later) reloads for free
        ItemStack other = magazinePistol();
        other.getOrCreateTag().putInt(Firing.CHARGE, 0);
        other.getOrCreateTag().putLong(Firing.RELOAD_UNTIL, now - 1);
        Firing.settle(other, now, null);
        h.assertTrue(Firing.charge(other) > 0, "no holder: the magazine refills");
        h.assertTrue(ModItems.ENERGY_ROUND.get().getMaxStackSize() == 64, "rounds stack to 64");
        h.succeed();
    }
}
