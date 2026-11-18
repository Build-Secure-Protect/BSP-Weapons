package com.mrgregles.bsp_weapons.gametest;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.WeaponsConfig;
import com.mrgregles.bsp_weapons.enhancer.Enhancing;
import com.mrgregles.bsp_weapons.enhancer.MachineEnergy;
import com.mrgregles.bsp_weapons.enhancer.WeaponEnhancerBlock;
import com.mrgregles.bsp_weapons.enhancer.WeaponEnhancerBlockEntity;
import com.mrgregles.bsp_weapons.enhancer.WeaponEnhancerTopBlock;
import com.mrgregles.bsp_weapons.item.AugmentChipItem;
import com.mrgregles.bsp_weapons.item.CoreItem;
import com.mrgregles.bsp_weapons.item.WeaponItem;
import com.mrgregles.bsp_weapons.network.EnhancerActionPacket;
import com.mrgregles.bsp_weapons.parts.FittedChip;
import com.mrgregles.bsp_weapons.parts.PartKind;
import com.mrgregles.bsp_weapons.parts.PartSpec;
import com.mrgregles.bsp_weapons.parts.Rarity;
import com.mrgregles.bsp_weapons.parts.WeaponSpec;
import com.mrgregles.bsp_weapons.registry.ModBlocks;
import com.mrgregles.bsp_weapons.registry.ModItems;
import com.mrgregles.bsp_weapons.weapon.WeaponEnergy;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;

/** The Weapon Enhancer's block and machine (WP-6c; D75, D84, D88; R15, R21, R23): the two-space block, the slots, the charge, FIT, REMOVE and swap through the inventory. */
@GameTestHolder(BspWeapons.MODID)
@PrefixGameTestTemplate(false)
public class EnhancerGameTests {
    private static final String EMPTY = SetupGameTests.EMPTY;

    private static ResourceLocation id(String path) {
        return new ResourceLocation(BspWeapons.MODID, path);
    }

    private static String key(Component c) {
        return c.getContents() instanceof TranslatableContents t ? t.getKey() : c.getString();
    }

    /** A Tier {@code tier} Laser Pistol item with chips: two sockets at Tier III. */
    private static ItemStack pistol(int tier, List<FittedChip> chips) {
        return ModItems.weapon(id("laser_pistol")).of(new WeaponSpec(id("laser_pistol"), tier, List.of(
                new PartSpec(PartKind.BARREL, id("focused_barrel"), id("tetrium"), Rarity.NORMAL),
                new PartSpec(PartKind.CELL, id("energy_cell"), id("steel"), Rarity.NORMAL),
                new PartSpec(PartKind.GRIP, id("standard_grip"), id("magnatite"), Rarity.NORMAL),
                new PartSpec(PartKind.OPTIC, id("iron_sight"), id("tetrium"), Rarity.NORMAL)), Rarity.NORMAL, chips));
    }

    private static ItemStackHandler inv(ItemStack weapon, ItemStack chipIn) {
        ItemStackHandler inv = new ItemStackHandler(Enhancing.SLOTS);
        inv.setStackInSlot(Enhancing.WEAPON, weapon);
        inv.setStackInSlot(Enhancing.CHIP_IN, chipIn);
        return inv;
    }

    private static MachineEnergy charged() {
        return new MachineEnergy(20000, 1000, 20000);
    }

