package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.ModSounds;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.PedestrianButtonBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightControlBoxBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Pedestrian call button. Ported from the 1.12.2 {@code BlockPedestrianButton}: pressing it queues a
 * pedestrian phase on every paired {@link TrafficLightControlBoxBlockEntity}. The 1.12.2 {@code ABOVE}
 * visual state (auto-derived from the block above) is dropped; rotation is stored in
 * {@link RTCProperties#ROTATION}.
 */
public class BlockPedestrianButton extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = box(5, 0, 5, 11, 16, 11);

    public BlockPedestrianButton(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(RTCProperties.ROTATION, 0));
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PedestrianButtonBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        level.playSound(null, pos, ModSounds.PED_BUTTON.get(), SoundSource.BLOCKS, 0.3F, 1F);

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (level.getBlockEntity(pos) instanceof PedestrianButtonBlockEntity pedTE) {
            int rotation = state.getValue(RTCProperties.ROTATION);
            for (BlockPos controller : pedTE.getPairedBoxes()) {
                if (!(level.getBlockEntity(controller) instanceof TrafficLightControlBoxBlockEntity ctrlr)) {
                    pedTE.removePairedBox(controller);
                    continue;
                }
                if (CustomAngleCalculator.isNorthSouth(rotation)) {
                    ctrlr.getAutomator().setWestEastPedQueued(true);
                } else {
                    ctrlr.getAutomator().setNorthSouthPedQueued(true);
                }
            }
        }

        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof PedestrianButtonBlockEntity te) {
                int rotation = state.getValue(RTCProperties.ROTATION);
                te.onBreak(level, CustomAngleCalculator.isNorthSouth(rotation));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
