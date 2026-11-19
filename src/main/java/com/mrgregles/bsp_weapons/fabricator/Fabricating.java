package com.mrgregles.bsp_weapons.fabricator;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.WeaponsConfig;
import com.mrgregles.bsp_weapons.data.WeaponData;
import com.mrgregles.bsp_weapons.enhancer.MachineEnergy;
import com.mrgregles.bsp_weapons.item.BlankEngramItem;
import com.mrgregles.bsp_weapons.item.CatalystItem;
import com.mrgregles.bsp_weapons.item.CoreItem;
import com.mrgregles.bsp_weapons.item.PartItem;
import com.mrgregles.bsp_weapons.parts.Augment;
import com.mrgregles.bsp_weapons.parts.Catalyst;
import com.mrgregles.bsp_weapons.parts.Need;
import com.mrgregles.bsp_weapons.parts.PartKind;
import com.mrgregles.bsp_weapons.parts.PartType;
import com.mrgregles.bsp_weapons.parts.Rarity;
import com.mrgregles.bsp_weapons.parts.Rolls;
import com.mrgregles.bsp_weapons.parts.WeaponMaterial;
import com.mrgregles.bsp_weapons.registry.ModItems;
import com.mrgregles.bsp_weapons.registry.ModRecipes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.Tags;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The Weapon Fabricator's rules on an inventory, a charge and a {@link Fabrication} (D99 to D104, D112 to D122; R27 to R30),
 * with no block, player or screen, so GameTests run them as they are. Slots: 0 MATERIAL (the ingots or gems), 1 ROLL (an
 * optional catalyst for a part's or chip's roll), 2 the second slot (the Blank Engram on the ENGRAM tab, otherwise a job's
 * second ingredient: the screen labels it after the job, D113), 3 OUT. A job spends its inputs and charge when it starts
 * and rolls then (D119); the product waits in the {@link Fabrication} for the config's ticks and then goes to OUT.
 */
public final class Fabricating {
    public static final int MATERIAL = 0, ROLL = 1, EXTRA = 2, OUT = 3, SLOTS = 4;

    private Fabricating() {
    }

    // ---- the catalogue -------------------------------------------------------------------------------------------------------------

    /**
     * Every job this server can make, in catalogue order: the part types that are items, the chips of the available augments
     * with ingredients, then the fabricating recipes whose needs and result exist here (D122), engrams first.
     */
    public static List<Job> catalogue(Level level) {
        List<Job> jobs = new ArrayList<>();
        for (String[] t : ModItems.PART_TYPES) {
            ResourceLocation id = new ResourceLocation(BspWeapons.MODID, t[0]);
            if (WeaponData.partType(id) != null && ModItems.part(id) != null) jobs.add(Job.part(id));
        }
        WeaponData.augments().values().stream()
                .filter(a -> a.available() && !a.ingredients().isEmpty() && a.ingredients().stream().allMatch(Need::obtainable))
                .sorted(Comparator.comparing(a -> a.id().toString()))
                .forEach(a -> jobs.add(Job.chip(a.id())));
        List<FabricatingRecipe> recipes = new ArrayList<>(level.getRecipeManager().getAllRecipesFor(ModRecipes.FABRICATING.get()));
        recipes.sort(Comparator.comparing((FabricatingRecipe r) -> tabOf(r).ordinal()).thenComparing(r -> kindOrder(r.kind())).thenComparing(r -> r.id().toString()));
        for (FabricatingRecipe r : recipes) if (r.obtainable()) jobs.add(Job.recipe(r.id()));
        return jobs;
    }

    /** The jobs of one tab, in catalogue order. */
    public static List<Job> catalogue(Level level, Job.Tab tab) {
        List<Job> out = new ArrayList<>();
        for (Job j : catalogue(level)) if (tabOf(j, level) == tab) out.add(j);
        return out;
    }

    public static Job.Tab tabOf(Job job, Level level) {
        return switch (job.kind()) {
            case PART -> Job.Tab.PARTS;
            case CHIP -> Job.Tab.CHIPS;
            case RECIPE -> {
                FabricatingRecipe r = recipe(level, job.id());
                yield r == null ? Job.Tab.MORE : tabOf(r);
            }
        };
    }

    static Job.Tab tabOf(FabricatingRecipe r) {
        return FabricatingRecipe.ENGRAM.equals(r.kind()) ? Job.Tab.ENGRAM : Job.Tab.MORE;
    }

    private static int kindOrder(String kind) {
        return switch (kind) {
            case FabricatingRecipe.ENGRAM -> 0;
            case FabricatingRecipe.CORE -> 1;
            case FabricatingRecipe.CATALYST -> 2;
            case FabricatingRecipe.ROUND -> 3;
            default -> 4;
        };
    }

    /** The fabricating recipe with this id, or null. */
    @Nullable
    public static FabricatingRecipe recipe(Level level, ResourceLocation id) {
        Optional<? extends Recipe<?>> r = level.getRecipeManager().byKey(id);
        return r.isPresent() && r.get() instanceof FabricatingRecipe f ? f : null;
    }

