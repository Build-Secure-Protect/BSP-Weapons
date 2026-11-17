package com.mrgregles.bsp_weapons.item;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.WeaponsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** The Weapon Core (D8, D22): every pattern's first part; its tier, I to V, sets the weapon's level and is upgraded in place. */
public class CoreItem extends Item {
    public static final String TIER = "Tier";
    /** The tier glow colours I to V, as the mock-ups drew them. */
    public static final int[] TIER_COLOURS = {0x9AA3B5, 0x19D3B0, 0x4DA3FF, 0xB58CFF, 0xFFD23A};
    public static final String[] ROMAN = {"I", "II", "III", "IV", "V"};

    public CoreItem(Properties properties) {
        super(properties);
    }

    public ItemStack of(int tier) {
        ItemStack stack = new ItemStack(this);
        stack.getOrCreateTag().putInt(TIER, clamp(tier));
        return stack;
    }

    /** The core's tier, 1 to the config's count; a core with no tag is Tier I. */
    public static int tierOf(ItemStack stack) {
        CompoundTag t = stack.getTag();
        return t == null ? 1 : clamp(t.getInt(TIER));
    }

    static int clamp(int tier) {
        return Math.max(1, Math.min(WeaponsConfig.tiers(), tier));
    }

    public static int colourOf(ItemStack stack) {
        return TIER_COLOURS[Math.min(TIER_COLOURS.length, tierOf(stack)) - 1];
    }

    public static String roman(int tier) {
        return tier >= 1 && tier <= ROMAN.length ? ROMAN[tier - 1] : String.valueOf(tier);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        int tier = tierOf(stack);
        lines.add(Component.translatable("tooltip." + BspWeapons.MODID + ".core_tier", roman(tier)).withStyle(ChatFormatting.LIGHT_PURPLE));
        int slots = WeaponsConfig.tierAugmentSlots(tier);
        lines.add(Component.translatable("tooltip." + BspWeapons.MODID + (slots == 1 ? ".core_slot" : ".core_slots"), slots).withStyle(ChatFormatting.GRAY));
    }
}
