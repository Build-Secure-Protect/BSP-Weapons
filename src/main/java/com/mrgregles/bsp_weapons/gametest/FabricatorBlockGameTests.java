package com.mrgregles.bsp_weapons.gametest;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.WeaponsConfig;
import com.mrgregles.bsp_weapons.fabricator.Fabricating;
import com.mrgregles.bsp_weapons.fabricator.Job;
import com.mrgregles.bsp_weapons.fabricator.WeaponFabricatorBlock;
import com.mrgregles.bsp_weapons.fabricator.WeaponFabricatorBlockEntity;
import com.mrgregles.bsp_weapons.fabricator.WeaponFabricatorConsoleBlock;
import com.mrgregles.bsp_weapons.fabricator.WeaponFabricatorMenu;
import com.mrgregles.bsp_weapons.item.PartItem;
import com.mrgregles.bsp_weapons.network.FabricatorActionPacket;
import com.mrgregles.bsp_weapons.parts.Rarity;
import com.mrgregles.bsp_weapons.registry.ModBlocks;
import com.mrgregles.bsp_weapons.registry.ModItems;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraftforge.items.IItemHandler;

import java.util.List;

/**
 * The Weapon Fabricator's block and machine (WP-7c; D98, D105, D108; R24, R27, R30): the two-space block, the job running on the
 * server's ticks and surviving a save, the energy and item capabilities for pipes, the menu's data and the action packet.
 */
@GameTestHolder(BspWeapons.MODID)
@PrefixGameTestTemplate(false)
public class FabricatorBlockGameTests {
    private static final String EMPTY = SetupGameTests.EMPTY;

    private static ResourceLocation id(String path) {
        return new ResourceLocation(BspWeapons.MODID, path);
    }

    private static String key(Component c) {
        return c == null ? "null" : c.getContents() instanceof TranslatableContents t ? t.getKey() : c.getString();
    }

    private static WeaponFabricatorBlockEntity place(GameTestHelper h, BlockPos tower, Direction facing) {
        WeaponFabricatorBlock.placeMachine(h.getLevel(), h.absolutePos(tower), facing);
        WeaponFabricatorBlockEntity be = (WeaponFabricatorBlockEntity) h.getBlockEntity(tower);
        be.energy().set(be.energy().getMaxEnergyStored());
        return be;
    }

