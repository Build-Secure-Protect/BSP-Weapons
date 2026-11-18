package com.mrgregles.bsp_weapons.gametest;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.WeaponsConfig;
import com.mrgregles.bsp_weapons.data.WeaponData;
import com.mrgregles.bsp_weapons.enhancer.Enhancement;
import com.mrgregles.bsp_weapons.item.AugmentChipItem;
import com.mrgregles.bsp_weapons.item.WeaponItem;
import com.mrgregles.bsp_weapons.parts.Augment;
import com.mrgregles.bsp_weapons.parts.FittedChip;
import com.mrgregles.bsp_weapons.parts.PartKind;
import com.mrgregles.bsp_weapons.parts.PartSpec;
import com.mrgregles.bsp_weapons.parts.Rarity;
import com.mrgregles.bsp_weapons.parts.Sheet;
import com.mrgregles.bsp_weapons.parts.SheetCalculator;
import com.mrgregles.bsp_weapons.parts.Stat;
import com.mrgregles.bsp_weapons.parts.Validation;
import com.mrgregles.bsp_weapons.parts.WeaponSpec;
import com.mrgregles.bsp_weapons.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * WP-6b: the Augment Chip and the enhancer's chip rules (D71 to D73, D81, D83; R21, R23): the augment data, the chip
 * item and its tooltip, a chip's roll scaling its effect, fitting, removing, swapping, wear and burn-out, and the weapon
 * tooltip's chip lines. No block, screen or power: those come with the machine.
 */
@GameTestHolder(BspWeapons.MODID)
@PrefixGameTestTemplate(false)
public class EnhancementGameTests {
    private static final String EMPTY = SetupGameTests.EMPTY;

    private static ResourceLocation id(String path) {
        return new ResourceLocation(BspWeapons.MODID, path);
    }

    private static String key(Component c) {
        return c.getContents() instanceof TranslatableContents t ? t.getKey() : c.getString();
    }

    private static AugmentChipItem chips() {
        return ModItems.AUGMENT_CHIP.get();
    }

    /** A Tier {@code tier} pistol with a roll and chips. Tier I has one socket, Tier III two, Tier V three; S adds one. */
    private static WeaponSpec pistol(int tier, Rarity roll, List<FittedChip> fitted) {
        return new WeaponSpec(id("laser_pistol"), tier, List.of(
                new PartSpec(PartKind.BARREL, id("focused_barrel"), id("tetrium"), Rarity.NORMAL),
                new PartSpec(PartKind.CELL, id("energy_cell"), id("steel"), Rarity.NORMAL),
                new PartSpec(PartKind.GRIP, id("standard_grip"), id("magnatite"), Rarity.NORMAL),
                new PartSpec(PartKind.OPTIC, id("iron_sight"), id("tetrium"), Rarity.NORMAL)), roll, fitted);
    }

    private static boolean close(double a, double b) {
        return Math.abs(a - b) < 1e-6 * Math.max(1, Math.abs(b));
    }

