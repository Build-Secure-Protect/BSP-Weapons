package com.mrgregles.bsp_weapons.item;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.data.WeaponData;
import com.mrgregles.bsp_weapons.parts.PartKind;
import com.mrgregles.bsp_weapons.parts.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.StringJoiner;

/** An Engram (D17, D61): names a weapon class and the parts it takes; loaded into the assembler and never used up. */
public class EngramItem extends Item {
    private final ResourceLocation pattern;

    public EngramItem(ResourceLocation pattern, Properties properties) {
        super(properties);
        this.pattern = pattern;
    }

    public ResourceLocation pattern() {
        return pattern;
    }

    /** The pattern an engram stack names, or null for anything else or an unknown pattern. */
    @Nullable
    public static Pattern patternOf(ItemStack stack) {
        return stack.getItem() instanceof EngramItem e ? WeaponData.pattern(e.pattern) : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        Pattern p = WeaponData.pattern(pattern);
        if (p == null) {
            lines.add(Component.translatable("tooltip." + BspWeapons.MODID + ".engram_unknown").withStyle(ChatFormatting.GRAY));
            return;
        }
        StringJoiner slots = new StringJoiner(", ");
        slots.add(Component.translatable("slot." + BspWeapons.MODID + ".core").getString());
        for (PartKind k : p.slots()) slots.add(Component.translatable("slot." + BspWeapons.MODID + "." + k.id()).getString());
        lines.add(Component.translatable("tooltip." + BspWeapons.MODID + ".engram", slots.toString()).withStyle(ChatFormatting.GRAY));
    }
}