    // ---- what the slots take and what a job needs ------------------------------------------------------------------------------------

    /** What each slot takes: ingots and gems; a catalyst; a second ingredient (dust, ingot, gem or Blank Engram); nothing (OUT is taken from). */
    public static boolean fits(int slot, ItemStack stack) {
        return switch (slot) {
            case MATERIAL -> stack.is(Tags.Items.INGOTS) || stack.is(Tags.Items.GEMS);
            case ROLL -> stack.getItem() instanceof CatalystItem;
            case EXTRA -> stack.is(Tags.Items.DUSTS) || stack.is(Tags.Items.INGOTS) || stack.is(Tags.Items.GEMS) || stack.getItem() instanceof BlankEngramItem;
            default -> false;
        };
    }

    /** The material a stack of ingots or gems stands for (D34: by tag), or null when no material claims it. */
    @Nullable
    public static WeaponMaterial materialOf(ItemStack stack) {
        if (stack.isEmpty()) return null;
        WeaponMaterial found = null;
        for (WeaponMaterial m : WeaponData.materials().values()) {
            if (stack.is(TagKey.create(Registries.ITEM, m.tag())) && (found == null || m.id().compareTo(found.id()) < 0)) found = m;
        }
        return found;
    }

    /** The materials a part of this kind may be made of that exist on this server (D35, D122), by id: what the needs line cycles through (R28). */
    public static List<WeaponMaterial> materialsFor(PartKind kind) {
        List<WeaponMaterial> out = new ArrayList<>();
        for (WeaponMaterial m : WeaponData.materials().values()) {
            if (m.allows(kind) && new Need(m.tag(), true, 1).obtainable()) out.add(m);
        }
        out.sort(Comparator.comparing(m -> m.id().toString()));
        return out;
    }

    /** What the MATERIAL slot must hold for a job, or null for a part (any allowed material, {@link PartType#cost()} of it). */
    @Nullable
    public static Need materialNeed(Job job, Level level) {
        return switch (job.kind()) {
            case PART -> null;
            case CHIP -> {
                Augment a = WeaponData.augment(job.id());
                yield a == null || a.ingredients().isEmpty() ? null : a.ingredients().get(0);
            }
            case RECIPE -> {
                FabricatingRecipe r = recipe(level, job.id());
                yield r == null ? null : r.material();
            }
        };
    }

    /** What the second slot must hold for a job, or null when the job takes nothing there. */
    @Nullable
    public static Need extraNeed(Job job, Level level) {
        return switch (job.kind()) {
            case PART -> null;
            case CHIP -> {
                Augment a = WeaponData.augment(job.id());
                yield a == null || a.ingredients().size() < 2 ? null : a.ingredients().get(1);
            }
            case RECIPE -> {
                FabricatingRecipe r = recipe(level, job.id());
                yield r == null ? null : r.extra();
            }
        };
    }

    /** The charge a job draws when it starts (D114). */
    public static int cost(Job job, Level level) {
        return switch (job.kind()) {
            case PART -> WeaponsConfig.PART_CHARGE.get();
            case CHIP -> WeaponsConfig.CHIP_CHARGE.get();
            case RECIPE -> {
                FabricatingRecipe r = recipe(level, job.id());
                if (r == null) yield WeaponsConfig.OTHER_CHARGE.get();
                yield switch (r.kind()) {
                    case FabricatingRecipe.ENGRAM -> WeaponsConfig.ENGRAM_CHARGE.get();
                    case FabricatingRecipe.CORE -> WeaponsConfig.CORE_CHARGE_PER_TIER.get() * (r.result().getItem() instanceof CoreItem ? CoreItem.tierOf(r.result()) : 1);
                    case FabricatingRecipe.CATALYST -> WeaponsConfig.CATALYST_CHARGE.get();
                    case FabricatingRecipe.ROUND -> WeaponsConfig.ROUND_CHARGE.get();
                    default -> WeaponsConfig.OTHER_CHARGE.get();
                };
            }
        };
    }

    /** Ticks from a job's start to its product reaching OUT (D99, D114). */
    public static int ticks() {
        return WeaponsConfig.FABRICATE_TICKS.get();
    }

    /** A job's name for the screen: the part type's item name, "<Augment> Chip", or the recipe's result. */
    public static Component name(Job job, Level level) {
        return switch (job.kind()) {
            case PART -> {
                PartItem item = ModItems.part(job.id());
                yield item == null ? Component.literal(job.id().toString()) : item.getDescription();
            }
            case CHIP -> {
                Augment a = WeaponData.augment(job.id());
                yield a == null ? Component.literal(job.id().toString()) : Component.translatable("item." + BspWeapons.MODID + ".augment_chip.named", Component.translatable(a.nameKey()));
            }
            case RECIPE -> {
                FabricatingRecipe r = recipe(level, job.id());
                yield r == null ? Component.literal(job.id().toString()) : r.result().getHoverName();
            }
        };
    }

