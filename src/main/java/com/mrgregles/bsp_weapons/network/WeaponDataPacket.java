package com.mrgregles.bsp_weapons.network;

import com.mrgregles.bsp_weapons.data.WeaponData;
import com.mrgregles.bsp_weapons.parts.BandDefaults;
import com.mrgregles.bsp_weapons.parts.Catalyst;
import com.mrgregles.bsp_weapons.parts.Pattern;
import com.mrgregles.bsp_weapons.parts.Rarity;
import com.mrgregles.bsp_weapons.parts.Sheet;
import com.mrgregles.bsp_weapons.parts.WeaponKind;
import com.mrgregles.bsp_weapons.parts.Modifiers;
import com.mrgregles.bsp_weapons.parts.PartKind;
import com.mrgregles.bsp_weapons.parts.PartType;
import com.mrgregles.bsp_weapons.parts.Stat;
import com.mrgregles.bsp_weapons.parts.WeaponMaterial;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The part data a client needs for icons and tooltips (materials, bands, part types), sent when a player joins and on
 * every {@code /reload}. Lists are capped on the reading side so a bad packet cannot ask for more than a few thousand
 * entries. Patterns, augments and catalysts are not needed by the client yet.
 */
public record WeaponDataPacket(Map<ResourceLocation, WeaponMaterial> materials, Map<Integer, BandDefaults> bands, Map<ResourceLocation, PartType> partTypes,
                               Map<ResourceLocation, Pattern> patterns, Map<ResourceLocation, Catalyst> catalysts) {
    public static final int MAX_ENTRIES = 4096;

    public static WeaponDataPacket current() {
        return new WeaponDataPacket(WeaponData.materials(), WeaponData.bands(), WeaponData.partTypes(), WeaponData.patterns(), WeaponData.catalysts());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(materials.size());
        for (WeaponMaterial m : materials.values()) {
            buf.writeResourceLocation(m.id());
            buf.writeResourceLocation(m.tag());
            buf.writeByte(m.band());
            buf.writeInt(m.colour());
            buf.writeByte(kindsMask(m.kinds()));
            buf.writeVarInt(m.modifiers().size());
            for (Map.Entry<PartKind, Modifiers> e : m.modifiers().entrySet()) {
                buf.writeByte(e.getKey().ordinal());
                writeModifiers(buf, e.getValue());
            }
            buf.writeUtf(m.trait(), 64);
        }
        buf.writeVarInt(bands.size());
        for (BandDefaults b : bands.values()) {
            buf.writeByte(b.band());
            buf.writeVarInt(b.byKind().size());
            for (Map.Entry<PartKind, Modifiers> e : b.byKind().entrySet()) {
                buf.writeByte(e.getKey().ordinal());
                writeModifiers(buf, e.getValue());
            }
            writeModifiers(buf, b.any());
        }
        buf.writeVarInt(partTypes.size());
        for (PartType t : partTypes.values()) {
            buf.writeResourceLocation(t.id());
            buf.writeByte(t.kind().ordinal());
            writeModifiers(buf, t.modifiers());
            buf.writeVarInt(t.classes().size());
            for (ResourceLocation c : t.classes()) buf.writeResourceLocation(c);
            buf.writeBoolean(t.holdsEnergy());
        }
        buf.writeVarInt(patterns.size());
        for (Pattern p : patterns.values()) {
            buf.writeResourceLocation(p.id());
            buf.writeByte(p.kind().ordinal());
            buf.writeVarInt(p.slots().size());
            for (PartKind k : p.slots()) buf.writeByte(k.ordinal());
            for (Stat s : Stat.values()) buf.writeDouble(p.base().get(s));
        }
        buf.writeVarInt(catalysts.size());
        for (Catalyst c : catalysts.values()) {
            buf.writeResourceLocation(c.id());
            buf.writeUtf(c.ingredient(), 128);
            for (Rarity r : Rarity.values()) buf.writeVarInt(c.weight(r));
            buf.writeInt(c.colour());
        }
    }

    public static WeaponDataPacket decode(FriendlyByteBuf buf) {
        Map<ResourceLocation, WeaponMaterial> materials = new HashMap<>();
        int n = Math.min(MAX_ENTRIES, buf.readVarInt());
        for (int i = 0; i < n; i++) {
            ResourceLocation id = buf.readResourceLocation(), tag = buf.readResourceLocation();
            int band = buf.readByte(), colour = buf.readInt();
            Set<PartKind> kinds = kindsFromMask(buf.readByte());
            int mc = Math.min(PartKind.values().length, buf.readVarInt());
            Map<PartKind, Modifiers> mods = new EnumMap<>(PartKind.class);
            for (int j = 0; j < mc; j++) {
                PartKind k = PartKind.values()[Math.min(PartKind.values().length - 1, Math.max(0, buf.readByte()))];
                mods.put(k, readModifiers(buf));
            }
            String trait = buf.readUtf(64);
            materials.put(id, new WeaponMaterial(id, tag, Math.max(1, Math.min(5, band)), colour, kinds, mods, trait));
        }
        Map<Integer, BandDefaults> bands = new HashMap<>();
        int bn = Math.min(16, buf.readVarInt());
        for (int i = 0; i < bn; i++) {
            int band = buf.readByte();
            int kc = Math.min(PartKind.values().length, buf.readVarInt());
            Map<PartKind, Modifiers> byKind = new EnumMap<>(PartKind.class);
            for (int j = 0; j < kc; j++) {
                PartKind k = PartKind.values()[Math.min(PartKind.values().length - 1, Math.max(0, buf.readByte()))];
                byKind.put(k, readModifiers(buf));
            }
            bands.put(band, new BandDefaults(band, byKind, readModifiers(buf)));
        }
        Map<ResourceLocation, PartType> types = new HashMap<>();
        int tn = Math.min(MAX_ENTRIES, buf.readVarInt());
        for (int i = 0; i < tn; i++) {
            ResourceLocation id = buf.readResourceLocation();
            PartKind k = PartKind.values()[Math.min(PartKind.values().length - 1, Math.max(0, buf.readByte()))];
            Modifiers m = readModifiers(buf);
            int cc = Math.min(256, buf.readVarInt());
            Set<ResourceLocation> classes = new HashSet<>();
            for (int j = 0; j < cc; j++) classes.add(buf.readResourceLocation());
            boolean energy = buf.readBoolean();
            types.put(id, new PartType(id, k, m, classes, energy ? PartType.ENERGY : PartType.ROUNDS));
        }
        Map<ResourceLocation, Pattern> patterns = new HashMap<>();
        int pn = Math.min(MAX_ENTRIES, buf.readVarInt());
        for (int i = 0; i < pn; i++) {
            ResourceLocation id = buf.readResourceLocation();
            WeaponKind kind = WeaponKind.values()[Math.min(WeaponKind.values().length - 1, Math.max(0, buf.readByte()))];
            int sc = Math.min(PartKind.values().length, buf.readVarInt());
            java.util.List<PartKind> slots = new java.util.ArrayList<>();
            for (int j = 0; j < sc; j++) {
                PartKind k = PartKind.values()[Math.min(PartKind.values().length - 1, Math.max(0, buf.readByte()))];
                if (k != PartKind.CORE && !slots.contains(k)) slots.add(k);
            }
            Sheet base = new Sheet();
            for (Stat s : Stat.values()) base.set(s, buf.readDouble());
            patterns.put(id, new Pattern(id, kind, slots, base));
        }
        Map<ResourceLocation, Catalyst> catalysts = new HashMap<>();
        int cn = Math.min(MAX_ENTRIES, buf.readVarInt());
        for (int i = 0; i < cn; i++) {
            ResourceLocation id = buf.readResourceLocation();
            String ingredient = buf.readUtf(128);
            int[] odds = new int[Rarity.values().length];
            for (int j = 0; j < odds.length; j++) odds[j] = Math.max(0, buf.readVarInt());
            int colour = buf.readInt();
            try {
                catalysts.put(id, new Catalyst(id, ingredient, odds, colour));
            } catch (IllegalArgumentException ignored) {
                // odds of all zero in a bad packet: skip the catalyst
            }
        }
        return new WeaponDataPacket(materials, bands, types, patterns, catalysts);
    }

    static void writeModifiers(FriendlyByteBuf buf, Modifiers m) {
        buf.writeVarInt(m.changes().size());
        for (Map.Entry<Stat, double[]> e : m.changes().entrySet()) {
            buf.writeByte(e.getKey().ordinal());
            buf.writeDouble(e.getValue()[0]);
            buf.writeDouble(e.getValue()[1]);
        }
    }

    static Modifiers readModifiers(FriendlyByteBuf buf) {
        Modifiers m = new Modifiers();
        int n = Math.min(Stat.values().length, buf.readVarInt());
        for (int i = 0; i < n; i++) {
            Stat s = Stat.values()[Math.min(Stat.values().length - 1, Math.max(0, buf.readByte()))];
            m.flat(s, buf.readDouble()).percent(s, buf.readDouble());
        }
        return m;
    }

    static int kindsMask(Set<PartKind> kinds) {
        int mask = 0;
        for (PartKind k : kinds) mask |= 1 << k.ordinal();
        return mask;
    }

    static Set<PartKind> kindsFromMask(int mask) {
        Set<PartKind> kinds = EnumSet.noneOf(PartKind.class);
        for (PartKind k : PartKind.values()) if ((mask & (1 << k.ordinal())) != 0) kinds.add(k);
        return kinds;
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> WeaponData.acceptSync(materials, bands, partTypes, patterns, catalysts));
        ctx.get().setPacketHandled(true);
    }
}
