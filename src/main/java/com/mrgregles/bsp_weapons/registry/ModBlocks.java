package com.mrgregles.bsp_weapons.registry;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.assembler.WeaponAssemblerBlock;
import com.mrgregles.bsp_weapons.assembler.WeaponAssemblerBlockEntity;
import com.mrgregles.bsp_weapons.assembler.WeaponAssemblerMenu;
import com.mrgregles.bsp_weapons.assembler.WeaponAssemblerPartBlock;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** The mod's blocks, block entities and menus. */
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, BspWeapons.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, BspWeapons.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, BspWeapons.MODID);

    private static BlockBehaviour.Properties machine() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion().lightLevel(s -> 7)
                .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK); // pistons never move half a machine
    }

    public static final RegistryObject<WeaponAssemblerBlock> WEAPON_ASSEMBLER = BLOCKS.register("weapon_assembler", () -> new WeaponAssemblerBlock(machine()));
    public static final RegistryObject<WeaponAssemblerPartBlock> WEAPON_ASSEMBLER_PART = BLOCKS.register("weapon_assembler_part", () -> new WeaponAssemblerPartBlock(machine()));
    public static final RegistryObject<BlockEntityType<WeaponAssemblerBlockEntity>> WEAPON_ASSEMBLER_ENTITY = BLOCK_ENTITIES.register("weapon_assembler",
            () -> BlockEntityType.Builder.of(WeaponAssemblerBlockEntity::new, WEAPON_ASSEMBLER.get()).build(null));
    public static final RegistryObject<MenuType<WeaponAssemblerMenu>> WEAPON_ASSEMBLER_MENU = MENUS.register("weapon_assembler",
            () -> IForgeMenuType.create((windowId, inventory, data) -> new WeaponAssemblerMenu(windowId, inventory, data.readBlockPos())));

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
    }
}
