package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.StreetLightSingleBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Port of 1.12.2 {@code BlockStreetLightSingle}. */
@EventBusSubscriber(modid = com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl.MODID)
public class BlockStreetLightSingle extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 16, 10);

    public BlockStreetLightSingle(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(RTCProperties.ROTATION, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RTCProperties.ROTATION);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        int rotation = CustomAngleCalculator.getRotationForYaw(context.getRotation());
        return defaultBlockState().setValue(RTCProperties.ROTATION, rotation);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StreetLightSingleBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide) {
            addLightSources(pos, level, state);
        }
        super.onPlace(state, level, pos, oldState, movedByPiston);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            removeLightSources(pos, level, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
            boolean isMoving) {
        if (!level.isClientSide) {
            if (level.hasNeighborSignal(pos)) {
                removeLightSources(pos, level, state);
            } else {
                addLightSources(pos, level, state);
            }
        }
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        if (level.isClientSide) {
            return;
        }
        BlockPos workingPos = event.getPos().north(2).west(2);
        BlockState state = level.getBlockState(workingPos);
        if (state.getBlock() == ModBlocks.STREET_LIGHT_SINGLE.get()) {
            int rotation = state.getValue(RTCProperties.ROTATION);
            if (!CustomAngleCalculator.isEast(rotation) && !CustomAngleCalculator.isSouth(rotation)) {
                level.setBlockAndUpdate(event.getPos(), ModBlocks.LIGHT_SOURCE.get().defaultBlockState());
                event.setCanceled(true);
                return;
            }
        }
        workingPos = workingPos.east(4);
        state = level.getBlockState(workingPos);
        if (state.getBlock() == ModBlocks.STREET_LIGHT_SINGLE.get()) {
            int rotation = state.getValue(RTCProperties.ROTATION);
            if (!CustomAngleCalculator.isSouth(rotation) && !CustomAngleCalculator.isWest(rotation)) {
                level.setBlockAndUpdate(event.getPos(), ModBlocks.LIGHT_SOURCE.get().defaultBlockState());
                event.setCanceled(true);
                return;
            }
        }
        workingPos = workingPos.south(4);
        state = level.getBlockState(workingPos);
        if (state.getBlock() == ModBlocks.STREET_LIGHT_SINGLE.get()) {
            int rotation = state.getValue(RTCProperties.ROTATION);
            if (!CustomAngleCalculator.isWest(rotation) && !CustomAngleCalculator.isNorth(rotation)) {
                level.setBlockAndUpdate(event.getPos(), ModBlocks.LIGHT_SOURCE.get().defaultBlockState());
                event.setCanceled(true);
                return;
            }
        }
        workingPos = workingPos.west(4);
        state = level.getBlockState(workingPos);
        if (state.getBlock() == ModBlocks.STREET_LIGHT_SINGLE.get()) {
            int rotation = state.getValue(RTCProperties.ROTATION);
            if (!CustomAngleCalculator.isNorth(rotation) && !CustomAngleCalculator.isEast(rotation)) {
                level.setBlockAndUpdate(event.getPos(), ModBlocks.LIGHT_SOURCE.get().defaultBlockState());
                event.setCanceled(true);
            }
        }
    }

    private static void addLightSources(BlockPos pos, Level level, BlockState lampState) {
        int rotation = lampState.getValue(RTCProperties.ROTATION);
        tryPlaceLightSource(level, pos.above());
        BlockPos angle;
        if (CustomAngleCalculator.isNorth(rotation)) {
            angle = pos.south(2).west(2);
            tryPlaceLightSource(level, angle);
            tryPlaceLightSource(level, angle.east(4));
        } else if (CustomAngleCalculator.isWest(rotation)) {
            angle = pos.east(2).north(2);
            tryPlaceLightSource(level, angle);
            tryPlaceLightSource(level, angle.south(4));
        } else if (CustomAngleCalculator.isSouth(rotation)) {
            angle = pos.north(2).west(2);
            tryPlaceLightSource(level, angle);
            tryPlaceLightSource(level, angle.east(4));
        } else if (CustomAngleCalculator.isEast(rotation)) {
            angle = pos.west(2).north(2);
            tryPlaceLightSource(level, angle);
            tryPlaceLightSource(level, angle.south(4));
        }
    }

    private static void tryPlaceLightSource(Level level, BlockPos pos) {
        BlockState proposed = level.getBlockState(pos);
        if (proposed.getBlock() != ModBlocks.LIGHT_SOURCE.get() && proposed.getBlock() != Blocks.AIR) {
            pos = pos.above();
            proposed = level.getBlockState(pos);
            if (proposed.getBlock() != ModBlocks.LIGHT_SOURCE.get() && proposed.getBlock() != Blocks.AIR) {
                return;
            }
        }
        level.setBlockAndUpdate(pos, ModBlocks.LIGHT_SOURCE.get().defaultBlockState());
    }

    private static void removeLightSources(BlockPos pos, Level level, BlockState lampState) {
        int rotation = lampState.getValue(RTCProperties.ROTATION);
        tryRemoveLightSource(level, pos.above());
        BlockPos angle;
        if (CustomAngleCalculator.isNorth(rotation)) {
            angle = pos.south(2).west(2);
            tryRemoveLightSource(level, angle);
            tryRemoveLightSource(level, angle.east(4));
        } else if (CustomAngleCalculator.isWest(rotation)) {
            angle = pos.east(2).north(2);
            tryRemoveLightSource(level, angle);
            tryRemoveLightSource(level, angle.south(4));
        } else if (CustomAngleCalculator.isSouth(rotation)) {
            angle = pos.north(2).west(2);
            tryRemoveLightSource(level, angle);
            tryRemoveLightSource(level, angle.east(4));
        } else if (CustomAngleCalculator.isEast(rotation)) {
            angle = pos.west(2).north(2);
            tryRemoveLightSource(level, angle);
            tryRemoveLightSource(level, angle.south(4));
        }
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
}
