package com.mrgregles.bsp_weapons.registry;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.assembler.WeaponAssemblerBlock;
import com.mrgregles.bsp_weapons.assembler.WeaponAssemblerBlockEntity;
import com.mrgregles.bsp_weapons.assembler.WeaponAssemblerMenu;
import com.mrgregles.bsp_weapons.assembler.WeaponAssemblerPartBlock;
import com.mrgregles.bsp_weapons.enhancer.WeaponEnhancerBlock;
import com.mrgregles.bsp_weapons.enhancer.WeaponEnhancerBlockEntity;
import com.mrgregles.bsp_weapons.enhancer.WeaponEnhancerMenu;
import com.mrgregles.bsp_weapons.enhancer.WeaponEnhancerTopBlock;
import com.mrgregles.bsp_weapons.fabricator.WeaponFabricatorBlock;
import com.mrgregles.bsp_weapons.fabricator.WeaponFabricatorBlockEntity;
import com.mrgregles.bsp_weapons.fabricator.WeaponFabricatorConsoleBlock;
import com.mrgregles.bsp_weapons.fabricator.WeaponFabricatorMenu;
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
    public static final RegistryObject<WeaponAssemblerPartBlock> WEAPON_ASSEMBLER_PART = BLOCKS.register("weapon_assembler_part", () -> new WeaponAssemblerPartBlock(machine().noLootTable()));
    public static final RegistryObject<BlockEntityType<WeaponAssemblerBlockEntity>> WEAPON_ASSEMBLER_ENTITY = BLOCK_ENTITIES.register("weapon_assembler",
            () -> BlockEntityType.Builder.of(WeaponAssemblerBlockEntity::new, WEAPON_ASSEMBLER.get()).build(null));
    public static final RegistryObject<MenuType<WeaponAssemblerMenu>> WEAPON_ASSEMBLER_MENU = MENUS.register("weapon_assembler",
            () -> IForgeMenuType.create((windowId, inventory, data) -> new WeaponAssemblerMenu(windowId, inventory, data.readBlockPos())));

    public static final RegistryObject<WeaponEnhancerBlock> WEAPON_ENHANCER = BLOCKS.register("weapon_enhancer", () -> new WeaponEnhancerBlock(machine()));
    public static final RegistryObject<WeaponEnhancerTopBlock> WEAPON_ENHANCER_TOP = BLOCKS.register("weapon_enhancer_top", () -> new WeaponEnhancerTopBlock(machine().noLootTable()));
    public static final RegistryObject<BlockEntityType<WeaponEnhancerBlockEntity>> WEAPON_ENHANCER_ENTITY = BLOCK_ENTITIES.register("weapon_enhancer",
            () -> BlockEntityType.Builder.of(WeaponEnhancerBlockEntity::new, WEAPON_ENHANCER.get()).build(null));
    public static final RegistryObject<MenuType<WeaponEnhancerMenu>> WEAPON_ENHANCER_MENU = MENUS.register("weapon_enhancer",
            () -> IForgeMenuType.create((windowId, inventory, data) -> new WeaponEnhancerMenu(windowId, inventory, data.readBlockPos())));

    public static final RegistryObject<WeaponFabricatorBlock> WEAPON_FABRICATOR = BLOCKS.register("weapon_fabricator", () -> new WeaponFabricatorBlock(machine()));
    public static final RegistryObject<WeaponFabricatorConsoleBlock> WEAPON_FABRICATOR_CONSOLE = BLOCKS.register("weapon_fabricator_console", () -> new WeaponFabricatorConsoleBlock(machine().noLootTable()));
    public static final RegistryObject<BlockEntityType<WeaponFabricatorBlockEntity>> WEAPON_FABRICATOR_ENTITY = BLOCK_ENTITIES.register("weapon_fabricator",
            () -> BlockEntityType.Builder.of(WeaponFabricatorBlockEntity::new, WEAPON_FABRICATOR.get()).build(null));
    public static final RegistryObject<MenuType<WeaponFabricatorMenu>> WEAPON_FABRICATOR_MENU = MENUS.register("weapon_fabricator",
            () -> IForgeMenuType.create((windowId, inventory, data) -> new WeaponFabricatorMenu(windowId, inventory, data.readBlockPos())));

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
    }
}
