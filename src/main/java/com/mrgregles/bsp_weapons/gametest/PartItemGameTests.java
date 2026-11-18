package com.mrgregles.bsp_weapons.gametest;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.WeaponsConfig;
import com.mrgregles.bsp_weapons.data.WeaponData;
import com.mrgregles.bsp_weapons.item.CoreItem;
import com.mrgregles.bsp_weapons.item.PartItem;
import com.mrgregles.bsp_weapons.network.WeaponDataPacket;
import com.mrgregles.bsp_weapons.parts.PartKind;
import com.mrgregles.bsp_weapons.parts.PartSpec;
import com.mrgregles.bsp_weapons.parts.PartType;
import com.mrgregles.bsp_weapons.parts.Rarity;
import com.mrgregles.bsp_weapons.parts.SheetCalculator;
import com.mrgregles.bsp_weapons.parts.Stat;
import com.mrgregles.bsp_weapons.parts.Validation;
import com.mrgregles.bsp_weapons.parts.WeaponMaterial;
import com.mrgregles.bsp_weapons.parts.WeaponSpec;
import com.mrgregles.bsp_weapons.registry.ModItems;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/** The part items (WP-2; D18, D48 to D50): registry, tags, tooltips, the data sent to clients, and a weapon built from items. */
@GameTestHolder(BspWeapons.MODID)
@PrefixGameTestTemplate(false)
public class PartItemGameTests {
    private static final String EMPTY = SetupGameTests.EMPTY;

    private static ResourceLocation id(String path) {
        return new ResourceLocation(BspWeapons.MODID, path);
    }

    private static String key(Component c) {
        return c.getContents() instanceof TranslatableContents t ? t.getKey() : c.getString();
    }

