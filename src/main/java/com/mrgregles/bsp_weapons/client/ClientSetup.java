package com.mrgregles.bsp_weapons.client;

import com.mrgregles.bsp_weapons.item.CoreItem;
import com.mrgregles.bsp_weapons.item.PartItem;
import com.mrgregles.bsp_weapons.registry.ModItems;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

/** Client-only wiring: the model tints (D48). Loaded on the client only; the server never touches this class. */
public final class ClientSetup {
    private ClientSetup() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ClientSetup::onItemColours);
    }

    /** Tint index 0 is the material on a part, tint index 1 the tier glow on a core; other faces have their own textures. */
    private static void onItemColours(RegisterColorHandlersEvent.Item event) {
        for (RegistryObject<PartItem> p : ModItems.PARTS) {
            event.register((stack, tint) -> tint == 0 ? PartItem.colourOf(stack) : (tint == 1 ? CoreItem.TIER_COLOURS[1] : 0xFFFFFF), p.get());
        }
        event.register((stack, tint) -> tint == 1 ? CoreItem.colourOf(stack) : 0xFFFFFF, ModItems.WEAPON_CORE.get());
    }
}
