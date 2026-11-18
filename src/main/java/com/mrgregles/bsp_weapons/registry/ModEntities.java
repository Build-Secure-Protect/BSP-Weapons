package com.mrgregles.bsp_weapons.registry;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.weapon.LaserBoltEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** The mod's entities: the energy weapons' lance. */
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BspWeapons.MODID);
    public static final RegistryObject<EntityType<LaserBoltEntity>> LASER_BOLT = ENTITIES.register("laser_bolt",
            () -> EntityType.Builder.<LaserBoltEntity>of(LaserBoltEntity::new, MobCategory.MISC).sized(0.2f, 0.2f).clientTrackingRange(8).updateInterval(1).build("laser_bolt"));

    private ModEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }
}
