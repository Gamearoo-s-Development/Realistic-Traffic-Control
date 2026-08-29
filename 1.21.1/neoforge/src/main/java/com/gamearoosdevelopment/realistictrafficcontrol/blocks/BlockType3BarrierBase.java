package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.menu.Type3BarrierMenu;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.Type3BarrierBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.RTCBlockProperties;
import com.gamearoosdevelopment.realistictrafficcontrol.util.RTCShapes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.SimpleMenuProvider;

/** Port of 1.12.2 {@code BlockType3BarrierBase}. */
public abstract class BlockType3BarrierBase extends Block implements EntityBlock {

    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING =
            HorizontalDirectionalBlock.FACING;

    protected BlockType3BarrierBase(Properties properties) {
        super(RTCBlockProperties.groundSnapOffset(properties));
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(RTCProperties.ISFURTHESTLEFT, true)
                .setValue(RTCProperties.ISFURTHESTRIGHT, true));
    }

    public abstract Block getBlockInstance();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, RTCProperties.ISFURTHESTLEFT, RTCProperties.ISFURTHESTRIGHT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Type3BarrierBlockEntity(pos, state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return computeActualState(state, level, pos);
    }

    public BlockState computeActualState(BlockState state, BlockGetter level, BlockPos pos) {
        boolean isFurthestLeft = true;
        boolean isFurthestRight = true;
        Direction currentFacing = state.getValue(FACING);

        Direction right = currentFacing.getClockWise();
        BlockPos rightPos = pos.relative(right);
        BlockState rightState = level.getBlockState(rightPos);
        if (rightState.getBlock() instanceof BlockType3BarrierBase
                && rightState.getValue(FACING) == currentFacing) {
            double thisTopY = level.getBlockState(pos.below()).getShape(level, pos.below()).max(Direction.Axis.Y);
            double neighborTopY = level.getBlockState(rightPos.below()).getShape(level, rightPos.below())
                    .max(Direction.Axis.Y);
            if (Math.abs(thisTopY - neighborTopY) < 0.01) {
                isFurthestRight = false;
            }
        }

        Direction left = currentFacing.getCounterClockWise();
        BlockPos leftPos = pos.relative(left);
        BlockState leftState = level.getBlockState(leftPos);
        if (leftState.getBlock() instanceof BlockType3BarrierBase
                && leftState.getValue(FACING) == currentFacing) {
            double thisTopY = level.getBlockState(pos.below()).getShape(level, pos.below()).max(Direction.Axis.Y);
            double neighborTopY = level.getBlockState(leftPos.below()).getShape(level, leftPos.below())
                    .max(Direction.Axis.Y);
            if (Math.abs(thisTopY - neighborTopY) < 0.01) {
                isFurthestLeft = false;
            }
        }

        return state.setValue(RTCProperties.ISFURTHESTLEFT, isFurthestLeft)
                .setValue(RTCProperties.ISFURTHESTRIGHT, isFurthestRight);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH, SOUTH -> RTCShapes.blockBox(0, 0, 9, 16, 23, 7);
            case WEST, EAST -> RTCShapes.blockBox(7, 0, 0, 9, 23, 16);
            default -> super.getShape(state, level, pos, context);
        };
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
            boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide) {
            refreshBarrierChainState(level, pos);
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.isClientSide) {
            return;
        }
        refreshBarrierChainState(level, pos);
    }

    private static void refreshBarrierChainState(Level level, BlockPos origin) {
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-1, 0, -1), origin.offset(1, 0, 1))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BlockType3BarrierBase base) {
                BlockState actual = base.computeActualState(state, level, pos);
                if (actual != state) {
                    level.setBlock(pos, actual, Block.UPDATE_ALL);
                }
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = new SimpleMenuProvider(
                    (id, inv, p) -> new Type3BarrierMenu(id, inv, pos),
                    Component.literal("Type 3 Barrier"));
            serverPlayer.openMenu(provider, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }
        BlockState actualState = computeActualState(state, level, pos);
        if (!actualState.getValue(RTCProperties.ISFURTHESTLEFT) || !actualState.getValue(RTCProperties.ISFURTHESTRIGHT)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof Type3BarrierBlockEntity myTE)) {
                return;
            }
            Direction facing = actualState.getValue(FACING);
            BlockEntity leftBE = level.getBlockEntity(pos.relative(facing.getCounterClockWise()));
            BlockEntity rightBE = level.getBlockEntity(pos.relative(facing.getClockWise()));
            Type3BarrierBlockEntity leftTE = leftBE instanceof Type3BarrierBlockEntity l ? l : null;
            Type3BarrierBlockEntity rightTE = rightBE instanceof Type3BarrierBlockEntity r ? r : null;
            if (leftTE == null && rightTE != null) {
                myTE.setRenderSign(rightTE.getRenderSign());
                myTE.setSignType(rightTE.getSignType());
            } else if (rightTE == null && leftTE != null) {
                myTE.setRenderSign(leftTE.getRenderSign());
                myTE.setSignType(leftTE.getSignType());
            } else if (leftTE != null && rightTE != null) {
                boolean renderSign = leftTE.getRenderSign() || rightTE.getRenderSign();
                Type3BarrierBlockEntity furthestLeft = myTE.findFurthestLeft();
                furthestLeft.setRenderSign(renderSign);
                furthestLeft.syncConnectedBarriers(false);
            }
        }
    }
}
