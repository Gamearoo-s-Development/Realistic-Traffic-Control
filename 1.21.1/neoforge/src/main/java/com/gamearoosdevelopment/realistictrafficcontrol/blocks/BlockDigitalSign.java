package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.menu.SignMenu;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.DigitalSignBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class BlockDigitalSign extends BlockDisplayBase {
    public BlockDigitalSign(Properties properties) { super(properties); }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DigitalSignBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer server) {
            server.openMenu(new SimpleMenuProvider((id, inv, p) -> new SignMenu(id, inv, pos),
                    Component.translatable(getDescriptionId())), buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static boolean hasNeighbor(BlockGetter level, BlockPos pos, BlockState state, int horizontal, int vertical) {
        BlockPos target;
        if (vertical != 0) {
            target = pos.offset(0, vertical, 0);
        } else {
            double angle = Math.toRadians(state.getValue(RTCProperties.ROTATION) * 22.5);
            target = pos.offset((int) Math.round(Math.cos(angle)) * horizontal, 0,
                    (int) Math.round(Math.sin(angle)) * horizontal);
        }
        BlockState other = level.getBlockState(target);
        return other.getBlock() instanceof BlockDigitalSign
                && other.getValue(RTCProperties.ROTATION).equals(state.getValue(RTCProperties.ROTATION));
    }
}
