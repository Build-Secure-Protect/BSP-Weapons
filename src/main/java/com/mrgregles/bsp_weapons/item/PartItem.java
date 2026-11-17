package com.mrgregles.bsp_weapons.item;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.data.WeaponData;
import com.mrgregles.bsp_weapons.parts.Modifiers;
import com.mrgregles.bsp_weapons.parts.PartKind;
import com.mrgregles.bsp_weapons.parts.PartSpec;
import com.mrgregles.bsp_weapons.parts.Rarity;
import com.mrgregles.bsp_weapons.parts.SheetCalculator;
import com.mrgregles.bsp_weapons.parts.Stat;
import com.mrgregles.bsp_weapons.parts.WeaponMaterial;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A weapon part: one item per part type (a Focused Barrel, an Energy Cell), the material and the part's own roll in its
 * tag (D18, D20). Its icon is its voxel model, tinted by the material (D48); the tooltip shows the rarity and material
 * on one line and the part's own changes to the sheet under it (D50). Nothing but the ids is kept on the item.
 */
public class PartItem extends Item {
    public static final String MATERIAL = "Material", RARITY = "Rarity";
    public static final int UNKNOWN_COLOUR = 0x9AA3B5;
    private final PartKind kind;
    private final ResourceLocation typeId;

    public PartItem(PartKind kind, String typeName, Properties properties) {
        super(properties);
        this.kind = kind;
        this.typeId = new ResourceLocation(BspWeapons.MODID, typeName);
    }

    public PartKind kind() {
        return kind;
    }

    public ResourceLocation typeId() {
        return typeId;
    }

    /** A part of this type made of the material, with its roll. */
    public ItemStack of(ResourceLocation material, Rarity rarity) {
        ItemStack stack = new ItemStack(this);
        CompoundTag t = stack.getOrCreateTag();
        t.putString(MATERIAL, material.toString());
        t.putString(RARITY, rarity.id());
        return stack;
    }

    public static ResourceLocation materialOf(ItemStack stack) {
        CompoundTag t = stack.getTag();
        ResourceLocation id = t == null ? null : ResourceLocation.tryParse(t.getString(MATERIAL));
        return id == null ? new ResourceLocation(BspWeapons.MODID, "unknown") : id;
    }

    public static Rarity rarityOf(ItemStack stack) {
        CompoundTag t = stack.getTag();
        Rarity r = t == null ? null : Rarity.byId(t.getString(RARITY));
        return r == null ? Rarity.NORMAL : r;
    }

    /** The part as the weapon will remember it, or null for a stack that is not a part. */
    @Nullable
    public static PartSpec specOf(ItemStack stack) {
        if (!(stack.getItem() instanceof PartItem p)) return null;
        return new PartSpec(p.kind, p.typeId, materialOf(stack), rarityOf(stack));
    }

    /** The material's tint for the model's tinted faces, or the unknown grey. */
    public static int colourOf(ItemStack stack) {
        WeaponMaterial m = WeaponData.material(materialOf(stack));
        return m == null ? UNKNOWN_COLOUR : m.colour();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        PartSpec spec = specOf(stack);
        if (spec == null) return;
        WeaponMaterial material = WeaponData.material(spec.material());
        Component rarity = Component.translatable("rarity." + BspWeapons.MODID + "." + spec.rarity().id());
        if (material == null) {
            lines.add(Component.translatable("tooltip." + BspWeapons.MODID + ".part", rarity, Component.translatable("tooltip." + BspWeapons.MODID + ".unknown_material")).withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        Component materialName = Component.translatable("material." + BspWeapons.MODID + "." + material.id().getPath());
        lines.add(Component.translatable("tooltip." + BspWeapons.MODID + ".part", rarity, materialName).withStyle(ChatFormatting.LIGHT_PURPLE));
        Modifiers m = SheetCalculator.partModifiers(spec);
        for (Map.Entry<Stat, double[]> e : m.changes().entrySet()) {
            double flat = e.getValue()[0], percent = e.getValue()[1];
            if (flat == 0 && percent == 0) continue;
            StringBuilder v = new StringBuilder();
            if (flat != 0) v.append(signed(flat, false));
            if (percent != 0) v.append(flat != 0 ? " " : "").append(signed(percent, true));
            lines.add(Component.translatable("stat." + BspWeapons.MODID + "." + e.getKey().id()).withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(" " + v).withStyle(ChatFormatting.AQUA)));
        }
    }

    /** "+14%", "-30%", "+60", "+1.5": a sign always, no trailing zeros. */
    static String signed(double v, boolean percent) {
        String n = Math.abs(v - Math.rint(v)) < 1e-9 ? String.valueOf((long) Math.rint(v)) : String.format(Locale.ROOT, "%.1f", v);
        return (v > 0 ? "+" : "") + n + (percent ? "%" : "");
    }
}
