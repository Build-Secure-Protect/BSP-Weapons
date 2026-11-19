package com.mrgregles.bsp_weapons.fabricator;

import com.mrgregles.bsp_weapons.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * The Weapon Fabricator (D97, D98, D108; R24): two block spaces side by side, one high. This block is the tower space on
 * the viewer's left, holding the block entity, the inventory, the charge, the job and the whole Gantry Printer model; the
 * companion {@link WeaponFabricatorConsoleBlock} stands on this block's counter-clockwise side and opens the same screen.
 * The model's front is its south face, so {@code FACING} is the way the front points. Breaking either block removes both
 * and drops this one with nothing inside.
 */
public class WeaponFabricatorBlock extends HorizontalDirectionalBlock implements EntityBlock {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public WeaponFabricatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    /** Where the companion stands for a machine facing {@code facing}: the tower is on the viewer's left, the console on their right. */
    public static BlockPos companionPos(BlockPos pos, Direction facing) {
        return pos.relative(facing.getCounterClockWise());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getHorizontalDirection().getOpposite();
        BlockPos other = companionPos(ctx.getClickedPos(), facing);
        if (!ctx.getLevel().getBlockState(other).canBeReplaced(ctx) || !ctx.getLevel().getWorldBorder().isWithinBounds(other)) return null;
        BlockState companion = ModBlocks.WEAPON_FABRICATOR_CONSOLE.get().defaultBlockState().setValue(FACING, facing);
        if (!ctx.getLevel().isUnobstructed(companion, other, CollisionContext.empty())) return null; // nobody is shut inside the second space
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        placeCompanion(level, pos, state.getValue(FACING));
    }

    public static void placeCompanion(LevelAccessor level, BlockPos pos, Direction facing) {
        level.setBlock(companionPos(pos, facing), ModBlocks.WEAPON_FABRICATOR_CONSOLE.get().defaultBlockState().setValue(FACING, facing), 3);
    }

    /** Places the whole machine: the tower at {@code pos} facing {@code facing} and its console beside it. */
    public static void placeMachine(LevelAccessor level, BlockPos pos, Direction facing) {
        level.setBlock(pos, ModBlocks.WEAPON_FABRICATOR.get().defaultBlockState().setValue(FACING, facing), 3);
        placeCompanion(level, pos, facing);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState other = level.getBlockState(companionPos(pos, state.getValue(FACING)));
        return other.is(ModBlocks.WEAPON_FABRICATOR_CONSOLE.get()) || other.canBeReplaced();
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof WeaponFabricatorBlockEntity be) be.dropContents(level, pos);
            BlockPos other = companionPos(pos, state.getValue(FACING));
            if (!moving && level.getBlockState(other).is(ModBlocks.WEAPON_FABRICATOR_CONSOLE.get())) level.removeBlock(other, false);
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof WeaponFabricatorBlockEntity be && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            NetworkHooks.openScreen(sp, be, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WeaponFabricatorBlockEntity(pos, state);
    }

    /** The job under way runs on the server's ticks (R30: with the screen closed too). */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlocks.WEAPON_FABRICATOR_ENTITY.get()) return null;
        return (lvl, pos, st, be) -> ((WeaponFabricatorBlockEntity) be).serverTick();
    }

    static void drop(Level level, BlockPos pos, ItemStack stack) {
        if (!stack.isEmpty()) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
    }
}
