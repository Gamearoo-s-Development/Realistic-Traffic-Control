package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.BellBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Concrete, parameterized port of the 1.12.2 bell / horn blocks ({@code BlockWCHBell},
 * {@code BlockWaysideHorn}, {@code BlockTeardropBell}, {@code BlockSafetran*}, ...). One class covers all
 * types; the looping sound + collision shapes are supplied at registration time.
 *
 * <p>In 1.12.2 the bells were rung exclusively by the crossing-relay orchestration. Until that subsystem
 * is ported, the bell also responds to redstone power ({@link #neighborChanged}) so it is usable/testable
 * standalone; the relay driver will call {@link BellBlockEntity#setIsRinging} directly later.
 */
public class BellBlock extends Block implements EntityBlock, IBellBlock {

    private final Holder<SoundEvent> sound;
    private final VoxelShape nsShape;
    private final VoxelShape ewShape;

    public BellBlock(Properties properties, Holder<SoundEvent> sound, VoxelShape nsShape, VoxelShape ewShape) {
        super(properties);
        this.sound = sound;
        this.nsShape = nsShape;
        this.ewShape = ewShape;
        registerDefaultState(getStateDefinition().any().setValue(RTCProperties.ROTATION, 0));
    }

    @Override
    public MapCodec<? extends Block> codec() {
        return RecordCodecBuilder.mapCodec(inst -> inst.group(
                propertiesCodec(),
                BuiltInRegistries.SOUND_EVENT.holderByNameCodec().fieldOf("sound")
                        .forGetter(b -> ((BellBlock) b).sound)
        ).apply(inst, (props, snd) -> new BellBlock(props, snd,
                Block.box(6, 0, 6, 10, 16, 10), Block.box(6, 0, 6, 10, 16, 10))));
    }

    @Override
    public Holder<SoundEvent> getSound() {
        return sound;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RTCProperties.ROTATION);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(RTCProperties.ROTATION,
                CustomAngleCalculator.getRotationForYaw(context.getRotation()));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BellBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.BELL.get(),
                (lvl, pos, st, be) -> BellBlockEntity.clientTick(lvl, pos, st, be));
    }

    @SuppressWarnings("unchecked")
    private static <A extends BlockEntity, E extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> served, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == served ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
            boolean movedByPiston) {
        if (level.isClientSide) {
            return;
        }
        boolean powered = level.hasNeighborSignal(pos);
        if (level.getBlockEntity(pos) instanceof BellBlockEntity bell && bell.getIsRinging() != powered) {
            bell.setIsRinging(powered);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int rotation = state.getValue(RTCProperties.ROTATION);
        boolean northSouth = rotation >= 14 || rotation < 2 || (rotation >= 6 && rotation < 10);
        return northSouth ? nsShape : ewShape;
    }
}
