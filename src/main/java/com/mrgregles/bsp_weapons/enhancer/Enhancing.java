package com.mrgregles.bsp_weapons.enhancer;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.WeaponsConfig;
import com.mrgregles.bsp_weapons.item.AugmentChipItem;
import com.mrgregles.bsp_weapons.item.CoreItem;
import com.mrgregles.bsp_weapons.item.WeaponItem;
import com.mrgregles.bsp_weapons.data.WeaponData;
import com.mrgregles.bsp_weapons.parts.FittedChip;
import com.mrgregles.bsp_weapons.parts.PartSpec;
import com.mrgregles.bsp_weapons.parts.WeaponMaterial;
import com.mrgregles.bsp_weapons.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import com.mrgregles.bsp_weapons.parts.Validation;
import com.mrgregles.bsp_weapons.parts.WeaponSpec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.Tags;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The Weapon Enhancer's operations on its inventory and charge (D75, D80, D88; R21, R23), with no block, player or
 * screen, so GameTests run them as they are. Slots: 0 the weapon, 1 the chip going in or coming out, 2 a Core for an
 * upgrade, 3 an ingot or gem for a repair. The chips themselves live on the weapon; a socket number counts them in
 * fitting order. The rules of what may be fitted are {@link Enhancement}'s; this class moves items and charge.
 */
public final class Enhancing {
    public static final int WEAPON = 0, CHIP_IN = 1, CORE = 2, REPAIR = 3, SLOTS = 4;
    /** The operations, as the action packet names them. */
    public static final int FIT = 0, REMOVE = 1, SWAP = 2, OP_UPGRADE = 3, OP_REPAIR = 4;

    /** What an operation left behind: whether it ran, and a message (a refusal when it did not, a notice such as a burnt-out chip when it did). */
    public record Report(boolean ok, @Nullable Component message) {
        static Report refused(Component why) {
            return new Report(false, why);
        }
    }

    private Enhancing() {
    }

    /** What each slot takes: a weapon, a chip, a Core, an ingot or gem. */
    public static boolean fits(int slot, ItemStack stack) {
        return switch (slot) {
            case WEAPON -> stack.getItem() instanceof WeaponItem;
            case CHIP_IN -> stack.getItem() instanceof AugmentChipItem;
            case CORE -> stack.getItem() instanceof CoreItem;
            case REPAIR -> stack.is(Tags.Items.INGOTS) || stack.is(Tags.Items.GEMS);
            default -> false;
        };
    }

    /** The weapon in the machine as a spec, or null when the slot is empty or holds something that is no weapon. */
    @Nullable
    public static WeaponSpec weapon(IItemHandlerModifiable inv) {
        return WeaponItem.specOf(inv.getStackInSlot(WEAPON));
    }

    /** The charge a fit or a swap burns. */
    public static int fitCost() {
        return WeaponsConfig.FIT_CHARGE.get();
    }

    public static int upgradeCost() {
        return WeaponsConfig.UPGRADE_CHARGE.get();
    }

    public static int repairCost() {
        return WeaponsConfig.REPAIR_CHARGE.get();
    }

    /** The durability one ingot or gem restores: the config's share of the weapon's whole, at least a point. */
    public static int repairStep(ItemStack weapon) {
        return Math.max(1, (int) Math.round(weapon.getMaxDamage() * WeaponsConfig.REPAIR_SHARE.get()));
    }

    /** Whether the stack is an ingot or gem of a material fitted on the weapon's parts (D77). */
    public static boolean fittedMaterial(WeaponSpec spec, ItemStack stack) {
        for (PartSpec p : spec.parts()) {
            WeaponMaterial m = WeaponData.material(p.material());
            if (m != null && stack.is(TagKey.create(Registries.ITEM, m.tag()))) return true;
        }
        return false;
    }

    @Nullable
    public static Component cannotUpgrade(IItemHandlerModifiable inv, MachineEnergy energy) {
        WeaponSpec spec = weapon(inv);
        if (spec == null) return refusal("no_weapon");
        ItemStack core = inv.getStackInSlot(CORE);
        if (!(core.getItem() instanceof CoreItem)) return refusal("no_core");
        if (core.getCount() != 1) return refusal("one_core_at_a_time");
        if (CoreItem.tierOf(core) <= spec.coreTier()) return refusal("core_not_higher", CoreItem.roman(spec.coreTier()));
        Component low = charge(energy, upgradeCost());
        if (low != null) return low;
        if (!Validation.isValid(spec) || !Validation.isValid(withTier(spec, CoreItem.tierOf(core)))) return refusal("invalid_weapon");
        return null;
    }

