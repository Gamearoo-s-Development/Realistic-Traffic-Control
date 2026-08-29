package com.gamearoosdevelopment.realistictrafficcontrol.client;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockCrossingGateLamps;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockOverheadLamps;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties;
import com.gamearoosdevelopment.realistictrafficcontrol.menu.CrossingLampsMenu;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.CrossingLampsBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;
import java.util.function.IntSupplier;

/** Port of 1.12.2 {@code CrossingLampsGui} with the original quadrant control layout. */
public class CrossingLampsScreen extends AbstractContainerScreen<CrossingLampsMenu> {

    private static final Component TITLE = Component.literal("Crossing Lamp Configuration");
    private static final String SHOWN = "Bulb attached";

    private int blockRotation = 0;

    public CrossingLampsScreen(CrossingLampsMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    private int nwX;
    private int nwY;
    private int neX;
    private int neY;
    private int seX;
    private int seY;
    private int swX;
    private int swY;

    @Override
    protected void init() {
        super.init();
        BlockState state = minecraft.player.level().getBlockState(menu.getBlockPos());
        if (state.getBlock() instanceof BlockCrossingGateLamps) {
            blockRotation = state.getValue(RTCProperties.ROTATION);
        } else if (state.getBlock() instanceof BlockOverheadLamps) {
            blockRotation = state.getValue(BlockOverheadLamps.FACING).get2DDataValue() * 4;
        }
        float yaw = minecraft.player.getYHeadRot() % 360 + blockRotation * -22.5F;
        nwX = -(int) (Mth.cos((float) Math.toRadians(360 - yaw + 45)) * 110) + width / 2;
        nwY = -(int) (Mth.sin((float) Math.toRadians(360 - yaw + 45)) * 110) + height / 2;
        neX = -(int) (Mth.cos((float) Math.toRadians(360 - yaw + 135)) * 110) + width / 2;
        neY = -(int) (Mth.sin((float) Math.toRadians(360 - yaw + 135)) * 110) + height / 2;
        seX = -(int) (Mth.cos((float) Math.toRadians(360 - yaw + 225)) * 110) + width / 2;
        seY = -(int) (Mth.sin((float) Math.toRadians(360 - yaw + 225)) * 110) + height / 2;
        swX = -(int) (Mth.cos((float) Math.toRadians(360 - yaw + 315)) * 110) + width / 2;
        swY = -(int) (Mth.sin((float) Math.toRadians(360 - yaw + 315)) * 110) + height / 2;

        CrossingLampsBlockEntity te = menu.getLamps(minecraft.player);
        if (te == null) {
            return;
        }

        int shownWidth = font.width(SHOWN);
        addQuadrantControls(0, nwX, nwY, shownWidth, te::getNwBulbRotation, te::setNwBulbRotation);
        addQuadrantControls(1, neX, neY, shownWidth, te::getNeBulbRotation, te::setNeBulbRotation);
        addQuadrantControls(2, swX, swY, shownWidth, te::getSwBulbRotation, te::setSwBulbRotation);
        addQuadrantControls(3, seX, seY, shownWidth, te::getSeBulbRotation, te::setSeBulbRotation);
    }

    private void addQuadrantControls(int quadrant, int thisX, int thisY, int shownWidth,
            IntSupplier getter, Consumer<Integer> setter) {
        boolean isRightAligned = thisX < width / 2;
        boolean isTopAligned = thisY > height / 2;
        int currentAngle = getter.getAsInt();

        Checkbox box = Checkbox.builder(Component.literal(SHOWN), font)
                .pos(isRightAligned ? thisX - 11 - shownWidth : thisX, isTopAligned ? thisY : thisY - 11)
                .selected(currentAngle >= 0)
                .onValueChange((cb, checked) -> {
                    setter.accept(checked ? 0 : -1);
                    syncToServer();
                })
                .build();
        addRenderableWidget(box);

        Button decrease = Button.builder(Component.literal("<"), b -> {
            int current = getter.getAsInt();
            if (current >= 15) {
                current = -1;
            }
            current++;
            setter.accept(current);
            syncToServer();
        }).bounds(isRightAligned ? thisX - 80 : thisX, isTopAligned ? thisY + 14 : thisY - 34, 20, 20).build();

        Button increase = Button.builder(Component.literal(">"), b -> {
            int current = getter.getAsInt();
            if (current <= 0) {
                current = 16;
            }
            current--;
            setter.accept(current);
            syncToServer();
        }).bounds(isRightAligned ? thisX - 20 : thisX + 60, isTopAligned ? thisY + 15 : thisY - 34, 20, 20).build();

        addRenderableWidget(decrease);
        addRenderableWidget(increase);
    }

    private void syncToServer() {
        CrossingLampsBlockEntity te = menu.getLamps(minecraft.player);
        if (te != null) {
            te.performClientToServerSync();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, TITLE, width / 2, 30, 0xFFFFFF);
        renderPreview(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        drawAngleLabels(graphics);
    }

    private void renderPreview(GuiGraphics graphics) {
        BlockState state = minecraft.player.level().getBlockState(menu.getBlockPos());
        if (state.getRenderShape() == RenderShape.INVISIBLE) {
            return;
        }
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(width / 2.0F, height / 2.0F, 150.0F);
        poseStack.scale(128.0F, -128.0F, 128.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(minecraft.player.getYHeadRot() + blockRotation * -22.5F));
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, buffer, 15728880,
                OverlayTexture.NO_OVERLAY);
        buffer.endBatch();
        poseStack.popPose();
    }

    private void drawAngleLabels(GuiGraphics graphics) {
        CrossingLampsBlockEntity te = menu.getLamps(minecraft.player);
        if (te == null) {
            return;
        }
        drawAngleLabel(graphics, nwX, nwY, te.getNwBulbRotation());
        drawAngleLabel(graphics, neX, neY, te.getNeBulbRotation());
        drawAngleLabel(graphics, swX, swY, te.getSwBulbRotation());
        drawAngleLabel(graphics, seX, seY, te.getSeBulbRotation());
    }

    private void drawAngleLabel(GuiGraphics graphics, int thisX, int thisY, int rotationConsidered) {
        if (rotationConsidered == -1) {
            return;
        }
        boolean isRightAligned = thisX < width / 2;
        boolean isTopAligned = thisY > height / 2;
        String displayValue = Integer.toString(360 - (int) (rotationConsidered * 22.5));
        if ("360".equalsIgnoreCase(displayValue)) {
            displayValue = "0";
        }
        displayValue += "\u00B0";
        int displayValueWidth = font.width(displayValue);
        graphics.drawString(font, displayValue,
                isRightAligned ? thisX - 40 - displayValueWidth / 2 : thisX + 40 - displayValueWidth / 2,
                isTopAligned ? thisY + 24 - font.lineHeight / 2 : thisY - 24 - font.lineHeight / 2,
                0xFFFFFF, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    }
}
