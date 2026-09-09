package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;
import com.gamearoosdevelopment.realistictrafficcontrol.menu.DisplayMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Common sixteen-angle base for digital signs and portable message boards. */
public abstract class BlockDisplayBase extends Block implements EntityBlock {
    protected BlockDisplayBase(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(RTCProperties.ROTATION, 0)
                .setValue(RTCProperties.MOUNT_FACE, net.minecraft.core.Direction.SOUTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RTCProperties.ROTATION, RTCProperties.MOUNT_FACE);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(RTCProperties.ROTATION, CustomAngleCalculator.rotationForPlacement(context, false))
                .setValue(RTCProperties.MOUNT_FACE, com.gamearoosdevelopment.realistictrafficcontrol.util.PoleAssembly.mountFace(context));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return RTCProperties.rotate16(state, rotation);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return RTCProperties.mirror16(state, mirror);
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

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return com.gamearoosdevelopment.realistictrafficcontrol.util.RTCShapes.clipToBlock(
                getShape(state, level, pos, context));
    }

    /**
     * Unrotated plate matching the BER face, then the same pole-mount yaw as the renderer.
     */
    protected static VoxelShape displayShape(BlockState state, double halfWidth, double halfDepth,
            double height) {
        VoxelShape base = Block.box((.5 - halfWidth) * 16, 0, (.5 - halfDepth) * 16,
                (.5 + halfWidth) * 16, height * 16, (.5 + halfDepth) * 16);
        return com.gamearoosdevelopment.realistictrafficcontrol.util.RTCShapes.rotateYPoleMounted(
                base, state.getValue(RTCProperties.ROTATION),
                com.gamearoosdevelopment.realistictrafficcontrol.util.PoleAssembly.mountCardinal(state));
    }

    @Override
    protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        return com.gamearoosdevelopment.realistictrafficcontrol.util.RTCDrops.self(state);
    }
}