    @GameTest(template = EMPTY, batch = "part_items")
    public void every_part_type_in_the_data_is_an_item_of_its_kind(GameTestHelper h) {
        h.assertTrue(ModItems.PARTS.size() == 14, "14 part items, got " + ModItems.PARTS.size());
        for (RegistryObject<PartItem> p : ModItems.PARTS) {
            PartType type = WeaponData.partType(p.get().typeId());
            h.assertTrue(type != null, p.getId() + " has a part type data file");
            h.assertTrue(type.kind() == p.get().kind(), p.getId() + " is registered as a " + p.get().kind() + " and its data says " + type.kind());
            h.assertTrue(ForgeRegistries.ITEMS.getKey(p.get()).equals(p.get().typeId()), "the item id is the type id");
        }
        for (PartType t : WeaponData.partTypes().values()) h.assertTrue(ModItems.part(t.id()) != null, "part type " + t.id() + " has an item");
        h.assertTrue(ForgeRegistries.ITEMS.containsKey(id("weapon_core")), "the Weapon Core is registered");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "part_items")
    public void a_part_remembers_only_its_material_and_roll(GameTestHelper h) {
        PartItem barrel = ModItems.part(id("focused_barrel"));
        ItemStack stack = barrel.of(id("illyrium"), Rarity.EPIC);
        PartSpec spec = PartItem.specOf(stack);
        h.assertTrue(spec.equals(new PartSpec(PartKind.BARREL, id("focused_barrel"), id("illyrium"), Rarity.EPIC)), "the spec from the stack: " + spec);
        h.assertTrue(stack.getTag().getAllKeys().size() == 2, "two tag keys and nothing else: " + stack.getTag());
        h.assertTrue(PartItem.colourOf(stack) == WeaponData.material(id("illyrium")).colour(), "the tint is Illyrium's colour");
        // a bare stack (creative menu without a tag, or a forged one): an unknown material, refused by validation, grey tint
        ItemStack bare = new ItemStack(barrel);
        h.assertTrue(PartItem.specOf(bare).material().equals(id("unknown")), "no tag means an unknown material");
        h.assertTrue(PartItem.colourOf(bare) == PartItem.UNKNOWN_COLOUR, "an unknown material tints grey");
        h.assertTrue(PartItem.specOf(new ItemStack(net.minecraft.world.item.Items.STICK)) == null, "a stick is not a part");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "part_items")
    public void a_core_keeps_its_tier_within_the_configs_tiers(GameTestHelper h) {
        CoreItem core = ModItems.WEAPON_CORE.get();
        h.assertTrue(CoreItem.tierOf(core.of(3)) == 3, "tier 3 round-trips");
        h.assertTrue(CoreItem.tierOf(core.of(0)) == 1, "tier 0 becomes I");
        h.assertTrue(CoreItem.tierOf(core.of(99)) == WeaponsConfig.tiers(), "tier 99 becomes the top tier");
        h.assertTrue(CoreItem.tierOf(new ItemStack(core)) == 1, "no tag is Tier I");
        h.assertTrue(CoreItem.colourOf(core.of(5)) == CoreItem.TIER_COLOURS[4], "Tier V glows gold");
        List<Component> lines = new ArrayList<>();
        core.appendHoverText(core.of(2), null, lines, TooltipFlag.Default.NORMAL);
        h.assertTrue(lines.size() == 2 && key(lines.get(0)).equals("tooltip.bsp_weapons.core_tier"), "the core's tooltip names its tier: " + lines);
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "part_items")
    public void the_tooltip_shows_rarity_material_and_the_parts_own_changes(GameTestHelper h) {
        PartItem barrel = ModItems.part(id("focused_barrel"));
        List<Component> lines = new ArrayList<>();
        barrel.appendHoverText(barrel.of(id("tetrium"), Rarity.RARE), null, lines, TooltipFlag.Default.NORMAL);
        h.assertTrue(!lines.isEmpty() && key(lines.get(0)).equals("tooltip.bsp_weapons.part"), "the first line is rarity and material: " + lines);
        // a Focused Tetrium barrel changes damage, range, spread (type and band), durability and heat cap (band ANY)
        List<String> statKeys = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) statKeys.add(key(lines.get(i)));
        for (Stat s : new Stat[]{Stat.DAMAGE, Stat.RANGE, Stat.SPREAD, Stat.DURABILITY, Stat.HEAT_CAP}) {
            h.assertTrue(statKeys.contains("stat.bsp_weapons." + s.id()), "the tooltip lists " + s.id() + ": " + statKeys);
        }
        h.assertTrue(!statKeys.contains("stat.bsp_weapons.zoom"), "a barrel says nothing about zoom");
        h.assertTrue(lines.get(1).getString().contains("%") || lines.get(1).getString().matches(".*[+-]\\d.*"), "a change reads as a signed number: " + lines.get(1).getString());
        // an unknown material: one line, no numbers
        lines.clear();
        barrel.appendHoverText(new ItemStack(barrel), null, lines, TooltipFlag.Default.NORMAL);
        h.assertTrue(lines.size() == 1, "an unknown material gives one line: " + lines);
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "part_items")
    public void the_part_data_reaches_the_client_unchanged(GameTestHelper h) {
        WeaponDataPacket packet = WeaponDataPacket.current();
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(buf);
        int bytes = buf.readableBytes();
        WeaponDataPacket back = WeaponDataPacket.decode(buf);
        h.assertTrue(back.materials().size() == WeaponData.materials().size(), "every material arrives: " + back.materials().size());
        h.assertTrue(back.bands().size() == 5 && back.partTypes().size() == WeaponData.partTypes().size(), "bands and part types arrive");
        WeaponMaterial a = WeaponData.material(id("magnatite")), b = back.materials().get(id("magnatite"));
        h.assertTrue(b != null && b.tag().equals(a.tag()) && b.band() == a.band() && b.colour() == a.colour() && b.kinds().equals(a.kinds()) && b.trait().equals(a.trait()), "Magnatite arrives whole");
        h.assertTrue(Math.abs(b.own(PartKind.BARREL).percentOf(Stat.RANGE) - a.own(PartKind.BARREL).percentOf(Stat.RANGE)) < 1e-4, "its own barrel modifiers arrive");
        h.assertTrue(Math.abs(back.bands().get(2).forKind(PartKind.CELL).percentOf(Stat.CAPACITY) - WeaponData.band(2).forKind(PartKind.CELL).percentOf(Stat.CAPACITY)) < 1e-4, "band 2's cell capacity arrives");
        h.assertTrue(back.augments().size() == WeaponData.augments().size() && back.augments().size() >= 9, "every augment arrives: " + back.augments().size());
        com.mrgregles.bsp_weapons.parts.Augment piercing = back.augments().get(id("piercing")), breacher = back.augments().get(id("breacher"));
        h.assertTrue(piercing != null && piercing.colour() == 0xFFD23A && piercing.durability() == 10 && piercing.available(), "Piercing arrives whole: " + piercing);
        h.assertTrue(Math.abs(piercing.modifiers().flatOf(Stat.PIERCING) - 10) < 1e-6 && Math.abs(piercing.modifiers().percentOf(Stat.PIERCING) - 15) < 1e-6, "its modifiers arrive");
        h.assertTrue(breacher != null && !breacher.available() && breacher.kinds().equals(java.util.Set.of(com.mrgregles.bsp_weapons.parts.WeaponKind.ENERGY)), "Breacher arrives unavailable and energy-only");
        h.assertTrue(bytes < 64 * 1024, "the packet stays small: " + bytes + " bytes");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "part_items")
    public void a_pistol_built_from_items_is_the_same_weapon_as_its_spec(GameTestHelper h) {
        List<PartSpec> parts = List.of(
                PartItem.specOf(ModItems.part(id("focused_barrel")).of(id("tetrium"), Rarity.NORMAL)),
                PartItem.specOf(ModItems.part(id("energy_cell")).of(id("steel"), Rarity.RARE)),
                PartItem.specOf(ModItems.part(id("standard_grip")).of(id("magnatite"), Rarity.NORMAL)),
                PartItem.specOf(ModItems.part(id("iron_sight")).of(id("tetrium"), Rarity.NORMAL)));
        int tier = CoreItem.tierOf(ModItems.WEAPON_CORE.get().of(3));
        WeaponSpec spec = new WeaponSpec(id("laser_pistol"), tier, parts, Rarity.NORMAL, List.of());
        h.assertTrue(Validation.isValid(spec), "the pistol from items validates: " + Validation.problems(spec));
        WeaponSpec direct = new WeaponSpec(id("laser_pistol"), 3, List.of(
                new PartSpec(PartKind.BARREL, id("focused_barrel"), id("tetrium"), Rarity.NORMAL),
                new PartSpec(PartKind.CELL, id("energy_cell"), id("steel"), Rarity.RARE),
                new PartSpec(PartKind.GRIP, id("standard_grip"), id("magnatite"), Rarity.NORMAL),
                new PartSpec(PartKind.OPTIC, id("iron_sight"), id("tetrium"), Rarity.NORMAL)), Rarity.NORMAL, List.of());
        h.assertTrue(spec.equals(direct), "items give the same spec as the hand-written one");
        h.assertTrue(SheetCalculator.compute(spec).get(Stat.DAMAGE) == SheetCalculator.compute(direct).get(Stat.DAMAGE), "and the same sheet");
        h.succeed();
    }
}
