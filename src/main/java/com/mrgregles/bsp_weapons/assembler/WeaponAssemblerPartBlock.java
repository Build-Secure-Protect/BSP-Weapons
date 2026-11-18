package com.mrgregles.bsp_weapons.assembler;

import com.mrgregles.bsp_weapons.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The Weapon Assembler's second space: invisible (the master draws the whole machine), it opens the master's screen and takes the master with it when broken. */
public class WeaponAssemblerPartBlock extends HorizontalDirectionalBlock {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 20, 16);

    public WeaponAssemblerPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    /** The master's position for a companion facing {@code facing}: the other way from {@link WeaponAssemblerBlock#companionPos}. */
    public static BlockPos masterPos(BlockPos pos, Direction facing) {
        return pos.relative(facing.getClockWise());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos master = masterPos(pos, state.getValue(FACING));
        BlockState ms = level.getBlockState(master);
        if (ms.is(ModBlocks.WEAPON_ASSEMBLER.get())) return ms.use(level, player, hand, hit.withPosition(master));
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !moving) {
            BlockPos master = masterPos(pos, state.getValue(FACING));
            if (level.getBlockState(master).is(ModBlocks.WEAPON_ASSEMBLER.get())) level.destroyBlock(master, true);
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(ModBlocks.WEAPON_ASSEMBLER.get());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
