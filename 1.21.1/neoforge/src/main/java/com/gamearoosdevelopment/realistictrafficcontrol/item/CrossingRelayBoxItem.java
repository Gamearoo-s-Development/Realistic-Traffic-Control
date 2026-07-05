package com.gamearoosdevelopment.realistictrafficcontrol.item;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.RelayBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Port of 1.12.2 {@code ItemCrossingRelayBox}: places the eight-part crossing-relay multiblock and
 * designates the SE lower block as master.
 */
public class CrossingRelayBoxItem extends Item {

    public CrossingRelayBoxItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();
        Direction facing = context.getHorizontalDirection();
        BlockPos pos = context.getClickedPos();

        if (!checkSpacing(level, pos, facing)) {
            return InteractionResult.FAIL;
        }

        placeMultiblock(level, pos, facing);

        if (player != null && !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }

        return InteractionResult.SUCCESS;
    }

    private boolean checkSpacing(Level world, BlockPos pos, Direction facing) {
        BlockPos placingPos = pos.above();
        Direction lastFacing = facing;

        boolean success = checkSpacing(world, placingPos);
        lastFacing = rotateLeft(lastFacing);
        placingPos = placingPos.relative(lastFacing);
        success = success && checkSpacing(world, placingPos);

        for (int i = 0; i < 2; i++) {
            lastFacing = rotateRight(lastFacing);
            placingPos = placingPos.relative(lastFacing);
            success = success && checkSpacing(world, placingPos);
        }

        placingPos = placingPos.above();
        success = success && checkSpacing(world, placingPos);

        for (int i = 0; i < 3; i++) {
            lastFacing = rotateRight(lastFacing);
            placingPos = placingPos.relative(lastFacing);
            success = success && checkSpacing(world, placingPos);
        }
        return success;
    }

    private boolean checkSpacing(Level world, BlockPos pos) {
        return world.getBlockState(pos).canBeReplaced();
    }

    private void placeMultiblock(Level world, BlockPos pos, Direction facing) {
        BlockPos placingPos = pos.above();
        Direction lastFacing = facing;
        BlockPos masterTEPos = placingPos.immutable();

        setRelayBlock(world, placingPos, ModBlocks.CROSSING_RELAY_SE.get().defaultBlockState(), facing);
        if (world.getBlockEntity(placingPos) instanceof RelayBlockEntity te) {
            te.setMaster();
        }

        lastFacing = rotateLeft(lastFacing);
        placingPos = placingPos.relative(lastFacing);
        setRelayBlock(world, placingPos, ModBlocks.CROSSING_RELAY_SW.get().defaultBlockState(), facing);
        setMasterLocation(world, placingPos, masterTEPos);

        lastFacing = rotateRight(lastFacing);
        placingPos = placingPos.relative(lastFacing);
        setRelayBlock(world, placingPos, ModBlocks.CROSSING_RELAY_NW.get().defaultBlockState(), facing);
        setMasterLocation(world, placingPos, masterTEPos);

        lastFacing = rotateRight(lastFacing);
        placingPos = placingPos.relative(lastFacing);
        setRelayBlock(world, placingPos, ModBlocks.CROSSING_RELAY_NE.get().defaultBlockState(), facing);
        setMasterLocation(world, placingPos, masterTEPos);

        placingPos = placingPos.above();
        setRelayBlock(world, placingPos, ModBlocks.CROSSING_RELAY_TOP_NE.get().defaultBlockState(), facing);
        setMasterLocation(world, placingPos, masterTEPos);

        lastFacing = rotateRight(lastFacing);
        placingPos = placingPos.relative(lastFacing);
        setRelayBlock(world, placingPos, ModBlocks.CROSSING_RELAY_TOP_SE.get().defaultBlockState(), facing);
        setMasterLocation(world, placingPos, masterTEPos);

        lastFacing = rotateRight(lastFacing);
        placingPos = placingPos.relative(lastFacing);
        setRelayBlock(world, placingPos, ModBlocks.CROSSING_RELAY_TOP_SW.get().defaultBlockState(), facing);
        setMasterLocation(world, placingPos, masterTEPos);

        lastFacing = rotateRight(lastFacing);
        placingPos = placingPos.relative(lastFacing);
        setRelayBlock(world, placingPos, ModBlocks.CROSSING_RELAY_TOP_NW.get().defaultBlockState(), facing);
        setMasterLocation(world, placingPos, masterTEPos);
    }

    private static void setRelayBlock(Level world, BlockPos pos, BlockState state, Direction facing) {
        world.setBlock(pos, state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing), 3);
    }

    private static void setMasterLocation(Level world, BlockPos pos, BlockPos master) {
        if (world.getBlockEntity(pos) instanceof RelayBlockEntity relay) {
            relay.setMasterLocation(master);
        }
    }

    private static Direction rotateLeft(Direction in) {
        return switch (in) {
            case NORTH -> Direction.WEST;
            case WEST -> Direction.SOUTH;
            case SOUTH -> Direction.EAST;
            case EAST -> Direction.NORTH;
            default -> in;
        };
    }

    private static Direction rotateRight(Direction in) {
        return switch (in) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> in;
        };
    }
}