    @GameTest(template = EMPTY, batch = "enhancer")
    public void placing_the_enhancer_puts_its_companion_above_and_breaking_either_drops_one(GameTestHelper h) {
        BlockPos lower = new BlockPos(5, 2, 5);
        WeaponEnhancerBlock.placeMachine(h.getLevel(), h.absolutePos(lower), Direction.WEST);
        BlockPos upper = WeaponEnhancerBlock.companionPos(lower);
        h.assertTrue(upper.equals(lower.above()), "the companion stands above");
        h.assertBlockPresent(ModBlocks.WEAPON_ENHANCER_TOP.get(), upper);
        h.assertTrue(h.getBlockState(upper).getValue(HorizontalDirectionalBlock.FACING) == Direction.WEST, "the companion faces the same way");
        h.assertTrue(WeaponEnhancerTopBlock.masterPos(upper).equals(lower), "the companion knows its lower block");
        h.assertTrue(h.getBlockEntity(lower) instanceof WeaponEnhancerBlockEntity, "the lower block has the block entity");
        // a lower block cannot stand under something that is not its companion
        h.setBlock(new BlockPos(9, 3, 5), Blocks.STONE);
        h.setBlock(new BlockPos(9, 2, 5), ModBlocks.WEAPON_ENHANCER.get().defaultBlockState());
        h.assertTrue(!h.getBlockState(new BlockPos(9, 2, 5)).canSurvive(h.getLevel(), h.absolutePos(new BlockPos(9, 2, 5))), "no room above means it cannot survive");
        h.setBlock(new BlockPos(9, 2, 5), Blocks.AIR);
        h.setBlock(new BlockPos(9, 3, 5), Blocks.AIR);
        // breaking the companion takes the lower block with it and drops one machine
        h.getLevel().destroyBlock(h.absolutePos(upper), true);
        h.assertBlockPresent(Blocks.AIR, lower);
        h.assertBlockPresent(Blocks.AIR, upper);
        h.runAfterDelay(2, () -> {
            List<ItemEntity> drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(lower)).inflate(3));
            int machines = 0;
            for (ItemEntity e : drops) if (e.getItem().is(ModItems.WEAPON_ENHANCER.get())) machines += e.getItem().getCount();
            h.assertTrue(machines == 1, "one Weapon Enhancer dropped, got " + machines);
            drops.forEach(ItemEntity::discard);
            BlockPos l2 = new BlockPos(12, 2, 5);
            WeaponEnhancerBlock.placeMachine(h.getLevel(), h.absolutePos(l2), Direction.NORTH);
            h.getLevel().destroyBlock(h.absolutePos(l2), false);
            h.assertBlockPresent(Blocks.AIR, l2.above());
            h.succeed();
        });
    }

    @GameTest(template = EMPTY, batch = "enhancer")
    public void the_slots_take_a_weapon_a_chip_a_core_and_an_ingot_or_gem(GameTestHelper h) {
        ItemStack gun = pistol(3, List.of()), chip = ModItems.AUGMENT_CHIP.get().of(id("coolant"), Rarity.NORMAL), core = ModItems.WEAPON_CORE.get().of(4);
        h.assertTrue(Enhancing.fits(Enhancing.WEAPON, gun) && !Enhancing.fits(Enhancing.WEAPON, chip), "WEAPON takes a weapon and not a chip");
        h.assertTrue(Enhancing.fits(Enhancing.CHIP_IN, chip) && !Enhancing.fits(Enhancing.CHIP_IN, core), "CHIP IN takes a chip and not a core");
        h.assertTrue(Enhancing.fits(Enhancing.CORE, core) && !Enhancing.fits(Enhancing.CORE, gun), "CORE takes a core and not a weapon");
        h.assertTrue(Enhancing.fits(Enhancing.REPAIR, new ItemStack(Items.IRON_INGOT)) && Enhancing.fits(Enhancing.REPAIR, new ItemStack(Items.DIAMOND)), "REPAIR takes an ingot or a gem");
        h.assertTrue(!Enhancing.fits(Enhancing.REPAIR, new ItemStack(Items.STICK)) && !Enhancing.fits(Enhancing.REPAIR, chip), "and nothing else");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "enhancer")
    public void fit_through_the_machine_spends_the_chip_and_the_charge_and_keeps_the_weapons_own_tags(GameTestHelper h) {
        ItemStack gun = pistol(3, List.of());
        WeaponEnergy.setStored(gun, 1234);
        ItemStackHandler inv = inv(gun, ModItems.AUGMENT_CHIP.get().of(id("piercing"), Rarity.RARE));
        inv.getStackInSlot(Enhancing.CHIP_IN).setCount(2);
        MachineEnergy empty = new MachineEnergy(20000, 1000, 100);
        Component why = Enhancing.fit(inv, empty);
        h.assertTrue(why != null && key(why).equals("enhance.bsp_weapons.no_power"), "no charge, no fit: " + why);
        h.assertTrue(WeaponItem.specOf(inv.getStackInSlot(Enhancing.WEAPON)).augments().isEmpty() && inv.getStackInSlot(Enhancing.CHIP_IN).getCount() == 2, "nothing moved");
        MachineEnergy energy = charged();
        why = Enhancing.fit(inv, energy);
        h.assertTrue(why == null, "fitted: " + why);
        WeaponSpec spec = WeaponItem.specOf(inv.getStackInSlot(Enhancing.WEAPON));
        h.assertTrue(spec.augments().equals(List.of(new FittedChip(id("piercing"), Rarity.RARE, 9))), "the chip is on the weapon a point down: " + spec.augments());
        h.assertTrue(inv.getStackInSlot(Enhancing.CHIP_IN).getCount() == 1, "one chip was taken from the stack");
        h.assertTrue(energy.getEnergyStored() == 20000 - WeaponsConfig.FIT_CHARGE.get(), "the fit charge was burnt: " + energy.getEnergyStored());
        h.assertTrue(WeaponEnergy.stored(inv.getStackInSlot(Enhancing.WEAPON)) == 1234, "the weapon keeps its own charge");
        h.assertTrue(inv.getStackInSlot(Enhancing.WEAPON).getCount() == 1 && inv.getStackInSlot(Enhancing.WEAPON).getItem() == gun.getItem(), "the same weapon item");
        why = Enhancing.fit(inv(pistol(3, List.of()), ItemStack.EMPTY), charged());
        h.assertTrue(why != null && key(why).equals("enhance.bsp_weapons.no_chip_in"), "an empty CHIP IN is named: " + why);
        why = Enhancing.fit(inv(ItemStack.EMPTY, ModItems.AUGMENT_CHIP.get().of(id("piercing"), Rarity.RARE)), charged());
        h.assertTrue(why != null && key(why).equals("enhance.bsp_weapons.no_weapon"), "an empty WEAPON slot is named: " + why);
        why = Enhancing.fit(inv(pistol(3, List.of()), ModItems.AUGMENT_CHIP.get().of(id("breacher"), Rarity.NORMAL)), charged());
        h.assertTrue(why != null && key(why).equals("enhance.bsp_weapons.not_yet"), "the rules' refusals come through: " + why);
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "enhancer")
    public void remove_puts_the_chip_in_chip_in_for_nothing_and_swap_exchanges_them_for_the_fit_charge(GameTestHelper h) {
        ItemStackHandler inv = inv(pistol(5, List.of(new FittedChip(id("piercing"), Rarity.RARE, 9), new FittedChip(id("overcharge"), Rarity.NORMAL, 7))), ItemStack.EMPTY);
        MachineEnergy energy = new MachineEnergy(20000, 1000, 0);
        Component why = Enhancing.remove(inv, energy, 1);
        h.assertTrue(why == null, "removed with no charge at all (D88): " + why);
        h.assertTrue(AugmentChipItem.chipOf(inv.getStackInSlot(Enhancing.CHIP_IN)).equals(new FittedChip(id("overcharge"), Rarity.NORMAL, 7)), "Overcharge is in CHIP IN with its 7 points");
        h.assertTrue(WeaponItem.specOf(inv.getStackInSlot(Enhancing.WEAPON)).augments().size() == 1, "and off the weapon");
        why = Enhancing.remove(inv, energy, 0);
        h.assertTrue(why != null && key(why).equals("enhance.bsp_weapons.chip_in_full"), "a second removal waits for CHIP IN to be emptied: " + why);
        // a swap: Coolant for the Piercing in socket 1
        inv.setStackInSlot(Enhancing.CHIP_IN, ModItems.AUGMENT_CHIP.get().of(id("coolant"), Rarity.EPIC));
        why = Enhancing.swap(inv, energy, 0);
        h.assertTrue(why != null && key(why).equals("enhance.bsp_weapons.no_power"), "a swap needs the fit charge: " + why);
        energy = charged();
        inv.getStackInSlot(Enhancing.CHIP_IN).setCount(2);
        why = Enhancing.swap(inv, energy, 0);
        h.assertTrue(why != null && key(why).equals("enhance.bsp_weapons.one_at_a_time"), "a swap takes one chip at a time: " + why);
        inv.getStackInSlot(Enhancing.CHIP_IN).setCount(1);
        why = Enhancing.swap(inv, energy, 0);
        h.assertTrue(why == null, "swapped: " + why);
        h.assertTrue(WeaponItem.specOf(inv.getStackInSlot(Enhancing.WEAPON)).augments().equals(List.of(new FittedChip(id("coolant"), Rarity.EPIC, 9))), "Coolant took the socket a point down");
        h.assertTrue(AugmentChipItem.chipOf(inv.getStackInSlot(Enhancing.CHIP_IN)).equals(new FittedChip(id("piercing"), Rarity.RARE, 9)), "Piercing came out whole");
        h.assertTrue(energy.getEnergyStored() == 20000 - WeaponsConfig.FIT_CHARGE.get(), "the fit charge was burnt");
        h.assertTrue(key(Enhancing.run(7, inv, energy, 0).message()).equals("enhance.bsp_weapons.no_such_operation"), "an unknown operation number is refused");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "enhancer")
    public void the_machine_takes_forge_energy_from_any_side_gives_none_out_and_keeps_it_through_a_save(GameTestHelper h) {
        BlockPos lower = new BlockPos(5, 2, 5);
        WeaponEnhancerBlock.placeMachine(h.getLevel(), h.absolutePos(lower), Direction.SOUTH);
        WeaponEnhancerBlockEntity be = (WeaponEnhancerBlockEntity) h.getBlockEntity(lower);
        IEnergyStorage top = be.getCapability(ForgeCapabilities.ENERGY, Direction.UP).orElse(null), side = be.getCapability(ForgeCapabilities.ENERGY, Direction.WEST).orElse(null);
        h.assertTrue(top != null && side != null && top.canReceive() && !top.canExtract(), "the energy capability is on every side, intake only");
        int taken = side.receiveEnergy(5000, false);
        h.assertTrue(taken == WeaponsConfig.ENHANCER_MAX_RECEIVE.get(), "one push takes at most the intake a tick: " + taken);
        h.assertTrue(side.extractEnergy(100, false) == 0, "nothing comes out");
        h.assertTrue(be.energy().getMaxEnergyStored() == WeaponsConfig.ENHANCER_CAPACITY.get(), "the capacity is the config's");
        be.energy().set(be.energy().getMaxEnergyStored());
        h.assertTrue(top.receiveEnergy(100, false) == 0, "a full store takes nothing");
        be.energy().spend(700);
        net.minecraft.nbt.CompoundTag saved = be.saveWithoutMetadata();
        WeaponEnhancerBlockEntity loaded = new WeaponEnhancerBlockEntity(be.getBlockPos(), be.getBlockState());
        loaded.load(saved);
        h.assertTrue(loaded.energy().getEnergyStored() == be.energy().getEnergyStored() && loaded.energy().getEnergyStored() == WeaponsConfig.ENHANCER_CAPACITY.get() - 700, "the charge survives the save: " + loaded.energy().getEnergyStored());
        h.assertTrue(be.getUpdateTag().getInt("Energy") == be.energy().getEnergyStored(), "clients hear the charge");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "enhancer")
    public void the_checks_the_screen_shows_name_the_same_reasons_as_the_moves_and_move_nothing(GameTestHelper h) {
        ItemStackHandler inv = inv(pistol(3, List.of(new FittedChip(id("piercing"), Rarity.RARE, 9))), ModItems.AUGMENT_CHIP.get().of(id("piercing"), Rarity.NORMAL));
        MachineEnergy energy = charged();
        h.assertTrue(key(Enhancing.cannotFit(inv, energy)).equals("enhance.bsp_weapons.already_fitted"), "FIT's check names the doubled augment");
        h.assertTrue(Enhancing.cannotSwap(inv, energy, 0) == null, "a swap into the Piercing socket is fine (the same augment, a different roll)");
        h.assertTrue(key(Enhancing.cannotRemove(inv, 0)).equals("enhance.bsp_weapons.chip_in_full"), "REMOVE waits for CHIP IN");
        h.assertTrue(key(Enhancing.cannotSwap(inv, energy, 1)).equals("enhance.bsp_weapons.no_chip"), "a swap needs a chip in the socket");
        h.assertTrue(key(Enhancing.cannotSwap(inv, new MachineEnergy(20000, 1000, 0), 0)).equals("enhance.bsp_weapons.no_power"), "and the charge");
        h.assertTrue(WeaponItem.specOf(inv.getStackInSlot(Enhancing.WEAPON)).augments().get(0).durability() == 9 && inv.getStackInSlot(Enhancing.CHIP_IN).getCount() == 1 && energy.getEnergyStored() == 20000, "the checks moved nothing");
        inv.setStackInSlot(Enhancing.CHIP_IN, ItemStack.EMPTY);
        h.assertTrue(Enhancing.cannotRemove(inv, 0) == null && key(Enhancing.cannotRemove(inv, 1)).equals("enhance.bsp_weapons.no_chip"), "REMOVE is fine for socket 1 and not for an empty socket");
        h.assertTrue(key(Enhancing.cannotFit(inv, energy)).equals("enhance.bsp_weapons.no_chip_in"), "FIT wants a chip");
        h.assertTrue(Enhancing.fitCost() == WeaponsConfig.FIT_CHARGE.get(), "the fit cost is the config's");
        h.succeed();
    }

    /** A Tier {@code tier} pistol with an iron grip, so an iron ingot repairs it. */
    private static ItemStack ironPistol(int tier, List<FittedChip> chips) {
        return ModItems.weapon(id("laser_pistol")).of(new WeaponSpec(id("laser_pistol"), tier, List.of(
                new PartSpec(PartKind.BARREL, id("focused_barrel"), id("tetrium"), Rarity.NORMAL),
                new PartSpec(PartKind.CELL, id("energy_cell"), id("steel"), Rarity.NORMAL),
                new PartSpec(PartKind.GRIP, id("standard_grip"), id("iron"), Rarity.NORMAL),
                new PartSpec(PartKind.OPTIC, id("iron_sight"), id("tetrium"), Rarity.NORMAL)), Rarity.NORMAL, chips));
    }

    @GameTest(template = EMPTY, batch = "enhancer")
    public void upgrade_takes_the_higher_core_returns_the_old_one_and_wears_every_chip(GameTestHelper h) {
        ItemStackHandler inv = inv(pistol(3, List.of(new FittedChip(id("piercing"), Rarity.RARE, 9), new FittedChip(id("overcharge"), Rarity.NORMAL, 1))), ItemStack.EMPTY); // Tier III: two sockets
        h.assertTrue(key(Enhancing.cannotUpgrade(inv, charged())).equals("enhance.bsp_weapons.no_core"), "an empty CORE slot is named");
        inv.setStackInSlot(Enhancing.CORE, ModItems.WEAPON_CORE.get().of(3));
        h.assertTrue(key(Enhancing.cannotUpgrade(inv, charged())).equals("enhance.bsp_weapons.core_not_higher"), "a Core of the same tier is refused");
        ItemStack two = ModItems.WEAPON_CORE.get().of(5); two.setCount(2);
        inv.setStackInSlot(Enhancing.CORE, two);
        h.assertTrue(key(Enhancing.cannotUpgrade(inv, charged())).equals("enhance.bsp_weapons.one_core_at_a_time"), "two Cores at once are refused");
        inv.setStackInSlot(Enhancing.CORE, ModItems.WEAPON_CORE.get().of(5));
        h.assertTrue(key(Enhancing.cannotUpgrade(inv, new MachineEnergy(20000, 1000, 10))).equals("enhance.bsp_weapons.no_power"), "the upgrade charge is needed");
        MachineEnergy energy = charged();
        Enhancing.Report r = Enhancing.upgrade(inv, energy);
        h.assertTrue(r.ok(), "upgraded: " + r.message());
        WeaponSpec spec = WeaponItem.specOf(inv.getStackInSlot(Enhancing.WEAPON));
        h.assertTrue(spec.coreTier() == 5, "the weapon is Tier V: " + spec.coreTier());
        h.assertTrue(spec.augments().equals(List.of(new FittedChip(id("piercing"), Rarity.RARE, 8))), "Piercing wore a point and Overcharge burnt out: " + spec.augments());
        h.assertTrue(r.message() != null && key(r.message()).equals("enhance.bsp_weapons.burnt_out"), "the notice names the burnt chip: " + r.message());
        h.assertTrue(CoreItem.tierOf(inv.getStackInSlot(Enhancing.CORE)) == 3 && inv.getStackInSlot(Enhancing.CORE).getCount() == 1, "the old Tier III Core is in the CORE slot");
        h.assertTrue(energy.getEnergyStored() == 20000 - WeaponsConfig.UPGRADE_CHARGE.get(), "the upgrade charge was burnt");
        h.assertTrue(com.mrgregles.bsp_weapons.parts.SheetCalculator.augmentSlots(spec) == WeaponsConfig.tierAugmentSlots(5), "the weapon has its new tier's sockets");
        h.assertTrue(Enhancing.upgrade(inv, energy).message() != null && key(Enhancing.cannotUpgrade(inv, energy)).equals("enhance.bsp_weapons.core_not_higher"), "the returned Core cannot upgrade it again");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "enhancer")
    public void repair_takes_the_ingots_it_needs_of_a_fitted_material_and_wears_every_chip(GameTestHelper h) {
        ItemStack gun = ironPistol(3, List.of(new FittedChip(id("coolant"), Rarity.NORMAL, 5)));
        int max = gun.getMaxDamage(), step = Enhancing.repairStep(gun);
        h.assertTrue(max > 4 && step == Math.max(1, (int) Math.round(max * WeaponsConfig.REPAIR_SHARE.get())), "one ingot restores the config's share: " + step + " of " + max);
        ItemStackHandler inv = inv(gun, ItemStack.EMPTY);
        h.assertTrue(key(Enhancing.cannotRepair(inv, charged())).equals("enhance.bsp_weapons.no_ingot"), "an empty REPAIR slot is named");
        inv.setStackInSlot(Enhancing.REPAIR, new ItemStack(Items.COPPER_INGOT, 3));
        h.assertTrue(key(Enhancing.cannotRepair(inv, charged())).equals("enhance.bsp_weapons.wrong_material"), "copper is on no part of this pistol");
        inv.setStackInSlot(Enhancing.REPAIR, new ItemStack(Items.IRON_INGOT, 2));
        h.assertTrue(key(Enhancing.cannotRepair(inv, charged())).equals("enhance.bsp_weapons.nothing_to_repair"), "an unworn weapon has nothing to repair");
        gun.setDamageValue(3 * step - 10);
        h.assertTrue(key(Enhancing.cannotRepair(inv, new MachineEnergy(20000, 1000, 10))).equals("enhance.bsp_weapons.no_power"), "the repair charge is needed");
        MachineEnergy energy = charged();
        Enhancing.Report r = Enhancing.repair(inv, energy);
        h.assertTrue(r.ok() && r.message() == null, "repaired, nothing burnt: " + r.message());
        h.assertTrue(gun.getDamageValue() == step - 10, "two ingots restored two steps, leaving " + gun.getDamageValue() + " of wear");
        h.assertTrue(inv.getStackInSlot(Enhancing.REPAIR).isEmpty(), "both ingots were used");
        h.assertTrue(WeaponItem.specOf(gun).augments().get(0).durability() == 4, "the Coolant chip wore a point");
        h.assertTrue(energy.getEnergyStored() == 20000 - WeaponsConfig.REPAIR_CHARGE.get(), "the repair charge was burnt");
        inv.setStackInSlot(Enhancing.REPAIR, new ItemStack(Items.IRON_INGOT, 5));
        h.assertTrue(Enhancing.repair(inv, energy).ok() && gun.getDamageValue() == 0 && inv.getStackInSlot(Enhancing.REPAIR).getCount() == 4, "the last step takes one ingot and stops at full");
        h.assertTrue(Enhancing.run(Enhancing.OP_REPAIR, inv, energy, 0).message() != null, "run() carries a repair's refusal");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "enhancer")
    public void the_menu_reads_the_charge_live_on_the_server_and_keeps_what_it_is_sent_elsewhere(GameTestHelper h) {
        BlockPos lower = new BlockPos(5, 2, 5);
        WeaponEnhancerBlock.placeMachine(h.getLevel(), h.absolutePos(lower), Direction.SOUTH);
        WeaponEnhancerBlockEntity be = (WeaponEnhancerBlockEntity) h.getBlockEntity(lower);
        be.energy().set(70000 + 3); // more than sixteen bits, so both halves are exercised
        net.minecraft.server.level.ServerPlayer player = TestPlayers.mockServerPlayer(h);
        com.mrgregles.bsp_weapons.enhancer.WeaponEnhancerMenu live = new com.mrgregles.bsp_weapons.enhancer.WeaponEnhancerMenu(1, player.getInventory(), be);
        h.assertTrue(live.energyStored() == Math.min(70003, be.energy().getMaxEnergyStored()) && live.energyCapacity() == be.energy().getMaxEnergyStored(), "the server's menu reads the store live: " + live.energyStored() + " / " + live.energyCapacity());
        // a menu on a block entity with no level, as a client builds one: the four shorts it is sent are what it reads back
        WeaponEnhancerBlockEntity bare = new WeaponEnhancerBlockEntity(be.getBlockPos(), be.getBlockState());
        com.mrgregles.bsp_weapons.enhancer.WeaponEnhancerMenu sent = new com.mrgregles.bsp_weapons.enhancer.WeaponEnhancerMenu(2, player.getInventory(), bare);
        sent.setData(0, 70003 & 0xFFFF); sent.setData(1, 70003 >>> 16); sent.setData(2, 20000 & 0xFFFF); sent.setData(3, 0);
        h.assertTrue(sent.energyStored() == 70003 && sent.energyCapacity() == 20000, "what the server sends is what the client reads: " + sent.energyStored() + " / " + sent.energyCapacity());
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "enhancer")
    public void the_action_packet_carries_the_operation_and_the_socket(GameTestHelper h) {
        EnhancerActionPacket p = new EnhancerActionPacket(new BlockPos(1, -60, 300000), Enhancing.SWAP, 2);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        p.encode(buf);
        EnhancerActionPacket back = EnhancerActionPacket.decode(buf);
        h.assertTrue(back.equals(p), "the packet round-trips: " + back);
        h.succeed();
    }
}
