package com.mrgregles.bsp_weapons.weapon;

import com.mrgregles.bsp_weapons.WeaponsConfig;
import com.mrgregles.bsp_weapons.data.WeaponData;
import com.mrgregles.bsp_weapons.item.WeaponItem;
import com.mrgregles.bsp_weapons.parts.PartKind;
import com.mrgregles.bsp_weapons.parts.PartSpec;
import com.mrgregles.bsp_weapons.parts.PartType;
import com.mrgregles.bsp_weapons.parts.Sheet;
import com.mrgregles.bsp_weapons.parts.Stat;
import com.mrgregles.bsp_weapons.parts.WeaponSpec;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

/**
 * A weapon's Forge Energy (D10): a weapon whose cell holds energy stores Capacity shots' worth of FE in its tag
 * ({@code Energy}), takes energy from any charger at up to a full charge per Recharge time, and gives none out. The
 * store is offered as the item's energy capability, so Mekanism, Flux and Thermal chargers fill it like any tool.
 */
public final class WeaponEnergy implements IEnergyStorage, ICapabilityProvider {
    public static final String ENERGY = "Energy";
    private final ItemStack stack;
    private final LazyOptional<IEnergyStorage> self = LazyOptional.of(() -> this);

    public WeaponEnergy(ItemStack stack) {
        this.stack = stack;
    }

    /** Whether the weapon's cell holds energy (an energy, overcharge or EMP cell) rather than rounds. */
    public static boolean holdsEnergy(ItemStack stack) {
        WeaponSpec spec = WeaponItem.specOf(stack);
        if (spec == null) return false;
        PartSpec cell = spec.part(PartKind.CELL);
        PartType type = cell == null ? null : WeaponData.partType(cell.type());
        return type == null || type.holdsEnergy();
    }

    public static int perShot() {
        return WeaponsConfig.ENERGY_PER_SHOT.get();
    }

    /** The store's size: Capacity shots' worth; 0 for a weapon with no sheet. */
    public static int capacity(ItemStack stack) {
        Sheet sheet = Firing.sheetOf(stack);
        if (sheet == null) return 0;
        long cap = Math.round(sheet.get(Stat.CAPACITY)) * (long) perShot();
        return (int) Math.min(Integer.MAX_VALUE / 2, cap); // never overflows into a negative, unfillable store
    }

    public static int stored(ItemStack stack) {
        CompoundTag t = stack.getTag();
        return t == null || !t.contains(ENERGY) ? 0 : Math.max(0, Math.min(capacity(stack), t.getInt(ENERGY)));
    }

    public static void setStored(ItemStack stack, int energy) {
        stack.getOrCreateTag().putInt(ENERGY, Math.max(0, Math.min(capacity(stack), energy)));
    }

    /** FE a tick the weapon accepts: its whole store over the sheet's Recharge ticks, at least 1. */
    public static int maxReceive(ItemStack stack) {
        Sheet sheet = Firing.sheetOf(stack);
        if (sheet == null) return 0;
        return Math.max(1, (int) Math.ceil(capacity(stack) / Math.max(1.0, sheet.get(Stat.RECHARGE))));
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!holdsEnergy(stack)) return 0;
        int room = capacity(stack) - stored(stack);
        int taken = Math.max(0, Math.min(Math.min(maxReceive, maxReceive(stack)), room));
        if (!simulate && taken > 0) setStored(stack, stored(stack) + taken);
        return taken;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return stored(stack);
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity(stack);
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return holdsEnergy(stack);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        return cap == ForgeCapabilities.ENERGY && holdsEnergy(stack) ? self.cast() : LazyOptional.empty();
    }
}
