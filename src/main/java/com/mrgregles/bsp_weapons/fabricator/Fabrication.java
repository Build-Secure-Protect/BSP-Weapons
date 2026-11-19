package com.mrgregles.bsp_weapons.fabricator;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * What a Weapon Fabricator remembers besides its inventory and charge (R27, R30, D119): the catalogue row the player
 * chose, and the job under way as the product already made and the ticks left before it reaches OUT. Saved with the block
 * entity, so a server stop loses nothing; a plain object here, so GameTests run it without a block.
 */
public final class Fabrication {
    @Nullable
    private Job chosen;
    private ItemStack product = ItemStack.EMPTY;
    private int ticksLeft, ticksTotal;

    @Nullable
    public Job chosen() {
        return chosen;
    }

    public void choose(@Nullable Job job) {
        this.chosen = job;
    }

    /** The product being made, empty when the machine is idle. */
    public ItemStack product() {
        return product;
    }

    public int ticksLeft() {
        return ticksLeft;
    }

    public int ticksTotal() {
        return ticksTotal;
    }

    public boolean working() {
        return !product.isEmpty();
    }

    /** 0 when idle or just started, 1 when the product is about to reach OUT. */
    public float progress() {
        return ticksTotal <= 0 || !working() ? 0 : (ticksTotal - ticksLeft) / (float) ticksTotal;
    }

    void start(ItemStack made, int ticks) {
        product = made;
        ticksTotal = Math.max(1, ticks);
        ticksLeft = ticksTotal;
    }

    /** One tick of work; true when the time has run out and the product waits to be placed. */
    boolean tick() {
        if (!working()) return false;
        if (ticksLeft > 0) ticksLeft--;
        return ticksLeft == 0;
    }

    ItemStack takeProduct() {
        ItemStack made = product;
        product = ItemStack.EMPTY;
        ticksLeft = ticksTotal = 0;
        return made;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        if (chosen != null) t.put("Chosen", chosen.save());
        if (!product.isEmpty()) {
            t.put("Product", product.save(new CompoundTag()));
            t.putInt("TicksLeft", ticksLeft);
            t.putInt("TicksTotal", ticksTotal);
        }
        return t;
    }

    public void load(CompoundTag t) {
        chosen = t.contains("Chosen") ? Job.load(t.getCompound("Chosen")) : null;
        product = t.contains("Product") ? ItemStack.of(t.getCompound("Product")) : ItemStack.EMPTY;
        ticksTotal = product.isEmpty() ? 0 : Math.max(1, t.getInt("TicksTotal"));
        ticksLeft = product.isEmpty() ? 0 : Math.max(0, Math.min(ticksTotal, t.getInt("TicksLeft")));
    }
}
