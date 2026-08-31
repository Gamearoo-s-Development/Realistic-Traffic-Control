package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;
import com.gamearoosdevelopment.realistictrafficcontrol.menu.DisplayMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Common sixteen-angle base for digital signs and portable message boards. */
public abstract class BlockDisplayBase extends Block implements EntityBlock {
    protected BlockDisplayBase(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(RTCProperties.ROTATION, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RTCProperties.ROTATION);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        // Display blocks used the player's raw yaw in 1.12.2. The shared RTC
        // calculator adds 180 degrees for traffic-light placement, which made
        // digital signs and message boards face backwards after the port.
        int rotation = Mth.floor(context.getRotation() * 16.0F / 360.0F + 0.5D) & 15;
        return defaultBlockState().setValue(RTCProperties.ROTATION, rotation);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (player.getMainHandItem().is(ModItems.CROSSING_RELAY_TUNER.get())) return InteractionResult.PASS;
        if (!level.isClientSide && player instanceof ServerPlayer server) {
            server.openMenu(new SimpleMenuProvider((id, inv, p) -> new DisplayMenu(id, inv, pos),
                    Component.translatable(getDescriptionId())), buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return displayShape(state, .5, .22, 1);
    }

    /**
     * Reproduces the axis-aligned bounds used by the 1.12.2 display blocks for
     * every one of their sixteen placement angles.
     */
    protected static VoxelShape displayShape(BlockState state, double halfWidth, double halfDepth,
            double height) {
        double angle = Math.toRadians(state.getValue(RTCProperties.ROTATION) * 22.5);
        double xRadius = Math.abs(Math.cos(angle)) * halfWidth + Math.abs(Math.sin(angle)) * halfDepth;
        double zRadius = Math.abs(Math.sin(angle)) * halfWidth + Math.abs(Math.cos(angle)) * halfDepth;
        return Block.box((.5 - xRadius) * 16, 0, (.5 - zRadius) * 16,
                (.5 + xRadius) * 16, height * 16, (.5 + zRadius) * 16);
    }
}