    @GameTest(template = EMPTY, batch = "enhancement")
    public void the_augment_data_names_a_colour_points_and_whether_a_chip_is_available(GameTestHelper h) {
        Augment piercing = WeaponData.augment(id("piercing"));
        h.assertTrue(piercing != null && piercing.colour() == 0xFFD23A && piercing.durability() == 10 && piercing.available(), "Piercing: gold, 10 points, available");
        h.assertTrue(WeaponData.augment(id("overcharge")).durability() == 8, "Overcharge has 8 points");
        for (String n : new String[]{"breacher", "ricochet"}) {
            Augment a = WeaponData.augment(id(n));
            h.assertTrue(a != null && !a.available(), n + " stays in the data but is not available (D73)");
        }
        long available = WeaponData.augments().values().stream().filter(Augment::available).count();
        h.assertTrue(available == 7, "seven augments are available: " + available);
        h.assertTrue(piercing.nameKey().equals("augment.bsp_weapons.piercing"), "the name key: " + piercing.nameKey());
        h.assertTrue(WeaponData.augment(id("knockback")).push() == 1.0 && piercing.push() == 0, "Knockback pushes a block, Piercing none (D74)");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "enhancement")
    public void a_chip_remembers_its_augment_roll_and_points_and_is_named_and_tinted_by_the_augment(GameTestHelper h) {
        ItemStack stack = chips().of(id("coolant"), Rarity.EPIC);
        FittedChip chip = AugmentChipItem.chipOf(stack);
        h.assertTrue(chip.equals(new FittedChip(id("coolant"), Rarity.EPIC, 10)), "a fresh chip has the augment's points: " + chip);
        h.assertTrue(stack.getTag().getAllKeys().size() == 3, "three tag keys and nothing else: " + stack.getTag());
        h.assertTrue(AugmentChipItem.colourOf(stack) == WeaponData.augment(id("coolant")).colour(), "tinted the augment's colour");
        h.assertTrue(key(stack.getHoverName()).equals("item.bsp_weapons.augment_chip.named"), "named '<Augment> Chip': " + stack.getHoverName());
        ItemStack worn = chips().of(new FittedChip(id("coolant"), Rarity.EPIC, 4));
        h.assertTrue(AugmentChipItem.chipOf(worn).durability() == 4, "a chip out of a weapon keeps its points");
        h.assertTrue(AugmentChipItem.chipOf(chips().of(id("coolant"), Rarity.NORMAL, 9999)).durability() == FittedChip.MAX_DURABILITY, "points are clamped");
        // a bare stack: an unknown augment with no points, grey, plain name
        ItemStack bare = new ItemStack(chips());
        h.assertTrue(AugmentChipItem.chipOf(bare).augment().equals(id("unknown")) && AugmentChipItem.chipOf(bare).durability() == 0, "no tag is an unknown augment with no points");
        h.assertTrue(AugmentChipItem.colourOf(bare) == AugmentChipItem.UNKNOWN_COLOUR, "an unknown augment tints grey");
        h.assertTrue(key(bare.getHoverName()).equals("item.bsp_weapons.augment_chip"), "and keeps the plain name");
        h.assertTrue(AugmentChipItem.chipOf(new ItemStack(Items.STICK)) == null, "a stick is not a chip");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "enhancement")
    public void the_chip_tooltip_shows_the_roll_the_scaled_effects_the_points_and_what_it_fits(GameTestHelper h) {
        List<Component> lines = new ArrayList<>();
        chips().appendHoverText(chips().of(id("overcharge"), Rarity.S), null, lines, TooltipFlag.Default.NORMAL);
        h.assertTrue(key(lines.get(0)).equals("tooltip.bsp_weapons.chip"), "the first line is the roll: " + lines);
        h.assertTrue(lines.get(0).getString().contains("1.15"), "with the S multiplier: " + lines.get(0).getString());
        h.assertTrue(key(lines.get(1)).equals("stat.bsp_weapons.damage") && lines.get(1).getString().contains("+23%"), "damage +20 % scaled by 1.15: " + lines.get(1).getString());
        h.assertTrue(key(lines.get(2)).equals("stat.bsp_weapons.heat_cap") && lines.get(2).getString().contains("-23%"), "heat cap -20 % scaled: " + lines.get(2).getString());
        h.assertTrue(key(lines.get(3)).equals("tooltip.bsp_weapons.chip_durability"), "then the points: " + lines);
        h.assertTrue(key(lines.get(4)).equals("tooltip.bsp_weapons.chip_fits_any") && key(lines.get(5)).equals("tooltip.bsp_weapons.chip_wear"), "fits any weapon, and the wear line: " + lines);
        lines.clear();
        chips().appendHoverText(chips().of(id("breacher"), Rarity.NORMAL), null, lines, TooltipFlag.Default.NORMAL);
        h.assertTrue(lines.stream().anyMatch(l -> key(l).equals("tooltip.bsp_weapons.chip_not_yet")), "a Breacher chip says it cannot be fitted yet: " + lines);
        lines.clear();
        chips().appendHoverText(new ItemStack(chips()), null, lines, TooltipFlag.Default.NORMAL);
        h.assertTrue(lines.size() == 1 && key(lines.get(0)).equals("tooltip.bsp_weapons.chip_unknown"), "an unknown augment gives one line: " + lines);
        h.assertTrue(AugmentChipItem.multiplier(1.0).equals("1") && AugmentChipItem.multiplier(1.05).equals("1.05") && AugmentChipItem.multiplier(1.1).equals("1.1"), "multipliers print without trailing zeros");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "enhancement")
    public void a_chips_roll_scales_its_own_effect_by_the_part_multipliers(GameTestHelper h) {
        Sheet plain = SheetCalculator.compute(pistol(3, Rarity.NORMAL, List.of()));
        Sheet normal = SheetCalculator.compute(pistol(3, Rarity.NORMAL, List.of(new FittedChip(id("quick_draw"), Rarity.NORMAL, 10))));
        Sheet s = SheetCalculator.compute(pistol(3, Rarity.NORMAL, List.of(new FittedChip(id("quick_draw"), Rarity.S, 10))));
        h.assertTrue(close(normal.get(Stat.FIRE_RATE), plain.get(Stat.FIRE_RATE) * 1.15), "a Normal Quick-draw adds its 15 %");
        double mult = WeaponsConfig.rarityPartMultiplier(Rarity.S);
        h.assertTrue(close(s.get(Stat.FIRE_RATE), plain.get(Stat.FIRE_RATE) * (1 + 0.15 * mult)), "an S Quick-draw adds 15 % times " + mult + ": " + s.get(Stat.FIRE_RATE));
        h.assertTrue(close(s.get(Stat.DAMAGE), plain.get(Stat.DAMAGE)), "and touches nothing else");
        // a chip's points change nothing on the sheet
        Sheet worn = SheetCalculator.compute(pistol(3, Rarity.NORMAL, List.of(new FittedChip(id("quick_draw"), Rarity.S, 1))));
        h.assertTrue(close(worn.get(Stat.FIRE_RATE), s.get(Stat.FIRE_RATE)), "points do not change the effect");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "enhancement")
    public void fitting_spends_a_point_and_is_refused_for_the_right_reasons(GameTestHelper h) {
        WeaponSpec gun = pistol(3, Rarity.NORMAL, List.of()); // two sockets
        Enhancement.Result r = Enhancement.fit(gun, chips().of(id("piercing"), Rarity.RARE));
        h.assertTrue(r.ok(), "a Piercing chip fits: " + r.problem());
        h.assertTrue(r.spec().augments().equals(List.of(new FittedChip(id("piercing"), Rarity.RARE, 9))), "fitted with one point spent: " + r.spec().augments());
        h.assertTrue(Validation.isValid(r.spec()) && r.chip().isEmpty(), "the weapon is still valid and no chip comes back");
        WeaponSpec one = r.spec();
        h.assertTrue(key(Enhancement.fit(one, new ItemStack(Items.STICK)).problem()).equals("enhance.bsp_weapons.not_a_chip"), "a stick is not a chip");
        h.assertTrue(key(Enhancement.fit(one, new ItemStack(chips())).problem()).equals("enhance.bsp_weapons.unknown_chip"), "a bare chip names no augment");
        h.assertTrue(key(Enhancement.fit(one, chips().of(id("breacher"), Rarity.NORMAL)).problem()).equals("enhance.bsp_weapons.not_yet"), "Breacher is not yet fittable (D73)");
        h.assertTrue(key(Enhancement.fit(one, chips().of(id("piercing"), Rarity.NORMAL)).problem()).equals("enhance.bsp_weapons.already_fitted"), "the same augment twice is refused");
        h.assertTrue(key(Enhancement.fit(one, chips().of(id("coolant"), Rarity.NORMAL, 1)).problem()).equals("enhance.bsp_weapons.worn_out"), "a chip with one point left is too worn to fit");
        h.assertTrue(key(Enhancement.fit(one, chips().of(id("coolant"), Rarity.NORMAL, 0)).problem()).equals("enhance.bsp_weapons.worn_out"), "so is one with none");
        Enhancement.Result two = Enhancement.fit(one, chips().of(id("coolant"), Rarity.NORMAL, 2));
        h.assertTrue(two.ok() && two.spec().augments().get(1).durability() == 1, "a chip with two points fits and keeps one");
        h.assertTrue(key(Enhancement.fit(two.spec(), chips().of(id("stabiliser"), Rarity.NORMAL)).problem()).equals("enhance.bsp_weapons.full"), "a third chip finds no free socket");
        Enhancement.Result sTier = Enhancement.fit(pistol(3, Rarity.S, two.spec().augments()), chips().of(id("stabiliser"), Rarity.NORMAL));
        h.assertTrue(sTier.ok() && sTier.spec().augments().size() == 3, "an S roll's extra socket takes it");
        WeaponSpec broken = new WeaponSpec(id("laser_pistol"), 3, List.of(), Rarity.NORMAL, List.of());
        h.assertTrue(key(Enhancement.fit(broken, chips().of(id("piercing"), Rarity.NORMAL)).problem()).equals("enhance.bsp_weapons.invalid_weapon"), "a weapon with empty slots is refused before anything is touched");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "enhancement")
    public void removing_returns_the_chip_with_its_points_and_swapping_does_both(GameTestHelper h) {
        WeaponSpec gun = pistol(5, Rarity.NORMAL, List.of(new FittedChip(id("piercing"), Rarity.RARE, 9), new FittedChip(id("overcharge"), Rarity.NORMAL, 7)));
        Enhancement.Result out = Enhancement.remove(gun, 0);
        h.assertTrue(out.ok() && out.spec().augments().equals(List.of(new FittedChip(id("overcharge"), Rarity.NORMAL, 7))), "the Piercing chip leaves: " + out.spec().augments());
        h.assertTrue(AugmentChipItem.chipOf(out.chip()).equals(new FittedChip(id("piercing"), Rarity.RARE, 9)), "and comes back as it was: " + out.chip().getTag());
        h.assertTrue(key(Enhancement.remove(gun, 2).problem()).equals("enhance.bsp_weapons.no_chip") && key(Enhancement.remove(gun, -1).problem()).equals("enhance.bsp_weapons.no_chip"), "an empty or missing socket has no chip");
        // a swap: the old chip comes back whole, the new one goes in that socket a point down; the leaving chip's augment may come back in
        Enhancement.Result swapped = Enhancement.swap(gun, 1, chips().of(id("coolant"), Rarity.EPIC));
        h.assertTrue(swapped.ok(), "Coolant for Overcharge: " + swapped.problem());
        h.assertTrue(swapped.spec().augments().equals(List.of(new FittedChip(id("piercing"), Rarity.RARE, 9), new FittedChip(id("coolant"), Rarity.EPIC, 9))), "Coolant sits in socket 2: " + swapped.spec().augments());
        h.assertTrue(AugmentChipItem.chipOf(swapped.chip()).equals(new FittedChip(id("overcharge"), Rarity.NORMAL, 7)), "Overcharge comes back with its 7 points");
        h.assertTrue(Enhancement.swap(gun, 1, chips().of(id("overcharge"), Rarity.S)).ok(), "swapping a chip for a better roll of the same augment is allowed");
        h.assertTrue(key(Enhancement.swap(gun, 1, chips().of(id("piercing"), Rarity.S)).problem()).equals("enhance.bsp_weapons.already_fitted"), "but not for one fitted in another socket");
        h.assertTrue(key(Enhancement.swap(gun, 2, chips().of(id("coolant"), Rarity.NORMAL)).problem()).equals("enhance.bsp_weapons.no_chip"), "a swap needs a chip in the socket");
        h.assertTrue(key(Enhancement.swap(gun, 1, new ItemStack(Items.STICK)).problem()).equals("enhance.bsp_weapons.not_a_chip"), "and a chip in hand");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "enhancement")
    public void an_upgrade_or_repair_wears_every_chip_and_a_chip_at_zero_burns_out(GameTestHelper h) {
        WeaponSpec gun = pistol(5, Rarity.S, List.of(new FittedChip(id("piercing"), Rarity.RARE, 9), new FittedChip(id("overcharge"), Rarity.NORMAL, 1), new FittedChip(id("coolant"), Rarity.NORMAL, 2)));
        Enhancement.Worn worn = Enhancement.wear(gun);
        h.assertTrue(worn.spec().augments().equals(List.of(new FittedChip(id("piercing"), Rarity.RARE, 8), new FittedChip(id("coolant"), Rarity.NORMAL, 1))), "each chip is a point down and Overcharge is gone: " + worn.spec().augments());
        h.assertTrue(worn.burnt().size() == 1 && worn.burnt().get(0).augment().equals(id("overcharge")), "Overcharge is reported burnt out");
        h.assertTrue(Validation.isValid(worn.spec()), "the weapon stays valid");
        h.assertTrue(!Validation.isValid(pistol(5, Rarity.S, List.of(new FittedChip(id("piercing"), Rarity.RARE, 0)))), "a weapon carrying a burnt-out chip does not validate");
        h.assertTrue(Enhancement.wear(worn.spec()).burnt().size() == 1 && Enhancement.wear(Enhancement.wear(worn.spec()).spec()).burnt().isEmpty(), "Coolant goes next, then nothing is left to burn");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "enhancement")
    public void the_weapon_tooltip_lists_its_chips(GameTestHelper h) {
        WeaponItem pistol = ModItems.weapon(id("laser_pistol"));
        ItemStack stack = pistol.of(pistol(3, Rarity.NORMAL, List.of(new FittedChip(id("piercing"), Rarity.RARE, 9))));
        List<Component> lines = new ArrayList<>();
        pistol.appendHoverText(stack, null, lines, TooltipFlag.Default.NORMAL);
        h.assertTrue(lines.size() > 2 && key(lines.get(1)).equals("tooltip.bsp_weapons.weapon_chips"), "the second line counts the chips: " + lines);
        String chip = lines.get(2).getString();
        h.assertTrue(chip.contains("9 / 10"), "then each chip with its points: " + chip);
        WeaponSpec back = WeaponItem.specOf(stack);
        h.assertTrue(back.augments().equals(List.of(new FittedChip(id("piercing"), Rarity.RARE, 9))), "the chip survives the item's tag");
        h.succeed();
    }
}
