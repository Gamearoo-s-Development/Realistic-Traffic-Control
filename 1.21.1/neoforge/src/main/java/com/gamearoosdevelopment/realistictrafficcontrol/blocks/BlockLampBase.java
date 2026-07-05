package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;
import com.gamearoosdevelopment.realistictrafficcontrol.menu.CrossingLampsMenu;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.CrossingLampsBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CrossingLampState;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.SimpleMenuProvider;

/**
 * Abstract base for crossing-lamp blocks. Port of 1.12.2 {@code BlockLampBase}; bulb sub-models are rendered
 * by {@link com.gamearoosdevelopment.realistictrafficcontrol.client.render.CrossingLampsBlockEntityRenderer}.
 */
public abstract class BlockLampBase extends Block implements EntityBlock {

    protected BlockLampBase(Properties properties) {
        super(properties);
    }

    public abstract String getLampRegistryName();

    protected abstract void defineLampState(StateDefinition.Builder<Block, BlockState> builder);

    @Override
    protected final void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        defineLampState(builder);
        builder.add(RTCProperties.LAMP_STATE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrossingLampsBlockEntity(pos, state);
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        CrossingLampState lampState = state.getValue(RTCProperties.LAMP_STATE);
        return lampState == CrossingLampState.Flash1 || lampState == CrossingLampState.Flash2 ? 15 : 0;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (player.getMainHandItem().is(ModItems.CROSSING_RELAY_TUNER.get())
                || player.getMainHandItem().is(ModItems.SCREWDRIVER.get())) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = new SimpleMenuProvider(
                    (id, inv, p) -> new CrossingLampsMenu(id, inv, pos),
                    Component.literal("Crossing Lamps"));
            serverPlayer.openMenu(provider, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
