package com.mrgregles.bsp_weapons.gametest;

import com.mrgregles.bsp_weapons.WeaponsConfig;
import com.mrgregles.bsp_weapons.data.WeaponData;
import com.mrgregles.bsp_weapons.parts.Catalyst;
import com.mrgregles.bsp_weapons.parts.FittedChip;
import com.mrgregles.bsp_weapons.parts.PartKind;
import com.mrgregles.bsp_weapons.parts.PartSpec;
import com.mrgregles.bsp_weapons.parts.Pattern;
import com.mrgregles.bsp_weapons.parts.Rarity;
import com.mrgregles.bsp_weapons.parts.Rolls;
import com.mrgregles.bsp_weapons.parts.Sheet;
import com.mrgregles.bsp_weapons.parts.SheetCalculator;
import com.mrgregles.bsp_weapons.parts.Stat;
import com.mrgregles.bsp_weapons.parts.Validation;
import com.mrgregles.bsp_weapons.parts.WeaponMaterial;
import com.mrgregles.bsp_weapons.parts.WeaponSpec;
import com.mrgregles.bsp_weapons.BspWeapons;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The part system's data model (WP-1; owner decisions D5 to D36): the data loads, the sheet comes out in the right
 * order with the right direction for every stat, the rolls follow their odds, the spec survives its tag and ignores
 * anything else written there, and validation refuses what the server would never make.
 */
@GameTestHolder(BspWeapons.MODID)
@PrefixGameTestTemplate(false)
public class PartSystemGameTests {
    private static final String EMPTY = SetupGameTests.EMPTY;

    private static ResourceLocation id(String path) {
        return new ResourceLocation(BspWeapons.MODID, path);
    }

    private static PartSpec part(PartKind kind, String type, String material, Rarity rarity) {
        return new PartSpec(kind, id(type), id(material), rarity);
    }

    /** A plain Tier II Tetrium pistol: Focused barrel, Energy cell, Standard grip, Iron sight, every roll Normal. */
    /** A fresh Normal chip of the augment (D81: a Normal chip's effect is the augment's own). */
    private static FittedChip chip(String augment) {
        return new FittedChip(id(augment), Rarity.NORMAL, 10);
    }

    private static WeaponSpec pistol(int tier, Rarity roll, List<FittedChip> chips) {
        return new WeaponSpec(id("laser_pistol"), tier, List.of(
                part(PartKind.BARREL, "focused_barrel", "tetrium", Rarity.NORMAL),
                part(PartKind.CELL, "energy_cell", "steel", Rarity.NORMAL),
                part(PartKind.GRIP, "standard_grip", "magnatite", Rarity.NORMAL),
                part(PartKind.OPTIC, "iron_sight", "tetrium", Rarity.NORMAL)), roll, chips);
    }

    private static boolean close(double a, double b) {
        return Math.abs(a - b) < 1e-6 * Math.max(1, Math.abs(b));
    }