    /** The catalyst in ROLL, for the odds line (R29); null when the slot is empty. */
    @Nullable
    public static Catalyst rollCatalyst(IItemHandlerModifiable inv) {
        return CatalystItem.catalystOf(inv.getStackInSlot(ROLL));
    }

    // ---- the check: why a job cannot start right now, or null. The screen shows it before MAKE is pressed; make runs it first. ----------

    @Nullable
    public static Component cannotMake(@Nullable Job job, IItemHandlerModifiable inv, MachineEnergy energy, Fabrication state, Level level) {
        if (job == null || !catalogue(level).contains(job)) return cannot("no_job");
        if (state.working()) return cannot("busy");
        if (!inv.getStackInSlot(OUT).isEmpty()) return cannot("out_full");
        ItemStack material = inv.getStackInSlot(MATERIAL), extra = inv.getStackInSlot(EXTRA);
        if (job.kind() == Job.Kind.PART) {
            PartType type = WeaponData.partType(job.id());
            if (type == null) return cannot("no_job");
            if (material.isEmpty()) return cannot("no_material", type.cost());
            WeaponMaterial m = materialOf(material);
            if (m == null) return cannot("not_a_material", material.getHoverName());
            if (!m.allows(type.kind())) return cannot("wrong_kind", materialName(m), Component.translatable("part_kind." + BspWeapons.MODID + "." + type.kind().id()));
            if (material.getCount() < type.cost()) return cannot("too_few", type.cost(), material.getHoverName(), material.getCount());
        } else {
            Need needMaterial = materialNeed(job, level), needExtra = extraNeed(job, level);
            if (needMaterial == null) return cannot("no_job");
            if (!needMaterial.satisfiedBy(material)) return cannot("need_material", needMaterial.count(), needMaterial.name());
            if (needExtra != null && !needExtra.satisfiedBy(extra)) return cannot("need_extra", needExtra.count(), needExtra.name());
        }
        int cost = cost(job, level);
        if (energy.getEnergyStored() < cost) return cannot("no_power", String.format(Locale.ROOT, "%,d", energy.getEnergyStored()), String.format(Locale.ROOT, "%,d", cost));
        return null;
    }

    // ---- the move ---------------------------------------------------------------------------------------------------------------------

    /**
     * MAKE: the job's inputs, its catalyst if it rolls, and its charge are spent now; the product is made (and rolled) now and held by
     * the state until {@link #ticks()} have passed (D119). Returns the refusal, or null when the job started.
     */
    @Nullable
    public static Component make(@Nullable Job job, IItemHandlerModifiable inv, MachineEnergy energy, Fabrication state, Level level, RandomSource random) {
        Component why = cannotMake(job, inv, energy, state, level);
        if (why != null) return why;
        ItemStack material = inv.getStackInSlot(MATERIAL), extra = inv.getStackInSlot(EXTRA);
        ItemStack made;
        switch (job.kind()) {
            case PART -> {
                PartType type = WeaponData.partType(job.id());
                PartItem item = ModItems.part(job.id());
                WeaponMaterial m = materialOf(material);
                made = item.of(m.id(), roll(inv, random));
                material.shrink(type.cost());
            }
            case CHIP -> {
                Augment a = WeaponData.augment(job.id());
                made = ModItems.AUGMENT_CHIP.get().of(a.id(), roll(inv, random));
                material.shrink(a.ingredients().get(0).count());
                if (a.ingredients().size() > 1) extra.shrink(a.ingredients().get(1).count());
            }
            default -> {
                FabricatingRecipe r = recipe(level, job.id());
                made = r.result().copy();
                material.shrink(r.material().count());
                if (r.extra() != null) extra.shrink(r.extra().count());
            }
        }
        inv.setStackInSlot(MATERIAL, material);
        inv.setStackInSlot(EXTRA, extra);
        energy.spend(cost(job, level));
        state.start(made, ticks());
        return null;
    }

    /** The roll for a part or chip: the catalyst in ROLL sets the odds and is spent; none means Normal (D29, D30). */
    static Rarity roll(IItemHandlerModifiable inv, RandomSource random) {
        ItemStack stack = inv.getStackInSlot(ROLL);
        Catalyst catalyst = CatalystItem.catalystOf(stack);
        Rarity r = Rolls.roll(random, catalyst);
        if (catalyst != null) {
            stack.shrink(1);
            inv.setStackInSlot(ROLL, stack);
        }
        return r;
    }

    /** One tick of the job under way: when the time is up and OUT is empty the product goes there. True when the product was placed. */
    public static boolean tick(Fabrication state, IItemHandlerModifiable inv) {
        if (!state.tick()) return false;
        if (!inv.getStackInSlot(OUT).isEmpty()) return false;
        inv.setStackInSlot(OUT, state.takeProduct());
        return true;
    }

    static Component materialName(WeaponMaterial m) {
        return Component.translatable("material." + BspWeapons.MODID + "." + m.id().getPath());
    }

    static Component cannot(String key, Object... args) {
        return Component.translatable("fabricate." + BspWeapons.MODID + "." + key, args);
    }
}
