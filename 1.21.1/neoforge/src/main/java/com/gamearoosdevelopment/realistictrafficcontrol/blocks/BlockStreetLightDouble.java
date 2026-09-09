package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.StreetLightDoubleBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Port of 1.12.2 {@code BlockStreetLightDouble}. */
public class BlockStreetLightDouble extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 16, 10);

    public BlockStreetLightDouble(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(RTCProperties.ROTATION, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RTCProperties.ROTATION);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(RTCProperties.ROTATION, CustomAngleCalculator.rotationForPlacement(context));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return RTCProperties.rotate16(state, rotation);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return RTCProperties.mirror16(state, mirror);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StreetLightDoubleBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (lvl.getGameTime() % 20L == 0L) {
                refreshLight(lvl, pos);
            }
        };
    }

    private static void refreshLight(Level level, BlockPos pos) {
        if (com.gamearoosdevelopment.realistictrafficcontrol.compat.PowerGridCompat
                .streetLightShouldBeOff(level, pos)) {
            removeLightSources(level, pos);
        } else {
            addLightSources(level, pos);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return com.gamearoosdevelopment.realistictrafficcontrol.util.RTCShapes.rotateY(SHAPE,
                state.getValue(RTCProperties.ROTATION));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide) {
            addLightSources(level, pos);
        }
        super.onPlace(state, level, pos, oldState, movedByPiston);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            removeLightSources(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
            boolean isMoving) {
        if (!level.isClientSide) {
            refreshLight(level, pos);
        }
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
    }

    private static void addLightSources(Level level, BlockPos pos) {
        tryPlaceLightSource(level, pos.above());
        tryPlaceLightSource(level, pos.north(2).west(2));
        tryPlaceLightSource(level, pos.north(2).east(2));
        tryPlaceLightSource(level, pos.south(2).west(2));
        tryPlaceLightSource(level, pos.south(2).east(2));
    }

    private static void tryPlaceLightSource(Level level, BlockPos pos) {
        BlockState proposed = level.getBlockState(pos);
        if (proposed.is(ModBlocks.LIGHT_SOURCE.get())) {
            return;
        }
        if (!proposed.isAir()) {
            pos = pos.above();
            proposed = level.getBlockState(pos);
            if (proposed.is(ModBlocks.LIGHT_SOURCE.get())) {
                return;
            }
            if (!proposed.isAir()) {
                return;
            }
        }
        level.setBlockAndUpdate(pos, ModBlocks.LIGHT_SOURCE.get().defaultBlockState());
    }

    private static void removeLightSources(Level level, BlockPos pos) {
        tryRemoveLightSource(level, pos.above());
        tryRemoveLightSource(level, pos.north(2).west(2));
        tryRemoveLightSource(level, pos.north(2).east(2));
        tryRemoveLightSource(level, pos.south(2).west(2));
        tryRemoveLightSource(level, pos.south(2).east(2));
    }

    private static void tryRemoveLightSource(Level level, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() == ModBlocks.LIGHT_SOURCE.get()) {
            level.removeBlock(pos, false);
        }
        pos = pos.above();
        if (level.getBlockState(pos).getBlock() == ModBlocks.LIGHT_SOURCE.get()) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        return com.gamearoosdevelopment.realistictrafficcontrol.util.RTCDrops.self(state);
    }
}
