package com.mrgregles.bsp_weapons.registry;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.item.CoreItem;
import com.mrgregles.bsp_weapons.item.PartItem;
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
    public static final RegistryObject<CreativeModeTab> TAB;

    static {
        List<RegistryObject<PartItem>> parts = new ArrayList<>();
        for (String[] t : PART_TYPES) {
            PartKind kind = PartKind.byId(t[1]);
            parts.add(ITEMS.register(t[0], () -> new PartItem(kind, t[0], new Item.Properties().stacksTo(16))));
        }
        PARTS = Collections.unmodifiableList(parts);
        TAB = TABS.register("bsp_weapons", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + BspWeapons.MODID))
                .icon(() -> WEAPON_CORE.get().of(2))
                .displayItems((parameters, output) -> {
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

    /** The part item of a type, or null. */
    public static PartItem part(ResourceLocation typeId) {
        for (RegistryObject<PartItem> p : PARTS) if (p.get().typeId().equals(typeId)) return p.get();
        return null;
    }
}