    @Nullable
    public static Component cannotRepair(IItemHandlerModifiable inv, MachineEnergy energy) {
        WeaponSpec spec = weapon(inv);
        if (spec == null) return refusal("no_weapon");
        ItemStack ingot = inv.getStackInSlot(REPAIR);
        if (!fits(REPAIR, ingot)) return refusal("no_ingot");
        if (!fittedMaterial(spec, ingot)) return refusal("wrong_material", ingot.getHoverName());
        if (inv.getStackInSlot(WEAPON).getDamageValue() <= 0) return refusal("nothing_to_repair");
        Component low = charge(energy, repairCost());
        if (low != null) return low;
        if (!Validation.isValid(spec)) return refusal("invalid_weapon");
        return null;
    }

    // ---- the checks: why an operation cannot run right now, or null. The screen shows these before a key is pressed; the moves run them first. ----

    @Nullable
    public static Component cannotFit(IItemHandlerModifiable inv, MachineEnergy energy) {
        WeaponSpec spec = weapon(inv);
        if (spec == null) return refusal("no_weapon");
        ItemStack chip = inv.getStackInSlot(CHIP_IN);
        if (!(chip.getItem() instanceof AugmentChipItem)) return refusal("no_chip_in");
        Component low = charge(energy, fitCost());
        if (low != null) return low;
        return Enhancement.fitProblem(spec, AugmentChipItem.chipOf(chip), -1);
    }

    @Nullable
    public static Component cannotRemove(IItemHandlerModifiable inv, int socket) {
        WeaponSpec spec = weapon(inv);
        if (spec == null) return refusal("no_weapon");
        if (!inv.getStackInSlot(CHIP_IN).isEmpty()) return refusal("chip_in_full");
        if (!Validation.isValid(spec)) return refusal("invalid_weapon");
        if (socket < 0 || socket >= spec.augments().size()) return refusal("no_chip");
        return null;
    }

    @Nullable
    public static Component cannotSwap(IItemHandlerModifiable inv, MachineEnergy energy, int socket) {
        WeaponSpec spec = weapon(inv);
        if (spec == null) return refusal("no_weapon");
        ItemStack chip = inv.getStackInSlot(CHIP_IN);
        if (!(chip.getItem() instanceof AugmentChipItem)) return refusal("no_chip_in");
        if (chip.getCount() != 1) return refusal("one_at_a_time");
        Component low = charge(energy, fitCost());
        if (low != null) return low;
        if (!Validation.isValid(spec)) return refusal("invalid_weapon");
        if (socket < 0 || socket >= spec.augments().size()) return refusal("no_chip");
        return Enhancement.fitProblem(spec, AugmentChipItem.chipOf(chip), socket);
    }

    // ---- the moves -------------------------------------------------------------------------------------------------------------------

    /** FIT: the chip in CHIP IN goes into the weapon's first free socket; one chip and the fit charge are spent. */
    @Nullable
    public static Component fit(IItemHandlerModifiable inv, MachineEnergy energy) {
        Component why = cannotFit(inv, energy);
        if (why != null) return why;
        ItemStack chip = inv.getStackInSlot(CHIP_IN);
        Enhancement.Result r = Enhancement.fit(weapon(inv), chip);
        if (!r.ok()) return r.problem();
        energy.spend(fitCost());
        chip.shrink(1);
        inv.setStackInSlot(CHIP_IN, chip);
        WeaponItem.setSpec(inv.getStackInSlot(WEAPON), r.spec());
        return null;
    }

    /** REMOVE: the chosen socket's chip comes out into CHIP IN with its points (D91); no charge (D88). */
    @Nullable
    public static Component remove(IItemHandlerModifiable inv, MachineEnergy energy, int socket) {
        Component why = cannotRemove(inv, socket);
        if (why != null) return why;
        Enhancement.Result r = Enhancement.remove(weapon(inv), socket);
        if (!r.ok()) return r.problem();
        inv.setStackInSlot(CHIP_IN, r.chip());
        WeaponItem.setSpec(inv.getStackInSlot(WEAPON), r.spec());
        return null;
    }

