package com.mrgregles.bsp_weapons.registry;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.fabricator.FabricatingRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** The mod's recipe types: {@code bsp_weapons:fabricating}, the Weapon Fabricator's fixed products (D112). */
public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, BspWeapons.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, BspWeapons.MODID);
    public static final String FABRICATING_ID = "fabricating";
    public static final RegistryObject<RecipeType<FabricatingRecipe>> FABRICATING = TYPES.register(FABRICATING_ID, () -> RecipeType.simple(new ResourceLocation(BspWeapons.MODID, FABRICATING_ID)));
    public static final RegistryObject<RecipeSerializer<FabricatingRecipe>> FABRICATING_SERIALIZER = SERIALIZERS.register(FABRICATING_ID, FabricatingRecipe.Serializer::new);

    private ModRecipes() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        SERIALIZERS.register(modBus);
    }
}
