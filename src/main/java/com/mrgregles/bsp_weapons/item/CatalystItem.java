package com.mrgregles.bsp_weapons.item;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.data.WeaponData;
import com.mrgregles.bsp_weapons.parts.Catalyst;
import com.mrgregles.bsp_weapons.parts.Rarity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** A roll catalyst (D21, D30, D62): lapis and one ingredient in a vial; its odds come from the data, its motes take the ingredient's colour. */
public class CatalystItem extends Item {
    private final ResourceLocation catalyst;

    public CatalystItem(ResourceLocation catalyst, Properties properties) {
        super(properties);
        this.catalyst = catalyst;
    }

    public ResourceLocation catalystId() {
        return catalyst;
    }

    @Nullable
    public static Catalyst catalystOf(ItemStack stack) {
        return stack.getItem() instanceof CatalystItem c ? WeaponData.catalyst(c.catalyst) : null;
    }

    public static int colourOf(ItemStack stack) {
        Catalyst c = catalystOf(stack);
        return c == null ? 0x9AA3B5 : c.colour();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        Catalyst c = WeaponData.catalyst(catalyst);
        if (c == null) {
            lines.add(Component.translatable("tooltip." + BspWeapons.MODID + ".catalyst_unknown").withStyle(ChatFormatting.GRAY));
            return;
        }
        int total = 0;
        for (Rarity r : Rarity.values()) total += c.weight(r);
        lines.add(Component.translatable("tooltip." + BspWeapons.MODID + ".catalyst_odds",
                pct(c, Rarity.NORMAL, total), pct(c, Rarity.RARE, total), pct(c, Rarity.EPIC, total), pct(c, Rarity.S, total)).withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable("tooltip." + BspWeapons.MODID + ".catalyst_use").withStyle(ChatFormatting.GRAY));
    }

    private static String pct(Catalyst c, Rarity r, int total) {
        return String.valueOf(Math.round(100.0 * c.weight(r) / total));
    }
}
