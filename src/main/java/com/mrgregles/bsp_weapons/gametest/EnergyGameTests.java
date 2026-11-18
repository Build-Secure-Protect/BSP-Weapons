package com.mrgregles.bsp_weapons.gametest;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.WeaponsConfig;
import com.mrgregles.bsp_weapons.parts.PartKind;
import com.mrgregles.bsp_weapons.parts.PartSpec;
import com.mrgregles.bsp_weapons.parts.Rarity;
import com.mrgregles.bsp_weapons.parts.Sheet;
import com.mrgregles.bsp_weapons.parts.Stat;
import com.mrgregles.bsp_weapons.parts.WeaponSpec;
import com.mrgregles.bsp_weapons.registry.ModItems;
import com.mrgregles.bsp_weapons.weapon.Firing;
import com.mrgregles.bsp_weapons.weapon.WeaponEnergy;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Energy cells (WP-5, D10): the weapon's Forge Energy store, what fills it, what a shot costs, and that magazines have none. */
@GameTestHolder(BspWeapons.MODID)
@PrefixGameTestTemplate(false)
public class EnergyGameTests {
    private static final String EMPTY = SetupGameTests.EMPTY;

    private static ResourceLocation id(String path) {
        return new ResourceLocation(BspWeapons.MODID, path);
    }

    private static ItemStack pistol(String cell) {
        WeaponSpec spec = new WeaponSpec(id("laser_pistol"), 2, List.of(
                new PartSpec(PartKind.BARREL, id("focused_barrel"), id("tetrium"), Rarity.NORMAL),
                new PartSpec(PartKind.CELL, id(cell), id("steel"), Rarity.NORMAL),
                new PartSpec(PartKind.GRIP, id("standard_grip"), id("magnatite"), Rarity.NORMAL),
                new PartSpec(PartKind.OPTIC, id("iron_sight"), id("tetrium"), Rarity.NORMAL)), Rarity.NORMAL, List.of());
        return ModItems.weapon(id("laser_pistol")).of(spec);
    }

    @GameTest(template = EMPTY, batch = "energy")
    public void an_energy_cell_weapon_offers_a_forge_energy_store_sized_by_its_capacity(GameTestHelper h) {
        ItemStack gun = pistol("energy_cell");
        Sheet sheet = Firing.sheetOf(gun);
        int capacity = (int) Math.round(sheet.get(Stat.CAPACITY)) * WeaponsConfig.ENERGY_PER_SHOT.get();
        IEnergyStorage e = gun.getCapability(ForgeCapabilities.ENERGY).orElse(null);
        h.assertTrue(e != null, "the weapon has an energy capability");
        h.assertTrue(e.getMaxEnergyStored() == capacity, "the store is Capacity shots' worth: " + e.getMaxEnergyStored());
        h.assertTrue(e.getEnergyStored() == capacity, "it leaves the assembler full (config)");
        h.assertTrue(!e.canExtract() && e.extractEnergy(1000, false) == 0, "nothing comes out");
        h.assertTrue(Firing.shots(gun) == Math.round(sheet.get(Stat.CAPACITY)), "shots left reads from the energy");
        // a magazine has no energy store at all
        ItemStack mag = pistol("magazine");
        h.assertTrue(!mag.getCapability(ForgeCapabilities.ENERGY).isPresent(), "a magazine weapon offers no energy capability");
        h.assertTrue(!WeaponEnergy.holdsEnergy(mag) && WeaponEnergy.holdsEnergy(gun), "the cell type decides (D10)");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "energy")
    public void charging_is_limited_to_a_full_store_per_recharge_time_and_stops_at_the_top(GameTestHelper h) {
        ItemStack gun = pistol("energy_cell");
        WeaponEnergy.setStored(gun, 0);
        IEnergyStorage e = gun.getCapability(ForgeCapabilities.ENERGY).orElse(null);
        int perTick = WeaponEnergy.maxReceive(gun);
        Sheet sheet = Firing.sheetOf(gun);
        h.assertTrue(perTick == (int) Math.ceil(WeaponEnergy.capacity(gun) / Math.max(1.0, sheet.get(Stat.RECHARGE))), "the rate is the store over the Recharge ticks: " + perTick);
        h.assertTrue(e.receiveEnergy(1_000_000, true) == perTick, "a simulated big push is cut to the rate");
        h.assertTrue(e.getEnergyStored() == 0, "a simulation stores nothing");
        int total = 0;
        for (int i = 0; i < 10_000 && total < WeaponEnergy.capacity(gun); i++) total += e.receiveEnergy(perTick, false);
        h.assertTrue(e.getEnergyStored() == WeaponEnergy.capacity(gun), "filled to the top and no further");
        h.assertTrue(e.receiveEnergy(perTick, false) == 0, "a full store takes nothing");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "energy")
    public void a_shot_costs_energy_and_an_empty_store_refuses_without_a_free_reload(GameTestHelper h) {
        ItemStack gun = pistol("energy_cell");
        ServerPlayer p = TestPlayers.mockServerPlayer(h);
        Vec3 pos = h.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(2, 2, 2)));
        p.moveTo(pos.x, pos.y, pos.z, -90f, 0f);
        int before = WeaponEnergy.stored(gun);
        h.assertTrue(Firing.tryFire(h.getLevel(), p, gun) == Firing.Refusal.NONE, "fired");
        h.assertTrue(WeaponEnergy.stored(gun) == before - WeaponEnergy.perShot(), "one shot's energy spent: " + WeaponEnergy.stored(gun));
        WeaponEnergy.setStored(gun, WeaponEnergy.perShot() - 1);
        gun.getOrCreateTag().putLong(Firing.LAST_SHOT, 0);
        h.assertTrue(Firing.tryFire(h.getLevel(), p, gun) == Firing.Refusal.EMPTY, "under a shot's worth refuses");
        h.assertTrue(!Firing.reloading(gun, h.getLevel().getGameTime()), "and starts no free reload: it must be charged");
        gun.getCapability(ForgeCapabilities.ENERGY).ifPresent(e -> { for (int i = 0; i < 1000 && e.getEnergyStored() < WeaponEnergy.perShot(); i++) e.receiveEnergy(WeaponEnergy.maxReceive(gun), false); });
        h.assertTrue(Firing.tryFire(h.getLevel(), p, gun) == Firing.Refusal.NONE, "charged, it fires again");
        h.getLevel().getEntitiesOfClass(com.mrgregles.bsp_weapons.weapon.LaserBoltEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(0, 0, 0)), h.absolutePos(new BlockPos(24, 10, 24)))).forEach(b -> { if (b.getOwner() == p) b.discard(); });
        h.succeed();
    }
}
