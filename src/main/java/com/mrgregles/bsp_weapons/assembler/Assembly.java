package com.mrgregles.bsp_weapons.assembler;

import com.mrgregles.bsp_weapons.data.WeaponData;
import com.mrgregles.bsp_weapons.item.CatalystItem;
import com.mrgregles.bsp_weapons.item.CoreItem;
import com.mrgregles.bsp_weapons.item.EngramItem;
import com.mrgregles.bsp_weapons.item.PartItem;
import com.mrgregles.bsp_weapons.item.WeaponItem;
import com.mrgregles.bsp_weapons.parts.Catalyst;
import com.mrgregles.bsp_weapons.parts.PartKind;
import com.mrgregles.bsp_weapons.parts.PartSpec;
import com.mrgregles.bsp_weapons.parts.Pattern;
import com.mrgregles.bsp_weapons.parts.Rarity;
import com.mrgregles.bsp_weapons.parts.Rolls;
import com.mrgregles.bsp_weapons.parts.Validation;
import com.mrgregles.bsp_weapons.parts.WeaponSpec;
import com.mrgregles.bsp_weapons.registry.ModItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The assembler's rules (D5, D17, D20, D21, D53, D54, D59), on an inventory and nothing else, so GameTests run them
 * without a player. Slots: 0 the engram, 1 the core, 2 to 5 the parts by kind (barrel, cell, grip, optic), 6 the roll
 * slot (a catalyst), 7 the finished weapon.
 */
public final class Assembly {
    public static final int ENGRAM = 0, CORE = 1, FIRST_PART = 2, ROLL = 6, OUTPUT = 7, SLOTS = 8;

    private Assembly() {
    }

    /** The slot a part of this kind goes in; the Core has its own slot. */
    public static int slotFor(PartKind kind) {
        return kind == PartKind.CORE ? CORE : FIRST_PART + kind.ordinal() - 1;
    }

    public static PartKind kindOf(int slot) {
        if (slot < FIRST_PART || slot >= ROLL) return null;
        return PartKind.values()[slot - FIRST_PART + 1];
    }

    /** What the inputs would make, whether or not it is complete: null only without an engram that names a known pattern. */
    @Nullable
    public static WeaponSpec preview(IItemHandlerModifiable inv) {
        Pattern pattern = EngramItem.patternOf(inv.getStackInSlot(ENGRAM));
        if (pattern == null) return null;
        ItemStack coreStack = inv.getStackInSlot(CORE);
        int tier = coreStack.getItem() instanceof CoreItem ? CoreItem.tierOf(coreStack) : 0;
        List<PartSpec> parts = new ArrayList<>();
        for (PartKind kind : pattern.slots()) {
            PartSpec p = PartItem.specOf(inv.getStackInSlot(slotFor(kind)));
            if (p != null && p.kind() == kind) parts.add(p);
        }
        return new WeaponSpec(pattern.id(), tier, parts, Rarity.NORMAL, List.of());
    }

    /** Why the inputs cannot be assembled right now, as one plain sentence, or null when they can. */
    @Nullable
    public static String cannotAssemble(IItemHandlerModifiable inv) {
        if (!(inv.getStackInSlot(ENGRAM).getItem() instanceof EngramItem)) return "Put an engram in the engram slot.";
        WeaponSpec spec = preview(inv);
        if (spec == null) return "That engram names no weapon this server knows.";
        if (!(inv.getStackInSlot(CORE).getItem() instanceof CoreItem)) return "The core slot is empty.";
        List<String> problems = Validation.problems(spec);
        if (!problems.isEmpty()) return problems.get(0);
        if (!inv.getStackInSlot(OUTPUT).isEmpty()) return "Take the finished weapon out first.";
        return null;
    }

