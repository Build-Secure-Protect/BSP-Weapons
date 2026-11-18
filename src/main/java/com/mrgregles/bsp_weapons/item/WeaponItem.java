package com.mrgregles.bsp_weapons.item;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.data.WeaponData;
import com.mrgregles.bsp_weapons.parts.PartKind;
import com.mrgregles.bsp_weapons.parts.PartSpec;
import com.mrgregles.bsp_weapons.parts.Sheet;
import com.mrgregles.bsp_weapons.parts.SheetCalculator;
import com.mrgregles.bsp_weapons.parts.Stat;
import com.mrgregles.bsp_weapons.parts.WeaponMaterial;
import com.mrgregles.bsp_weapons.parts.WeaponSpec;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.mrgregles.bsp_weapons.weapon.Firing;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

/**
 * An assembled weapon: one item per class, the {@link WeaponSpec} in its tag and nothing else. The model is the pistol's
 * voxel model with each slot's metal on its own tint (barrel 0, cell 1, grip 2, optic 3) and the core glow on 4. Firing
 * is WP-4; this item holds, shows and takes apart.
 */
public class WeaponItem extends Item {
    /** The stats the tooltip lists, in order. */
    public static final Stat[] SHOWN = {Stat.DAMAGE, Stat.PIERCING, Stat.RANGE, Stat.FIRE_RATE, Stat.CAPACITY, Stat.HEAT_CAP, Stat.DURABILITY};
    private static final PartKind[] TINTS = {PartKind.BARREL, PartKind.CELL, PartKind.GRIP, PartKind.OPTIC};
    private final ResourceLocation pattern;

    public WeaponItem(ResourceLocation pattern, Properties properties) {
        super(properties);
        this.pattern = pattern;
    }

    public ResourceLocation pattern() {
        return pattern;
    }

    public ItemStack of(WeaponSpec spec) {
        ItemStack stack = new ItemStack(this);
        stack.getOrCreateTag().put(WeaponSpec.TAG, spec.toTag());
        boolean full = com.mrgregles.bsp_weapons.WeaponsConfig.ASSEMBLED_FULL.get();
        if (com.mrgregles.bsp_weapons.weapon.WeaponEnergy.holdsEnergy(stack)) {
            if (full) com.mrgregles.bsp_weapons.weapon.WeaponEnergy.setStored(stack, com.mrgregles.bsp_weapons.weapon.WeaponEnergy.capacity(stack));
        } else {
            Sheet sheet = Firing.sheetOf(stack);
            stack.getOrCreateTag().putInt(Firing.CHARGE, full && sheet != null ? (int) Math.round(sheet.get(Stat.CAPACITY)) : 0);
        }
        return stack;
    }

