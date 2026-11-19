package com.mrgregles.bsp_weapons.enhancer;

import com.mrgregles.bsp_weapons.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * The enhancer's container, laid out as the Board screen (D85): the weapon at the rail's end, CHIP IN, REPAIR under the
 * grip and CORE under the heart in a row beneath the rail; the machine's part of the screen is the top 148 rows, the
 * player's inventory the rest. 300 x 232. The charge reaches the client as four shorts
 * (low and high halves of the stored and the capacity), since container data is sixteen bits wide on the wire.
 */
public class WeaponEnhancerMenu extends AbstractContainerMenu {
    /** Slot positions on the screen, by {@link Enhancing} slot number: weapon, chip in, core, repair. */
    public static final int[][] SLOT_XY = {{6, 53}, {20, 80}, {96, 80}, {58, 80}};
    public static final int INVENTORY_X = 69, INVENTORY_Y = 150, HOTBAR_Y = 208;
    private final WeaponEnhancerBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final ContainerData data;

    public WeaponEnhancerMenu(int windowId, Inventory inventory, BlockPos pos) {
        this(windowId, inventory, inventory.player.level().getBlockEntity(pos) instanceof WeaponEnhancerBlockEntity be ? be
                : new WeaponEnhancerBlockEntity(pos, ModBlocks.WEAPON_ENHANCER.get().defaultBlockState())); // a client without the block entity gets a menu that stillValid closes
    }

    public WeaponEnhancerMenu(int windowId, Inventory inventory, WeaponEnhancerBlockEntity blockEntity) {
        super(ModBlocks.WEAPON_ENHANCER_MENU.get(), windowId);
        this.blockEntity = blockEntity;
        this.pos = blockEntity.getBlockPos();
        this.access = blockEntity.getLevel() == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(blockEntity.getLevel(), pos);
        ItemStackHandler items = blockEntity.items();
        for (int i = 0; i < Enhancing.SLOTS; i++) {
            final int slot = i;
            addSlot(new SlotItemHandler(items, i, SLOT_XY[i][0] + 1, SLOT_XY[i][1] + 1) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return Enhancing.fits(slot, stack);
                }

                @Override
                public int getMaxStackSize() {
                    return slot == Enhancing.WEAPON ? 1 : super.getMaxStackSize();
                }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + 1 + col * 18, INVENTORY_Y + 1 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, INVENTORY_X + 1 + col * 18, HOTBAR_Y + 1));
        MachineEnergy energy = blockEntity.energy();
        // the server reads the store live; a client keeps what the server sends it (a live reader on the client would drop every update)
        boolean server = blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide;
        this.data = server ? new ContainerData() {
            @Override
            public int get(int i) {
                int v = i < 2 ? energy.getEnergyStored() : energy.getMaxEnergyStored();
                return (i % 2 == 0 ? v : v >>> 16) & 0xFFFF;
            }

            @Override
            public void set(int i, int value) {
                // the server's store is written by the machine, never by a client
            }

            @Override
            public int getCount() {
                return 4;
            }
        } : new net.minecraft.world.inventory.SimpleContainerData(4);
        addDataSlots(data);
    }

    public WeaponEnhancerBlockEntity blockEntity() {
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

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.WEAPON_ENHANCER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index < Enhancing.SLOTS) {
            if (!moveItemStackTo(stack, Enhancing.SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int i = 0; i < Enhancing.SLOTS && !moved; i++) {
                if (Enhancing.fits(i, stack)) moved = moveItemStackTo(stack, i, i + 1, false);
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }
}
