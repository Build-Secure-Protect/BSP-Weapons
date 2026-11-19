package com.mrgregles.bsp_weapons.gametest;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.WeaponsConfig;
import com.mrgregles.bsp_weapons.data.WeaponData;
import com.mrgregles.bsp_weapons.enhancer.MachineEnergy;
import com.mrgregles.bsp_weapons.fabricator.Fabricating;
import com.mrgregles.bsp_weapons.fabricator.FabricatingRecipe;
import com.mrgregles.bsp_weapons.fabricator.Fabrication;
import com.mrgregles.bsp_weapons.fabricator.Job;
import com.mrgregles.bsp_weapons.item.AugmentChipItem;
import com.mrgregles.bsp_weapons.item.CoreItem;
import com.mrgregles.bsp_weapons.item.PartItem;
import com.mrgregles.bsp_weapons.parts.Augment;
import com.mrgregles.bsp_weapons.parts.FittedChip;
import com.mrgregles.bsp_weapons.parts.Need;
import com.mrgregles.bsp_weapons.parts.PartKind;
import com.mrgregles.bsp_weapons.parts.PartType;
import com.mrgregles.bsp_weapons.parts.Pattern;
import com.mrgregles.bsp_weapons.parts.Rarity;
import com.mrgregles.bsp_weapons.registry.ModItems;
import com.mrgregles.bsp_weapons.registry.ModRecipes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Optional;

/**
 * The Weapon Fabricator's data and rules (WP-7b; D100 to D104, D112 to D122; R27, R30): the costs, ingredients, signatures and
 * recipes load; the catalogue lists what this server can make; parts, chips, engrams, Cores, catalysts and rounds are made from the
 * right inputs with the right charge and roll; the refusals; the time; the state's tag; the Blank Engram's table recipe.
 */
@GameTestHolder(BspWeapons.MODID)
@PrefixGameTestTemplate(false)
public class FabricatorGameTests {
    private static final String EMPTY = SetupGameTests.EMPTY;

    private static ResourceLocation id(String path) {
        return new ResourceLocation(BspWeapons.MODID, path);
    }

    private static String key(Component c) {
        return c == null ? "null" : c.getContents() instanceof TranslatableContents t ? t.getKey() : c.getString();
    }

    private static String fabricate(String key) {
        return "fabricate." + BspWeapons.MODID + "." + key;
    }

    private static ItemStackHandler inv() {
        return new ItemStackHandler(Fabricating.SLOTS);
    }

    private static MachineEnergy charged() {
        return new MachineEnergy(20000, 1000, 20000);
    }

