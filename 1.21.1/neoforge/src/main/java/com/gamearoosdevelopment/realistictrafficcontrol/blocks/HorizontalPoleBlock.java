package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

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

import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;
import com.gamearoosdevelopment.realistictrafficcontrol.util.PoleAssembly;
import com.gamearoosdevelopment.realistictrafficcontrol.util.RTCDrops;
import com.gamearoosdevelopment.realistictrafficcontrol.util.RTCRotation;
import com.gamearoosdevelopment.realistictrafficcontrol.util.RTCShapes;

/**
 * Horizontal cross-bar with the same 16-step Y rotation as vertical poles, so bars can be
 * placed at 22.5° increments and bind to the clicked face. Matching neighbors along the bar
 * (including 45° runs) grow connector arms toward each other.
 */
public class HorizontalPoleBlock extends Block implements IHorizontalPoleConnectable {

    /** Matches {@code horizontal_pole.json} ({@code z=-6..16}) so the outline follows the bar. */
    private static final VoxelShape BAR_SHAPE = Block.box(6, 6, -6, 10, 10, 16);
    private static final int EXTEND_RANGE = 32;
    private static final VoxelShape ARM_NORTH = Block.box(6, 6, 0, 10, 10, 8);
    private static final VoxelShape ARM_SOUTH = Block.box(6, 6, 8, 10, 10, 16);
    private static final VoxelShape ARM_WEST = Block.box(0, 6, 6, 8, 10, 10);
    private static final VoxelShape ARM_EAST = Block.box(8, 6, 6, 16, 10, 10);
    private static final java.util.Map<BlockState, VoxelShape> SHAPE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    public HorizontalPoleBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(RTCProperties.ROTATION, 0)
                .setValue(RTCProperties.MOUNT_FACE, Direction.SOUTH)
                .setValue(RTCProperties.NORTH, false)
                .setValue(RTCProperties.SOUTH, false)
                .setValue(RTCProperties.EAST, false)
                .setValue(RTCProperties.WEST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RTCProperties.ROTATION, RTCProperties.MOUNT_FACE,
                RTCProperties.NORTH, RTCProperties.SOUTH, RTCProperties.EAST, RTCProperties.WEST);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState support = CustomAngleCalculator.supportState(context);
        BlockState placed = defaultBlockState();
        if (support.getBlock() instanceof HorizontalPoleBlock) {
            placed = placed
                    .setValue(RTCProperties.ROTATION, support.getValue(RTCProperties.ROTATION))
                    .setValue(RTCProperties.MOUNT_FACE, support.getValue(RTCProperties.MOUNT_FACE));
        } else {
            placed = placed
                    .setValue(RTCProperties.ROTATION, CustomAngleCalculator.rotationForBarPlacement(context))
                    .setValue(RTCProperties.MOUNT_FACE, PoleAssembly.mountFace(context));
        }
        return computeConnections(placed, context.getLevel(), context.getClickedPos());
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
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return computeConnections(state, level, pos);
    }

    private BlockState computeConnections(BlockState state, BlockGetter level, BlockPos pos) {
        boolean north = false;
        boolean south = false;
        boolean east = false;
        boolean west = false;
        int rotation = state.getValue(RTCProperties.ROTATION);
        for (Direction world : Direction.Plane.HORIZONTAL) {
            if (!connectsToward(state, world)) {
                continue;
            }
            BlockState other = level.getBlockState(pos.relative(world));
            if (other.getBlock() instanceof HorizontalPoleBlock) {
                if (other.getValue(RTCProperties.ROTATION) != rotation) {
                    continue;
                }
                if (!connectsToward(other, world.getOpposite())) {
                    continue;
                }
            } else if (other.getBlock() instanceof IHorizontalPoleConnectable connectable) {
                if (!connectable.canConnectHorizontalPole(other, world.getOpposite())) {
                    continue;
                }
            } else {
                continue;
            }
            Direction local = worldToLocal(world, rotation);
            switch (local) {
                case NORTH -> north = true;
                case SOUTH -> south = true;
                case EAST -> east = true;
                case WEST -> west = true;
                default -> {
                }
            }
        }
        return state
                .setValue(RTCProperties.NORTH, north)
                .setValue(RTCProperties.SOUTH, south)
                .setValue(RTCProperties.EAST, east)
                .setValue(RTCProperties.WEST, west);
    }

    /**
     * Cardinal and 22.5° bars join only along their length. 45° bars can also
     * join on the other axis so a diagonal run can step through the grid.
     */
    static boolean connectsToward(BlockState state, Direction world) {
        if (!world.getAxis().isHorizontal()) {
            return false;
        }
        int extra = Math.abs(RTCRotation.deltaFromCardinal(state.getValue(RTCProperties.ROTATION)));
        if (extra >= 2) {
            return true;
        }
        return world.getAxis() == lengthAxis(state);
    }

    /** Last existing bar in the run and the step used to place the next one. */
    public record Extend(BlockPos last, Direction along) {
    }

    /**
     * Create {@code PoleHelper}: only along the bar, look/click picks the end, then walk
     * matching bars and place in the first empty cell.
     */
    public static Extend findExtend(BlockGetter level, BlockPos hozPos, BlockState hoz,
            Direction clickedFace, net.minecraft.world.phys.Vec3 hit, net.minecraft.world.phys.Vec3 look) {
        for (Direction along : orderedExtendDirs(hoz, hozPos, clickedFace, hit, look)) {
            double alongLook = along.getStepX() * look.x + along.getStepZ() * look.z;
            if (along != clickedFace && alongLook < 0) {
                continue;
            }
            int attached = attachedPoles(level, hozPos, along, hoz);
            if (attached >= EXTEND_RANGE) {
                continue;
            }
            BlockPos last = hozPos.relative(along, attached);
            if (level.getBlockState(last.relative(along)).canBeReplaced()) {
                return new Extend(last, along);
            }
        }
        return null;
    }

    static int attachedPoles(BlockGetter level, BlockPos origin, Direction along, BlockState originState) {
        int count = 0;
        BlockPos cursor = origin.relative(along);
        while (matchesRun(originState, level.getBlockState(cursor)) && count < EXTEND_RANGE) {
            count++;
            cursor = cursor.relative(along);
        }
        return count;
    }

    static boolean matchesRun(BlockState origin, BlockState other) {
        return other.getBlock() instanceof HorizontalPoleBlock
                && other.getValue(RTCProperties.ROTATION).intValue()
                    == origin.getValue(RTCProperties.ROTATION).intValue();
    }

    private static java.util.List<Direction> orderedExtendDirs(BlockState state, BlockPos hozPos,
            Direction clickedFace, net.minecraft.world.phys.Vec3 hit, net.minecraft.world.phys.Vec3 look) {
        java.util.List<Direction> dirs = new java.util.ArrayList<>(4);
        for (Direction world : Direction.Plane.HORIZONTAL) {
            if (!connectsToward(state, world)) {
                continue;
            }
            dirs.add(world);
        }
        dirs.sort((a, b) -> Double.compare(
                extendScore(b, hozPos, clickedFace, hit, look),
                extendScore(a, hozPos, clickedFace, hit, look)));
        return dirs;
    }

    /** Create {@code orderedByDistance}: closer face of the clicked cell wins; look is a tie-break. */
    private static double extendScore(Direction world, BlockPos hozPos, Direction clickedFace,
            net.minecraft.world.phys.Vec3 hit, net.minecraft.world.phys.Vec3 look) {
        double faceX = hozPos.getX() + 0.5 + world.getStepX() * 0.5;
        double faceY = hozPos.getY() + 0.5 + world.getStepY() * 0.5;
        double faceZ = hozPos.getZ() + 0.5 + world.getStepZ() * 0.5;
        double dist = hit.distanceToSqr(faceX, faceY, faceZ);
        double alongLook = world.getStepX() * look.x + world.getStepZ() * look.z;
        double score = -dist + alongLook * 0.2;
        if (clickedFace == world) {
            score += 0.05;
        }
        return score;
    }

    /** Unrotated bar is +Z; after the same yaw as the model. */
    static double[] barVector(BlockState state) {
        float yaw = (float) Math.toRadians(
                RTCRotation.placementRotationDegrees(state.getValue(RTCProperties.ROTATION)));
        return new double[] { Math.sin(yaw), Math.cos(yaw) };
    }

    static Direction.Axis lengthAxis(BlockState state) {
        double[] bar = barVector(state);
        return Math.abs(bar[0]) > Math.abs(bar[1]) ? Direction.Axis.X : Direction.Axis.Z;
    }

    private static Direction worldToLocal(Direction world, int rotation) {
        int local = (CustomAngleCalculator.getRotationForFacingCardinal(world) - rotation) & 15;
        return CustomAngleCalculator.rotationToFacing(CustomAngleCalculator.nearestCardinal(local));
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return RTCProperties.rotate16(state, rotation);
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return RTCProperties.mirror16(state, mirror);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_CACHE.computeIfAbsent(state, HorizontalPoleBlock::buildShape);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return RTCShapes.clipToBlock(getShape(state, level, pos, context));
    }

    private static VoxelShape buildShape(BlockState state) {
        int rotation = state.getValue(RTCProperties.ROTATION);
        VoxelShape shape = RTCShapes.rotateY(BAR_SHAPE, rotation);
        if (state.getValue(RTCProperties.NORTH)) {
            shape = Shapes.or(shape, RTCShapes.rotateY(ARM_NORTH, rotation));
        }
        if (state.getValue(RTCProperties.SOUTH)) {
            shape = Shapes.or(shape, RTCShapes.rotateY(ARM_SOUTH, rotation));
        }
        if (state.getValue(RTCProperties.EAST)) {
            shape = Shapes.or(shape, RTCShapes.rotateY(ARM_EAST, rotation));
        }
        if (state.getValue(RTCProperties.WEST)) {
            shape = Shapes.or(shape, RTCShapes.rotateY(ARM_WEST, rotation));
        }
        return shape;
    }

    @Override
    public boolean canConnectHorizontalPole(BlockState state, Direction fromFacing) {
        return connectsToward(state, fromFacing);
    }

    @Override
    protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        return RTCDrops.self(state);
    }
}
