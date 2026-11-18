package com.mrgregles.bsp_weapons.assembler;

import com.mrgregles.bsp_weapons.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * The assembler's container, laid out as the Rail screen (D52): the engram at the rail's end, the Core at its heart, the
 * Optic above, the Barrel ahead, the Grip and Cell below, the roll slot and the output at the right. 256 x 232.
 */
public class WeaponAssemblerMenu extends AbstractContainerMenu {
    /** Slot positions on the screen, by {@link Assembly} slot number: engram, core, barrel, cell, grip, optic, roll, output. */
    public static final int[][] SLOT_XY = {{8, 48}, {119, 48}, {160, 48}, {119, 78}, {84, 78}, {92, 20}, {186, 78}, {218, 78}};
    public static final int INVENTORY_X = 47, INVENTORY_Y = 150, HOTBAR_Y = 208;
    private final WeaponAssemblerBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final BlockPos pos;

    public WeaponAssemblerMenu(int windowId, Inventory inventory, BlockPos pos) {
        this(windowId, inventory, inventory.player.level().getBlockEntity(pos) instanceof WeaponAssemblerBlockEntity be ? be
                : new WeaponAssemblerBlockEntity(pos, ModBlocks.WEAPON_ASSEMBLER.get().defaultBlockState())); // a client without the block entity gets a menu that stillValid closes
    }

    public WeaponAssemblerMenu(int windowId, Inventory inventory, WeaponAssemblerBlockEntity blockEntity) {
        super(ModBlocks.WEAPON_ASSEMBLER_MENU.get(), windowId);
        this.blockEntity = blockEntity;
        this.pos = blockEntity.getBlockPos();
        this.access = blockEntity.getLevel() == null ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(blockEntity.getLevel(), pos);
        ItemStackHandler items = blockEntity.items();
        for (int i = 0; i < Assembly.SLOTS; i++) {
            final int slot = i;
            addSlot(new SlotItemHandler(items, i, SLOT_XY[i][0] + 1, SLOT_XY[i][1] + 1) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return slot != Assembly.OUTPUT && Assembly.fits(slot, stack);
                }

                @Override
                public int getMaxStackSize() {
                    return slot == Assembly.OUTPUT ? 1 : super.getMaxStackSize();
                }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + 1 + col * 18, INVENTORY_Y + 1 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, INVENTORY_X + 1 + col * 18, HOTBAR_Y + 1));
    }

    public WeaponAssemblerBlockEntity blockEntity() {
        return blockEntity;
    }

    public BlockPos pos() {
        return pos;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.WEAPON_ASSEMBLER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (index < Assembly.SLOTS) {
            if (!moveItemStackTo(stack, Assembly.SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int i = 0; i < Assembly.SLOTS && !moved; i++) {
                if (i != Assembly.OUTPUT && Assembly.fits(i, stack)) moved = moveItemStackTo(stack, i, i + 1, false);
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }
}