    /** Makes the weapon: the core and one of each part are used, the catalyst (if any) is used for the roll, the engram stays. */
    @Nullable
    public static String assemble(IItemHandlerModifiable inv, RandomSource random) {
        String why = cannotAssemble(inv);
        if (why != null) return why;
        WeaponSpec spec = preview(inv);
        Catalyst catalyst = CatalystItem.catalystOf(inv.getStackInSlot(ROLL));
        Rarity roll = Rolls.roll(random, catalyst);
        WeaponSpec made = new WeaponSpec(spec.pattern(), spec.coreTier(), spec.parts(), roll, spec.augments());
        WeaponItem item = ModItems.weapon(made.pattern());
        if (item == null) return "No weapon item exists for that engram yet.";
        inv.getStackInSlot(CORE).shrink(1);
        for (PartSpec p : made.parts()) inv.getStackInSlot(slotFor(p.kind())).shrink(1);
        if (catalyst != null) inv.getStackInSlot(ROLL).shrink(1);
        inv.setStackInSlot(OUTPUT, item.of(made));
        return null;
    }

    /** Why the weapon in the output cannot be taken apart right now, or null. */
    @Nullable
    public static String cannotTakeApart(IItemHandlerModifiable inv) {
        WeaponSpec spec = WeaponItem.specOf(inv.getStackInSlot(OUTPUT));
        if (spec == null) return "Put a weapon in the output slot to take it apart.";
        if (!Validation.isValid(spec) || ModItems.weapon(spec.pattern()) != inv.getStackInSlot(OUTPUT).getItem()) return "That weapon is not one this server would make.";
        for (PartSpec p : spec.parts()) if (ModItems.part(p.type()) == null) return "No part item exists for '" + p.type() + "'.";
        if (CatalystItem.catalystOf(inv.getStackInSlot(ROLL)) == null) return "Taking apart costs a catalyst: put one in the roll slot.";
        if (!inv.getStackInSlot(CORE).isEmpty()) return "Empty the core slot first.";
        for (PartSpec p : spec.parts()) if (!inv.getStackInSlot(slotFor(p.kind())).isEmpty()) return "Empty the " + p.kind().id() + " slot first.";
        return null;
    }

    /** Takes the weapon apart: the catalyst is used, the core and parts come back with their own rolls, the weapon's roll is lost (D53). */
    @Nullable
    public static String takeApart(IItemHandlerModifiable inv) {
        String why = cannotTakeApart(inv);
        if (why != null) return why;
        WeaponSpec spec = WeaponItem.specOf(inv.getStackInSlot(OUTPUT));
        inv.getStackInSlot(ROLL).shrink(1);
        inv.setStackInSlot(CORE, ModItems.WEAPON_CORE.get().of(spec.coreTier()));
        for (PartSpec p : spec.parts()) {
            PartItem part = ModItems.part(p.type());
            if (part != null) inv.setStackInSlot(slotFor(p.kind()), part.of(p.material(), p.rarity()));
        }
        inv.setStackInSlot(OUTPUT, ItemStack.EMPTY);
        return null;
    }

    /** Whether a stack may go in a slot (the menu's rule and the hopper's, later). */
    public static boolean fits(int slot, ItemStack stack) {
        if (slot == ENGRAM) return stack.getItem() instanceof EngramItem;
        if (slot == CORE) return stack.getItem() instanceof CoreItem;
        if (slot == ROLL) return stack.getItem() instanceof CatalystItem;
        if (slot == OUTPUT) return stack.getItem() instanceof WeaponItem;
        PartKind kind = kindOf(slot);
        return kind != null && stack.getItem() instanceof PartItem p && p.kind() == kind;
    }

    /** The data the catalyst in the roll slot stands for, for the screen's odds line. */
    @Nullable
    public static Catalyst rollCatalyst(IItemHandlerModifiable inv) {
        return CatalystItem.catalystOf(inv.getStackInSlot(ROLL));
    }

    static boolean hasData() {
        return !WeaponData.patterns().isEmpty();
    }
}
