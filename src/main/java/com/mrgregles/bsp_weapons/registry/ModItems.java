package com.mrgregles.bsp_weapons.registry;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.item.CatalystItem;
import com.mrgregles.bsp_weapons.item.CoreItem;
import com.mrgregles.bsp_weapons.item.EngramItem;
import com.mrgregles.bsp_weapons.item.PartItem;
import com.mrgregles.bsp_weapons.item.WeaponItem;
import net.minecraft.world.item.BlockItem;
import com.mrgregles.bsp_weapons.parts.PartKind;
import com.mrgregles.bsp_weapons.parts.Rarity;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The mod's items: one part item per part type, the Weapon Core, and the BSP-Weapons creative tab. */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BspWeapons.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BspWeapons.MODID);

    /** The part types that are items, in creative-tab order; each name is also its model and its data file. */
    public static final String[][] PART_TYPES = {
            {"focused_barrel", "BARREL"}, {"scatter_barrel", "BARREL"}, {"thermal_barrel", "BARREL"}, {"pulse_barrel", "BARREL"},
            {"energy_cell", "CELL"}, {"overcharge_cell", "CELL"}, {"magazine", "CELL"}, {"emp_cell", "CELL"},
            {"standard_grip", "GRIP"}, {"light_grip", "GRIP"}, {"heavy_grip", "GRIP"},
            {"iron_sight", "OPTIC"}, {"scope", "OPTIC"}, {"holo_sight", "OPTIC"}
    };
    public static final List<RegistryObject<PartItem>> PARTS;
    public static final RegistryObject<CoreItem> WEAPON_CORE = ITEMS.register("weapon_core", () -> new CoreItem(new Item.Properties().stacksTo(16)));
    /** The weapon classes that are items, with their engrams; each name is a pattern data file and a weapon model. */
    public static final String[] WEAPONS = {"laser_pistol"};
    public static final String[] CATALYSTS = {"lapis_catalyst", "magnatite_catalyst", "illyrium_catalyst", "allthemodium_catalyst"};
    public static final List<RegistryObject<WeaponItem>> WEAPON_ITEMS;
    public static final List<RegistryObject<EngramItem>> ENGRAMS;
    public static final List<RegistryObject<CatalystItem>> CATALYST_ITEMS;
    /** The round every magazine weapon loads (D68, D69). */
    public static final RegistryObject<Item> ENERGY_ROUND = ITEMS.register("energy_round", () -> new Item(new Item.Properties()) {
        @Override
        public void appendHoverText(net.minecraft.world.item.ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.Level level, java.util.List<Component> lines, net.minecraft.world.item.TooltipFlag flag) {
            lines.add(Component.translatable("tooltip." + BspWeapons.MODID + ".energy_round").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    });
    public static final RegistryObject<BlockItem> WEAPON_ASSEMBLER = ITEMS.register("weapon_assembler", () -> new BlockItem(ModBlocks.WEAPON_ASSEMBLER.get(), new Item.Properties()));
    public static final RegistryObject<CreativeModeTab> TAB;

    static {
        List<RegistryObject<PartItem>> parts = new ArrayList<>();
        for (String[] t : PART_TYPES) {
            PartKind kind = PartKind.byId(t[1]);
            parts.add(ITEMS.register(t[0], () -> new PartItem(kind, t[0], new Item.Properties().stacksTo(16))));
        }
        PARTS = Collections.unmodifiableList(parts);
        List<RegistryObject<WeaponItem>> weapons = new ArrayList<>();
        List<RegistryObject<EngramItem>> engrams = new ArrayList<>();
        for (String w : WEAPONS) {
            ResourceLocation id = new ResourceLocation(BspWeapons.MODID, w);
            weapons.add(ITEMS.register(w, () -> new WeaponItem(id, new Item.Properties().stacksTo(1))));
            engrams.add(ITEMS.register(w + "_engram", () -> new EngramItem(id, new Item.Properties().stacksTo(1))));
        }
        WEAPON_ITEMS = Collections.unmodifiableList(weapons);
        ENGRAMS = Collections.unmodifiableList(engrams);
        List<RegistryObject<CatalystItem>> catalysts = new ArrayList<>();
        for (String c : CATALYSTS) {
            ResourceLocation id = new ResourceLocation(BspWeapons.MODID, c);
            catalysts.add(ITEMS.register(c, () -> new CatalystItem(id, new Item.Properties().stacksTo(16))));
        }
        CATALYST_ITEMS = Collections.unmodifiableList(catalysts);
        TAB = TABS.register("bsp_weapons", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + BspWeapons.MODID))
                .icon(() -> WEAPON_CORE.get().of(2))
                .displayItems((parameters, output) -> {
                    output.accept(WEAPON_ASSEMBLER.get());
                    for (RegistryObject<EngramItem> e : ENGRAMS) output.accept(e.get());
                    for (RegistryObject<CatalystItem> c : CATALYST_ITEMS) output.accept(c.get());
                    output.accept(ENERGY_ROUND.get());
                    for (int tier = 1; tier <= CoreItem.ROMAN.length; tier++) output.accept(WEAPON_CORE.get().of(tier));
                    ResourceLocation iron = new ResourceLocation(BspWeapons.MODID, "iron");
                    for (RegistryObject<PartItem> p : PARTS) output.accept(p.get().of(iron, Rarity.NORMAL));
                })
                .build());
    }

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        TABS.register(modBus);
    }

    /** The weapon item of a pattern, or null. */
    public static WeaponItem weapon(ResourceLocation pattern) {
        for (RegistryObject<WeaponItem> w : WEAPON_ITEMS) if (w.get().pattern().equals(pattern)) return w.get();
        return null;
    }

    /** The part item of a type, or null. */
    public static PartItem part(ResourceLocation typeId) {
        for (RegistryObject<PartItem> p : PARTS) if (p.get().typeId().equals(typeId)) return p.get();
        return null;
    }
}
