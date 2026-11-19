package com.mrgregles.bsp_weapons.item;

import com.mrgregles.bsp_weapons.BspWeapons;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Blank Engram (D102, D110, R31, R32): the Slate, made on the crafting table from iron, redstone and a glass pane, and
 * written into a weapon class's Engram at the Weapon Fabricator with that class's signature material.
 */
public class BlankEngramItem extends Item {
    public BlankEngramItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip." + BspWeapons.MODID + ".blank_engram").withStyle(ChatFormatting.GRAY));
    }
}
