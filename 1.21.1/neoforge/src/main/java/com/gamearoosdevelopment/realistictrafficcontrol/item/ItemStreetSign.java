package com.gamearoosdevelopment.realistictrafficcontrol.item;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockStreetSign;
import com.gamearoosdevelopment.realistictrafficcontrol.menu.StreetSignMenu;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.StreetSign;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.StreetSignBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.SimpleMenuProvider;

/** Port of 1.12.2 {@code ItemStreetSign}. */
public class ItemStreetSign extends BlockItem {

    public ItemStreetSign(BlockStreetSign block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (level.getBlockState(pos).is(ModBlocks.STREET_SIGN.get())) {
            if (level.getBlockEntity(pos) instanceof StreetSignBlockEntity streetSignTE) {
                StreetSign newSign = new StreetSign();
                if (player != null) {
                    newSign.setRotation(CustomAngleCalculator.getRotationForYaw(player.getYRot()));
                }
                if (streetSignTE.addStreetSign(newSign)) {
                    if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                        MenuProvider provider = new SimpleMenuProvider(
                                (id, inv, p) -> new StreetSignMenu(id, inv, pos),
                                Component.literal("Street Sign"));
                        serverPlayer.openMenu(provider, buf -> buf.writeBlockPos(pos));
                    }
                    context.getItemInHand().shrink(1);
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }
        return super.useOn(context);
    }
}
