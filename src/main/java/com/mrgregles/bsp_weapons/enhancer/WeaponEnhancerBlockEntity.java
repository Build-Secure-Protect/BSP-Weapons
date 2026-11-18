package com.mrgregles.bsp_weapons.enhancer;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.WeaponsConfig;
import com.mrgregles.bsp_weapons.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

/**
 * The enhancer's inventory (see {@link Enhancing} for the slots), its Forge Energy store (D75; capacity and intake from
 * the config, exposed on every side, never giving charge out) and the working state the Spire's look reads: which
 * operation ran last and until when the ring runs (R17, R19).
 */
public class WeaponEnhancerBlockEntity extends BlockEntity implements MenuProvider {
    /** Ticks the machine shows as working after an operation: about three seconds. */
    public static final int WORKING_TICKS = 60;
    private final ItemStackHandler items = new ItemStackHandler(Enhancing.SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return Enhancing.fits(slot, stack);
        }
    };
    private final MachineEnergy energy = new MachineEnergy(WeaponsConfig.ENHANCER_CAPACITY.get(), WeaponsConfig.ENHANCER_MAX_RECEIVE.get());
    private LazyOptional<MachineEnergy> energyCap = LazyOptional.of(() -> energy);
    private long workingUntil;
    private int lastOperation = -1;
    private int lastStep = -1;

    public WeaponEnhancerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.WEAPON_ENHANCER_ENTITY.get(), pos, state);
        energy.onChange(this::energyChanged);
    }

    public ItemStackHandler items() {
        return items;
    }

    public MachineEnergy energy() {
        return energy;
    }

    public long workingUntil() {
        return workingUntil;
    }

    public int lastOperation() {
        return lastOperation;
    }

    public boolean working() {
        return level != null && level.getGameTime() < workingUntil;
    }

    /** After an operation: the look runs for {@link #WORKING_TICKS} and clients hear of it. */
    public void startWorking(int operation) {
        if (level == null) return;
        workingUntil = level.getGameTime() + WORKING_TICKS;
        lastOperation = operation;
        setChanged();
        sync();
    }

    /** Charge changes are saved at once; clients hear of them at every twentieth of the store, not every tick. */
    private void energyChanged() {
        setChanged();
        int step = energy.getMaxEnergyStored() == 0 ? 0 : (int) (20L * energy.getEnergyStored() / energy.getMaxEnergyStored());
        if (step != lastStep) {
            lastStep = step;
            sync();
        }
    }

    private void sync() {
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return energyCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        energyCap = LazyOptional.of(() -> energy);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putLong("WorkingUntil", workingUntil);
        tag.putInt("LastOperation", lastOperation);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Items")) items.deserializeNBT(tag.getCompound("Items"));
        if (tag.contains("Energy")) energy.set(tag.getInt("Energy"));
        workingUntil = tag.getLong("WorkingUntil");
        lastOperation = tag.contains("LastOperation") ? tag.getInt("LastOperation") : -1;
    }

    /** What clients get: the charge and the working state, not the items (the menu carries those). */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putLong("WorkingUntil", workingUntil);
        tag.putInt("LastOperation", lastOperation);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < items.getSlots(); i++) WeaponEnhancerBlock.drop(level, pos, items.getStackInSlot(i));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui." + BspWeapons.MODID + ".enhancer.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new WeaponEnhancerMenu(windowId, inventory, this);
    }
}
