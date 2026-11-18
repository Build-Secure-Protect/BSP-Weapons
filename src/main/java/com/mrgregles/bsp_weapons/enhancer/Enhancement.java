package com.mrgregles.bsp_weapons.enhancer;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.data.WeaponData;
import com.mrgregles.bsp_weapons.item.AugmentChipItem;
import com.mrgregles.bsp_weapons.parts.Augment;
import com.mrgregles.bsp_weapons.parts.FittedChip;
import com.mrgregles.bsp_weapons.parts.Pattern;
import com.mrgregles.bsp_weapons.parts.SheetCalculator;
import com.mrgregles.bsp_weapons.parts.Validation;
import com.mrgregles.bsp_weapons.parts.WeaponSpec;
import com.mrgregles.bsp_weapons.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The Weapon Enhancer's rules for chips (D9, D73, D75, D81, D83; R21, R23), as pure functions of a weapon's spec: what
 * the machine does in WP-6c is call these and move items. Every refusal is a sentence for the screen's status line and
 * nothing is changed; the weapon is validated before anything touches it (audit A3). Fitting spends the chip one
 * point, so a chip needs two to be worth fitting; taking a chip out costs nothing and returns it with its points;
 * swapping is a removal and a fit in one; {@link #wear} is what an upgrade or a repair does to every fitted chip.
 */
public final class Enhancement {
    private Enhancement() {
    }

    /** The outcome: the weapon's new spec and the chip that came out (empty for a fit), or the refusal. */
    public record Result(@Nullable WeaponSpec spec, ItemStack chip, @Nullable Component problem) {
        public boolean ok() {
            return problem == null;
        }

        static Result refused(String key, Object... args) {
            return new Result(null, ItemStack.EMPTY, Component.translatable("enhance." + BspWeapons.MODID + "." + key, args));
        }
    }

    /** What an upgrade or a repair leaves: the spec with every chip a point down, and the chips that burnt out. */
    public record Worn(WeaponSpec spec, List<FittedChip> burnt) {
    }

    /** Fits the chip in the stack into the first free socket. */
    public static Result fit(WeaponSpec spec, ItemStack chipStack) {
        FittedChip chip = AugmentChipItem.chipOf(chipStack);
        if (chip == null) return Result.refused("not_a_chip");
        Component problem = fitProblem(spec, chip, -1);
        if (problem != null) return new Result(null, ItemStack.EMPTY, problem);
        List<FittedChip> chips = new ArrayList<>(spec.augments());
        chips.add(chip.worn());
        return new Result(with(spec, chips), ItemStack.EMPTY, null);
    }

    /** Takes the chip out of a socket (0-based, in fitting order) and returns it with its points. */
    public static Result remove(WeaponSpec spec, int socket) {
        if (!Validation.isValid(spec)) return Result.refused("invalid_weapon");
        if (socket < 0 || socket >= spec.augments().size()) return Result.refused("no_chip");
        List<FittedChip> chips = new ArrayList<>(spec.augments());
        FittedChip out = chips.remove(socket);
        return new Result(with(spec, chips), ModItems.AUGMENT_CHIP.get().of(out), null);
    }

    /** Puts the chip in the stack where the socket's chip was, and returns the old chip with its points. */
    public static Result swap(WeaponSpec spec, int socket, ItemStack chipStack) {
        FittedChip chip = AugmentChipItem.chipOf(chipStack);
        if (chip == null) return Result.refused("not_a_chip");
        if (!Validation.isValid(spec)) return Result.refused("invalid_weapon");
        if (socket < 0 || socket >= spec.augments().size()) return Result.refused("no_chip");
        Component problem = fitProblem(spec, chip, socket);
        if (problem != null) return new Result(null, ItemStack.EMPTY, problem);
        List<FittedChip> chips = new ArrayList<>(spec.augments());
        FittedChip out = chips.set(socket, chip.worn());
        return new Result(with(spec, chips), ModItems.AUGMENT_CHIP.get().of(out), null);
    }

    /** Every fitted chip spends a point (D83); a chip at zero burns out and leaves its socket. */
    public static Worn wear(WeaponSpec spec) {
        List<FittedChip> kept = new ArrayList<>(), burnt = new ArrayList<>();
        for (FittedChip c : spec.augments()) {
            FittedChip worn = c.worn();
            if (worn.burntOut()) burnt.add(worn); else kept.add(worn);
        }
        return new Worn(with(spec, kept), burnt);
    }

    /**
     * Why this chip cannot go on this weapon, or null when it can. {@code replacing} is the socket whose chip is leaving
     * in a swap (its slot and its augment do not count), or -1 for a plain fit.
     */
    @Nullable
    public static Component fitProblem(WeaponSpec spec, FittedChip chip, int replacing) {
        if (!Validation.isValid(spec)) return Component.translatable("enhance." + BspWeapons.MODID + ".invalid_weapon");
        Augment a = WeaponData.augment(chip.augment());
        if (a == null) return Component.translatable("enhance." + BspWeapons.MODID + ".unknown_chip");
        Component name = Component.translatable(a.nameKey());
        if (!a.available()) return Component.translatable("enhance." + BspWeapons.MODID + ".not_yet", name);
        Pattern pattern = WeaponData.pattern(spec.pattern());
        if (pattern == null || !a.allowedOn(pattern)) return Component.translatable("enhance." + BspWeapons.MODID + ".wrong_weapon", name);
        int used = 0;
        for (int i = 0; i < spec.augments().size(); i++) {
            if (i == replacing) continue;
            used++;
            if (spec.augments().get(i).augment().equals(chip.augment())) return Component.translatable("enhance." + BspWeapons.MODID + ".already_fitted", name);
        }
        int slots = SheetCalculator.augmentSlots(spec);
        if (used >= slots) return Component.translatable("enhance." + BspWeapons.MODID + ".full", used, slots);
        if (chip.durability() < 2) return Component.translatable("enhance." + BspWeapons.MODID + ".worn_out", name);
        return null;
    }

    private static WeaponSpec with(WeaponSpec spec, List<FittedChip> chips) {
        return new WeaponSpec(spec.pattern(), spec.coreTier(), spec.parts(), spec.roll(), chips);
    }
}
