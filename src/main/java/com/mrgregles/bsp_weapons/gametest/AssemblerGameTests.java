package com.mrgregles.bsp_weapons.gametest;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.assembler.Assembly;
import com.mrgregles.bsp_weapons.assembler.WeaponAssemblerBlock;
import com.mrgregles.bsp_weapons.assembler.WeaponAssemblerBlockEntity;
import com.mrgregles.bsp_weapons.assembler.WeaponAssemblerPartBlock;
import com.mrgregles.bsp_weapons.item.CoreItem;
import com.mrgregles.bsp_weapons.item.PartItem;
import com.mrgregles.bsp_weapons.item.WeaponItem;
import com.mrgregles.bsp_weapons.network.WeaponDataPacket;
import com.mrgregles.bsp_weapons.parts.PartKind;
import com.mrgregles.bsp_weapons.parts.PartSpec;
import com.mrgregles.bsp_weapons.parts.Rarity;
import com.mrgregles.bsp_weapons.parts.Stat;
import com.mrgregles.bsp_weapons.parts.WeaponSpec;
import com.mrgregles.bsp_weapons.registry.ModBlocks;
import com.mrgregles.bsp_weapons.registry.ModItems;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;

/** The Weapon Assembler (WP-3; D51 to D63): the two-space block, the assembly and take-apart rules, the slot rules, the data to clients. */
@GameTestHolder(BspWeapons.MODID)
@PrefixGameTestTemplate(false)
public class AssemblerGameTests {
    private static final String EMPTY = SetupGameTests.EMPTY;

    private static ResourceLocation id(String path) {
        return new ResourceLocation(BspWeapons.MODID, path);
    }

    /** A handler filled for a Tetrium pistol on a Tier II core, with the engram and (optionally) a catalyst. */
    private static ItemStackHandler filled(String catalyst) {
        ItemStackHandler inv = new ItemStackHandler(Assembly.SLOTS);
        inv.setStackInSlot(Assembly.ENGRAM, new ItemStack(ModItems.ENGRAMS.get(0).get()));
        inv.setStackInSlot(Assembly.CORE, ModItems.WEAPON_CORE.get().of(2));
        inv.setStackInSlot(Assembly.slotFor(PartKind.BARREL), ModItems.part(id("focused_barrel")).of(id("tetrium"), Rarity.NORMAL));
        inv.setStackInSlot(Assembly.slotFor(PartKind.CELL), ModItems.part(id("energy_cell")).of(id("steel"), Rarity.RARE));
        inv.setStackInSlot(Assembly.slotFor(PartKind.GRIP), ModItems.part(id("standard_grip")).of(id("magnatite"), Rarity.NORMAL));
        inv.setStackInSlot(Assembly.slotFor(PartKind.OPTIC), ModItems.part(id("iron_sight")).of(id("tetrium"), Rarity.NORMAL));
        if (catalyst != null) {
            for (var c : ModItems.CATALYST_ITEMS) if (c.getId().getPath().equals(catalyst)) inv.setStackInSlot(Assembly.ROLL, new ItemStack(c.get(), 2));
        }
        return inv;
    }