    @GameTest(template = EMPTY, batch = "parts")
    public void the_data_files_load_and_every_material_keys_to_a_forge_tag(GameTestHelper h) {
        h.assertTrue(WeaponData.pattern(id("laser_pistol")) != null, "the laser pistol pattern loaded");
        h.assertTrue(WeaponData.materials().size() >= 90, "at least 90 materials, got " + WeaponData.materials().size());
        h.assertTrue(WeaponData.bands().size() == 5, "five bands, got " + WeaponData.bands().size());
        h.assertTrue(WeaponData.partTypes().size() >= 12, "at least 12 part types");
        h.assertTrue(WeaponData.augments().size() >= 8, "at least 8 augments");
        h.assertTrue(WeaponData.catalysts().size() == 4, "four catalysts");
        for (WeaponMaterial m : WeaponData.materials().values()) {
            h.assertTrue(m.tag().getNamespace().equals("forge"), m.id() + " keys to a forge tag, not " + m.tag());
            h.assertTrue(m.tag().getPath().startsWith("ingots/") || m.tag().getPath().startsWith("gems/"), m.id() + " is an ingot or gem tag");
            h.assertTrue(m.band() >= 1 && m.band() <= 5, m.id() + " band in 1 to 5");
            h.assertTrue(!m.kinds().isEmpty(), m.id() + " can be made into something");
            if (m.tag().getPath().startsWith("gems/")) {
                h.assertTrue(m.kinds().equals(java.util.Set.of(PartKind.OPTIC)), m.id() + " is a gem, so an optic material only (D35)");
            }
        }
        Pattern p = WeaponData.pattern(id("laser_pistol"));
        h.assertTrue(p.slots().equals(List.of(PartKind.BARREL, PartKind.CELL, PartKind.GRIP, PartKind.OPTIC)), "the pistol's slots");
        for (Stat s : Stat.values()) h.assertTrue(p.base().has(s), "the pistol's base sheet sets " + s.id());
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "parts")
    public void the_sheet_follows_the_layers_in_order_and_each_stat_improves_the_right_way(GameTestHelper h) {
        Pattern p = WeaponData.pattern(id("laser_pistol"));
        Sheet bare = SheetCalculator.compute(new WeaponSpec(id("laser_pistol"), 1, List.of(), Rarity.NORMAL, List.of()));
        // tier I, no parts, Normal: the base sheet as written, since every multiplier is 1
        h.assertTrue(close(bare.get(Stat.DAMAGE), p.base().get(Stat.DAMAGE)), "bare damage is the base: " + bare.get(Stat.DAMAGE));
        h.assertTrue(close(bare.get(Stat.RECHARGE), p.base().get(Stat.RECHARGE)), "bare recharge is the base");
        // the core tier: higher-is-better grows, lower-is-better shrinks, by the config's factor
        Sheet t5 = SheetCalculator.compute(new WeaponSpec(id("laser_pistol"), 5, List.of(), Rarity.NORMAL, List.of()));
        double f = WeaponsConfig.tierMultiplier(5);
        h.assertTrue(close(t5.get(Stat.DAMAGE), p.base().get(Stat.DAMAGE) * f), "tier V damage is base x " + f + ": " + t5.get(Stat.DAMAGE));
        h.assertTrue(close(t5.get(Stat.RECHARGE), p.base().get(Stat.RECHARGE) / f), "tier V recharge is base / " + f + ": " + t5.get(Stat.RECHARGE));
        h.assertTrue(close(t5.get(Stat.SPREAD), p.base().get(Stat.SPREAD) / f), "tier V spread shrinks");
        // parts add on top: a Focused barrel of Tetrium (band 2: +8 % damage and range; own +6 % damage; type +20 % range, -30 % spread);
        // percentages add before they apply, so the barrel's damage change is +14 %
        Sheet withParts = SheetCalculator.compute(pistol(1, Rarity.NORMAL, List.of()));
        double expectedDamage = p.base().get(Stat.DAMAGE) * 1.14; // the cell, grip and optic touch no damage
        h.assertTrue(close(withParts.get(Stat.DAMAGE), expectedDamage), "damage with parts " + withParts.get(Stat.DAMAGE) + " expected " + expectedDamage);
        h.assertTrue(withParts.get(Stat.RANGE) > p.base().get(Stat.RANGE) * 1.2, "a Focused barrel adds range");
        h.assertTrue(withParts.get(Stat.SPREAD) < p.base().get(Stat.SPREAD), "a Focused barrel narrows the spread");
        h.assertTrue(withParts.get(Stat.DURABILITY) > p.base().get(Stat.DURABILITY), "band durability adds up over the parts");
        // the weapon's roll multiplies the finished sheet
        Sheet rare = SheetCalculator.compute(pistol(1, Rarity.RARE, List.of()));
        h.assertTrue(close(rare.get(Stat.DAMAGE), withParts.get(Stat.DAMAGE) * WeaponsConfig.rarityWeaponMultiplier(Rarity.RARE)), "Rare multiplies damage");
        h.assertTrue(close(rare.get(Stat.RECHARGE), withParts.get(Stat.RECHARGE) / WeaponsConfig.rarityWeaponMultiplier(Rarity.RARE)), "Rare shortens recharge");
        // chips come last, as a plain change on top of the roll
        Sheet chipped = SheetCalculator.compute(pistol(1, Rarity.RARE, List.of(chip("quick_draw"))));
        h.assertTrue(close(chipped.get(Stat.FIRE_RATE), rare.get(Stat.FIRE_RATE) * 1.15), "Quick-draw adds 15 % fire rate after the roll");
        h.assertTrue(close(chipped.get(Stat.DAMAGE), rare.get(Stat.DAMAGE)), "Quick-draw leaves damage alone");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "parts")
    public void a_parts_own_roll_scales_only_that_parts_change(GameTestHelper h) {
        WeaponSpec normal = pistol(2, Rarity.NORMAL, List.of());
        List<PartSpec> parts = new java.util.ArrayList<>(normal.parts());
        parts.set(0, part(PartKind.BARREL, "focused_barrel", "tetrium", Rarity.S));
        WeaponSpec sBarrel = new WeaponSpec(normal.pattern(), normal.coreTier(), parts, normal.roll(), normal.augments());
        Sheet a = SheetCalculator.compute(normal), b = SheetCalculator.compute(sBarrel);
        // the barrel is the only part that touches damage, so: before + (after - before) x 1.15
        double base = WeaponData.pattern(id("laser_pistol")).base().get(Stat.DAMAGE) * WeaponsConfig.tierMultiplier(2);
        double delta = base * 1.14 - base;
        h.assertTrue(close(b.get(Stat.DAMAGE), base + delta * WeaponsConfig.rarityPartMultiplier(Rarity.S)), "an S barrel keeps 115 % of its own damage change: " + b.get(Stat.DAMAGE));
        h.assertTrue(b.get(Stat.DAMAGE) > a.get(Stat.DAMAGE), "an S barrel beats a Normal one");
        h.assertTrue(close(b.get(Stat.CAPACITY), a.get(Stat.CAPACITY)), "the barrel's roll leaves the cell's capacity alone");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "parts")
    public void piercing_and_range_stop_at_their_caps(GameTestHelper h) {
        WeaponSpec spec = new WeaponSpec(id("laser_pistol"), 5, List.of(
                part(PartKind.BARREL, "focused_barrel", "unobtainium", Rarity.S),
                part(PartKind.CELL, "energy_cell", "steel", Rarity.NORMAL),
                part(PartKind.GRIP, "standard_grip", "steel", Rarity.NORMAL),
                part(PartKind.OPTIC, "iron_sight", "steel", Rarity.NORMAL)), Rarity.S,
                List.of(chip("piercing"), chip("piercing"), chip("piercing"), chip("piercing"), chip("piercing"), chip("piercing")));
        // six Piercing chips would reach about 100 % unclamped (validation refuses the doubled chips; the maths must still cap)
        Sheet s = SheetCalculator.compute(spec);
        h.assertTrue(close(s.get(Stat.PIERCING), WeaponsConfig.PIERCING_CAP.get()), "piercing stops at the cap: " + s.get(Stat.PIERCING));
        h.assertTrue(s.get(Stat.RANGE) <= WeaponsConfig.RANGE_CAP.get(), "range never passes the cap");
        for (Stat st : Stat.values()) h.assertTrue(s.get(st) >= 0, st.id() + " is never negative");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "parts")
    public void the_spec_survives_its_tag_and_the_sheet_ignores_anything_else_written_there(GameTestHelper h) {
        WeaponSpec spec = pistol(3, Rarity.EPIC, List.of(new FittedChip(id("coolant"), Rarity.RARE, 7), chip("stabiliser")));
        CompoundTag tag = spec.toTag();
        tag.putDouble("Damage", 9999); // a forged number: nothing reads it
        tag.putInt("Tier", 99);
        WeaponSpec back = WeaponSpec.fromTag(tag);
        h.assertTrue(back.equals(spec), "the spec round-trips through its tag: " + back);
        h.assertTrue(close(SheetCalculator.compute(back).get(Stat.DAMAGE), SheetCalculator.compute(spec).get(Stat.DAMAGE)), "the forged Damage changes nothing");
        // a forged tag with a thousand chips is cut at the cap, and validation refuses what is left; a chip's points are clamped
        CompoundTag forged = spec.toTag();
        net.minecraft.nbt.ListTag chips = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < 1000; i++) chips.add(new FittedChip(id("coolant"), Rarity.S, 9999).toTag());
        forged.put("Chips", chips);
        WeaponSpec many = WeaponSpec.fromTag(forged);
        h.assertTrue(many.augments().size() == WeaponSpec.MAX_AUGMENTS, "chips are capped at " + WeaponSpec.MAX_AUGMENTS);
        h.assertTrue(many.augments().get(0).durability() == FittedChip.MAX_DURABILITY, "a chip's points are clamped at " + FittedChip.MAX_DURABILITY);
        h.assertTrue(!Validation.isValid(many), "a weapon with more chips than slots is refused");
        // the old string list of chips reads as no chips at all
        CompoundTag old = spec.toTag();
        net.minecraft.nbt.ListTag strings = new net.minecraft.nbt.ListTag();
        strings.add(net.minecraft.nbt.StringTag.valueOf("bsp_weapons:coolant"));
        old.put("Chips", strings);
        h.assertTrue(WeaponSpec.fromTag(old).augments().isEmpty(), "a string chip list is ignored");
        // garbage in the tag is a spec that validation refuses, never an exception
        CompoundTag junk = new CompoundTag();
        junk.putString("Pattern", "not a location!!");
        junk.putInt("Core", -4);
        WeaponSpec j = WeaponSpec.fromTag(junk);
        h.assertTrue(!Validation.isValid(j), "junk is refused: " + Validation.problems(j));
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "parts")
    public void validation_refuses_what_the_server_would_never_make(GameTestHelper h) {
        h.assertTrue(Validation.isValid(pistol(2, Rarity.RARE, List.of(chip("coolant")))), "a proper pistol passes: " + Validation.problems(pistol(2, Rarity.RARE, List.of(chip("coolant")))));
        // an empty slot
        WeaponSpec noOptic = new WeaponSpec(id("laser_pistol"), 2, pistol(2, Rarity.NORMAL, List.of()).parts().subList(0, 3), Rarity.NORMAL, List.of());
        h.assertTrue(Validation.problems(noOptic).stream().anyMatch(s -> s.contains("optic slot is empty")), "an empty optic slot is named: " + Validation.problems(noOptic));
        // a type in the wrong slot
        List<PartSpec> wrong = new java.util.ArrayList<>(pistol(2, Rarity.NORMAL, List.of()).parts());
        wrong.set(1, part(PartKind.CELL, "focused_barrel", "steel", Rarity.NORMAL));
        h.assertTrue(Validation.problems(new WeaponSpec(id("laser_pistol"), 2, wrong, Rarity.NORMAL, List.of())).stream().anyMatch(s -> s.contains("is a barrel, not a cell")), "a barrel type in the cell slot is named");
        // a gem on a barrel (D35)
        List<PartSpec> gem = new java.util.ArrayList<>(pistol(2, Rarity.NORMAL, List.of()).parts());
        gem.set(0, part(PartKind.BARREL, "focused_barrel", "diamond", Rarity.NORMAL));
        h.assertTrue(Validation.problems(new WeaponSpec(id("laser_pistol"), 2, gem, Rarity.NORMAL, List.of())).stream().anyMatch(s -> s.contains("cannot be made of diamond")), "a diamond barrel is refused");
        // unknown ids
        List<PartSpec> unknown = new java.util.ArrayList<>(pistol(2, Rarity.NORMAL, List.of()).parts());
        unknown.set(2, part(PartKind.GRIP, "standard_grip", "cheese", Rarity.NORMAL));
        h.assertTrue(Validation.problems(new WeaponSpec(id("laser_pistol"), 2, unknown, Rarity.NORMAL, List.of())).stream().anyMatch(s -> s.contains("No material")), "an unknown material is named");
        h.assertTrue(!Validation.isValid(new WeaponSpec(id("plasma_sword"), 2, List.of(), Rarity.NORMAL, List.of())), "an unknown pattern is refused");
        // the tier, the chip slots and a doubled chip
        h.assertTrue(!Validation.isValid(pistol(6, Rarity.NORMAL, List.of())), "tier 6 is refused");
        h.assertTrue(!Validation.isValid(pistol(1, Rarity.NORMAL, List.of(chip("coolant"), chip("stabiliser")))), "two chips on a tier I core (one slot) are refused");
        h.assertTrue(Validation.isValid(pistol(1, Rarity.S, List.of(chip("coolant"), chip("stabiliser")))), "an S roll adds a slot, so two chips fit a tier I core");
        h.assertTrue(!Validation.isValid(pistol(5, Rarity.NORMAL, List.of(chip("coolant"), chip("coolant")))), "the same chip twice is refused");
        h.assertTrue(SheetCalculator.augmentSlots(pistol(5, Rarity.S, List.of())) == WeaponsConfig.tierAugmentSlots(5) + 1, "tier V S-tier slots");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "parts")
    public void rolls_follow_the_catalysts_odds(GameTestHelper h) {
        RandomSource r = RandomSource.create(20261117L);
        for (int i = 0; i < 100; i++) h.assertTrue(Rolls.roll(r, null) == Rarity.NORMAL, "no catalyst is always Normal");
        Map<Rarity, Integer> counts = count(r, WeaponData.catalyst(id("lapis_catalyst")), 4000);
        h.assertTrue(counts.get(Rarity.S) == 0, "lapis never gives S");
        double normalShare = counts.get(Rarity.NORMAL) / 4000.0;
        h.assertTrue(normalShare > 0.64 && normalShare < 0.76, "lapis gives Normal about 70 %: " + normalShare);
        counts = count(r, WeaponData.catalyst(id("illyrium_catalyst")), 4000);
        h.assertTrue(counts.get(Rarity.NORMAL) == 0 && counts.get(Rarity.RARE) == 0, "Illyrium gives Epic or S only (D21)");
        h.assertTrue(counts.get(Rarity.S) > 0, "Illyrium can give S");
        counts = count(r, WeaponData.catalyst(id("allthemodium_catalyst")), 4000);
        h.assertTrue(counts.get(Rarity.S) > counts.get(Rarity.EPIC), "Allthemodium gives S more often than Epic (D30)");
        h.succeed();
    }

    private static Map<Rarity, Integer> count(RandomSource r, Catalyst c, int n) {
        Map<Rarity, Integer> counts = new EnumMap<>(Rarity.class);
        for (Rarity x : Rarity.values()) counts.put(x, 0);
        for (int i = 0; i < n; i++) counts.merge(Rolls.roll(r, c), 1, Integer::sum);
        return counts;
    }
}
