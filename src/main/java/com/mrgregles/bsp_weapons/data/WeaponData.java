package com.mrgregles.bsp_weapons.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.parts.Augment;
import com.mrgregles.bsp_weapons.parts.BandDefaults;
import com.mrgregles.bsp_weapons.parts.Catalyst;
import com.mrgregles.bsp_weapons.parts.Modifiers;
import com.mrgregles.bsp_weapons.parts.PartKind;
import com.mrgregles.bsp_weapons.parts.PartType;
import com.mrgregles.bsp_weapons.parts.Pattern;
import com.mrgregles.bsp_weapons.parts.Sheet;
import com.mrgregles.bsp_weapons.parts.Stat;
import com.mrgregles.bsp_weapons.parts.WeaponKind;
import com.mrgregles.bsp_weapons.parts.WeaponMaterial;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * The part system's data, read from every datapack under {@code data/<namespace>/weapons/}: {@code patterns/},
 * {@code materials/}, {@code bands/}, {@code part_types/}, {@code augments/}, {@code catalysts/}. The mod's own files are
 * written by {@code tools/gen_weapon_data.py}; a server datapack can add or override any of them. A file that does not
 * parse is logged and skipped, so one typo never stops the server.
 */
public final class WeaponData {
    private static final Gson GSON = new GsonBuilder().create();
    private static volatile Map<ResourceLocation, Pattern> patterns = Map.of();
    private static volatile Map<ResourceLocation, WeaponMaterial> materials = Map.of();
    private static volatile Map<Integer, BandDefaults> bands = Map.of();
    private static volatile Map<ResourceLocation, PartType> partTypes = Map.of();
    private static volatile Map<ResourceLocation, Augment> augments = Map.of();
    private static volatile Map<ResourceLocation, Catalyst> catalysts = Map.of();

    private WeaponData() {
    }

    public static void register(AddReloadListenerEvent event) {
        event.addListener(new Loader<>("weapons/patterns", WeaponData::readPattern, m -> patterns = m));
        event.addListener(new Loader<>("weapons/materials", WeaponData::readMaterial, m -> materials = m));
        event.addListener(new Loader<>("weapons/bands", WeaponData::readBand, m -> {
            Map<Integer, BandDefaults> byBand = new HashMap<>();
            m.values().forEach(b -> byBand.put(b.band(), b));
            bands = Collections.unmodifiableMap(byBand);
        }));
        event.addListener(new Loader<>("weapons/part_types", WeaponData::readPartType, m -> partTypes = m));
        event.addListener(new Loader<>("weapons/augments", WeaponData::readAugment, m -> augments = m));
        event.addListener(new Loader<>("weapons/catalysts", WeaponData::readCatalyst, m -> catalysts = m));
    }

    public static Map<ResourceLocation, Pattern> patterns() { return patterns; }
    public static Map<ResourceLocation, WeaponMaterial> materials() { return materials; }
    public static Map<Integer, BandDefaults> bands() { return bands; }
    public static Map<ResourceLocation, PartType> partTypes() { return partTypes; }
    public static Map<ResourceLocation, Augment> augments() { return augments; }
    public static Map<ResourceLocation, Catalyst> catalysts() { return catalysts; }

    /** The client's copy of the part data, from {@code WeaponDataPacket}; on a single-player client it matches what the server loaded. */
    public static void acceptSync(Map<ResourceLocation, WeaponMaterial> m, Map<Integer, BandDefaults> b, Map<ResourceLocation, PartType> t,
                                  Map<ResourceLocation, Pattern> p, Map<ResourceLocation, Catalyst> c, Map<ResourceLocation, Augment> a) {
        materials = Collections.unmodifiableMap(new HashMap<>(m));
        bands = Collections.unmodifiableMap(new HashMap<>(b));
        partTypes = Collections.unmodifiableMap(new HashMap<>(t));
        patterns = Collections.unmodifiableMap(new HashMap<>(p));
        catalysts = Collections.unmodifiableMap(new HashMap<>(c));
        augments = Collections.unmodifiableMap(new HashMap<>(a));
    }