    @GameTest(template = EMPTY, batch = "fabricator_block")
    public void placing_the_fabricator_puts_its_console_beside_it_and_breaking_either_drops_one(GameTestHelper h) {
        BlockPos tower = new BlockPos(5, 2, 5);
        WeaponFabricatorBlock.placeMachine(h.getLevel(), h.absolutePos(tower), Direction.WEST);
        BlockPos console = WeaponFabricatorBlock.companionPos(tower, Direction.WEST);
        h.assertTrue(console.equals(tower.relative(Direction.WEST.getCounterClockWise())), "the console stands on the tower's counter-clockwise side (the viewer's right, R24)");
        h.assertBlockPresent(ModBlocks.WEAPON_FABRICATOR_CONSOLE.get(), console);
        h.assertTrue(h.getBlockState(console).getValue(HorizontalDirectionalBlock.FACING) == Direction.WEST, "the console faces the same way");
        h.assertTrue(WeaponFabricatorConsoleBlock.masterPos(console, Direction.WEST).equals(tower), "the console knows its tower");
        h.assertTrue(h.getBlockEntity(tower) instanceof WeaponFabricatorBlockEntity, "the tower has the block entity");
        // a tower cannot stand beside something that is not its console
        BlockPos lone = new BlockPos(12, 2, 5);
        h.setBlock(WeaponFabricatorBlock.companionPos(lone, Direction.SOUTH), Blocks.STONE);
        h.setBlock(lone, ModBlocks.WEAPON_FABRICATOR.get().defaultBlockState());
        h.assertTrue(!h.getBlockState(lone).canSurvive(h.getLevel(), h.absolutePos(lone)), "no room beside it means it cannot survive");
        h.setBlock(lone, Blocks.AIR);
        h.setBlock(WeaponFabricatorBlock.companionPos(lone, Direction.SOUTH), Blocks.AIR);
        // breaking the console takes the tower with it and drops one machine
        h.getLevel().destroyBlock(h.absolutePos(console), true);
        h.assertBlockPresent(Blocks.AIR, tower);
        h.assertBlockPresent(Blocks.AIR, console);
        h.runAfterDelay(2, () -> {
            List<ItemEntity> drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(tower)).inflate(3));
            int machines = 0, other = 0;
            for (ItemEntity e : drops) { if (e.getItem().is(ModItems.WEAPON_FABRICATOR.get())) machines += e.getItem().getCount(); else other++; }
            h.assertTrue(machines == 1 && other == 0, "one Weapon Fabricator dropped and nothing else, got " + machines + " and " + other);
            drops.forEach(ItemEntity::discard);
            BlockPos t2 = new BlockPos(5, 2, 12);
            WeaponFabricatorBlock.placeMachine(h.getLevel(), h.absolutePos(t2), Direction.NORTH);
            h.getLevel().destroyBlock(h.absolutePos(t2), false);
            h.assertBlockPresent(Blocks.AIR, WeaponFabricatorBlock.companionPos(t2, Direction.NORTH));
            h.succeed();
        });
    }

    @GameTest(template = EMPTY, batch = "fabricator_block", timeoutTicks = 400)
    public void the_machine_runs_a_job_on_its_own_ticks_keeps_it_through_a_save_and_drops_the_product_when_broken(GameTestHelper h) {
        BlockPos tower = new BlockPos(5, 2, 5);
        WeaponFabricatorBlockEntity be = place(h, tower, Direction.SOUTH);
        Job barrel = Job.part(id("focused_barrel"));
        h.assertTrue(key(be.make()).equals("fabricate.bsp_weapons.no_job"), "MAKE with no row chosen is refused");
        be.choose(barrel);
        be.items().setStackInSlot(Fabricating.MATERIAL, new ItemStack(Items.IRON_INGOT, 3));
        Component why = be.make();
        h.assertTrue(why == null, "the job starts: " + why);
        h.assertTrue(be.working() && be.state().ticksLeft() == Fabricating.ticks(), "the machine works for the config's ticks");
        h.assertTrue(be.energy().getEnergyStored() == be.energy().getMaxEnergyStored() - WeaponsConfig.PART_CHARGE.get(), "the charge was drawn");
        h.assertTrue(be.getUpdateTag().getBoolean("Working"), "clients hear that it works");
        // a save mid-job keeps the row, the product and the time
        CompoundTag saved = be.saveWithoutMetadata();
        WeaponFabricatorBlockEntity loaded = new WeaponFabricatorBlockEntity(be.getBlockPos(), be.getBlockState());
        loaded.load(saved);
        h.assertTrue(barrel.equals(loaded.state().chosen()) && loaded.state().working() && ItemStack.matches(loaded.state().product(), be.state().product()) && loaded.state().ticksLeft() > 0, "the job survives the save");
        h.assertTrue(loaded.energy().getEnergyStored() == be.energy().getEnergyStored() && loaded.items().getStackInSlot(Fabricating.MATERIAL).isEmpty(), "and so do the charge and the emptied slot");
        int half = Fabricating.ticks() / 2;
        h.runAfterDelay(half, () -> {
            h.assertTrue(be.working() && be.items().getStackInSlot(Fabricating.OUT).isEmpty(), "half way: still working, OUT empty");
            h.assertTrue(be.state().ticksLeft() <= Fabricating.ticks() - half + 1 && be.state().ticksLeft() >= Fabricating.ticks() - half - 2, "the server's ticks drive it: " + be.state().ticksLeft() + " left");
        });
        h.runAfterDelay(Fabricating.ticks() + 3, () -> {
            ItemStack out = be.items().getStackInSlot(Fabricating.OUT);
            h.assertTrue(!be.working() && out.getItem() == ModItems.part(id("focused_barrel")) && PartItem.materialOf(out).equals(id("iron")) && PartItem.rarityOf(out) == Rarity.NORMAL, "the barrel is in OUT and the machine idle, got " + out);
            h.assertTrue(!be.getUpdateTag().getBoolean("Working"), "clients hear that it stopped");
            h.assertTrue(barrel.equals(be.state().chosen()), "the chosen row is kept after the job (R27)");
            // a second job with OUT full is refused; emptied, it runs; broken mid-job, the product drops
            be.items().setStackInSlot(Fabricating.MATERIAL, new ItemStack(Items.IRON_INGOT, 3));
            h.assertTrue(key(be.make()).equals("fabricate.bsp_weapons.out_full"), "OUT full refuses (R30)");
            be.items().setStackInSlot(Fabricating.OUT, ItemStack.EMPTY);
            h.assertTrue(be.make() == null && be.working(), "emptied, the next job runs");
            be.items().setStackInSlot(Fabricating.ROLL, new ItemStack(ModItems.CATALYST_ITEMS.get(0).get(), 2));
            h.getLevel().destroyBlock(h.absolutePos(tower), true);
            h.runAfterDelay(2, () -> {
                List<ItemEntity> drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(tower)).inflate(3));
                int barrels = 0, catalysts = 0, machines = 0;
                for (ItemEntity e : drops) {
                    if (e.getItem().getItem() == ModItems.part(id("focused_barrel"))) barrels += e.getItem().getCount();
                    else if (e.getItem().is(ModItems.CATALYST_ITEMS.get(0).get())) catalysts += e.getItem().getCount();
                    else if (e.getItem().is(ModItems.WEAPON_FABRICATOR.get())) machines += e.getItem().getCount();
                }
                h.assertTrue(barrels == 1 && catalysts == 2 && machines == 1, "the product under way, the slots' items and the machine drop: " + barrels + " barrel, " + catalysts + " catalysts, " + machines + " machine");
                drops.forEach(ItemEntity::discard);
                h.succeed();
            });
        });
    }

    @GameTest(template = EMPTY, batch = "fabricator_block")
    public void pipes_put_inputs_in_from_any_side_and_take_the_product_out_below_and_charge_from_any_side(GameTestHelper h) {
        BlockPos tower = new BlockPos(5, 2, 5);
        WeaponFabricatorBlockEntity be = place(h, tower, Direction.SOUTH);
        IEnergyStorage top = be.getCapability(ForgeCapabilities.ENERGY, Direction.UP).orElse(null), side = be.getCapability(ForgeCapabilities.ENERGY, Direction.WEST).orElse(null);
        h.assertTrue(top != null && side != null && top.canReceive() && !top.canExtract(), "the energy capability is on every side, intake only");
        be.energy().set(0);
        h.assertTrue(side.receiveEnergy(5000, false) == WeaponsConfig.FABRICATOR_MAX_RECEIVE.get(), "one push takes at most the intake a tick");
        h.assertTrue(side.extractEnergy(100, false) == 0 && be.energy().getMaxEnergyStored() == WeaponsConfig.FABRICATOR_CAPACITY.get(), "nothing comes out; the capacity is the config's");
        be.energy().set(be.energy().getMaxEnergyStored());
        IItemHandler west = be.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.WEST).orElse(null), below = be.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).orElse(null), any = be.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
        h.assertTrue(west != null && below != null && any != null && west.getSlots() == Fabricating.SLOTS, "the item capability is on every side with the four slots");
        h.assertTrue(west.insertItem(Fabricating.MATERIAL, new ItemStack(Items.IRON_INGOT, 5), false).isEmpty() && be.items().getStackInSlot(Fabricating.MATERIAL).getCount() == 5, "a side pipe fills MATERIAL");
        h.assertTrue(west.insertItem(Fabricating.MATERIAL, new ItemStack(Items.STICK), false).getCount() == 1, "and not with a stick");
        h.assertTrue(west.insertItem(Fabricating.ROLL, new ItemStack(ModItems.CATALYST_ITEMS.get(1).get()), false).isEmpty(), "a side pipe fills ROLL with a catalyst");
        h.assertTrue(west.insertItem(Fabricating.EXTRA, new ItemStack(Items.REDSTONE, 2), false).isEmpty(), "and the second slot with redstone");
        h.assertTrue(west.insertItem(Fabricating.OUT, new ItemStack(Items.IRON_INGOT), false).getCount() == 1 && !west.isItemValid(Fabricating.OUT, new ItemStack(Items.IRON_INGOT)), "nothing goes into OUT from a pipe");
        h.assertTrue(below.insertItem(Fabricating.MATERIAL, new ItemStack(Items.IRON_INGOT), false).getCount() == 1, "the pipe below puts nothing in");
        be.choose(Job.part(id("focused_barrel")));
        h.assertTrue(be.make() == null, "the job starts from piped inputs");
        h.assertTrue(west.extractItem(Fabricating.MATERIAL, 2, false).isEmpty() && be.items().getStackInSlot(Fabricating.MATERIAL).getCount() == 2, "a side pipe takes no input back");
        be.items().setStackInSlot(Fabricating.OUT, ModItems.part(id("scope")).of(id("iron"), Rarity.NORMAL));
        h.assertTrue(west.extractItem(Fabricating.OUT, 1, false).isEmpty(), "nor the product from a side");
        h.assertTrue(below.extractItem(Fabricating.MATERIAL, 1, false).isEmpty(), "the pipe below takes no input");
        ItemStack taken = below.extractItem(Fabricating.OUT, 1, false);
        h.assertTrue(taken.getItem() == ModItems.part(id("scope")) && be.items().getStackInSlot(Fabricating.OUT).isEmpty(), "the pipe below takes the product out of OUT (D105)");
        h.assertTrue(any.insertItem(Fabricating.MATERIAL, new ItemStack(Items.IRON_INGOT), false).isEmpty() && any.extractItem(Fabricating.MATERIAL, 1, false).isEmpty(), "with no side given, inputs go in and never come out");
        be.items().setStackInSlot(Fabricating.OUT, new ItemStack(ModItems.ENERGY_ROUND.get(), 8));
        h.assertTrue(any.extractItem(Fabricating.OUT, 8, false).getCount() == 8, "and the product comes out");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "fabricator_block")
    public void the_menu_carries_the_charge_the_progress_and_the_chosen_row_and_the_packet_chooses_and_makes(GameTestHelper h) {
        BlockPos tower = new BlockPos(5, 2, 5);
        WeaponFabricatorBlockEntity be = place(h, tower, Direction.SOUTH);
        be.energy().set(70003); // more than sixteen bits, so both halves are exercised (the store is capped at the capacity)
        ServerPlayer player = TestPlayers.mockServerPlayer(h);
        player.setPos(h.absolutePos(tower).getX() + 0.5, h.absolutePos(tower).getY() + 1, h.absolutePos(tower).getZ() + 2.5);
        WeaponFabricatorMenu menu = new WeaponFabricatorMenu(1, player.getInventory(), be);
        h.assertTrue(menu.energyStored() == be.energy().getEnergyStored() && menu.energyCapacity() == be.energy().getMaxEnergyStored(), "the server's menu reads the store live: " + menu.energyStored() + " / " + menu.energyCapacity());
        h.assertTrue(menu.chosenIndex() == -1 && !menu.working() && menu.ticksLeft() == 0 && menu.ticksTotal() == 0, "no row chosen, idle");
        List<Job> jobs = Fabricating.catalogue(h.getLevel());
        int scope = jobs.indexOf(Job.part(id("scope")));
        h.assertTrue(scope > 0, "the Scope is a catalogue row");
        player.containerMenu = menu;
        h.assertTrue(FabricatorActionPacket.apply(player, be.getBlockPos(), FabricatorActionPacket.CHOOSE, scope) == null, "CHOOSE says nothing back");
        h.assertTrue(Job.part(id("scope")).equals(be.state().chosen()) && menu.chosenIndex() == scope, "the row is chosen and the menu carries its index, got " + menu.chosenIndex());
        h.assertTrue(key(FabricatorActionPacket.apply(player, be.getBlockPos(), FabricatorActionPacket.MAKE, 0)).equals("fabricate.bsp_weapons.no_material"), "MAKE with an empty slot sends the refusal back");
        be.items().setStackInSlot(Fabricating.MATERIAL, new ItemStack(Items.DIAMOND, 1));
        h.assertTrue(FabricatorActionPacket.apply(player, be.getBlockPos(), FabricatorActionPacket.MAKE, 0) == null, "MAKE starts the job");
        h.assertTrue(menu.working() && menu.ticksTotal() == Fabricating.ticks() && menu.ticksLeft() == Fabricating.ticks(), "the menu carries the progress: " + menu.ticksLeft() + " of " + menu.ticksTotal());
        h.assertTrue(key(FabricatorActionPacket.apply(player, be.getBlockPos(), FabricatorActionPacket.MAKE, 0)).equals("fabricate.bsp_weapons.busy"), "a second MAKE is refused while it works");
        h.assertTrue(FabricatorActionPacket.apply(player, be.getBlockPos().above(3), FabricatorActionPacket.CHOOSE, 0) == null && menu.chosenIndex() == scope, "a packet for another position does nothing");
        h.assertTrue(FabricatorActionPacket.apply(player, be.getBlockPos(), FabricatorActionPacket.CHOOSE, 9999) == null && be.state().chosen() == null && menu.chosenIndex() == -1, "an index past the catalogue clears the row");
        h.assertTrue(FabricatorActionPacket.apply(player, be.getBlockPos(), 7, 0) == null, "an unknown action does nothing");
        // a menu on a block entity with no level, as a client builds one, reads back what it is sent
        WeaponFabricatorBlockEntity bare = new WeaponFabricatorBlockEntity(be.getBlockPos(), be.getBlockState());
        WeaponFabricatorMenu sent = new WeaponFabricatorMenu(2, player.getInventory(), bare);
        sent.setData(0, 70003 & 0xFFFF); sent.setData(1, 70003 >>> 16); sent.setData(2, 20000 & 0xFFFF); sent.setData(3, 0); sent.setData(4, 33); sent.setData(5, 80); sent.setData(6, scope + 1);
        h.assertTrue(sent.energyStored() == 70003 && sent.energyCapacity() == 20000 && sent.ticksLeft() == 33 && sent.ticksTotal() == 80 && sent.working() && sent.chosenIndex() == scope, "what the server sends is what the client reads");
        FabricatorActionPacket p = new FabricatorActionPacket(new BlockPos(1, -60, 300000), FabricatorActionPacket.CHOOSE, 300);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        p.encode(buf);
        h.assertTrue(FabricatorActionPacket.decode(buf).equals(p), "the packet survives the wire");
        h.assertTrue(key(Fabricating.name(Job.part(id("focused_barrel")), h.getLevel())).equals("item.bsp_weapons.focused_barrel") && key(Fabricating.name(Job.chip(id("coolant")), h.getLevel())).equals("item.bsp_weapons.augment_chip.named")
                && key(Fabricating.name(Job.recipe(id("energy_round")), h.getLevel())).equals("item.bsp_weapons.energy_round"), "rows are named by their product");
        h.succeed();
    }
}