    /** SWAP: the single chip in CHIP IN (D92) takes the chosen socket and the chip that was there comes out into CHIP IN; the fit charge is spent. */
    @Nullable
    public static Component swap(IItemHandlerModifiable inv, MachineEnergy energy, int socket) {
        Component why = cannotSwap(inv, energy, socket);
        if (why != null) return why;
        Enhancement.Result r = Enhancement.swap(weapon(inv), socket, inv.getStackInSlot(CHIP_IN));
        if (!r.ok()) return r.problem();
        energy.spend(fitCost());
        inv.setStackInSlot(CHIP_IN, r.chip());
        WeaponItem.setSpec(inv.getStackInSlot(WEAPON), r.spec());
        return null;
    }

    /** UPGRADE: the single higher Core in CORE becomes the weapon's; the old Core takes its place in the slot (D76); every chip wears a point (D83). */
    public static Report upgrade(IItemHandlerModifiable inv, MachineEnergy energy) {
        Component why = cannotUpgrade(inv, energy);
        if (why != null) return Report.refused(why);
        WeaponSpec spec = weapon(inv);
        int newTier = CoreItem.tierOf(inv.getStackInSlot(CORE));
        Enhancement.Worn worn = Enhancement.wear(withTier(spec, newTier));
        energy.spend(upgradeCost());
        inv.setStackInSlot(CORE, ModItems.WEAPON_CORE.get().of(spec.coreTier()));
        WeaponItem.setSpec(inv.getStackInSlot(WEAPON), worn.spec());
        return new Report(true, burnt(worn));
    }

    /** REPAIR: as many ingots or gems from REPAIR as the weapon needs, each restoring the config's share, up to full (D77); every chip wears a point (D83). */
    public static Report repair(IItemHandlerModifiable inv, MachineEnergy energy) {
        Component why = cannotRepair(inv, energy);
        if (why != null) return Report.refused(why);
        ItemStack gun = inv.getStackInSlot(WEAPON), ingot = inv.getStackInSlot(REPAIR);
        int step = repairStep(gun), damage = gun.getDamageValue();
        int take = Math.min(ingot.getCount(), (damage + step - 1) / step);
        gun.setDamageValue(Math.max(0, damage - take * step));
        ingot.shrink(take);
        inv.setStackInSlot(REPAIR, ingot);
        Enhancement.Worn worn = Enhancement.wear(weapon(inv));
        energy.spend(repairCost());
        WeaponItem.setSpec(gun, worn.spec());
        return new Report(true, burnt(worn));
    }

    /** Runs an operation by its packet number. */
    public static Report run(int action, IItemHandlerModifiable inv, MachineEnergy energy, int socket) {
        Component why;
        switch (action) {
            case FIT -> why = fit(inv, energy);
            case REMOVE -> why = remove(inv, energy, socket);
            case SWAP -> why = swap(inv, energy, socket);
            case OP_UPGRADE -> { return upgrade(inv, energy); }
            case OP_REPAIR -> { return repair(inv, energy); }
            default -> why = refusal("no_such_operation");
        }
        return new Report(why == null, why);
    }

    /** The notice naming the chips that burnt out, or null when none did. */
    @Nullable
    static Component burnt(Enhancement.Worn worn) {
        if (worn.burnt().isEmpty()) return null;
        List<Component> names = new ArrayList<>();
        for (FittedChip c : worn.burnt()) {
            var a = WeaponData.augment(c.augment());
            names.add(a == null ? Component.translatable("tooltip." + BspWeapons.MODID + ".chip_unknown") : Component.translatable(a.nameKey()));
        }
        var joined = Component.empty();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) joined.append(", ");
            joined.append(names.get(i));
        }
        return refusal("burnt_out", joined);
    }

    static WeaponSpec withTier(WeaponSpec spec, int tier) {
        return new WeaponSpec(spec.pattern(), tier, spec.parts(), spec.roll(), spec.augments());
    }

    @Nullable
    static Component charge(MachineEnergy energy, int cost) {
        return energy.getEnergyStored() >= cost ? null : refusal("no_power", String.format(java.util.Locale.ROOT, "%,d", energy.getEnergyStored()), String.format(java.util.Locale.ROOT, "%,d", cost));
    }

    static Component refusal(String key, Object... args) {
        return Component.translatable("enhance." + BspWeapons.MODID + "." + key, args);
    }
}
