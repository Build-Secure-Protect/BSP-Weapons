package com.mrgregles.bsp_weapons.network;

import com.mrgregles.bsp_weapons.data.WeaponData;
import com.mrgregles.bsp_weapons.parts.BandDefaults;
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
public record WeaponDataPacket(Map<ResourceLocation, WeaponMaterial> materials, Map<Integer, BandDefaults> bands, Map<ResourceLocation, PartType> partTypes) {
    public static final int MAX_ENTRIES = 4096;

    public static WeaponDataPacket current() {
        return new WeaponDataPacket(WeaponData.materials(), WeaponData.bands(), WeaponData.partTypes());
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
            types.put(id, new PartType(id, k, m, classes));
        }
        return new WeaponDataPacket(materials, bands, types);
    }

    static void writeModifiers(FriendlyByteBuf buf, Modifiers m) {
        buf.writeVarInt(m.changes().size());
        for (Map.Entry<Stat, double[]> e : m.changes().entrySet()) {
            buf.writeByte(e.getKey().ordinal());
            buf.writeFloat((float) e.getValue()[0]);
            buf.writeFloat((float) e.getValue()[1]);
        }
    }

    static Modifiers readModifiers(FriendlyByteBuf buf) {
        Modifiers m = new Modifiers();
        int n = Math.min(Stat.values().length, buf.readVarInt());
        for (int i = 0; i < n; i++) {
            Stat s = Stat.values()[Math.min(Stat.values().length - 1, Math.max(0, buf.readByte()))];
            m.flat(s, buf.readFloat()).percent(s, buf.readFloat());
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
        ctx.get().enqueueWork(() -> WeaponData.acceptSync(materials, bands, partTypes));
        ctx.get().setPacketHandled(true);
    }
}