    public static Pattern pattern(ResourceLocation id) { return patterns.get(id); }
    public static WeaponMaterial material(ResourceLocation id) { return materials.get(id); }
    public static PartType partType(ResourceLocation id) { return partTypes.get(id); }
    public static Augment augment(ResourceLocation id) { return augments.get(id); }
    public static Catalyst catalyst(ResourceLocation id) { return catalysts.get(id); }

    /** The band's defaults, or empty defaults for a band no file describes. */
    public static BandDefaults band(int band) {
        BandDefaults b = bands.get(band);
        return b == null ? new BandDefaults(band, Map.of(), Modifiers.NONE) : b;
    }

    // ---- readers ---------------------------------------------------------------------------------------------------

    static Pattern readPattern(ResourceLocation id, JsonObject o) {
        WeaponKind kind = WeaponKind.byId(string(o, "kind"));
        if (kind == null) throw new IllegalArgumentException("unknown weapon kind '" + string(o, "kind") + "'");
        var slots = new java.util.ArrayList<PartKind>();
        for (JsonElement e : o.getAsJsonArray("slots")) {
            PartKind k = PartKind.byId(e.getAsString());
            if (k == null) throw new IllegalArgumentException("unknown slot '" + e.getAsString() + "'");
            slots.add(k);
        }
        Sheet base = new Sheet();
        for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("base").entrySet()) {
            Stat s = Stat.byId(e.getKey());
            if (s == null) throw new IllegalArgumentException("unknown stat '" + e.getKey() + "' in base");
            double v = e.getValue().getAsDouble();
            if (Double.isNaN(v) || Double.isInfinite(v)) throw new IllegalArgumentException("base '" + e.getKey() + "' is not a number");
            base.set(s, v);
        }
        return new Pattern(id, kind, slots, base);
    }

    static WeaponMaterial readMaterial(ResourceLocation id, JsonObject o) {
        ResourceLocation tag = new ResourceLocation(string(o, "tag"));
        int band = o.get("band").getAsInt();
        int colour = Integer.parseInt(string(o, "colour").replace("#", ""), 16);
        Set<PartKind> kinds = EnumSet.noneOf(PartKind.class);
        if (o.has("kinds")) for (JsonElement e : o.getAsJsonArray("kinds")) {
            PartKind k = PartKind.byId(e.getAsString());
            if (k == null) throw new IllegalArgumentException("unknown kind '" + e.getAsString() + "'");
            kinds.add(k);
        } else kinds.addAll(EnumSet.of(PartKind.BARREL, PartKind.CELL, PartKind.GRIP, PartKind.OPTIC));
        Map<PartKind, Modifiers> mods = new EnumMap<>(PartKind.class);
        if (o.has("modifiers")) for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("modifiers").entrySet()) {
            PartKind k = PartKind.byId(e.getKey());
            if (k == null) throw new IllegalArgumentException("unknown kind '" + e.getKey() + "' in modifiers");
            mods.put(k, Modifiers.fromJson(e.getValue().getAsJsonObject()));
        }
        String trait = o.has("trait") ? o.get("trait").getAsString() : "";
        return new WeaponMaterial(id, tag, band, colour, kinds, mods, trait);
    }

    static BandDefaults readBand(ResourceLocation id, JsonObject o) {
        int band = o.get("band").getAsInt();
        Map<PartKind, Modifiers> byKind = new EnumMap<>(PartKind.class);
        Modifiers any = Modifiers.NONE;
        for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("modifiers").entrySet()) {
            if (e.getKey().equalsIgnoreCase("ANY")) { any = Modifiers.fromJson(e.getValue().getAsJsonObject()); continue; }
            PartKind k = PartKind.byId(e.getKey());
            if (k == null) throw new IllegalArgumentException("unknown kind '" + e.getKey() + "'");
            byKind.put(k, Modifiers.fromJson(e.getValue().getAsJsonObject()));
        }
        return new BandDefaults(band, byKind, any);
    }

    static PartType readPartType(ResourceLocation id, JsonObject o) {
        PartKind kind = PartKind.byId(string(o, "kind"));
        if (kind == null || kind == PartKind.CORE) throw new IllegalArgumentException("a part type needs a kind other than the Core");
        Modifiers m = o.has("modifiers") ? Modifiers.fromJson(o.getAsJsonObject("modifiers")) : Modifiers.NONE;
        String ammo = o.has("ammo") ? o.get("ammo").getAsString() : PartType.ENERGY;
        if (!ammo.equals(PartType.ENERGY) && !ammo.equals(PartType.ROUNDS)) throw new IllegalArgumentException("ammo must be energy or rounds, not '" + ammo + "'");
        return new PartType(id, kind, m, ids(o, "classes"), ammo);
    }

    static Augment readAugment(ResourceLocation id, JsonObject o) {
        Modifiers m = o.has("modifiers") ? Modifiers.fromJson(o.getAsJsonObject("modifiers")) : Modifiers.NONE;
        Set<WeaponKind> kinds = EnumSet.noneOf(WeaponKind.class);
        if (o.has("kinds")) for (JsonElement e : o.getAsJsonArray("kinds")) {
            WeaponKind k = WeaponKind.byId(e.getAsString());
            if (k == null) throw new IllegalArgumentException("unknown weapon kind '" + e.getAsString() + "'");
            kinds.add(k);
        }
        int colour = o.has("colour") ? Integer.parseInt(string(o, "colour").replace("#", ""), 16) : Augment.DEFAULT_COLOUR;
        int durability = o.has("durability") ? o.get("durability").getAsInt() : Augment.DEFAULT_DURABILITY;
        if (durability < 1 || durability > 255) throw new IllegalArgumentException("durability must be 1 to 255, not " + durability);
        boolean available = !o.has("available") || o.get("available").getAsBoolean();
        double push = o.has("push") ? Modifiers.finite(o.get("push").getAsDouble(), "push") : 0;
        if (push < 0 || push > 64) throw new IllegalArgumentException("push must be 0 to 64 blocks, not " + push);
        return new Augment(id, m, kinds, ids(o, "classes"), colour, durability, available, push);
    }

    static Catalyst readCatalyst(ResourceLocation id, JsonObject o) {
        var arr = o.getAsJsonArray("odds");
        int[] odds = new int[arr.size()];
        for (int i = 0; i < arr.size(); i++) odds[i] = arr.get(i).getAsInt();
        int colour = o.has("colour") ? Integer.parseInt(string(o, "colour").replace("#", ""), 16) : 0x2A4AC8;
        return new Catalyst(id, string(o, "ingredient"), odds, colour);
    }

    private static String string(JsonObject o, String key) {
        if (!o.has(key)) throw new IllegalArgumentException("missing '" + key + "'");
        return o.get(key).getAsString();
    }

    private static Set<ResourceLocation> ids(JsonObject o, String key) {
        Set<ResourceLocation> set = new HashSet<>();
        if (o.has(key)) for (JsonElement e : o.getAsJsonArray(key)) set.add(new ResourceLocation(e.getAsString()));
        return set;
    }

    /** One folder of JSON files: each is read with {@code reader}; failures are logged, and the good ones are kept. */
    private static final class Loader<T> extends SimpleJsonResourceReloadListener {
        private final String folder;
        private final BiFunction<ResourceLocation, JsonObject, T> reader;
        private final java.util.function.Consumer<Map<ResourceLocation, T>> store;

        Loader(String folder, BiFunction<ResourceLocation, JsonObject, T> reader, java.util.function.Consumer<Map<ResourceLocation, T>> store) {
            super(GSON, folder);
            this.folder = folder;
            this.reader = reader;
            this.store = store;
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, T> out = new HashMap<>();
            int bad = 0;
            for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
                try {
                    out.put(e.getKey(), reader.apply(e.getKey(), e.getValue().getAsJsonObject()));
                } catch (RuntimeException ex) {
                    bad++;
                    BspWeapons.LOGGER.error("BSP-Weapons: {}/{}.json skipped: {}", folder, e.getKey(), ex.getMessage());
                }
            }
            store.accept(Collections.unmodifiableMap(out));
            BspWeapons.LOGGER.info("BSP-Weapons: {} {} loaded{}", out.size(), folder.substring(folder.indexOf('/') + 1), bad == 0 ? "" : " (" + bad + " skipped)");
        }
    }
}