    /** The Forge Energy store of a weapon with an energy cell (D10). */
    @Override
    public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new com.mrgregles.bsp_weapons.weapon.WeaponEnergy(stack);
    }

    /** Writes a new spec onto an existing weapon, keeping its charge, heat and everything else in the tag (the enhancer's way of changing a weapon). */
    public static void setSpec(ItemStack stack, WeaponSpec spec) {
        stack.getOrCreateTag().put(WeaponSpec.TAG, spec.toTag());
    }

    /** The weapon's spec, or null for a stack that is not a weapon or has no spec. */
    @Nullable
    public static WeaponSpec specOf(ItemStack stack) {
        if (!(stack.getItem() instanceof WeaponItem item)) return null;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(WeaponSpec.TAG)) return null;
        WeaponSpec spec = WeaponSpec.fromTag(tag.getCompound(WeaponSpec.TAG));
        return spec.pattern().equals(item.pattern) ? spec : null; // a pistol's tag on a rifle item is no weapon
    }

    /** The tint for an index: a slot's material colour, the core's tier colour for 4 (breathing red while locked), the glows for 5 (red while hot: D66). */
    public static int colourOf(ItemStack stack, int tint) {
        WeaponSpec spec = specOf(stack);
        if (spec == null) return PartItem.UNKNOWN_COLOUR;
        long now = clientTime();
        if (tint == 5) {
            Sheet sheet = Firing.sheetOf(stack);
            double share = sheet == null ? 0 : Firing.heat(stack, now) / Math.max(1, sheet.get(Stat.HEAT_CAP));
            boolean flash = share > 0.75 && (System.currentTimeMillis() / 120) % 2 == 0;
            return flash ? 0xFF6B5C : 0x19D3B0;
        }
        if (tint == 4) {
            int tierColour = CoreItem.TIER_COLOURS[Math.max(1, Math.min(CoreItem.TIER_COLOURS.length, spec.coreTier())) - 1];
            if (Firing.locked(stack, now)) {
                double k = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 300.0);
                int r = (int) (0x3A + (0xFF - 0x3A) * k), g = (int) (0x0E + (0x6B - 0x0E) * k), b = (int) (0x0A + (0x5C - 0x0A) * k);
                return (r << 16) | (g << 8) | b;
            }
            return tierColour;
        }
        if (tint < 0 || tint >= TINTS.length) return 0xFFFFFF;
        PartSpec p = spec.part(TINTS[tint]);
        WeaponMaterial m = p == null ? null : WeaponData.material(p.material());
        return m == null ? PartItem.UNKNOWN_COLOUR : m.colour();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        WeaponSpec spec = specOf(stack);
        if (spec == null) {
            lines.add(Component.translatable("tooltip." + BspWeapons.MODID + ".weapon_unassembled").withStyle(ChatFormatting.GRAY));
            return;
        }
        lines.add(Component.translatable("tooltip." + BspWeapons.MODID + ".weapon", Component.translatable("rarity." + BspWeapons.MODID + "." + spec.roll().id()), CoreItem.roman(spec.coreTier())).withStyle(ChatFormatting.LIGHT_PURPLE));
        Sheet sheet = SheetCalculator.compute(spec);
        // the chips (D79): how many of the sockets are used, then each chip in its augment's colour with its roll and points
        lines.add(Component.translatable("tooltip." + BspWeapons.MODID + ".weapon_chips", spec.augments().size(), SheetCalculator.augmentSlots(spec)).withStyle(ChatFormatting.GRAY));
        for (com.mrgregles.bsp_weapons.parts.FittedChip c : spec.augments()) {
            com.mrgregles.bsp_weapons.parts.Augment a = WeaponData.augment(c.augment());
            Component name = a == null ? Component.translatable("tooltip." + BspWeapons.MODID + ".chip_unknown") : Component.translatable(a.nameKey());
            int colour = a == null ? PartItem.UNKNOWN_COLOUR : a.colour();
            lines.add(Component.literal("  ").append(name).append(" · ").append(Component.translatable("rarity." + BspWeapons.MODID + "." + c.rarity().id()))
                    .append(" · " + c.durability() + " / " + (a == null ? "?" : String.valueOf(a.durability())))
                    .withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.fromRgb(colour))));
        }
        if (!com.mrgregles.bsp_weapons.weapon.WeaponEnergy.holdsEnergy(stack)) {
            lines.add(Component.translatable("tooltip." + BspWeapons.MODID + ".rounds", Firing.charge(stack), Math.round(sheet.get(Stat.CAPACITY))).withStyle(ChatFormatting.AQUA));
        }
        if (com.mrgregles.bsp_weapons.weapon.WeaponEnergy.holdsEnergy(stack)) {
            lines.add(Component.translatable("tooltip." + BspWeapons.MODID + ".energy", String.format(Locale.ROOT, "%,d", com.mrgregles.bsp_weapons.weapon.WeaponEnergy.stored(stack)), String.format(Locale.ROOT, "%,d", com.mrgregles.bsp_weapons.weapon.WeaponEnergy.capacity(stack))).withStyle(ChatFormatting.AQUA));
        }
        for (Stat s : SHOWN) {
            lines.add(Component.translatable("stat." + BspWeapons.MODID + "." + s.id()).withStyle(ChatFormatting.GRAY).append(Component.literal(" " + number(s, sheet.get(s))).withStyle(ChatFormatting.AQUA)));
        }
        for (PartSpec p : spec.parts()) {
            WeaponMaterial m = WeaponData.material(p.material());
            Component material = m == null ? Component.translatable("tooltip." + BspWeapons.MODID + ".unknown_material") : Component.translatable("material." + BspWeapons.MODID + "." + m.id().getPath());
            lines.add(Component.translatable("item." + BspWeapons.MODID + "." + p.type().getPath()).withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY)).append(material.copy().withStyle(ChatFormatting.DARK_GRAY)));
        }
    }

    /** The client's game time for the tints, supplied by the client setup; 0 on a server, where no tint is ever asked for. */
    public static java.util.function.LongSupplier CLIENT_TIME = () -> 0;

    private static long clientTime() {
        return CLIENT_TIME.getAsLong();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (specOf(stack) == null) return InteractionResultHolder.pass(stack);
        if (level instanceof ServerLevel sl) {
            Firing.Refusal why = Firing.tryFire(sl, player, stack);
            if (why == Firing.Refusal.NONE) {
                Sheet sheet = Firing.sheetOf(stack);
                if (sheet != null) player.getCooldowns().addCooldown(this, Firing.cooldownTicks(sheet));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Durability is the sheet's (D28); a weapon with no spec has none. */
    @Override
    public int getMaxDamage(ItemStack stack) {
        Sheet sheet = Firing.sheetOf(stack);
        return sheet == null ? 0 : Math.max(1, (int) Math.round(sheet.get(Stat.DURABILITY)));
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return getMaxDamage(stack) > 0;
    }

    @Override
    public boolean canBeDepleted() {
        return true;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return specOf(stack) != null;
    }

    @Override
    public int getEnchantmentValue() {
        return 10;
    }

    /** Unbreaking, Mending and the curses (D31); bow enchantments stay off. */
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment == Enchantments.UNBREAKING || enchantment == Enchantments.MENDING || enchantment == Enchantments.VANISHING_CURSE || enchantment == Enchantments.BINDING_CURSE;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return true;
    }

    /** "6.4", "8%", "36": one decimal where it matters, none where it does not. */
    public static String number(Stat s, double v) {
        if (s == Stat.PIERCING) return Math.round(v) + "%";
        if (s == Stat.DAMAGE || s == Stat.FIRE_RATE || s == Stat.SPREAD || s == Stat.CHARGE_TIME) return String.format(Locale.ROOT, "%.1f", v);
        return String.valueOf(Math.round(v));
    }
}
