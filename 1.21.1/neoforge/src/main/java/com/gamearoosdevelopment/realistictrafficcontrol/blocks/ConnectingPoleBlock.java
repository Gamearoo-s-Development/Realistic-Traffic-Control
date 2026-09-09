package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import java.util.EnumSet;
import java.util.Set;

import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Extra-Frames / 1.12 {@code BlockCrossingGatePole} auto-connect: poles grow
 * {@code crossing_gate_pole_ext} toward neighboring {@link IHorizontalPoleConnectable} blocks.
 *
 * <p>{@code north}/{@code west}/{@code south}/{@code east} are facing-relative, matching Extra-Frames
 * {@code getActualState}, not world directions.
 */
public class ConnectingPoleBlock extends RotatedBlock implements IHorizontalPoleConnectable {
    private static final VoxelShape ARM_NORTH = Block.box(7.0, 7.0, 0.0, 9.0, 9.0, 7.0);
    private static final VoxelShape ARM_SOUTH = Block.box(7.0, 7.0, 9.0, 9.0, 9.0, 16.0);
    private static final VoxelShape ARM_WEST = Block.box(0.0, 7.0, 7.0, 7.0, 9.0, 9.0);
    private static final VoxelShape ARM_EAST = Block.box(9.0, 7.0, 7.0, 16.0, 9.0, 9.0);

    /** Extra-Frames property slot to skip when the JSON already has an arm in that local direction. */
    public enum SkippedArm {
        FACING,
        COUNTER_CLOCKWISE,
        OPPOSITE,
        CLOCKWISE
    }

    private final Set<SkippedArm> skippedArms;

    public ConnectingPoleBlock(Properties properties, VoxelShape shape) {
        this(properties, shape, EnumSet.noneOf(SkippedArm.class));
    }

    public ConnectingPoleBlock(Properties properties, VoxelShape shape, Set<SkippedArm> skippedArms) {
        super(properties, shape);
        this.skippedArms = skippedArms.isEmpty() ? EnumSet.noneOf(SkippedArm.class) : EnumSet.copyOf(skippedArms);
    }

    public static Set<SkippedArm> cantileverArms() {
        return EnumSet.of(SkippedArm.OPPOSITE);
    }

    public static Set<SkippedArm> northSouthBeamArms() {
        return EnumSet.of(SkippedArm.FACING, SkippedArm.OPPOSITE);
    }

    public static Set<SkippedArm> diagonalTeeArms() {
        return EnumSet.of(SkippedArm.OPPOSITE, SkippedArm.COUNTER_CLOCKWISE);
    }

    @Override
    protected BlockState defaultRotatedState() {
        return super.defaultRotatedState()
                .setValue(RTCProperties.NORTH, false)
                .setValue(RTCProperties.WEST, false)
                .setValue(RTCProperties.SOUTH, false)
                .setValue(RTCProperties.EAST, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(RTCProperties.NORTH, RTCProperties.WEST, RTCProperties.SOUTH, RTCProperties.EAST);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide()) {
            return;
        }
        BlockState updated = computeConnections(state, level, pos);
        if (!updated.equals(state)) {
            level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return computeConnections(super.getStateForPlacement(context), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return computeConnections(state, level, pos);
    }

    private BlockState computeConnections(BlockState state, BlockGetter level, BlockPos pos) {
        int rotation = state.getValue(RTCProperties.ROTATION);
        Direction facing = CustomAngleCalculator.getFacingFromRotation(
                CustomAngleCalculator.nearestCardinal(rotation));

        boolean north = !skippedArms.contains(SkippedArm.FACING)
                && neighborConnects(level, pos, facing);
        boolean west = !skippedArms.contains(SkippedArm.COUNTER_CLOCKWISE)
                && neighborConnects(level, pos, facing.getCounterClockWise());
        boolean south = !skippedArms.contains(SkippedArm.OPPOSITE)
                && neighborConnects(level, pos, facing.getOpposite());
        boolean east = !skippedArms.contains(SkippedArm.CLOCKWISE)
                && neighborConnects(level, pos, facing.getClockWise());

        return state
                .setValue(RTCProperties.NORTH, north)
                .setValue(RTCProperties.WEST, west)
                .setValue(RTCProperties.SOUTH, south)
                .setValue(RTCProperties.EAST, east);
    }

    private static boolean neighborConnects(BlockGetter level, BlockPos pos, Direction direction) {
        BlockState other = level.getBlockState(pos.relative(direction));
        if (other.getBlock() instanceof IHorizontalPoleConnectable connectable) {
            return connectable.canConnectHorizontalPole(other, direction.getOpposite());
        }
        return false;
    }

    @Override
    public boolean canConnectHorizontalPole(BlockState state, Direction fromFacing) {
        return fromFacing.getAxis().isHorizontal();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = super.getShape(state, level, pos, context);
        int rotation = state.getValue(RTCProperties.ROTATION);
        Direction facing = CustomAngleCalculator.getFacingFromRotation(
                CustomAngleCalculator.nearestCardinal(rotation));
        boolean cardinal = CustomAngleCalculator.isCardinal(rotation);
        if (state.getValue(RTCProperties.NORTH)) {
            shape = Shapes.or(shape, cardinal
                    ? armForWorldDirection(facing)
                    : com.gamearoosdevelopment.realistictrafficcontrol.util.RTCShapes.rotateY(ARM_NORTH, rotation));
        }
        if (state.getValue(RTCProperties.WEST)) {
            shape = Shapes.or(shape, cardinal
                    ? armForWorldDirection(facing.getCounterClockWise())
                    : com.gamearoosdevelopment.realistictrafficcontrol.util.RTCShapes.rotateY(ARM_WEST, rotation));
        }
        if (state.getValue(RTCProperties.SOUTH)) {
            shape = Shapes.or(shape, cardinal
                    ? armForWorldDirection(facing.getOpposite())
                    : com.gamearoosdevelopment.realistictrafficcontrol.util.RTCShapes.rotateY(ARM_SOUTH, rotation));
        }
        if (state.getValue(RTCProperties.EAST)) {
            shape = Shapes.or(shape, cardinal
                    ? armForWorldDirection(facing.getClockWise())
                    : com.gamearoosdevelopment.realistictrafficcontrol.util.RTCShapes.rotateY(ARM_EAST, rotation));
        }
        return shape;
    }

    private static VoxelShape armForWorldDirection(Direction worldDirection) {
        return switch (worldDirection) {
            case NORTH -> ARM_NORTH;
            case SOUTH -> ARM_SOUTH;
            case WEST -> ARM_WEST;
            case EAST -> ARM_EAST;
            default -> Shapes.empty();
        };
    }
}
