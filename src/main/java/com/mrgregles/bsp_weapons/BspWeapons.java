package com.mrgregles.bsp_weapons;

import com.mojang.logging.LogUtils;
import com.mrgregles.bsp_weapons.data.WeaponData;
import com.mrgregles.bsp_weapons.network.WeaponsNetwork;
import com.mrgregles.bsp_weapons.registry.ModItems;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Entry point for BSP-Weapons: sci-fi weapons for the Build Secure Protect modpack, built from interchangeable parts.
 * Features live in their own packages and are wired up here. Other BSP mods and Mekanism are reached only through one
 * adapter class each in {@code compat/}.
 */
@Mod(BspWeapons.MODID)
public class BspWeapons {
    /** Mod id. Must match {@code mod_id} in gradle.properties and {@code modId} in mods.toml. */
    public static final String MODID = "bsp_weapons";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BspWeapons(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.SERVER, WeaponsConfig.SPEC);
        // the part system's data (patterns, materials, bands, part types, augments, catalysts) from every datapack
        MinecraftForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> WeaponData.register(e));
        com.mrgregles.bsp_weapons.registry.ModBlocks.register(context.getModEventBus());
        ModItems.register(context.getModEventBus());
        com.mrgregles.bsp_weapons.registry.ModRecipes.register(context.getModEventBus());
        com.mrgregles.bsp_weapons.registry.ModEntities.register(context.getModEventBus());
        WeaponsNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(WeaponsNetwork::onDatapackSync);
        if (FMLEnvironment.dist == Dist.CLIENT) com.mrgregles.bsp_weapons.client.ClientSetup.register(context.getModEventBus());
        LOGGER.info("BSP-Weapons loaded");
    }
}
