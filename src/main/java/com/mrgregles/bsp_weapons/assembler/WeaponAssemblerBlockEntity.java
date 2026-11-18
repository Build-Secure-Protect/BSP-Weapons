package com.mrgregles.bsp_weapons.assembler;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

/** The assembler's inventory: engram, core, four part slots, the roll slot and the output (see {@link Assembly} for the slot numbers and rules). */
public class WeaponAssemblerBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler items = new ItemStackHandler(Assembly.SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public WeaponAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.WEAPON_ASSEMBLER_ENTITY.get(), pos, state);
    }

    public ItemStackHandler items() {
        return items;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Items")) items.deserializeNBT(tag.getCompound("Items"));
    }

    void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < items.getSlots(); i++) WeaponAssemblerBlock.drop(level, pos, items.getStackInSlot(i));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui." + BspWeapons.MODID + ".assembler.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new WeaponAssemblerMenu(windowId, inventory, this);
    }
}
