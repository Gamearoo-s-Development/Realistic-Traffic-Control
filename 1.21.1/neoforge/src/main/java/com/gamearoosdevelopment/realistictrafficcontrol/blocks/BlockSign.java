package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.menu.SignMenu;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.Sign;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.SignBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;
import com.gamearoosdevelopment.realistictrafficcontrol.util.RTCShapes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.SimpleMenuProvider;

import java.util.Arrays;

/** Port of 1.12.2 {@code BlockSign}. */
public class BlockSign extends Block implements EntityBlock {

    public BlockSign(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(RTCProperties.ROTATION, 0)
                .setValue(RTCProperties.MOUNT_FACE, net.minecraft.core.Direction.SOUTH)
                .setValue(RTCProperties.VALIDHORIZONTALBAR, false)
                .setValue(RTCProperties.ISHALFHEIGHT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RTCProperties.ROTATION, RTCProperties.MOUNT_FACE, RTCProperties.VALIDHORIZONTALBAR,
                RTCProperties.ISHALFHEIGHT);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(RTCProperties.ROTATION, CustomAngleCalculator.rotationForPlacement(context))
                .setValue(RTCProperties.MOUNT_FACE, com.gamearoosdevelopment.realistictrafficcontrol.util.PoleAssembly.mountFace(context));
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
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SignBlockEntity(pos, state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return computeActualState(state, level, pos);
    }

    public BlockState computeActualState(BlockState state, BlockGetter level, BlockPos pos) {
        boolean validHorizontalBar = false;
        boolean isHalfHeight = false;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SignBlockEntity signTE && signTE.isHorizontalBarSuppressed()) {
            validHorizontalBar = false;
        } else {
            int rotation = state.getValue(RTCProperties.ROTATION);
            boolean isCardinal = CustomAngleCalculator.isCardinal(rotation);
            if (isCardinal && CustomAngleCalculator.isNorthSouth(rotation)) {
                validHorizontalBar = getValidStateForAttachableSubModels(state, level.getBlockState(pos.west()),
                        Direction.NORTH, Direction.SOUTH)
                        || getValidStateForAttachableSubModels(state, level.getBlockState(pos.east()),
                                Direction.NORTH, Direction.SOUTH);
            } else if (isCardinal) {
                validHorizontalBar = getValidStateForAttachableSubModels(state, level.getBlockState(pos.north()),
                        Direction.WEST, Direction.EAST)
                        || getValidStateForAttachableSubModels(state, level.getBlockState(pos.south()),
                                Direction.WEST, Direction.EAST);
            }
        }

        if (be instanceof SignBlockEntity signTE && signTE.getSign() != null) {
            isHalfHeight = signTE.getSign().getHalfHeight();
        }

        return state.setValue(RTCProperties.VALIDHORIZONTALBAR, validHorizontalBar)
                .setValue(RTCProperties.ISHALFHEIGHT, isHalfHeight);
    }

    private boolean getValidStateForAttachableSubModels(BlockState signState, BlockState state,
            Direction... validFacings) {
        if (state.getBlock() == ModBlocks.HORIZONTAL_POLE.get()) {
            boolean poleNorthSouth = CustomAngleCalculator.isNorthSouth(state.getValue(RTCProperties.ROTATION));
            boolean wantNorthSouth = Arrays.stream(validFacings).anyMatch(f -> f == Direction.NORTH);
            return poleNorthSouth != wantNorthSouth;
        }
        if (state.getBlock() instanceof BlockBaseTrafficLight) {
            return true;
        }
        if (state.getBlock() == ModBlocks.SIGN.get()) {
            int otherRotation = state.getValue(RTCProperties.ROTATION);
            boolean otherIsCardinal = CustomAngleCalculator.isCardinal(otherRotation);
            boolean isForNorthSouth = Arrays.stream(validFacings).anyMatch(f -> f == Direction.NORTH);
            boolean thisSignNorthSouth = CustomAngleCalculator.isNorthSouth(signState.getValue(RTCProperties.ROTATION));
            return otherIsCardinal && ((isForNorthSouth && thisSignNorthSouth) || (!isForNorthSouth && !thisSignNorthSouth));
        }
        return false;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        double poleHeight = 1;
        BlockEntity worldTE = level.getBlockEntity(pos);
        if (worldTE instanceof SignBlockEntity signTE && signTE.getSign() != null) {
            poleHeight = signTE.getSign().getHalfHeight() ? 0.5 : 1;
        }
        int rotation = state.getValue(RTCProperties.ROTATION);
        VoxelShape base = Block.box(0, 0, -7, 16, poleHeight * 16, -1);
        return RTCShapes.rotateYPoleMounted(base, rotation, com.gamearoosdevelopment.realistictrafficcontrol.util.PoleAssembly.mountCardinal(state));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return RTCShapes.clipToBlock(getShape(state, level, pos, context));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = new SimpleMenuProvider(
                    (id, inv, p) -> new SignMenu(id, inv, pos),
                    Component.literal("Sign"));
            serverPlayer.openMenu(provider, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SignBlockEntity signTE && signTE.getID() == null && signTE.getVariantLegacy() == -1
                && signTE.getTypeLegacy() == -1) {
            signTE.setID(Sign.DEFAULT_BLANK_SIGN);
            signTE.setChanged();
        }
        BlockState actualState = computeActualState(level.getBlockState(pos), level, pos);
        if (actualState != level.getBlockState(pos)) {
            level.setBlock(pos, actualState, Block.UPDATE_ALL);
        }
    }

    @Override
    protected java.util.List<ItemStack> getDrops(BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        return com.gamearoosdevelopment.realistictrafficcontrol.util.RTCDrops.self(state);
    }
}