    /** An item of BSP-Core's, which the dev run loads: {@code tetrium_ingot}, {@code magnatite_ingot}. */
    private static ItemStack core(GameTestHelper h, String name, int count) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("bsp_core", name));
        if (item == null || item == Items.AIR) h.fail("bsp_core:" + name + " is not registered in the test run");
        return new ItemStack(item, count);
    }

    private static ItemStack catalyst(int index, int count) {
        return new ItemStack(ModItems.CATALYST_ITEMS.get(index).get(), count);
    }

    /** Starts a job and fails the test with the refusal if it does not start. */
    private static void start(GameTestHelper h, Job job, ItemStackHandler inv, MachineEnergy energy, Fabrication state) {
        Component why = Fabricating.make(job, inv, energy, state, h.getLevel(), h.getLevel().random);
        if (why != null) h.fail(job + " refused: " + why.getString() + " (" + key(why) + ")");
        h.assertTrue(state.working(), job + " is under way");
    }

    /** Ticks the job to its end and returns what reached OUT. */
    private static ItemStack finish(GameTestHelper h, ItemStackHandler inv, Fabrication state) {
        int ticks = Fabricating.ticks();
        for (int i = 1; i < ticks; i++) {
            h.assertTrue(!Fabricating.tick(state, inv), "nothing reaches OUT before the time is up (tick " + i + ")");
            h.assertTrue(inv.getStackInSlot(Fabricating.OUT).isEmpty(), "OUT stays empty while the job runs");
        }
        h.assertTrue(Fabricating.tick(state, inv), "the product reaches OUT at tick " + ticks);
        h.assertTrue(!state.working(), "the machine is idle after the product is placed");
        return inv.getStackInSlot(Fabricating.OUT);
    }

    private static boolean has(List<Job> jobs, Job job) {
        return jobs.contains(job);
    }

    @GameTest(template = EMPTY, batch = "fabricator")
    public void the_data_names_costs_ingredients_signatures_and_recipes(GameTestHelper h) {
        Level level = h.getLevel();
        for (String[] t : ModItems.PART_TYPES) {
            PartType type = WeaponData.partType(id(t[0]));
            h.assertTrue(type != null, t[0] + " loads");
            int expect = switch (type.kind()) { case BARREL -> 3; case OPTIC -> 1; default -> 2; };
            h.assertTrue(type.cost() == expect, t[0] + " costs " + expect + ", got " + type.cost());
        }
        for (Augment a : WeaponData.augments().values()) {
            h.assertTrue(a.ingredients().size() == 2, a.id() + " names two ingredients, got " + a.ingredients().size());
            Need gem = a.ingredients().get(0), redstone = a.ingredients().get(1);
            h.assertTrue(gem.tag() && gem.id().getPath().startsWith("gems/") && gem.count() == 1, a.id() + " takes one gem by tag, got " + gem);
            h.assertTrue(redstone.tag() && redstone.id().equals(new ResourceLocation("forge", "dusts/redstone")) && redstone.count() == 2, a.id() + " takes two redstone, got " + redstone);
        }
        Pattern pistol = WeaponData.pattern(id("laser_pistol"));
        h.assertTrue(pistol.signature() != null && pistol.signature().equals(Need.ofTag("forge:ingots/tetrium", 4)), "the Laser Pistol's signature is four Tetrium, got " + pistol.signature());
        List<FabricatingRecipe> recipes = level.getRecipeManager().getAllRecipesFor(ModRecipes.FABRICATING.get());
        h.assertTrue(recipes.size() == 11, "11 fabricating recipes (one engram, five Cores, four catalysts, rounds), got " + recipes.size());
        int engrams = 0, cores = 0, catalysts = 0, rounds = 0;
        for (FabricatingRecipe r : recipes) {
            switch (r.kind()) { case FabricatingRecipe.ENGRAM -> engrams++; case FabricatingRecipe.CORE -> cores++; case FabricatingRecipe.CATALYST -> catalysts++; case FabricatingRecipe.ROUND -> rounds++; default -> h.fail("unexpected kind " + r.kind()); }
        }
        h.assertTrue(engrams == 1 && cores == 5 && catalysts == 4 && rounds == 1, "kinds: " + engrams + " engram, " + cores + " cores, " + catalysts + " catalysts, " + rounds + " rounds");
        FabricatingRecipe core3 = Fabricating.recipe(level, id("weapon_core_3"));
        h.assertTrue(core3 != null && core3.material().equals(Need.ofTag("forge:ingots/magnatite", 4)) && Need.ofTag("forge:gems/diamond", 1).equals(core3.extra())
                && core3.result().getItem() instanceof CoreItem && CoreItem.tierOf(core3.result()) == 3, "Core III: four Magnatite and a diamond make a Tier III Core");
        FabricatingRecipe engram = Fabricating.recipe(level, id("laser_pistol_engram"));
        h.assertTrue(engram != null && engram.material().equals(pistol.signature()) && Need.ofItem("bsp_weapons:blank_engram", 1).equals(engram.extra())
                && engram.result().is(ModItems.ENGRAMS.get(0).get()), "the engram recipe takes the signature and a Blank Engram");
        FabricatingRecipe round = Fabricating.recipe(level, id("energy_round"));
        h.assertTrue(round != null && round.result().is(ModItems.ENERGY_ROUND.get()) && round.result().getCount() == 8, "one round job makes eight rounds");
        h.assertTrue(level.getRecipeManager().byKey(id("blank_engram")).isPresent(), "the Blank Engram's table recipe loads");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "fabricator")
    public void the_catalogue_lists_what_this_server_can_make(GameTestHelper h) {
        Level level = h.getLevel();
        List<Job> parts = Fabricating.catalogue(level, Job.Tab.PARTS);
        h.assertTrue(parts.size() == ModItems.PART_TYPES.length, "every part type is a row, got " + parts.size());
        for (int i = 0; i < parts.size(); i++) h.assertTrue(parts.get(i).equals(Job.part(id(ModItems.PART_TYPES[i][0]))), "row " + i + " is " + ModItems.PART_TYPES[i][0]);
        List<Job> chips = Fabricating.catalogue(level, Job.Tab.CHIPS);
        h.assertTrue(chips.size() == 7, "seven chips can be made (D73), got " + chips.size());
        h.assertTrue(!has(chips, Job.chip(id("breacher"))) && !has(chips, Job.chip(id("ricochet"))), "Breacher and Ricochet are not listed");
        h.assertTrue(has(chips, Job.chip(id("piercing"))) && has(chips, Job.chip(id("knockback"))), "Piercing and Knockback are");
        List<Job> engrams = Fabricating.catalogue(level, Job.Tab.ENGRAM);
        h.assertTrue(engrams.equals(List.of(Job.recipe(id("laser_pistol_engram")))), "the ENGRAM tab holds the Laser Pistol Engram alone, got " + engrams);
        List<Job> more = Fabricating.catalogue(level, Job.Tab.MORE);
        for (int tier = 1; tier <= 5; tier++) {
            FabricatingRecipe r = Fabricating.recipe(level, id("weapon_core_" + tier));
            h.assertTrue(has(more, Job.recipe(r.id())) == r.obtainable(), "Core " + tier + " is listed exactly when its items exist here (D122)");
        }
        h.assertTrue(has(more, Job.recipe(id("weapon_core_1"))) && has(more, Job.recipe(id("weapon_core_2"))) && has(more, Job.recipe(id("weapon_core_3"))), "Cores I to III are listed (iron, Tetrium, Magnatite and diamond exist here)");
        h.assertTrue(has(more, Job.recipe(id("lapis_catalyst"))) && has(more, Job.recipe(id("magnatite_catalyst"))) && has(more, Job.recipe(id("illyrium_catalyst"))), "the three catalysts of this server's metals are listed");
        h.assertTrue(has(more, Job.recipe(id("allthemodium_catalyst"))) == Need.ofTag("forge:ingots/allthemodium", 1).obtainable(), "the Allthemodium Catalyst is listed exactly when Allthemodium exists here");
        h.assertTrue(has(more, Job.recipe(id("energy_round"))), "rounds are listed");
        h.assertTrue(more.indexOf(Job.recipe(id("weapon_core_1"))) < more.indexOf(Job.recipe(id("lapis_catalyst"))) && more.indexOf(Job.recipe(id("lapis_catalyst"))) < more.indexOf(Job.recipe(id("energy_round"))), "MORE runs Cores, catalysts, rounds");
        List<Job> all = Fabricating.catalogue(level);
        h.assertTrue(all.size() == parts.size() + chips.size() + engrams.size() + more.size(), "the tabs together are the whole catalogue");
        h.assertTrue(!has(all, Job.chip(id("breacher"))) && !has(all, Job.part(id("nothing"))), "no unavailable or unknown job");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "fabricator")
    public void a_part_is_made_from_any_tagged_ingot_and_rolls_with_the_catalyst(GameTestHelper h) {
        ItemStackHandler inv = inv();
        MachineEnergy energy = charged();
        Fabrication state = new Fabrication();
        Job barrel = Job.part(id("focused_barrel"));
        inv.setStackInSlot(Fabricating.MATERIAL, new ItemStack(Items.IRON_INGOT, 5));
        inv.setStackInSlot(Fabricating.ROLL, catalyst(3, 2)); // Allthemodium: Epic or S, never Normal
        start(h, barrel, inv, energy, state);
        h.assertTrue(inv.getStackInSlot(Fabricating.MATERIAL).getCount() == 2, "three iron spent, got " + inv.getStackInSlot(Fabricating.MATERIAL).getCount() + " left");
        h.assertTrue(inv.getStackInSlot(Fabricating.ROLL).getCount() == 1, "one catalyst spent");
        h.assertTrue(energy.getEnergyStored() == 20000 - WeaponsConfig.PART_CHARGE.get(), "the part charge is drawn at the start, got " + energy.getEnergyStored());
        h.assertTrue(inv.getStackInSlot(Fabricating.OUT).isEmpty(), "OUT is still empty");
        ItemStack made = finish(h, inv, state);
        h.assertTrue(made.getItem() == ModItems.part(id("focused_barrel")), "a Focused Barrel came out, got " + made);
        h.assertTrue(PartItem.materialOf(made).equals(id("iron")), "of iron, got " + PartItem.materialOf(made));
        Rarity roll = PartItem.rarityOf(made);
        h.assertTrue(roll == Rarity.EPIC || roll == Rarity.S, "the Allthemodium Catalyst rolls Epic or S, got " + roll);
        // no catalyst: Normal; a BSP-Core ingot is found by its tag
        inv.setStackInSlot(Fabricating.OUT, ItemStack.EMPTY);
        inv.setStackInSlot(Fabricating.ROLL, ItemStack.EMPTY);
        inv.setStackInSlot(Fabricating.MATERIAL, core(h, "tetrium_ingot", 3));
        start(h, barrel, inv, energy, state);
        h.assertTrue(inv.getStackInSlot(Fabricating.MATERIAL).isEmpty(), "exactly three Tetrium spent");
        made = finish(h, inv, state);
        h.assertTrue(PartItem.materialOf(made).equals(id("tetrium")) && PartItem.rarityOf(made) == Rarity.NORMAL, "a Normal Tetrium barrel, got " + made.getTag());
        // a gem makes an optic for one gem
        inv.setStackInSlot(Fabricating.OUT, ItemStack.EMPTY);
        inv.setStackInSlot(Fabricating.MATERIAL, new ItemStack(Items.DIAMOND, 1));
        start(h, Job.part(id("scope")), inv, energy, state);
        h.assertTrue(inv.getStackInSlot(Fabricating.MATERIAL).isEmpty(), "one diamond spent");
        made = finish(h, inv, state);
        h.assertTrue(made.getItem() == ModItems.part(id("scope")) && PartItem.materialOf(made).equals(id("diamond")), "a diamond Scope, got " + made);
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "fabricator")
    public void a_material_the_kind_refuses_too_little_and_no_material_are_refused(GameTestHelper h) {
        ItemStackHandler inv = inv();
        MachineEnergy energy = charged();
        Fabrication state = new Fabrication();
        Level level = h.getLevel();
        Job barrel = Job.part(id("focused_barrel"));
        h.assertTrue(key(Fabricating.cannotMake(barrel, inv, energy, state, level)).equals(fabricate("no_material")), "an empty MATERIAL slot: no_material");
        inv.setStackInSlot(Fabricating.MATERIAL, new ItemStack(Items.DIAMOND, 4));
        h.assertTrue(key(Fabricating.cannotMake(barrel, inv, energy, state, level)).equals(fabricate("wrong_kind")), "a gem cannot be a barrel (D35): wrong_kind");
        inv.setStackInSlot(Fabricating.MATERIAL, new ItemStack(Items.STICK, 4));
        h.assertTrue(key(Fabricating.cannotMake(barrel, inv, energy, state, level)).equals(fabricate("not_a_material")), "a stick is no weapon material: not_a_material");
        inv.setStackInSlot(Fabricating.MATERIAL, new ItemStack(Items.IRON_INGOT, 2));
        h.assertTrue(key(Fabricating.cannotMake(barrel, inv, energy, state, level)).equals(fabricate("too_few")), "two iron for a barrel of three: too_few");
        h.assertTrue(Fabricating.make(barrel, inv, energy, state, level, level.random) != null && inv.getStackInSlot(Fabricating.MATERIAL).getCount() == 2 && energy.getEnergyStored() == 20000 && !state.working(), "a refused job spends nothing");
        inv.setStackInSlot(Fabricating.MATERIAL, new ItemStack(Items.IRON_INGOT, 3));
        h.assertTrue(Fabricating.cannotMake(barrel, inv, energy, state, level) == null, "three iron make a barrel");
        h.assertTrue(key(Fabricating.cannotMake(Job.part(id("nothing")), inv, energy, state, level)).equals(fabricate("no_job")), "an unknown job: no_job");
        h.assertTrue(key(Fabricating.cannotMake(null, inv, energy, state, level)).equals(fabricate("no_job")), "no job chosen: no_job");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "fabricator")
    public void a_chip_comes_from_its_gem_and_redstone(GameTestHelper h) {
        ItemStackHandler inv = inv();
        MachineEnergy energy = charged();
        Fabrication state = new Fabrication();
        Level level = h.getLevel();
        Job piercing = Job.chip(id("piercing"));
        h.assertTrue(key(Fabricating.cannotMake(piercing, inv, energy, state, level)).equals(fabricate("need_material")), "no diamond: need_material");
        inv.setStackInSlot(Fabricating.MATERIAL, new ItemStack(Items.DIAMOND, 2));
        h.assertTrue(key(Fabricating.cannotMake(piercing, inv, energy, state, level)).equals(fabricate("need_extra")), "no redstone: need_extra");
        inv.setStackInSlot(Fabricating.EXTRA, new ItemStack(Items.REDSTONE, 1));
        h.assertTrue(key(Fabricating.cannotMake(piercing, inv, energy, state, level)).equals(fabricate("need_extra")), "one redstone of two: need_extra");
        inv.setStackInSlot(Fabricating.EXTRA, new ItemStack(Items.REDSTONE, 5));
        start(h, piercing, inv, energy, state);
        h.assertTrue(inv.getStackInSlot(Fabricating.MATERIAL).getCount() == 1 && inv.getStackInSlot(Fabricating.EXTRA).getCount() == 3, "one diamond and two redstone spent");
        h.assertTrue(energy.getEnergyStored() == 20000 - WeaponsConfig.CHIP_CHARGE.get(), "the chip charge is drawn");
        ItemStack made = finish(h, inv, state);
        FittedChip chip = AugmentChipItem.chipOf(made);
        h.assertTrue(chip != null && chip.augment().equals(id("piercing")) && chip.rarity() == Rarity.NORMAL && chip.durability() == WeaponData.augment(id("piercing")).durability(), "a fresh Normal Piercing Chip, got " + made.getTag());
        // with an Illyrium Catalyst the chip rolls Epic or S
        inv.setStackInSlot(Fabricating.OUT, ItemStack.EMPTY);
        inv.setStackInSlot(Fabricating.ROLL, catalyst(2, 1));
        start(h, piercing, inv, energy, state);
        h.assertTrue(inv.getStackInSlot(Fabricating.ROLL).isEmpty(), "the catalyst is spent on a chip's roll");
        made = finish(h, inv, state);
        Rarity roll = AugmentChipItem.chipOf(made).rarity();
        h.assertTrue(roll == Rarity.EPIC || roll == Rarity.S, "an Illyrium roll is Epic or S, got " + roll);
        // an unavailable augment is no job
        h.assertTrue(key(Fabricating.cannotMake(Job.chip(id("breacher")), inv, energy, state, level)).equals(fabricate("no_job")), "Breacher cannot be made yet (D73)");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "fabricator")
    public void an_engram_comes_from_a_blank_and_four_tetrium(GameTestHelper h) {
        ItemStackHandler inv = inv();
        MachineEnergy energy = charged();
        Fabrication state = new Fabrication();
        Level level = h.getLevel();
        Job engram = Job.recipe(id("laser_pistol_engram"));
        inv.setStackInSlot(Fabricating.MATERIAL, core(h, "tetrium_ingot", 3));
        inv.setStackInSlot(Fabricating.EXTRA, new ItemStack(ModItems.BLANK_ENGRAM.get(), 2));
        h.assertTrue(key(Fabricating.cannotMake(engram, inv, energy, state, level)).equals(fabricate("need_material")), "three Tetrium of four: need_material");
        inv.setStackInSlot(Fabricating.MATERIAL, core(h, "tetrium_ingot", 4));
        inv.setStackInSlot(Fabricating.EXTRA, ItemStack.EMPTY);
        h.assertTrue(key(Fabricating.cannotMake(engram, inv, energy, state, level)).equals(fabricate("need_extra")), "no blank: need_extra");
        inv.setStackInSlot(Fabricating.EXTRA, new ItemStack(Items.IRON_INGOT, 1));
        h.assertTrue(key(Fabricating.cannotMake(engram, inv, energy, state, level)).equals(fabricate("need_extra")), "an ingot is no blank: need_extra");
        inv.setStackInSlot(Fabricating.EXTRA, new ItemStack(ModItems.BLANK_ENGRAM.get(), 2));
        start(h, engram, inv, energy, state);
        h.assertTrue(inv.getStackInSlot(Fabricating.MATERIAL).isEmpty() && inv.getStackInSlot(Fabricating.EXTRA).getCount() == 1, "four Tetrium and one blank spent");
        h.assertTrue(energy.getEnergyStored() == 20000 - WeaponsConfig.ENGRAM_CHARGE.get(), "the engram charge is drawn");
        ItemStack made = finish(h, inv, state);
        h.assertTrue(made.is(ModItems.ENGRAMS.get(0).get()) && made.getCount() == 1, "a Laser Pistol Engram came out, got " + made);
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "fabricator")
    public void cores_catalysts_and_rounds_come_from_their_recipes(GameTestHelper h) {
        ItemStackHandler inv = inv();
        MachineEnergy energy = charged();
        Fabrication state = new Fabrication();
        Level level = h.getLevel();
        // Core I: four iron and two redstone
        inv.setStackInSlot(Fabricating.MATERIAL, new ItemStack(Items.IRON_INGOT, 4));
        inv.setStackInSlot(Fabricating.EXTRA, new ItemStack(Items.REDSTONE, 2));
        FabricatingRecipe core1 = Fabricating.recipe(level, id("weapon_core_1"));
        h.assertTrue(core1.matches(new RecipeWrapper(inv), level), "the recipe matches the machine's slots as a container");
        start(h, Job.recipe(core1.getId()), inv, energy, state);
        h.assertTrue(inv.getStackInSlot(Fabricating.MATERIAL).isEmpty() && inv.getStackInSlot(Fabricating.EXTRA).isEmpty(), "the iron and redstone are spent");
        h.assertTrue(energy.getEnergyStored() == 20000 - WeaponsConfig.CORE_CHARGE_PER_TIER.get(), "Core I draws one tier's charge, got " + (20000 - energy.getEnergyStored()));
        ItemStack made = finish(h, inv, state);
        h.assertTrue(made.getItem() instanceof CoreItem && CoreItem.tierOf(made) == 1 && made.getCount() == 1, "a Tier I Core, got " + made);
        h.assertTrue(!core1.matches(new RecipeWrapper(inv), level), "and no longer matches the emptied slots");
        // Core III: four Magnatite and a diamond, three tiers' charge
        inv.setStackInSlot(Fabricating.OUT, ItemStack.EMPTY);
        energy.set(20000);
        inv.setStackInSlot(Fabricating.MATERIAL, core(h, "magnatite_ingot", 4));
        inv.setStackInSlot(Fabricating.EXTRA, new ItemStack(Items.DIAMOND, 1));
        start(h, Job.recipe(id("weapon_core_3")), inv, energy, state);
        h.assertTrue(energy.getEnergyStored() == 20000 - 3 * WeaponsConfig.CORE_CHARGE_PER_TIER.get(), "Core III draws three tiers' charge");
        made = finish(h, inv, state);
        h.assertTrue(CoreItem.tierOf(made) == 3, "a Tier III Core, got " + made.getTag());
        // a Lapis Catalyst: two lapis and a redstone
        inv.setStackInSlot(Fabricating.OUT, ItemStack.EMPTY);
        energy.set(20000);
        inv.setStackInSlot(Fabricating.MATERIAL, new ItemStack(Items.LAPIS_LAZULI, 3));
        inv.setStackInSlot(Fabricating.EXTRA, new ItemStack(Items.REDSTONE, 1));
        start(h, Job.recipe(id("lapis_catalyst")), inv, energy, state);
        h.assertTrue(inv.getStackInSlot(Fabricating.MATERIAL).getCount() == 1 && inv.getStackInSlot(Fabricating.EXTRA).isEmpty(), "two lapis and the redstone spent");
        h.assertTrue(energy.getEnergyStored() == 20000 - WeaponsConfig.CATALYST_CHARGE.get(), "the catalyst charge is drawn");
        made = finish(h, inv, state);
        h.assertTrue(made.is(ModItems.CATALYST_ITEMS.get(0).get()) && made.getCount() == 1, "a Lapis Catalyst, got " + made);
        // rounds: an iron and a redstone make eight
        inv.setStackInSlot(Fabricating.OUT, ItemStack.EMPTY);
        energy.set(20000);
        inv.setStackInSlot(Fabricating.MATERIAL, new ItemStack(Items.IRON_INGOT, 1));
        inv.setStackInSlot(Fabricating.EXTRA, new ItemStack(Items.REDSTONE, 1));
        start(h, Job.recipe(id("energy_round")), inv, energy, state);
        h.assertTrue(energy.getEnergyStored() == 20000 - WeaponsConfig.ROUND_CHARGE.get(), "the round charge is drawn");
        made = finish(h, inv, state);
        h.assertTrue(made.is(ModItems.ENERGY_ROUND.get()) && made.getCount() == 8, "eight Energy Rounds, got " + made);
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "fabricator")
    public void no_power_out_full_and_a_running_job_refuse_and_a_stray_catalyst_is_left_alone(GameTestHelper h) {
        ItemStackHandler inv = inv();
        MachineEnergy energy = new MachineEnergy(20000, 1000, WeaponsConfig.PART_CHARGE.get() - 1);
        Fabrication state = new Fabrication();
        Level level = h.getLevel();
        Job barrel = Job.part(id("focused_barrel"));
        inv.setStackInSlot(Fabricating.MATERIAL, new ItemStack(Items.IRON_INGOT, 6));
        h.assertTrue(key(Fabricating.cannotMake(barrel, inv, energy, state, level)).equals(fabricate("no_power")), "one FE short: no_power");
        energy.set(20000);
        inv.setStackInSlot(Fabricating.OUT, new ItemStack(Items.STICK));
        h.assertTrue(key(Fabricating.cannotMake(barrel, inv, energy, state, level)).equals(fabricate("out_full")), "anything in OUT: out_full (R30, D120)");
        inv.setStackInSlot(Fabricating.OUT, ItemStack.EMPTY);
        start(h, barrel, inv, energy, state);
        h.assertTrue(key(Fabricating.cannotMake(barrel, inv, energy, state, level)).equals(fabricate("busy")), "a second job while one runs: busy");
        h.assertTrue(inv.getStackInSlot(Fabricating.MATERIAL).getCount() == 3, "the refused second job spent nothing");
        finish(h, inv, state);
        // a product that cannot be placed waits for OUT to empty
        inv.setStackInSlot(Fabricating.OUT, ItemStack.EMPTY);
        start(h, barrel, inv, energy, state);
        inv.setStackInSlot(Fabricating.OUT, new ItemStack(Items.STICK));
        for (int i = 0; i < Fabricating.ticks() + 5; i++) h.assertTrue(!Fabricating.tick(state, inv), "nothing is placed over a stick");
        h.assertTrue(state.working() && state.ticksLeft() == 0, "the product waits, time up");
        inv.setStackInSlot(Fabricating.OUT, ItemStack.EMPTY);
        h.assertTrue(Fabricating.tick(state, inv) && inv.getStackInSlot(Fabricating.OUT).getItem() == ModItems.part(id("focused_barrel")), "and is placed once OUT is empty");
        // a catalyst in ROLL on a Core job is left alone (D121)
        inv.setStackInSlot(Fabricating.OUT, ItemStack.EMPTY);
        inv.setStackInSlot(Fabricating.MATERIAL, new ItemStack(Items.IRON_INGOT, 4));
        inv.setStackInSlot(Fabricating.EXTRA, new ItemStack(Items.REDSTONE, 2));
        inv.setStackInSlot(Fabricating.ROLL, catalyst(0, 1));
        start(h, Job.recipe(id("weapon_core_1")), inv, energy, state);
        h.assertTrue(inv.getStackInSlot(Fabricating.ROLL).getCount() == 1, "the catalyst is not spent on a job that rolls nothing");
        ItemStack made = finish(h, inv, state);
        h.assertTrue(made.getItem() instanceof CoreItem && !made.getOrCreateTag().contains(PartItem.RARITY), "a Core carries no roll");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "fabricator")
    public void the_state_survives_its_tag(GameTestHelper h) {
        ItemStackHandler inv = inv();
        MachineEnergy energy = charged();
        Fabrication state = new Fabrication();
        Job barrel = Job.part(id("focused_barrel"));
        state.choose(barrel);
        inv.setStackInSlot(Fabricating.MATERIAL, new ItemStack(Items.IRON_INGOT, 3));
        start(h, barrel, inv, energy, state);
        for (int i = 0; i < 30; i++) Fabricating.tick(state, inv);
        CompoundTag saved = state.save();
        Fabrication loaded = new Fabrication();
        loaded.load(saved);
        h.assertTrue(barrel.equals(loaded.chosen()), "the chosen row is kept (R27), got " + loaded.chosen());
        h.assertTrue(loaded.working() && ItemStack.matches(loaded.product(), state.product()), "the product under way is kept");
        h.assertTrue(loaded.ticksLeft() == Fabricating.ticks() - 30 && loaded.ticksTotal() == Fabricating.ticks(), "the ticks are kept, got " + loaded.ticksLeft() + " of " + loaded.ticksTotal());
        h.assertTrue(Math.abs(loaded.progress() - 30f / Fabricating.ticks()) < 1e-6, "progress reads from the ticks");
        for (int i = 1; i < Fabricating.ticks() - 30; i++) h.assertTrue(!Fabricating.tick(loaded, inv), "the loaded job runs on");
        h.assertTrue(Fabricating.tick(loaded, inv) && inv.getStackInSlot(Fabricating.OUT).getItem() == ModItems.part(id("focused_barrel")), "and finishes into OUT");
        Fabrication idle = new Fabrication();
        idle.load(new Fabrication().save());
        h.assertTrue(idle.chosen() == null && !idle.working() && idle.progress() == 0, "an idle state saves and loads as idle");
        h.assertTrue(Job.load(null) == null && Job.load(new CompoundTag()) == null, "a tag that names no job loads as none");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "fabricator")
    public void the_slots_take_what_the_jobs_need(GameTestHelper h) {
        h.assertTrue(Fabricating.fits(Fabricating.MATERIAL, new ItemStack(Items.IRON_INGOT)) && Fabricating.fits(Fabricating.MATERIAL, new ItemStack(Items.DIAMOND)) && Fabricating.fits(Fabricating.MATERIAL, new ItemStack(Items.LAPIS_LAZULI)), "MATERIAL takes ingots and gems");
        h.assertTrue(!Fabricating.fits(Fabricating.MATERIAL, new ItemStack(Items.REDSTONE)) && !Fabricating.fits(Fabricating.MATERIAL, new ItemStack(ModItems.BLANK_ENGRAM.get())), "and not dust or a blank");
        h.assertTrue(Fabricating.fits(Fabricating.ROLL, catalyst(1, 1)) && !Fabricating.fits(Fabricating.ROLL, new ItemStack(Items.LAPIS_LAZULI)), "ROLL takes a catalyst alone");
        h.assertTrue(Fabricating.fits(Fabricating.EXTRA, new ItemStack(Items.REDSTONE)) && Fabricating.fits(Fabricating.EXTRA, new ItemStack(ModItems.BLANK_ENGRAM.get())) && Fabricating.fits(Fabricating.EXTRA, new ItemStack(Items.DIAMOND)) && Fabricating.fits(Fabricating.EXTRA, new ItemStack(Items.IRON_INGOT)), "the second slot takes dust, a blank, a gem or an ingot");
        h.assertTrue(!Fabricating.fits(Fabricating.EXTRA, new ItemStack(Items.STICK)) && !Fabricating.fits(Fabricating.EXTRA, catalyst(0, 1)), "and not a stick or a catalyst");
        h.assertTrue(!Fabricating.fits(Fabricating.OUT, new ItemStack(Items.IRON_INGOT)), "nothing goes into OUT by hand");
        h.assertTrue(Fabricating.ticks() == WeaponsConfig.FABRICATE_TICKS.get() && Fabricating.cost(Job.part(id("scope")), h.getLevel()) == WeaponsConfig.PART_CHARGE.get()
                && Fabricating.cost(Job.chip(id("coolant")), h.getLevel()) == WeaponsConfig.CHIP_CHARGE.get() && Fabricating.cost(Job.recipe(id("laser_pistol_engram")), h.getLevel()) == WeaponsConfig.ENGRAM_CHARGE.get(), "charges and time come from the config");
        h.assertTrue(Fabricating.tabOf(Job.recipe(id("laser_pistol_engram")), h.getLevel()) == Job.Tab.ENGRAM && Fabricating.tabOf(Job.recipe(id("energy_round")), h.getLevel()) == Job.Tab.MORE, "engram recipes sit on ENGRAM, the rest on MORE");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "fabricator")
    public void the_materials_a_kind_takes_are_those_that_allow_it_and_exist_here(GameTestHelper h) {
        List<com.mrgregles.bsp_weapons.parts.WeaponMaterial> barrels = Fabricating.materialsFor(PartKind.BARREL), optics = Fabricating.materialsFor(PartKind.OPTIC);
        h.assertTrue(barrels.stream().anyMatch(m -> m.id().equals(id("iron"))) && barrels.stream().anyMatch(m -> m.id().equals(id("tetrium"))), "iron and Tetrium make barrels");
        h.assertTrue(barrels.stream().noneMatch(m -> m.id().equals(id("diamond"))), "a diamond makes no barrel (D35)");
        h.assertTrue(optics.stream().anyMatch(m -> m.id().equals(id("diamond"))), "a diamond makes an optic");
        h.assertTrue(barrels.stream().allMatch(m -> Need.ofTag(m.tag().toString(), 1).obtainable()), "every listed material has an item on this server (D122)");
        h.assertTrue(barrels.stream().noneMatch(m -> m.id().equals(id("allthemodium"))) == !Need.ofTag("forge:ingots/allthemodium", 1).obtainable(), "Allthemodium is listed exactly when its mod is here");
        for (int i = 1; i < barrels.size(); i++) h.assertTrue(barrels.get(i - 1).id().toString().compareTo(barrels.get(i).id().toString()) < 0, "the list runs by id");
        h.succeed();
    }

    @GameTest(template = EMPTY, batch = "fabricator")
    public void the_blank_engram_is_crafted_from_iron_redstone_and_a_glass_pane(GameTestHelper h) {
        Level level = h.getLevel();
        AbstractContainerMenu menu = new AbstractContainerMenu(null, 0) {
            @Override
            public ItemStack quickMoveStack(Player player, int index) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return true;
            }
        };
        TransientCraftingContainer grid = new TransientCraftingContainer(menu, 3, 3);
        for (int i = 0; i < 4; i++) grid.setItem(i, new ItemStack(Items.IRON_INGOT));
        grid.setItem(4, new ItemStack(Items.REDSTONE));
        grid.setItem(5, new ItemStack(Items.GLASS_PANE));
        Optional<CraftingRecipe> recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
        h.assertTrue(recipe.isPresent() && recipe.get().getId().equals(id("blank_engram")), "four iron, a redstone and a glass pane match the Blank Engram's recipe (R32), got " + recipe.map(r -> r.getId().toString()).orElse("nothing"));
        ItemStack made = recipe.get().assemble(grid, level.registryAccess());
        h.assertTrue(made.is(ModItems.BLANK_ENGRAM.get()) && made.getCount() == 1, "one Blank Engram, got " + made);
        h.assertTrue(made.getMaxStackSize() == 16, "it stacks to 16");
        grid.setItem(5, new ItemStack(Items.GLASS));
        h.assertTrue(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).map(r -> !r.getId().equals(id("blank_engram"))).orElse(true), "a glass block is not a pane");
        h.succeed();
    }
}
