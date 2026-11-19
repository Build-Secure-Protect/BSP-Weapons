package com.mrgregles.bsp_weapons.fabricator;

import com.mrgregles.bsp_weapons.enhancer.MachineEnergy;
import com.mrgregles.bsp_weapons.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * The fabricator's container, laid out as the Catalogue screen (D109): MATERIAL, ROLL, the second slot and OUT in a row
 * on the right over the progress bar; the machine's part of the screen is the top 148 rows, the player's inventory the
 * rest. 300 x 232. The data the client reads: the charge and capacity as two shorts each, the ticks left and the job's
 * whole time, and the chosen catalogue row as its index plus one (0 for none). The server reads them live; a client
 * keeps what it is sent.
 */
public class WeaponFabricatorMenu extends AbstractContainerMenu {
    /** Slot positions on the screen, by {@link Fabricating} slot number: material, roll, the second slot, out. */
    public static final int[][] SLOT_XY = {{134, 70}, {182, 70}, {230, 70}, {278, 70}};
    public static final int INVENTORY_X = 69, INVENTORY_Y = 150, HOTBAR_Y = 208;
    public static final int DATA_COUNT = 7;
    private final WeaponFabricatorBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final ContainerData data;

    public WeaponFabricatorMenu(int windowId, Inventory inventory, BlockPos pos) {
        this(windowId, inventory, inventory.player.level().getBlockEntity(pos) instanceof WeaponFabricatorBlockEntity be ? be
                : new WeaponFabricatorBlockEntity(pos, ModBlocks.WEAPON_FABRICATOR.get().defaultBlockState())); // a client without the block entity gets a menu that stillValid closes
    }

    public WeaponFabricatorMenu(int windowId, Inventory inventory, WeaponFabricatorBlockEntity blockEntity) {
        super(ModBlocks.WEAPON_FABRICATOR_MENU.get(), windowId);
        this.blockEntity = blockEntity;
        this.pos = blockEntity.getBlockPos();
        this.access = blockEntity.getLevel() == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(blockEntity.getLevel(), pos);
        ItemStackHandler items = blockEntity.items();
        for (int i = 0; i < Fabricating.SLOTS; i++) {
            final int slot = i;
            addSlot(new SlotItemHandler(items, i, SLOT_XY[i][0] + 1, SLOT_XY[i][1] + 1) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return Fabricating.fits(slot, stack);
                }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + 1 + col * 18, INVENTORY_Y + 1 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, INVENTORY_X + 1 + col * 18, HOTBAR_Y + 1));
        MachineEnergy energy = blockEntity.energy();
        Fabrication state = blockEntity.state();
        boolean server = blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide;
        this.data = server ? new ContainerData() {
            @Override
            public int get(int i) {
                return switch (i) {
                    case 0 -> energy.getEnergyStored() & 0xFFFF;
                    case 1 -> energy.getEnergyStored() >>> 16 & 0xFFFF;
                    case 2 -> energy.getMaxEnergyStored() & 0xFFFF;
                    case 3 -> energy.getMaxEnergyStored() >>> 16 & 0xFFFF;
                    case 4 -> Math.min(0xFFFF, state.ticksLeft());
                    case 5 -> Math.min(0xFFFF, state.working() ? state.ticksTotal() : 0);
                    case 6 -> state.chosen() == null ? 0 : Fabricating.catalogue(blockEntity.getLevel()).indexOf(state.chosen()) + 1;
                    default -> 0;
                };
            }

            @Override
            public void set(int i, int value) {
                // the server's machine is written by the machine and the action packet, never by a client
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        } : new SimpleContainerData(DATA_COUNT);
        addDataSlots(data);
    }

    public WeaponFabricatorBlockEntity blockEntity() {
        return blockEntity;
    }

    public BlockPos pos() {
        return pos;
    }

    /** The charge as the client last heard it. */
    public int energyStored() {
        return (data.get(0) & 0xFFFF) | (data.get(1) & 0xFFFF) << 16;
    }

    public int energyCapacity() {
        return (data.get(2) & 0xFFFF) | (data.get(3) & 0xFFFF) << 16;
    }

    public int ticksLeft() {
        return data.get(4) & 0xFFFF;
    }

    /** The whole time of the job under way, 0 when the machine is idle. */
    public int ticksTotal() {
        return data.get(5) & 0xFFFF;
    }

    public boolean working() {
        return ticksTotal() > 0;
    }

    /** The chosen catalogue row's index, or -1 for none. */
    public int chosenIndex() {
        return (data.get(6) & 0xFFFF) - 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.WEAPON_FABRICATOR.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index < Fabricating.SLOTS) {
            if (!moveItemStackTo(stack, Fabricating.SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int i = 0; i < Fabricating.SLOTS && !moved; i++) {
                if (i != Fabricating.OUT && Fabricating.fits(i, stack)) moved = moveItemStackTo(stack, i, i + 1, false);
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }
}
