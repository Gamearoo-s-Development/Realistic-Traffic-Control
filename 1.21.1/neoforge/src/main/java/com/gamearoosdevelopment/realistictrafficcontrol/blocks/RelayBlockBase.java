package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import java.util.List;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;
import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;
import com.gamearoosdevelopment.realistictrafficcontrol.item.CrossingRelayTunerItem;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.RelayBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Port of 1.12.2 {@code BlockRelayBase}: shared behaviour for the eight-part crossing-relay multiblock.
 * Each corner/segment is a distinct block type; all share {@link RelayBlockEntity}.
 */
public abstract class RelayBlockBase extends Block implements EntityBlock {

    public RelayBlockBase(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    protected abstract String registryName();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(BlockStateProperties.HORIZONTAL_FACING,
                rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(ModItems.CROSSING_RELAY_BOX.get()));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RelayBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.RELAY.get(),
                (lvl, pos, st, be) -> RelayBlockEntity.serverTick(lvl, pos, st, be));
    }

    @SuppressWarnings("unchecked")
    private static <A extends BlockEntity, E extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> served, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == served ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof CrossingRelayTunerItem) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            com.gamearoosdevelopment.realistictrafficcontrol.client.RTCClientScreens.openCrossingRelaySettings(level, pos);
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RelayBlockEntity relay) {
            relay.onPlaced(level);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            breakMultiblockExcept(level, pos, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** When any relay segment is removed, destroy the other seven parts without drops. */
    static void breakMultiblockExcept(Level level, BlockPos pos, BlockPos exclude) {
        BlockPos anchor = findMultiblockAnchor(level, pos);
        if (anchor == null) {
            return;
        }
        for (BlockPos part : RelayBlockEntity.enumerateMultiblockParts(anchor, level.getBlockState(anchor))) {
            if (exclude != null && part.equals(exclude)) {
                continue;
            }
            if (level.getBlockState(part).getBlock() instanceof RelayBlockBase) {
                level.destroyBlock(part, false);
            }
        }
    }

    private static BlockPos findMultiblockAnchor(Level level, BlockPos pos) {
        BlockPos workingPos = pos.relative(Direction.SOUTH);
        while (level.getBlockState(workingPos).getBlock() instanceof RelayBlockBase) {
            workingPos = workingPos.relative(Direction.SOUTH);
        }
        workingPos = workingPos.relative(Direction.NORTH);

        workingPos = workingPos.relative(Direction.EAST);
        while (level.getBlockState(workingPos).getBlock() instanceof RelayBlockBase) {
            workingPos = workingPos.relative(Direction.EAST);
        }
        workingPos = workingPos.relative(Direction.WEST);

        workingPos = workingPos.relative(Direction.DOWN);
        while (level.getBlockState(workingPos).getBlock() instanceof RelayBlockBase) {
            workingPos = workingPos.relative(Direction.DOWN);
        }
        return workingPos.relative(Direction.UP);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        if (level.isClientSide) {
            return;
        }
        if (level.getBlockState(pos.below()).getBlock() instanceof RelayBlockBase
                || level.getBlockState(fromPos).getBlock() instanceof RelayBlockBase
                || level.isEmptyBlock(fromPos)) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof RelayBlockEntity relay) {
            RelayBlockEntity master = relay.getMaster(level);
            if (master != null) {
                master.setPowered(level.hasNeighborSignal(pos));
            }
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof RelayBlockEntity relay)) {
            return 0;
        }
        RelayBlockEntity master = relay.getMaster(level);
        return master != null && master.getPowered() ? 15 : 0;
    }

    public static boolean isRelayBlock(Block block) {
        return block instanceof RelayBlockBase;
    }
}
