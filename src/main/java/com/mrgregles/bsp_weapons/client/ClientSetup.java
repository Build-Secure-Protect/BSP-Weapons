package com.mrgregles.bsp_weapons.client;

import com.mrgregles.bsp_weapons.item.AugmentChipItem;
import com.mrgregles.bsp_weapons.item.CatalystItem;
import com.mrgregles.bsp_weapons.item.CoreItem;
import com.mrgregles.bsp_weapons.item.PartItem;
import com.mrgregles.bsp_weapons.item.WeaponItem;
import com.mrgregles.bsp_weapons.registry.ModBlocks;
import com.mrgregles.bsp_weapons.registry.ModItems;
import com.mrgregles.bsp_weapons.registry.ModEntities;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

/** Client-only wiring: the model tints (D48). Loaded on the client only; the server never touches this class. */
public final class ClientSetup {
    private ClientSetup() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ClientSetup::onItemColours);
        modBus.addListener(ClientSetup::onClientSetup);
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(ModEntities.LASER_BOLT.get(), LaserBoltRenderer::new));
        modBus.addListener(WeaponHud::register);
    }

    private static Object tintTag;
    private static long tintTick = Long.MIN_VALUE;
    private static final int[] tints = new int[6];

    /** A pistol has about two hundred tinted quads: the six colours are worked out once per stack and tick, not per quad. */
    private static int weaponTint(net.minecraft.world.item.ItemStack stack, int tint) {
        long now = WeaponItem.CLIENT_TIME.getAsLong();
        if (stack.getTag() != tintTag || now != tintTick) {
            tintTag = stack.getTag(); tintTick = now;
            for (int i = 0; i < tints.length; i++) tints[i] = WeaponItem.colourOf(stack, i);
        }
        return tint >= 0 && tint < tints.length ? tints[tint] : 0xFFFFFF;
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModBlocks.WEAPON_ASSEMBLER_MENU.get(), WeaponAssemblerScreen::new);
            MenuScreens.register(ModBlocks.WEAPON_ENHANCER_MENU.get(), WeaponEnhancerScreen::new);
            MenuScreens.register(ModBlocks.WEAPON_FABRICATOR_MENU.get(), WeaponFabricatorScreen::new);
        });
        WeaponItem.CLIENT_TIME = () -> { var level = net.minecraft.client.Minecraft.getInstance().level; return level == null ? 0 : level.getGameTime(); };
    }

    /** Tint index 0 is the material on a part, tint index 1 the tier glow on a core, 0 and 1 the crystal and its edges on a chip; other faces have their own textures. */
    private static void onItemColours(RegisterColorHandlersEvent.Item event) {
        for (RegistryObject<PartItem> p : ModItems.PARTS) {
            event.register((stack, tint) -> tint == 0 ? PartItem.colourOf(stack) : 0xFFFFFF, p.get());
        }
        event.register((stack, tint) -> tint == 1 ? CoreItem.colourOf(stack) : 0xFFFFFF, ModItems.WEAPON_CORE.get());
        for (var w : ModItems.WEAPON_ITEMS) event.register(ClientSetup::weaponTint, w.get());
        for (var c : ModItems.CATALYST_ITEMS) event.register((stack, tint) -> tint == 2 ? CatalystItem.colourOf(stack) : 0xFFFFFF, c.get());
        event.register((stack, tint) -> tint == 0 || tint == 1 ? AugmentChipItem.colourOf(stack) : 0xFFFFFF, ModItems.AUGMENT_CHIP.get());
    }
}
