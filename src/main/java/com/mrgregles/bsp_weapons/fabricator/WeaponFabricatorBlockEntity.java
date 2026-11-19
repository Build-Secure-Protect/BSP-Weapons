package com.mrgregles.bsp_weapons.fabricator;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.WeaponsConfig;
import com.mrgregles.bsp_weapons.enhancer.MachineEnergy;
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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

/**
 * The fabricator's inventory (see {@link Fabricating} for the slots), its Forge Energy store (D99, D114; capacity and
 * intake from the config, on every side, never giving charge out), its {@link Fabrication} (the chosen row and the job
 * under way, saved with the block, R30) and its item capability for pipes (D105): inputs go in from any side into their
 * slots, the product comes out of OUT from below. The server ticks the job; clients hear the charge at every twentieth and
 * whether the machine is working, for the look (R25).
 */
public class WeaponFabricatorBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler items = new ItemStackHandler(Fabricating.SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return Fabricating.fits(slot, stack);
        }
    };
    private final MachineEnergy energy = new MachineEnergy(WeaponsConfig.FABRICATOR_CAPACITY.get(), WeaponsConfig.FABRICATOR_MAX_RECEIVE.get());
    private final Fabrication state = new Fabrication();
    private LazyOptional<MachineEnergy> energyCap = LazyOptional.of(() -> energy);
    private LazyOptional<IItemHandler> itemsAny = LazyOptional.of(() -> new Sided(true, true));
    private LazyOptional<IItemHandler> itemsIn = LazyOptional.of(() -> new Sided(true, false));
    private LazyOptional<IItemHandler> itemsOut = LazyOptional.of(() -> new Sided(false, true));
    private int lastStep = -1;
    private boolean workingSynced;

    public WeaponFabricatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.WEAPON_FABRICATOR_ENTITY.get(), pos, state);
        energy.onChange(this::energyChanged);
    }

    public ItemStackHandler items() {
        return items;
    }

    public MachineEnergy energy() {
        return energy;
    }

    public Fabrication state() {
        return state;
    }

    /** Whether a job is under way, as the server knows it or the client last heard it. */
    public boolean working() {
        return level != null && level.isClientSide ? workingSynced : state.working();
    }

    /** Chooses a catalogue row (R27); null clears it. */
    public void choose(@Nullable Job job) {
        state.choose(job);
        setChanged();
    }

    /** MAKE on the chosen row: the refusal, or null when the job started. */
    @Nullable
    public Component make() {
        if (level == null) return Fabricating.cannot("no_job");
        Component why = Fabricating.make(state.chosen(), items, energy, state, level, level.random);
        if (why == null) {
            setChanged();
            sync();
        }
        return why;
    }

    /** One server tick: the job under way runs on, with the screen open or closed (R30). */
    public void serverTick() {
        if (!state.working()) return;
        boolean placed = Fabricating.tick(state, items);
        setChanged();
        if (placed) sync();
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
        if (cap == ForgeCapabilities.ITEM_HANDLER) return (side == null ? itemsAny : side == Direction.DOWN ? itemsOut : itemsIn).cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
        itemsAny.invalidate();
        itemsIn.invalidate();
        itemsOut.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        energyCap = LazyOptional.of(() -> energy);
        itemsAny = LazyOptional.of(() -> new Sided(true, true));
        itemsIn = LazyOptional.of(() -> new Sided(true, false));
        itemsOut = LazyOptional.of(() -> new Sided(false, true));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
        tag.putInt("Energy", energy.getEnergyStored());
        tag.put("Job", state.save());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Items")) items.deserializeNBT(tag.getCompound("Items"));
        if (tag.contains("Energy")) energy.set(tag.getInt("Energy"));
        if (tag.contains("Job")) state.load(tag.getCompound("Job"));
        if (tag.contains("Working")) workingSynced = tag.getBoolean("Working");
    }

    /** What clients get: the charge and whether the machine works, not the items or the product (the menu carries those). */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putBoolean("Working", state.working());
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Drops every item in the machine and the product under way; called when it is broken. */
    void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < items.getSlots(); i++) WeaponFabricatorBlock.drop(level, pos, items.getStackInSlot(i));
        if (state.working()) WeaponFabricatorBlock.drop(level, pos, state.product());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui." + BspWeapons.MODID + ".fabricator.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new WeaponFabricatorMenu(windowId, inventory, this);
    }

    /** The slots as a pipe sees them from one side (D105): inputs may go into MATERIAL, ROLL and the second slot, only the product may come out of OUT. */
    private final class Sided implements IItemHandlerModifiable {
        private final boolean in, out;

        Sided(boolean in, boolean out) {
            this.in = in;
            this.out = out;
        }

        @Override
        public int getSlots() {
            return items.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            items.setStackInSlot(slot, stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!in || slot == Fabricating.OUT || !Fabricating.fits(slot, stack)) return stack;
            return items.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!out || slot != Fabricating.OUT) return ItemStack.EMPTY;
            return items.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return in && slot != Fabricating.OUT && Fabricating.fits(slot, stack);
        }
    }
}
