package com.gamearoosdevelopment.realistictrafficcontrol.client;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.gui.FrameGuiType;
import com.gamearoosdevelopment.realistictrafficcontrol.gui.FrameGuiType.CheckboxOrientation;
import com.gamearoosdevelopment.realistictrafficcontrol.gui.FrameGuiType.SlotLayout;
import com.gamearoosdevelopment.realistictrafficcontrol.item.BaseItemTrafficLightFrame;
import com.gamearoosdevelopment.realistictrafficcontrol.menu.TrafficLightFrameMenu;
import com.gamearoosdevelopment.realistictrafficcontrol.network.FrameFacingUpdatePayload;
import com.gamearoosdevelopment.realistictrafficcontrol.network.FrameGuiUpdatePayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Pixel-accurate port of the 1.12.2 {@code BaseTrafficLightFrameGui} family. Uses the original per-frame
 * background PNGs at 174×H with matching slot positions, facing row, and Allow Flash checkboxes.
 */
public class TrafficLightFrameScreen extends AbstractContainerScreen<TrafficLightFrameMenu> {

    private static final int ID_FACING_AUTO = 1000;
    private static final int ID_FACING_NORTH = 1001;
    private static final int ID_FACING_SOUTH = 1002;
    private static final int ID_FACING_EAST = 1003;
    private static final int ID_FACING_WEST = 1004;

    private final FrameGuiType layout;
    private Button facingAuto;
    private Button facingNorth;
    private Button facingSouth;
    private Button facingEast;
    private Button facingWest;

    public TrafficLightFrameScreen(TrafficLightFrameMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.layout = menu.getLayout();
        this.imageWidth = FrameGuiType.WIDTH;
        this.imageHeight = layout.getHeight();
        this.inventoryLabelY = layout.playerInventoryTopY() - 11;
    }

    @Override
    protected void init() {
        super.init();
        ItemStack frameStack = menu.getFrameStack();
        int facingY = topPos - 24;

        facingAuto = addRenderableWidget(Button.builder(Component.literal("Auto"), b -> setFacing(null))
                .bounds(leftPos + 44, facingY, 34, 16).build());
        facingNorth = addRenderableWidget(Button.builder(Component.literal("N"), b -> setFacing(Direction.NORTH))
                .bounds(leftPos + 82, facingY, 22, 16).build());
        facingSouth = addRenderableWidget(Button.builder(Component.literal("S"), b -> setFacing(Direction.SOUTH))
                .bounds(leftPos + 106, facingY, 22, 16).build());
        facingEast = addRenderableWidget(Button.builder(Component.literal("E"), b -> setFacing(Direction.EAST))
                .bounds(leftPos + 130, facingY, 22, 16).build());
        facingWest = addRenderableWidget(Button.builder(Component.literal("W"), b -> setFacing(Direction.WEST))
                .bounds(leftPos + 154, facingY, 22, 16).build());

        for (int i = 0; i < layout.getSlots().size(); i++) {
            SlotLayout slotLayout = layout.getSlots().get(i);
            int x = leftPos + slotLayout.primaryX();
            int y = topPos + slotLayout.primaryY();
            CheckboxOrientation orientation = slotLayout.checkboxOrientation();
            int offset = slotLayout.secondaryX() - slotLayout.primaryX() > 30 ? 52 : 30;
            switch (orientation) {
                case ABOVE -> y -= 24;
                case BELOW -> y += 28;
                case LEFT -> x -= 92;
                case RIGHT -> x += offset;
                default -> {
                }
            }
            final int slotIndex = slotLayout.slotIndex();
            Checkbox box = Checkbox.builder(Component.literal("Allow Flash"),
                            this.font)
                    .pos(x, y)
                    .selected(BaseItemTrafficLightFrame.getAllowFlash(frameStack, slotIndex))
                    .onValueChange((cb, checked) -> {
                        BaseItemTrafficLightFrame.setAllowFlash(menu.getFrameStack(), slotIndex, checked);
                        PacketDistributor.sendToServer(new FrameGuiUpdatePayload(slotIndex, checked));
                    })
                    .build();
            addRenderableWidget(box);
        }

        updateFacingSelection();
    }

    private void setFacing(Direction facing) {
        BaseItemTrafficLightFrame.setConfiguredApproachFacing(menu.getFrameStack(), facing);
        int index = facing == null ? -1 : facing.get2DDataValue();
        PacketDistributor.sendToServer(new FrameFacingUpdatePayload(index));
        updateFacingSelection();
    }

    private void updateFacingSelection() {
        Direction facing = BaseItemTrafficLightFrame.getConfiguredApproachFacing(menu.getFrameStack());
        facingAuto.active = facing != null;
        facingNorth.active = facing != Direction.NORTH;
        facingSouth.active = facing != Direction.SOUTH;
        facingEast.active = facing != Direction.EAST;
        facingWest.active = facing != Direction.WEST;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID,
                "textures/gui/" + layout.getTextureName());
        graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, Component.literal("Facing:"), 4, -20, 0xFFFFFF, false);
        super.renderLabels(graphics, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        super.renderSlot(graphics, slot);
    }
}
