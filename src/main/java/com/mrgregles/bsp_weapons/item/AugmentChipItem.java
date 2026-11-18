package com.mrgregles.bsp_weapons.item;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.WeaponsConfig;
import com.mrgregles.bsp_weapons.data.WeaponData;
import com.mrgregles.bsp_weapons.parts.Augment;
import com.mrgregles.bsp_weapons.parts.FittedChip;
import com.mrgregles.bsp_weapons.parts.Rarity;
import com.mrgregles.bsp_weapons.parts.Stat;
import com.mrgregles.bsp_weapons.parts.WeaponKind;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Augment Chip (D71, D72, D81, D83, D86): one item for every augment, the augment's id, the chip's own roll and its
 * points left in the tag. Named "<Augment> Chip", its crystal tinted from the augment's data (tint 0 the body, 1 the
 * glowing edges). The tooltip: the roll on a violet line with the multiplier it gives the effect, each effect scaled by
 * it (teal where it helps, red where it costs), the points left, what it fits.
 */
public class AugmentChipItem extends Item {
    public static final String AUGMENT = "Augment", RARITY = "Rarity", DURABILITY = "Durability";
    public static final int UNKNOWN_COLOUR = PartItem.UNKNOWN_COLOUR;

    public AugmentChipItem(Properties properties) {
        super(properties);
    }

    /** A fresh chip of an augment with a roll: full points. */
    public ItemStack of(ResourceLocation augment, Rarity rarity) {
        Augment a = WeaponData.augment(augment);
        return of(augment, rarity, a == null ? Augment.DEFAULT_DURABILITY : a.durability());
    }

    public ItemStack of(ResourceLocation augment, Rarity rarity, int durability) {
        ItemStack stack = new ItemStack(this);
        CompoundTag t = stack.getOrCreateTag();
        t.putString(AUGMENT, augment.toString());
        t.putString(RARITY, rarity.id());
        t.putInt(DURABILITY, Math.max(0, Math.min(FittedChip.MAX_DURABILITY, durability)));
        return stack;
    }

    /** The chip as it comes out of a weapon, with the points it has left. */
    public ItemStack of(FittedChip chip) {
        return of(chip.augment(), chip.rarity(), chip.durability());
    }

    /** The chip as a weapon would remember it, or null for a stack that is not a chip. A bare stack is an unknown augment with no points. */
    @Nullable
    public static FittedChip chipOf(ItemStack stack) {
        if (!(stack.getItem() instanceof AugmentChipItem)) return null;
        CompoundTag t = stack.getTag();
        if (t == null) return new FittedChip(new ResourceLocation(BspWeapons.MODID, "unknown"), Rarity.NORMAL, 0);
        ResourceLocation id = ResourceLocation.tryParse(t.getString(AUGMENT));
        Rarity r = Rarity.byId(t.getString(RARITY));
        return new FittedChip(id == null ? new ResourceLocation(BspWeapons.MODID, "unknown") : id, r == null ? Rarity.NORMAL : r, t.getInt(DURABILITY));
    }

    @Nullable
    public static Augment augmentOf(ItemStack stack) {
        FittedChip chip = chipOf(stack);
        return chip == null ? null : WeaponData.augment(chip.augment());
    }

    /** The augment's colour for the crystal, or the unknown grey. */
    public static int colourOf(ItemStack stack) {
        Augment a = augmentOf(stack);
        return a == null ? UNKNOWN_COLOUR : a.colour();
    }

    @Override
    public Component getName(ItemStack stack) {
        Augment a = augmentOf(stack);
        if (a == null) return super.getName(stack);
        return Component.translatable("item." + BspWeapons.MODID + ".augment_chip.named", Component.translatable(a.nameKey()));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        FittedChip chip = chipOf(stack);
        Augment a = chip == null ? null : WeaponData.augment(chip.augment());
        if (chip == null || a == null) {
            lines.add(Component.translatable("tooltip." + BspWeapons.MODID + ".chip_unknown").withStyle(ChatFormatting.RED));
            return;
        }
        double mult = WeaponsConfig.rarityPartMultiplier(chip.rarity());
        lines.add(Component.translatable("tooltip." + BspWeapons.MODID + ".chip", Component.translatable("rarity." + BspWeapons.MODID + "." + chip.rarity().id()), multiplier(mult))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        for (Map.Entry<Stat, double[]> e : a.modifiers().changes().entrySet()) {
            double flat = e.getValue()[0] * mult, percent = e.getValue()[1] * mult;
            if (flat == 0 && percent == 0) continue;
            StringBuilder v = new StringBuilder();
            if (flat != 0) v.append(PartItem.signed(flat, false));
            if (percent != 0) v.append(flat != 0 ? " " : "").append(PartItem.signed(percent, true));
            boolean helps = (flat + percent > 0) != e.getKey().lowerIsBetter;
            lines.add(Component.translatable("stat." + BspWeapons.MODID + "." + e.getKey().id()).withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(" " + v).withStyle(helps ? ChatFormatting.AQUA : ChatFormatting.RED)));
        }
        if (a.push() > 0) lines.add(Component.translatable("tooltip." + BspWeapons.MODID + ".chip_push", String.format(Locale.ROOT, "%.1f", a.push() * mult)).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("tooltip." + BspWeapons.MODID + ".chip_durability", chip.durability(), a.durability()).withStyle(ChatFormatting.GRAY));
        if (!a.available()) lines.add(Component.translatable("tooltip." + BspWeapons.MODID + ".chip_not_yet").withStyle(ChatFormatting.RED));
        else if (a.kinds().isEmpty() && a.classes().isEmpty()) lines.add(Component.translatable("tooltip." + BspWeapons.MODID + ".chip_fits_any").withStyle(ChatFormatting.DARK_GRAY));
        else lines.add(Component.translatable("tooltip." + BspWeapons.MODID + ".chip_fits", fits(a)).withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("tooltip." + BspWeapons.MODID + ".chip_wear").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** "1", "1.05", "1.15": the multiplier without trailing zeros. */
    public static String multiplier(double mult) {
        String s = String.format(Locale.ROOT, "%.2f", mult);
        while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s;
    }

    /** What a limited augment fits: the kinds, then the classes by their item names. */
    static Component fits(Augment a) {
        List<Component> names = new ArrayList<>();
        for (WeaponKind k : a.kinds()) names.add(Component.translatable("kind." + BspWeapons.MODID + "." + k.name().toLowerCase(Locale.ROOT)));
        for (ResourceLocation c : a.classes()) names.add(Component.translatable("item." + c.getNamespace() + "." + c.getPath()));
        var out = Component.empty();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) out.append(", ");
            out.append(names.get(i));
        }
        return out;
    }
}
