package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;
import com.gamearoosdevelopment.realistictrafficcontrol.menu.StreetSignMenu;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.StreetSign;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.StreetSignBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.SimpleMenuProvider;

import java.util.ArrayList;
import java.util.List;

/** Port of 1.12.2 {@code BlockStreetSign}. */
public class BlockStreetSign extends Block implements EntityBlock {

    public BlockStreetSign(Properties properties) {
        super(properties);
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
        return new StreetSignBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockEntity tileEntity = level.getBlockEntity(pos);
        if (!(tileEntity instanceof StreetSignBlockEntity streetSignTE)) {
            return Shapes.block();
        }
        double pixelsHigh = 0;
        for (int i = 0; i < StreetSignBlockEntity.MAX_STREET_SIGNS; i++) {
            if (streetSignTE.getStreetSign(i) != null) {
                pixelsHigh += 4;
            }
        }
        return Block.box(0, 0, 0, 16, pixelsHigh, 16);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof StreetSignBlockEntity te) {
            StreetSign newSign = new StreetSign();
            newSign.setRotation(CustomAngleCalculator.getRotationForYaw(placer.getYRot()));
            te.addStreetSign(newSign);
        }
        if (!level.isClientSide && placer instanceof ServerPlayer serverPlayer) {
            openGui(serverPlayer, pos);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (main.is(ModItems.STREET_SIGN.get()) || off.is(ModItems.STREET_SIGN.get())) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            openGui(serverPlayer, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void openGui(ServerPlayer player, BlockPos pos) {
        MenuProvider provider = new SimpleMenuProvider(
                (id, inv, p) -> new StreetSignMenu(id, inv, pos),
                Component.literal("Street Sign"));
        player.openMenu(provider, buf -> buf.writeBlockPos(pos));
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>();
        BlockEntity be = builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if (be instanceof StreetSignBlockEntity streetSignTE) {
            int count = streetSignTE.getOccupiedCount();
            if (count > 0) {
                drops.add(new ItemStack(ModItems.STREET_SIGN.get(), count));
            }
        }
        return drops;
    }
}
