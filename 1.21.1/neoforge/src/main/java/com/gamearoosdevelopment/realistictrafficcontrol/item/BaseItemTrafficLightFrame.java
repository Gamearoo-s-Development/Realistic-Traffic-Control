package com.gamearoosdevelopment.realistictrafficcontrol.item;

import java.util.List;
import java.util.function.Supplier;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.RTCDataComponents;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties;
import com.gamearoosdevelopment.realistictrafficcontrol.gui.FrameGuiType;
import com.gamearoosdevelopment.realistictrafficcontrol.menu.TrafficLightFrameMenu;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Concrete, parameterized port of the 1.12.2 abstract {@code BaseItemTrafficLightFrame}. One class serves
 * every frame type. The configured bulbs are carried in the {@link RTCDataComponents#FRAME_DATA} data
 * component (replacing the old item-capability inventory + {@code getNBTShareTag} handshake) and copied
 * into the {@link TrafficLightBlockEntity} on placement.
 *
 * <p>The bulb-configuration GUI (opened on right-click-in-air in 1.12.2) is reconnected in the menu
 * phase; placement and drops already round-trip the full configuration.
 */
public class BaseItemTrafficLightFrame extends Item {

    private final int bulbCount;
    private final Supplier<? extends Block> baseBlock;
    private final String typeLabel;
    private final FrameGuiType guiLayout;
    private final boolean placesUpperHalf;

    public BaseItemTrafficLightFrame(Properties properties, int bulbCount, Supplier<? extends Block> baseBlock,
            String typeLabel, FrameGuiType guiLayout) {
        this(properties, bulbCount, baseBlock, typeLabel, guiLayout, false);
    }

    public BaseItemTrafficLightFrame(Properties properties, int bulbCount, Supplier<? extends Block> baseBlock,
            String typeLabel, FrameGuiType guiLayout, boolean placesUpperHalf) {
        super(properties.stacksTo(1));
        this.bulbCount = bulbCount;
        this.baseBlock = baseBlock;
        this.typeLabel = typeLabel;
        this.guiLayout = guiLayout;
        this.placesUpperHalf = placesUpperHalf;
    }

    public FrameGuiType getGuiLayout() {
        return guiLayout;
    }

    public int getBulbCount() {
        return bulbCount;
    }

    /** Read a mutable copy of the frame's stored configuration ({@code frame_data} component). */
    public static net.minecraft.nbt.CompoundTag getData(net.minecraft.world.item.ItemStack stack) {
        CompoundTag tag = stack.get(RTCDataComponents.FRAME_DATA.get());
        return tag == null ? new CompoundTag() : tag.copy();
    }

    /** Port of 1.12.2 {@code setConfiguredApproachFacing}; writes into the {@code frame_data} component. */
    public static void setConfiguredApproachFacing(net.minecraft.world.item.ItemStack stack,
            net.minecraft.core.Direction facing) {
        CompoundTag tag = getData(stack);
        tag.putInt("configuredApproachFacing", facing == null ? -1 : facing.get2DDataValue());
        stack.set(RTCDataComponents.FRAME_DATA.get(), tag);
    }

    /** Port of 1.12.2 {@code handleGuiAlwaysUpdate}; the BE reads {@code allowflash<slot>} on placement. */
    public static void setAllowFlash(net.minecraft.world.item.ItemStack stack, int slot, boolean allowFlash) {
        CompoundTag tag = getData(stack);
        tag.putBoolean("allowflash" + slot, allowFlash);
        stack.set(RTCDataComponents.FRAME_DATA.get(), tag);
    }

    public static boolean getAllowFlash(net.minecraft.world.item.ItemStack stack, int slot) {
        CompoundTag tag = getData(stack);
        String key = "allowflash" + slot;
        return !tag.contains(key) || tag.getBoolean(key);
    }

    public static Direction getConfiguredApproachFacing(net.minecraft.world.item.ItemStack stack) {
        CompoundTag tag = getData(stack);
        if (!tag.contains("configuredApproachFacing")) {
            return null;
        }
        int index = tag.getInt("configuredApproachFacing");
        return index < 0 ? null : Direction.from2DDataValue(index);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (context.getPlayer() == null) {
            return InteractionResult.PASS;
        }

        net.minecraft.core.BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).canBeReplaced()) {
            pos = pos.relative(context.getClickedFace());
        }

        int rotation = CustomAngleCalculator.getRotationForYawCardinal(context.getPlayer().getYRot());
        BlockState placed = baseBlock.get().defaultBlockState().setValue(RTCProperties.ROTATION, rotation);
        level.setBlock(pos, placed, 3);

        if (placesUpperHalf) {
            BlockPos above = pos.above();
            if (level.getBlockState(above).canBeReplaced()) {
                level.setBlock(above, ModBlocks.TRAFFIC_LIGHT_5_UPPER.get().defaultBlockState()
                        .setValue(RTCProperties.ROTATION, rotation), 3);
            }
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TrafficLightBlockEntity tl) {
            CompoundTag data = context.getItemInHand().get(RTCDataComponents.FRAME_DATA.get());
            if (data != null) {
                tl.applyFrameTag(data.copy());
            }
        }

        context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand == InteractionHand.MAIN_HAND && !level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = new SimpleMenuProvider(
                    (id, inv, p) -> new TrafficLightFrameMenu(id, inv, stack, guiLayout), stack.getHoverName());
            serverPlayer.openMenu(provider, b -> b.writeVarInt(guiLayout.ordinal()));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.literal("Type: " + typeLabel));
        tooltip.add(Component.literal("Bulb slots: " + bulbCount));
    }
}