    @GameTest(template = EMPTY, batch = "assembler")
    public void placing_the_assembler_puts_its_companion_on_the_viewers_right_and_breaking_either_drops_one(GameTestHelper h) {
        BlockPos master = new BlockPos(5, 2, 5);
        WeaponAssemblerBlock.placeMachine(h.getLevel(), h.absolutePos(master), Direction.SOUTH);
        BlockPos companion = WeaponAssemblerBlock.companionPos(master, Direction.SOUTH);
        h.assertTrue(companion.equals(master.east()), "facing south, the companion stands to the east (the viewer's right as they face north at the machine)");
        h.assertBlockPresent(ModBlocks.WEAPON_ASSEMBLER_PART.get(), companion);
        h.assertTrue(h.getBlockState(companion).getValue(HorizontalDirectionalBlock.FACING) == Direction.SOUTH, "the companion faces the same way");
        h.assertTrue(WeaponAssemblerPartBlock.masterPos(companion, Direction.SOUTH).equals(master), "the companion knows its master");
        h.assertTrue(h.getBlockEntity(master) instanceof WeaponAssemblerBlockEntity, "the master has the block entity");
        // breaking the companion takes the master with it and drops one machine
        h.getLevel().destroyBlock(h.absolutePos(companion), true);
        h.assertBlockPresent(Blocks.AIR, master);
        h.assertBlockPresent(Blocks.AIR, companion);
        h.runAfterDelay(2, () -> {
            List<ItemEntity> drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(master)).inflate(3));
            int machines = 0;
            for (ItemEntity e : drops) if (e.getItem().is(ModItems.WEAPON_ASSEMBLER.get())) machines += e.getItem().getCount();
            h.assertTrue(machines == 1, "one Weapon Assembler dropped, got " + machines);
            drops.forEach(ItemEntity::discard);
            // the other way round: break the master, the companion goes too
            BlockPos m2 = new BlockPos(12, 2, 5);
            WeaponAssemblerBlock.placeMachine(h.getLevel(), h.absolutePos(m2), Direction.EAST);
            BlockPos c2 = WeaponAssemblerBlock.companionPos(m2, Direction.EAST);
            h.assertTrue(c2.equals(m2.north()), "facing east, the companion stands to the north");
            h.getLevel().destroyBlock(h.absolutePos(m2), false);
            h.assertBlockPresent(Blocks.AIR, c2);
            h.succeed();
        });
    }

    @GameTest(template = EMPTY, batch = "assembler")
    public void assembling_makes_the_weapon_and_uses_the_core_the_parts_and_the_catalyst_but_not_the_engram(GameTestHelper h) {
        ItemStackHandler inv = filled("lapis_catalyst");
        h.assertTrue(Assembly.cannotAssemble(inv) == null, "a full set assembles: " + Assembly.cannotAssemble(inv));
        String why = Assembly.assemble(inv, RandomSource.create(1));
        h.assertTrue(why == null, "assembled: " + why);
        ItemStack out = inv.getStackInSlot(Assembly.OUTPUT);
        WeaponSpec spec = WeaponItem.specOf(out);
        h.assertTrue(spec != null && spec.pattern().equals(id("laser_pistol")) && spec.coreTier() == 2 && spec.parts().size() == 4, "the weapon's spec: " + spec);
        h.assertTrue(spec.part(PartKind.CELL).rarity() == Rarity.RARE, "the cell keeps its own roll");
        h.assertTrue(inv.getStackInSlot(Assembly.CORE).isEmpty() && inv.getStackInSlot(Assembly.slotFor(PartKind.BARREL)).isEmpty(), "the core and parts were used");
        h.assertTrue(inv.getStackInSlot(Assembly.ROLL).getCount() == 1, "one catalyst was used, one is left");
        h.assertTrue(!inv.getStackInSlot(Assembly.ENGRAM).isEmpty(), "the engram stays (D17)");
        h.assertTrue(out.getCount() == 1 && out.getItem() == ModItems.weapon(id("laser_pistol")), "one laser pistol item");
        inv.setStackInSlot(Assembly.CORE, ModItems.WEAPON_CORE.get().of(2));
        for (PartKind k : List.of(PartKind.BARREL, PartKind.CELL, PartKind.GRIP, PartKind.OPTIC)) inv.setStackInSlot(Assembly.slotFor(k), filled(null).getStackInSlot(Assembly.slotFor(k)));
        h.assertTrue(Assembly.cannotAssemble(inv) != null && Assembly.cannotAssemble(inv).contains("finished weapon"), "a second assembly waits for the output to be taken: " + Assembly.cannotAssemble(inv));
        // without a catalyst the roll is Normal every time
        ItemStackHandler plain = filled(null);
        Assembly.assemble(plain, RandomSource.create(2));
        h.assertTrue(WeaponItem.specOf(plain.getStackInSlot(Assembly.OUTPUT)).roll() == Rarity.NORMAL, "no catalyst means Normal");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "assembler")
    public void assembly_is_refused_with_a_slot_empty_or_a_part_of_the_wrong_kind(GameTestHelper h) {
        ItemStackHandler inv = filled(null);
        inv.setStackInSlot(Assembly.slotFor(PartKind.OPTIC), ItemStack.EMPTY);
        String why = Assembly.cannotAssemble(inv);
        h.assertTrue(why != null && why.contains("optic slot is empty"), "an empty optic slot is named: " + why);
        h.assertTrue(Assembly.assemble(inv, RandomSource.create(3)) != null && inv.getStackInSlot(Assembly.OUTPUT).isEmpty(), "nothing is made");
        h.assertTrue(!inv.getStackInSlot(Assembly.CORE).isEmpty(), "nothing is used");
        inv = filled(null);
        inv.setStackInSlot(Assembly.ENGRAM, ItemStack.EMPTY);
        h.assertTrue(Assembly.cannotAssemble(inv).contains("engram"), "no engram is named");
        inv = filled(null);
        inv.setStackInSlot(Assembly.CORE, ItemStack.EMPTY);
        h.assertTrue(Assembly.cannotAssemble(inv).contains("core slot"), "no core is named");
        // the slot rules: only the right things fit each slot
        h.assertTrue(Assembly.fits(Assembly.ENGRAM, new ItemStack(ModItems.ENGRAMS.get(0).get())) && !Assembly.fits(Assembly.ENGRAM, ModItems.WEAPON_CORE.get().of(1)), "engram slot");
        h.assertTrue(Assembly.fits(Assembly.slotFor(PartKind.BARREL), ModItems.part(id("pulse_barrel")).of(id("iron"), Rarity.NORMAL)) && !Assembly.fits(Assembly.slotFor(PartKind.BARREL), ModItems.part(id("scope")).of(id("iron"), Rarity.NORMAL)), "barrel slot takes barrels only");
        h.assertTrue(Assembly.fits(Assembly.ROLL, new ItemStack(ModItems.CATALYST_ITEMS.get(0).get())) && !Assembly.fits(Assembly.ROLL, new ItemStack(net.minecraft.world.item.Items.LAPIS_LAZULI)), "roll slot takes catalysts only");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "assembler")
    public void taking_apart_costs_a_catalyst_and_returns_the_core_and_parts_with_their_own_rolls(GameTestHelper h) {
        ItemStackHandler inv = filled("illyrium_catalyst");
        Assembly.assemble(inv, RandomSource.create(4));
        WeaponSpec made = WeaponItem.specOf(inv.getStackInSlot(Assembly.OUTPUT));
        h.assertTrue(made.roll() == Rarity.EPIC || made.roll() == Rarity.S, "Illyrium rolls Epic or S: " + made.roll());
        // one catalyst is left from the pair: it pays for taking apart
        String why = Assembly.cannotTakeApart(inv);
        h.assertTrue(why == null, "ready to take apart: " + why);
        h.assertTrue(Assembly.takeApart(inv) == null, "taken apart");
        h.assertTrue(inv.getStackInSlot(Assembly.OUTPUT).isEmpty(), "the weapon is gone");
        h.assertTrue(inv.getStackInSlot(Assembly.ROLL).isEmpty(), "the catalyst was spent");
        h.assertTrue(CoreItem.tierOf(inv.getStackInSlot(Assembly.CORE)) == 2 && inv.getStackInSlot(Assembly.CORE).getItem() instanceof CoreItem, "the Tier II core is back");
        ItemStack cell = inv.getStackInSlot(Assembly.slotFor(PartKind.CELL));
        h.assertTrue(PartItem.specOf(cell).material().equals(id("steel")) && PartItem.rarityOf(cell) == Rarity.RARE, "the Rare steel cell is back with its own roll");
        h.assertTrue(PartItem.specOf(inv.getStackInSlot(Assembly.slotFor(PartKind.OPTIC))).type().equals(id("iron_sight")), "the iron sight is back");
        // and without a catalyst, nothing happens
        ItemStackHandler again = filled(null);
        Assembly.assemble(again, RandomSource.create(5));
        h.assertTrue(Assembly.cannotTakeApart(again) != null && Assembly.cannotTakeApart(again).contains("catalyst"), "taking apart asks for a catalyst (D53)");
        h.assertTrue(Assembly.takeApart(again) != null && !again.getStackInSlot(Assembly.OUTPUT).isEmpty(), "the weapon stays");
        // nor while a slot it needs is occupied
        again.setStackInSlot(Assembly.ROLL, new ItemStack(ModItems.CATALYST_ITEMS.get(0).get()));
        again.setStackInSlot(Assembly.CORE, ModItems.WEAPON_CORE.get().of(1));
        h.assertTrue(Assembly.cannotTakeApart(again).contains("core slot"), "a full core slot blocks it");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "assembler")
    public void the_weapon_item_shows_its_sheet_and_tints_each_slot_from_its_material(GameTestHelper h) {
        ItemStackHandler inv = filled(null);
        Assembly.assemble(inv, RandomSource.create(6));
        ItemStack gun = inv.getStackInSlot(Assembly.OUTPUT);
        h.assertTrue(WeaponItem.colourOf(gun, 0) == com.mrgregles.bsp_weapons.data.WeaponData.material(id("tetrium")).colour(), "tint 0 is the barrel's Tetrium");
        h.assertTrue(WeaponItem.colourOf(gun, 1) == com.mrgregles.bsp_weapons.data.WeaponData.material(id("steel")).colour(), "tint 1 is the cell's steel");
        h.assertTrue(WeaponItem.colourOf(gun, 4) == CoreItem.TIER_COLOURS[1], "tint 4 is the Tier II glow");
        java.util.List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
        gun.getItem().appendHoverText(gun, null, lines, net.minecraft.world.item.TooltipFlag.Default.NORMAL);
        h.assertTrue(lines.size() == 2 + WeaponItem.SHOWN.length + 4, "a rarity line, the energy line, the shown stats and four part lines: " + lines.size());
        h.assertTrue(WeaponItem.number(Stat.PIERCING, 8.4).equals("8%") && WeaponItem.number(Stat.DAMAGE, 6.44).equals("6.4") && WeaponItem.number(Stat.RANGE, 36.4).equals("36"), "numbers read plainly");
        h.assertTrue(WeaponItem.specOf(new ItemStack(ModItems.weapon(id("laser_pistol")))) == null, "a bare weapon item has no spec");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "assembler")
    public void patterns_and_catalysts_reach_the_client_with_the_rest(GameTestHelper h) {
        WeaponDataPacket packet = WeaponDataPacket.current();
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(buf);
        WeaponDataPacket back = WeaponDataPacket.decode(buf);
        var p = back.patterns().get(id("laser_pistol"));
        h.assertTrue(p != null && p.slots().equals(List.of(PartKind.BARREL, PartKind.CELL, PartKind.GRIP, PartKind.OPTIC)), "the pistol pattern arrives with its slots");
        h.assertTrue(Math.abs(p.base().get(Stat.RANGE) - 24) < 1e-4, "and its base sheet");
        var c = back.catalysts().get(id("illyrium_catalyst"));
        h.assertTrue(c != null && c.weight(Rarity.EPIC) == 85 && c.colour() == 0x19D3B0, "the Illyrium catalyst arrives turquoise with its odds (D62)");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "assembler")
    public void audit_take_apart_refuses_a_weapon_the_server_would_not_make_and_the_output_slot_takes_nothing(GameTestHelper h) {
        ItemStackHandler inv = filled("lapis_catalyst");
        Assembly.assemble(inv, RandomSource.create(7));
        // a forged weapon: a Tier V core and an unknown material, in the output slot
        WeaponSpec forged = new WeaponSpec(id("laser_pistol"), 5, List.of(new PartSpec(PartKind.BARREL, id("focused_barrel"), id("cheese"), Rarity.S)), Rarity.S, List.of());
        inv.setStackInSlot(Assembly.OUTPUT, ModItems.weapon(id("laser_pistol")).of(forged));
        inv.setStackInSlot(Assembly.CORE, ItemStack.EMPTY);
        for (PartKind k : List.of(PartKind.BARREL, PartKind.CELL, PartKind.GRIP, PartKind.OPTIC)) inv.setStackInSlot(Assembly.slotFor(k), ItemStack.EMPTY);
        String why = Assembly.cannotTakeApart(inv);
        h.assertTrue(why != null && why.contains("not one this server would make"), "a forged weapon is refused: " + why);
        h.assertTrue(Assembly.takeApart(inv) != null && inv.getStackInSlot(Assembly.CORE).isEmpty(), "and nothing comes out of it");
        // the output slot of the menu takes no weapon from a player
        BlockPos at = new BlockPos(3, 2, 12);
        WeaponAssemblerBlock.placeMachine(h.getLevel(), h.absolutePos(at), Direction.SOUTH);
        WeaponAssemblerBlockEntity be = (WeaponAssemblerBlockEntity) h.getBlockEntity(at);
        var menu = new com.mrgregles.bsp_weapons.assembler.WeaponAssemblerMenu(1, TestPlayers.mockServerPlayer(h).getInventory(), be);
        h.assertTrue(!menu.slots.get(Assembly.OUTPUT).mayPlace(inv.getStackInSlot(Assembly.OUTPUT)), "the WEAPON slot refuses a weapon put in by hand");
        h.assertTrue(menu.slots.get(Assembly.CORE).mayPlace(ModItems.WEAPON_CORE.get().of(1)) && !menu.slots.get(Assembly.CORE).mayPlace(new ItemStack(ModItems.ENERGY_ROUND.get())), "the CORE slot takes cores only");
        h.getLevel().destroyBlock(h.absolutePos(at), false);
        h.succeed();
    }
}
