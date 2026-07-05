package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import java.util.function.Supplier;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;
import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;
import com.gamearoosdevelopment.realistictrafficcontrol.RTCDataComponents;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Concrete, parameterized port of the 1.12.2 abstract {@code BlockBaseTrafficLight}. One class serves
 * every frame type; the bulb count and dropped frame item are supplied at registration time. Implements
 * {@link EntityBlock} directly (instead of {@code BaseEntityBlock}) so the frame model still renders
 * while a {@link TrafficLightBlockEntity} drives the lit bulbs (renderer added in Phase 6).
 *
 * <p>Rotation is stored in {@link RTCProperties#ROTATION}; {@code cover_hook}/{@code wire_cutter}
 * toggle {@link RTCProperties#COVER}/{@link RTCProperties#POLE}. The 1.12.2 auto-detected bar sub-model
 * properties are dropped (they were effectively disabled).
 */
public class BlockBaseTrafficLight extends Block implements EntityBlock, ITrafficLightBlock {

    private final int bulbCount;
    private final Supplier<? extends Item> frameItem;
    private final Supplier<Block> upperHalfBlock;

    public BlockBaseTrafficLight(Properties properties, int bulbCount, Supplier<? extends Item> frameItem) {
        this(properties, bulbCount, frameItem, () -> null);
    }

    public BlockBaseTrafficLight(Properties properties, int bulbCount, Supplier<? extends Item> frameItem,
            Supplier<Block> upperHalfBlock) {
        super(properties);
        this.bulbCount = bulbCount;
        this.frameItem = frameItem;
        this.upperHalfBlock = upperHalfBlock;
        registerDefaultState(getStateDefinition().any()
                .setValue(RTCProperties.ROTATION, 0)
                .setValue(RTCProperties.COVER, true)
                .setValue(RTCProperties.POLE, false));
    }

    @Override
    public MapCodec<? extends Block> codec() {
        return RecordCodecBuilder.mapCodec(inst -> inst.group(
                propertiesCodec(),
                com.mojang.serialization.Codec.INT.fieldOf("bulb_count").forGetter(b -> ((BlockBaseTrafficLight) b).bulbCount),
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("frame_item").forGetter(b -> ((BlockBaseTrafficLight) b).frameItem.get())
        ).apply(inst, (props, count, item) -> new BlockBaseTrafficLight(props, count, () -> item)));
    }

    @Override
    public int getBulbCount() {
        return bulbCount;
    }

    @Override
    public Item getFrameItem() {
        return frameItem.get();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RTCProperties.ROTATION, RTCProperties.COVER, RTCProperties.POLE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrafficLightBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.TRAFFIC_LIGHT.get(),
                (lvl, pos, st, be) -> TrafficLightBlockEntity.clientTick(lvl, pos, st, be));
    }

    @SuppressWarnings("unchecked")
    private static <A extends BlockEntity, E extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> served, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == served ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        boolean isCover = stack.is(ModItems.COVER_HOOK.get());
        boolean isWireCutter = stack.is(ModItems.WIRE_CUTTER.get());
        if (!isCover && !isWireCutter) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TrafficLightBlockEntity tl) {
                if (isCover) {
                    boolean next = !state.getValue(RTCProperties.COVER);
                    tl.setCover(next);
                    level.setBlock(pos, state.setValue(RTCProperties.COVER, next), 3);
                } else {
                    boolean next = !state.getValue(RTCProperties.POLE);
                    tl.setPole(next);
                    level.setBlock(pos, state.setValue(RTCProperties.POLE, next), 3);
                }
            }
            if (!player.getAbilities().instabuild) {
                stack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof TrafficLightBlockEntity tl ? buildFrameStack(tl) : new ItemStack(getFrameItem());
    }

    @Override
    protected java.util.List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if (be instanceof TrafficLightBlockEntity tl) {
            return java.util.List.of(buildFrameStack(tl));
        }
        return java.util.List.of(new ItemStack(getFrameItem()));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int rotation = state.getValue(RTCProperties.ROTATION);
        switch (rotation) {
            case 0:
                return box(3, 0, 7, 13, 16, 12);
            case 8:
                return box(3, 0, 4, 13, 16, 9);
            case 4:
                return box(4, 0, 3, 9, 16, 13);
            case 12:
                return box(7, 0, 3, 12, 16, 13);
            case 1:
            case 15:
            case 7:
            case 9:
            case 3:
            case 5:
            case 11:
            case 13:
                return box(6, 0, 6, 12, 16, 12);
            default:
                return box(3.2, 0, 3.2, 12.8, 16, 12.8);
        }
    }

    public ItemStack buildFrameStack(TrafficLightBlockEntity be) {
        ItemStack stack = new ItemStack(getFrameItem());
        if (be != null) {
            CompoundTag tag = be.writeFrameTag();
            stack.set(RTCDataComponents.FRAME_DATA.get(), tag);
        }
        return stack;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && upperHalfBlock.get() != null) {
            BlockPos above = pos.above();
            if (level.getBlockState(above).is(upperHalfBlock.get())) {
                level.removeBlock(above, false);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
