package com.gamearoosdevelopment.realistictrafficcontrol.client;

import java.util.ArrayList;
import java.util.UUID;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.client.gui.SignImageListWidget;
import com.gamearoosdevelopment.realistictrafficcontrol.menu.DisplayMenu;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.Sign;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.SignHorizontalAlignment;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.SignVerticalAlignment;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.DigitalSignControllerBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.MessageBoardBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.MessageBoardControllerBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.DisplaySchedule;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Three deliberately separate display editors behind the shared display menu:
 * the simple board editor, the full message-board controller, and the digital
 * sign controller. Their controls and save behavior mirror the 1.12.2 GUIs.
 */
public final class DisplayScreen extends AbstractContainerScreen<DisplayMenu> {
    private BlockEntity display;
    private final EditBox[] lines = new EditBox[MessageBoardBlockEntity.MAX_LINES];
    private EditBox interval;
    private EditBox gameTimes;
    private EditBox signTime;
    private EditBox color;
    private Button modeButton;
    private Button fontButton;
    private Button sizeButton;
    private Button brightnessButton;
    private Button pageButton;
    private Button scheduleButton;
    private Button digitalTextEditorButton;
    private SignImageListWidget signList;
    private EditBox digitalSearch;
    private int editingPage = -1;
    private UUID editingSign;
    private final ArrayList<String> editingTextLines = new ArrayList<>();
    private boolean digitalTextEditMode;
    private int currentDigitalTextLine;

    public DisplayScreen(DisplayMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 1;
        imageHeight = 1;
    }

    @Override
    protected void init() {
        super.init();
        display = minecraft.player == null ? null : menu.getDisplay(minecraft.player);
        if (display instanceof DigitalSignControllerBlockEntity controller) {
            initDigitalController(controller);
        } else if (display instanceof MessageBoardControllerBlockEntity controller) {
            initMessageController(controller);
        } else if (display instanceof MessageBoardBlockEntity board) {
            initMessageBoard(board);
        }
    }

    private void initMessageBoard(MessageBoardBlockEntity board) {
        int x = width / 2 - 100;
        int y = height / 2 - 40;
        addLineFields(board, x, y);
    }

    private void initMessageController(MessageBoardControllerBlockEntity controller) {
        int startY = Math.max(18, height / 2 - 120);
        int controlsX = Math.max(4, width / 2 - 206);
        int previewX = width / 2 + 8;
        addLineFields(controller, controlsX, startY);
        editingPage = controller.getRotationPageCount() == 0 ? -1 : controller.getRotationIndex();

        modeButton = addRenderableWidget(Button.builder(modeLabel(controller), b -> {
            saveLines(controller);
            controller.setMode(controller.getMode().next());
            b.setMessage(modeLabel(controller));
        }).bounds(controlsX, startY + 60, 200, 20).build());
        addRenderableWidget(Button.builder(Component.literal("<"), b -> selectMessagePage(controller, -1))
                .bounds(controlsX, startY + 82, 30, 20).build());
        pageButton = addRenderableWidget(Button.builder(pageLabel(controller), b -> {})
                .bounds(controlsX + 34, startY + 82, 132, 20).build());
        pageButton.active = false;
        addRenderableWidget(Button.builder(Component.literal(">"), b -> selectMessagePage(controller, 1))
                .bounds(controlsX + 170, startY + 82, 30, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Add"), b -> {
            saveMessageController(controller, false);
            if (controller.addCurrentPage()) editingPage = controller.getRotationIndex();
            refreshMessageControls(controller);
        }).bounds(controlsX, startY + 104, 64, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Save page"), b -> {
            saveMessageController(controller, true);
            refreshMessageControls(controller);
            sync(controller);
        }).bounds(controlsX + 68, startY + 104, 64, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Delete"), b -> {
            controller.removeCurrentPage();
            editingPage = controller.getRotationIndex();
            reloadMessage(controller);
            sync(controller);
        }).bounds(controlsX + 136, startY + 104, 64, 20).build());

        scheduleButton = addRenderableWidget(Button.builder(scheduleLabel(controller), b -> {
            commitMessageSchedule(controller);
            controller.setScheduleMode(controller.getScheduleMode().next());
            refreshSchedule(controller);
            sync(controller);
        }).bounds(controlsX, startY + 126, 200, 20).build());
        interval = edit(controlsX, startY + 159, 200, 18, 7,
                Integer.toString(controller.getScheduleIntervalAmount()), "Interval amount");
        gameTimes = edit(controlsX, startY + 190, 200, 18, 128,
                controller.getScheduleTimesText(), "HH:MM, comma-separated");

        fontButton = addRenderableWidget(Button.builder(fontLabel(controller), b -> {
            controller.setFontStyle(controller.getFontStyle().next());
            refreshMessageControls(controller);
        }).bounds(previewX, startY + 90, 200, 20).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> {
            controller.setTextScale(controller.getTextScale() - .1F);
            refreshMessageControls(controller);
        }).bounds(previewX, startY + 112, 28, 20).build());
        sizeButton = addRenderableWidget(Button.builder(sizeLabel(controller), b -> {})
                .bounds(previewX + 32, startY + 112, 136, 20).build());
        sizeButton.active = false;
        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            controller.setTextScale(controller.getTextScale() + .1F);
            refreshMessageControls(controller);
        }).bounds(previewX + 172, startY + 112, 28, 20).build());
        brightnessButton = addRenderableWidget(Button.builder(brightnessLabel(controller), b -> {
            float next = controller.getBrightness() >= 1F ? .1F : controller.getBrightness() + .1F;
            controller.setBrightness(next);
            refreshMessageControls(controller);
        }).bounds(previewX, startY + 134, 98, 20).build());
        color = edit(previewX + 102, startY + 134, 98, 20, 6,
                String.format("%06X", controller.getColor()), "RGB hex");
        refreshSchedule(controller);
    }

    private void initDigitalController(DigitalSignControllerBlockEntity controller) {
        int x = 8;
        int buttonWidth = Math.min(188, Math.max(120, width / 4 - 16));
        int editPanelX = x + buttonWidth + 8;
        int editPanelWidth = Math.max(96, width - editPanelX - 136);
        int count = controller.getRotationPageCount();
        editingPage = count == 0 ? -1 : Math.max(0, Math.min(count - 1, controller.getRotationIndex()));
        editingSign = editingPage >= 0 ? controller.getPageSignId(editingPage) : controller.getSelectedSign();
        loadDigitalTextLines(controller);

        signList = addRenderableWidget(new SignImageListWidget(width - 128, 18, 112, height - 68,
                sign -> selectDigitalSign(controller, sign.getID())));
        digitalSearch = edit(width - 128, height - 40, 112, 20, 64, "", "Search...");
        digitalSearch.setResponder(signList::filter);
        addRenderableWidget(Button.builder(Component.literal(addDigitalLabel(controller)), b -> {
            if (editingSign != null) {
                saveDigitalPage(controller);
                if (controller.addRotationSign(editingSign)) {
                    editingPage = controller.getRotationPageCount() - 1;
                    controller.saveRotationPage(editingPage, editingSign, new ArrayList<>(editingTextLines));
                    reloadDigital(controller);
                }
                refreshDigitalControls(controller);
                sync(controller);
            }
        }).bounds(x, 8, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("<"), b -> selectDigitalPage(controller, -1))
                .bounds(x, 30, 30, 20).build());
        pageButton = addRenderableWidget(Button.builder(Component.literal(digitalPageLabel(controller)), b -> {})
                .bounds(x + 34, 30, buttonWidth - 68, 20).build());
        pageButton.active = false;
        addRenderableWidget(Button.builder(Component.literal(">"), b -> selectDigitalPage(controller, 1))
                .bounds(x + buttonWidth - 30, 30, 30, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Save page"), b -> {
            saveDigitalPage(controller);
            sync(controller);
        }).bounds(x, 52, buttonWidth / 2 - 2, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Delete page"), b -> {
            controller.removeRotationSign(editingPage);
            editingPage = controller.getRotationIndex();
            reloadDigital(controller);
            sync(controller);
        }).bounds(x + buttonWidth / 2 + 2, 52, buttonWidth / 2 - 2, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Clear pages"), b -> {
            controller.clearRotationSigns();
            editingPage = -1;
            editingSign = controller.getSelectedSign();
            signTime.setValue("");
            loadDigitalTextLines(controller);
            refreshDigitalControls(controller);
            sync(controller);
        }).bounds(x, 74, buttonWidth, 20).build());
        scheduleButton = addRenderableWidget(Button.builder(digitalScheduleLabel(controller), b -> {
            commitDigitalSchedule(controller);
            controller.setScheduleMode(controller.getScheduleMode().next());
            refreshDigitalControls(controller);
            sync(controller);
        }).bounds(x, 96, buttonWidth, 20).build());
        interval = edit(x, 130, buttonWidth, 18, 7,
                Integer.toString(controller.getScheduleIntervalAmount()), "Interval amount");
        signTime = edit(x, 160, buttonWidth, 18, 5,
                controller.getRotationPageTimeText(editingPage), "HH:MM");
        addRenderableWidget(Button.builder(Component.literal("Save selected time"), b -> {
            commitDigitalSchedule(controller);
            sync(controller);
        }).bounds(x, 182, buttonWidth, 20).build());
        digitalTextEditorButton = addRenderableWidget(Button.builder(Component.literal("Text Editor (T)"),
                b -> toggleDigitalTextEditor(controller))
                .bounds(editPanelX + Math.max(0, (editPanelWidth - Math.min(180, editPanelWidth)) / 2),
                        8, Math.min(180, editPanelWidth), 20).build());
        refreshDigitalControls(controller);
    }

    private void addLineFields(MessageBoardBlockEntity board, int x, int y) {
        for (int i = 0; i < lines.length; i++) {
            lines[i] = edit(x, y + i * 20, 200, 18, MessageBoardBlockEntity.MAX_LINE_LENGTH,
                    board.getLine(i), "Line " + (i + 1));
        }
    }

    private EditBox edit(int x, int y, int width, int height, int maxLength, String value, String hint) {
        EditBox box = new EditBox(font, x, y, width, height, Component.literal(hint));
        box.setMaxLength(maxLength);
        box.setValue(value == null ? "" : value);
        box.setHint(Component.literal(hint));
        return addRenderableWidget(box);
    }

    private void selectMessagePage(MessageBoardControllerBlockEntity controller, int delta) {
        saveMessageController(controller, true);
        if (controller.getRotationPageCount() > 0) {
            editingPage = Math.floorMod(editingPage + delta, controller.getRotationPageCount());
            controller.selectRotationPage(editingPage);
            reloadMessage(controller);
        }
        sync(controller);
    }

    private void saveMessageController(MessageBoardControllerBlockEntity controller, boolean updatePage) {
        saveLines(controller);
        if (color != null) {
            try {
                controller.setColor(Integer.parseInt(color.getValue().trim(), 16));
            } catch (NumberFormatException ignored) {
            }
        }
        commitMessageSchedule(controller);
        if (updatePage && editingPage >= 0) controller.updateCurrentPage();
    }

    private void reloadMessage(MessageBoardControllerBlockEntity controller) {
        for (int i = 0; i < lines.length; i++) lines[i].setValue(controller.getLine(i));
        color.setValue(String.format("%06X", controller.getColor()));
        refreshMessageControls(controller);
    }

    private void refreshMessageControls(MessageBoardControllerBlockEntity controller) {
        editingPage = controller.getRotationIndex();
        if (pageButton != null) pageButton.setMessage(pageLabel(controller));
        if (modeButton != null) modeButton.setMessage(modeLabel(controller));
        if (fontButton != null) fontButton.setMessage(fontLabel(controller));
        if (sizeButton != null) sizeButton.setMessage(sizeLabel(controller));
        if (brightnessButton != null) brightnessButton.setMessage(brightnessLabel(controller));
    }

    private void commitMessageSchedule(MessageBoardControllerBlockEntity controller) {
        if (interval != null) controller.setScheduleIntervalAmount(
                positiveInt(interval.getValue(), controller.getScheduleIntervalAmount()));
        if (gameTimes != null) controller.setScheduleTimes(gameTimes.getValue());
    }

    private void refreshSchedule(MessageBoardControllerBlockEntity controller) {
        scheduleButton.setMessage(scheduleLabel(controller));
        interval.setEditable(controller.getScheduleMode().isInterval());
        gameTimes.setEditable(controller.getScheduleMode() == DisplaySchedule.Mode.GAME_TIMES);
    }

    private void selectDigitalPage(DigitalSignControllerBlockEntity controller, int delta) {
        saveDigitalPage(controller);
        int count = controller.getRotationPageCount();
        if (count > 0) {
            editingPage = Math.floorMod(editingPage + delta, count);
            controller.selectRotationSign(editingPage);
            reloadDigital(controller);
        }
        sync(controller);
    }

    private void saveDigitalPage(DigitalSignControllerBlockEntity controller) {
        if (editingPage >= 0 && editingSign != null) {
            controller.saveRotationPage(editingPage, editingSign, new ArrayList<>(editingTextLines));
            commitDigitalTime(controller);
        }
        commitDigitalSchedule(controller);
    }

    private void reloadDigital(DigitalSignControllerBlockEntity controller) {
        editingPage = controller.getRotationIndex();
        editingSign = editingPage >= 0 && editingPage < controller.getRotationPageCount()
                ? controller.getPageSignId(editingPage) : controller.getSelectedSign();
        signTime.setValue(controller.getRotationPageTimeText(editingPage));
        loadDigitalTextLines(controller);
        refreshDigitalControls(controller);
    }

    private void commitDigitalSchedule(DigitalSignControllerBlockEntity controller) {
        controller.setScheduleIntervalAmount(positiveInt(interval.getValue(), controller.getScheduleIntervalAmount()));
        commitDigitalTime(controller);
    }

    private void commitDigitalTime(DigitalSignControllerBlockEntity controller) {
        if (editingPage >= 0 && signTime != null
                && controller.setRotationPageTime(editingPage, signTime.getValue())) {
            signTime.setValue(controller.getRotationPageTimeText(editingPage));
        }
    }

    private void refreshDigitalControls(DigitalSignControllerBlockEntity controller) {
        if (pageButton != null) pageButton.setMessage(Component.literal(digitalPageLabel(controller)));
        if (scheduleButton != null) scheduleButton.setMessage(digitalScheduleLabel(controller));
        boolean follower = controller.isSyncFollower();
        if (scheduleButton != null) scheduleButton.active = !follower && !digitalTextEditMode;
        if (interval != null) interval.setEditable(!follower && controller.getScheduleMode().isInterval()
                && !digitalTextEditMode);
        if (signTime != null) signTime.setEditable(!follower && editingPage >= 0 && !digitalTextEditMode);
        Sign sign = getEditingDigitalSign();
        boolean hasText = sign != null && !sign.getTextLines().isEmpty();
        if (!hasText) digitalTextEditMode = false;
        if (digitalTextEditorButton != null) {
            digitalTextEditorButton.active = hasText;
            digitalTextEditorButton.setMessage(Component.literal(
                    digitalTextEditMode ? "Finish Editing" : "Text Editor (T)"));
        }
        if (digitalSearch != null) digitalSearch.setEditable(!digitalTextEditMode);
        if (signList != null) signList.active = !digitalTextEditMode;
    }

    private Sign getEditingDigitalSign() {
        return editingSign == null ? null : ModRealisticTrafficControl.signRepo.getSignByID(editingSign);
    }

    private void loadDigitalTextLines(DigitalSignControllerBlockEntity controller) {
        editingTextLines.clear();
        Sign sign = getEditingDigitalSign();
        if (sign == null) return;
        for (int i = 0; i < sign.getTextLines().size(); i++) {
            editingTextLines.add(editingPage >= 0 ? controller.getPageTextLine(editingPage, i) : "");
        }
        if (currentDigitalTextLine >= editingTextLines.size()) currentDigitalTextLine = 0;
    }

    private String getEditingDigitalText(int index) {
        return index >= 0 && index < editingTextLines.size() && editingTextLines.get(index) != null
                ? editingTextLines.get(index) : "";
    }

    private void setEditingDigitalText(int index, String value) {
        while (editingTextLines.size() <= index) editingTextLines.add("");
        editingTextLines.set(index, value == null ? "" : value);
    }

    private void selectDigitalSign(DigitalSignControllerBlockEntity controller, UUID id) {
        saveDigitalPage(controller);
        editingSign = id;
        digitalTextEditMode = false;
        currentDigitalTextLine = 0;
        if (editingPage >= 0) controller.updateRotationSign(editingPage, id);
        loadDigitalTextLines(controller);
        if (signTime != null) signTime.setValue(controller.getRotationPageTimeText(editingPage));
        refreshDigitalControls(controller);
    }

    private void toggleDigitalTextEditor(DigitalSignControllerBlockEntity controller) {
        Sign sign = getEditingDigitalSign();
        if (sign == null || sign.getTextLines().isEmpty()) {
            digitalTextEditMode = false;
        } else {
            digitalTextEditMode = !digitalTextEditMode;
            if (!digitalTextEditMode) currentDigitalTextLine = 0;
        }
        refreshDigitalControls(controller);
    }

    private void saveLines(MessageBoardBlockEntity board) {
        for (int i = 0; i < lines.length; i++) board.setLine(i, lines[i].getValue());
    }

    private static Component modeLabel(MessageBoardBlockEntity board) {
        return Component.literal("Display: " + board.getMode().name().replace('_', ' '));
    }
    private static Component fontLabel(MessageBoardBlockEntity board) {
        return Component.literal("Font: " + board.getFontStyle().name().replace('_', ' '));
    }
    private static Component sizeLabel(MessageBoardBlockEntity board) {
        return Component.literal("Text size: " + Math.round(board.getTextScale() * 100) + "%");
    }
    private static Component brightnessLabel(MessageBoardBlockEntity board) {
        return Component.literal("Brightness: " + Math.round(board.getBrightness() * 100) + "%");
    }
    private static Component pageLabel(MessageBoardControllerBlockEntity controller) {
        int count = controller.getRotationPageCount();
        return Component.literal(count == 0 ? "Page 0 / 0"
                : "Page " + (controller.getRotationIndex() + 1) + " / " + count);
    }
    private static Component scheduleLabel(MessageBoardControllerBlockEntity controller) {
        return Component.literal("Timing: " + scheduleText(controller.getScheduleMode(),
                controller.getScheduleIntervalAmount()));
    }
    private static Component digitalScheduleLabel(DigitalSignControllerBlockEntity controller) {
        return Component.literal(controller.getScheduleMode() == DisplaySchedule.Mode.GAME_TIMES
                ? "Timing: Each sign's game time"
                : "Timing: " + scheduleText(controller.getScheduleMode(), controller.getScheduleIntervalAmount()));
    }
    private static String scheduleText(DisplaySchedule.Mode mode, int amount) {
        return switch (mode) {
            case MANUAL -> "Manual";
            case EVERY_SECOND -> "Every " + amount + " second" + (amount == 1 ? "" : "s");
            case EVERY_MINUTE -> "Every " + amount + " minute" + (amount == 1 ? "" : "s");
            case EVERY_HOUR -> "Every " + amount + " hour" + (amount == 1 ? "" : "s");
            case GAME_TIMES -> "At game times";
        };
    }
    private static String addDigitalLabel(DigitalSignControllerBlockEntity controller) {
        return "Add page (" + controller.getRotationPageCount() + "/"
                + DigitalSignControllerBlockEntity.MAX_ROTATION_SIGNS + ")";
    }
    private static String digitalPageLabel(DigitalSignControllerBlockEntity controller) {
        int count = controller.getRotationPageCount();
        return count == 0 ? "Page 0 / 0" : "Page " + (controller.getRotationIndex() + 1) + " / " + count;
    }
    private static int positiveInt(String value, int fallback) {
        try {
            return Math.max(1, Integer.parseInt(value.trim()));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
    private static void sync(com.gamearoosdevelopment.realistictrafficcontrol.tileentity.SyncableBlockEntity entity) {
        entity.performClientToServerSync();
    }

    @Override
    public void onClose() {
        if (display instanceof DigitalSignControllerBlockEntity controller) {
            saveDigitalPage(controller);
            sync(controller);
        } else if (display instanceof MessageBoardControllerBlockEntity controller) {
            saveMessageController(controller, true);
            sync(controller);
        } else if (display instanceof MessageBoardBlockEntity board) {
            saveLines(board);
            sync(board);
        }
        super.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        if (display instanceof MessageBoardControllerBlockEntity controller) {
            renderMessageController(graphics, controller);
        } else if (display instanceof MessageBoardBlockEntity) {
            graphics.drawCenteredString(font, "Message Board", width / 2, height / 2 - 78, 0xFFFFA000);
            graphics.drawCenteredString(font, "Use the controller or OpenComputers for remote control.",
                    width / 2, height / 2 + 42, 0xFFAAAAAA);
        } else if (display instanceof DigitalSignControllerBlockEntity controller) {
            renderDigitalController(graphics, controller);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderMessageController(GuiGraphics graphics, MessageBoardControllerBlockEntity controller) {
        int startY = Math.max(18, height / 2 - 120);
        int previewX = width / 2 + 8;
        graphics.drawCenteredString(font, "Message / Arrow Board Controller", width / 2, 5, 0xFFFFA000);
        graphics.drawCenteredString(font, "Linked boards: " + controller.getLinkedBoards().size(),
                width / 2, startY + 211, 0xFFFFFFFF);
        graphics.drawCenteredString(font, "Use tuner on controller, then board. OpenComputers can also control it.", width / 2,
                startY + 223, 0xFFAAAAAA);
        graphics.drawCenteredString(font, "Interval amount", width / 2 - 106, startY + 149, 0xFFAAAAAA);
        graphics.drawCenteredString(font, "Game times (HH:MM, comma-separated)", width / 2 - 106,
                startY + 180, 0xFFAAAAAA);
        int left = previewX;
        int top = startY;
        int right = previewX + 200;
        int bottom = startY + 86;
        graphics.fill(left, top, right, bottom, 0xFF181A1C);
        graphics.fill(left + 5, top + 5, right - 5, bottom - 5, 0xFF050606);
        int lit = litColor(controller);
        if (controller.getMode() == MessageBoardBlockEntity.DisplayMode.TEXT) {
            for (int i = 0; i < lines.length; i++) {
                String text = styled(lines[i].getValue(), controller.getFontStyle());
                graphics.drawCenteredString(font, text, (left + right) / 2, top + 14 + i * 20, lit);
            }
        } else if (controller.getMode() != MessageBoardBlockEntity.DisplayMode.OFF) {
            String text = controller.getMode() == MessageBoardBlockEntity.DisplayMode.ARROW_LEFT ? "\u2190"
                    : controller.getMode() == MessageBoardBlockEntity.DisplayMode.ARROW_RIGHT ? "\u2192" : "CAUTION";
            graphics.drawCenteredString(font, text, (left + right) / 2, (top + bottom) / 2 - 4, lit);
        }
    }

    private void renderDigitalController(GuiGraphics graphics, DigitalSignControllerBlockEntity controller) {
        int controlsWidth = Math.min(188, Math.max(120, width / 4 - 16));
        int controlsCenter = 8 + controlsWidth / 2;
        int editPanelX = 16 + controlsWidth;
        int editPanelWidth = Math.max(96, width - editPanelX - 136);
        int editCenter = editPanelX + editPanelWidth / 2;
        graphics.drawCenteredString(font, "Interval amount", controlsCenter, 120, 0xFFAAAAAA);
        graphics.drawCenteredString(font, controller.isSyncFollower()
                ? "Page time locked to master" : "Page time (HH:MM)", controlsCenter, 150,
                controller.isSyncFollower() ? 0xFFFF5555 : 0xFFAAAAAA);

        Sign selected = getEditingDigitalSign();
        if (selected != null) {
            graphics.drawCenteredString(font, selected.getName(), editCenter, 30, 0xFFFFA000);
            int previewSize = Math.max(16, Math.min(editPanelWidth - 16, height - 104));
            int previewX = editCenter - previewSize / 2;
            int previewY = 42;
            graphics.blit(selected.getFrontImageResourceLocation(), previewX, previewY, previewSize, previewSize,
                    0, 0, 16, 16, 16, 16);
            renderDigitalPreviewText(graphics, selected, previewX, previewY, previewSize);
        }

        graphics.drawCenteredString(font,
                "Signs: " + controller.getLinkedSigns().size() + "  |  Synced: "
                        + controller.getSyncedControllers().size()
                        + (controller.isSyncFollower() ? "  |  Following master" : ""),
                editCenter, height - 38, 0xFFFFFFFF);
        graphics.drawCenteredString(font, controller.isSyncFollower()
                ? "Edit timing on the master controller."
                : "Tuner: controller to sign, or controller to controller for timing sync.",
                editCenter, height - 25, controller.isSyncFollower() ? 0xFFFF5555 : 0xFFAAAAAA);
    }

    private void renderDigitalPreviewText(GuiGraphics graphics, Sign sign, int previewX, int previewY,
            int previewSize) {
        if (sign.getTextLines().isEmpty()) return;
        graphics.pose().pushPose();
        graphics.pose().translate(previewX, previewY, 100);
        float fullScale = (float) (((double) previewSize / font.lineHeight) / 16D);
        graphics.pose().scale(fullScale, fullScale, 1);
        for (int i = 0; i < sign.getTextLines().size(); i++) {
            Sign.TextLine textLine = sign.getTextLines().get(i);
            graphics.pose().pushPose();
            graphics.pose().translate(textLine.getX() * font.lineHeight, textLine.getY() * font.lineHeight, 0);
            graphics.pose().scale((float) textLine.getXScale(), (float) textLine.getYScale(), 1);
            if (textLine.getvAlign() == SignVerticalAlignment.Center)
                graphics.pose().translate(0, -font.lineHeight / 2F, 0);
            else if (textLine.getvAlign() == SignVerticalAlignment.Bottom)
                graphics.pose().translate(0, -font.lineHeight, 0);
            double availableWidth = textLine.getScaleAdjustedWidth() * font.lineHeight;
            if (textLine.gethAlign() == SignHorizontalAlignment.Center)
                graphics.pose().translate(-availableWidth / 2, 0, 0);
            else if (textLine.gethAlign() == SignHorizontalAlignment.Right)
                graphics.pose().translate(-availableWidth, 0, 0);
            if (digitalTextEditMode) {
                graphics.fill(0, 0, (int) availableWidth, font.lineHeight,
                        currentDigitalTextLine == i ? 0x6600FF00 : 0x66FF0000);
                graphics.pose().pushPose();
                graphics.pose().scale(.5F, .5F, 1);
                graphics.drawString(font, textLine.getLabel(), 0, -font.lineHeight,
                        currentDigitalTextLine == i ? 0xFF00FF00 : 0xFFFF0000);
                graphics.pose().popPose();
            }
            String text = getEditingDigitalText(i);
            int textWidth = font.width(text);
            if (textWidth > 0) {
                float widthScale = (float) Math.min(1D, availableWidth / textWidth);
                graphics.pose().scale(widthScale, 1, 1);
                int textX = 0;
                if (textLine.gethAlign() == SignHorizontalAlignment.Center && widthScale == 1F)
                    textX = (int) (availableWidth / 2) - textWidth / 2;
                else if (textLine.gethAlign() == SignHorizontalAlignment.Right)
                    textX = (int) availableWidth - textWidth;
                graphics.drawString(font, text, textX + 1, 1, textLine.getColor());
            }
            graphics.pose().popPose();
        }
        graphics.pose().popPose();
    }

    private static int litColor(MessageBoardBlockEntity board) {
        int color = board.getColor();
        float brightness = board.getBrightness();
        int red = (int) (((color >> 16) & 255) * brightness);
        int green = (int) (((color >> 8) & 255) * brightness);
        int blue = (int) ((color & 255) * brightness);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static String styled(String text, MessageBoardBlockEntity.FontStyle style) {
        return switch (style) {
            case BOLD -> "\u00a7l" + text;
            case ITALIC -> "\u00a7o" + text;
            case BOLD_ITALIC -> "\u00a7l\u00a7o" + text;
            default -> text;
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (display instanceof DigitalSignControllerBlockEntity && digitalTextEditMode
                && selectDigitalTextLine(mouseX, mouseY)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean selectDigitalTextLine(double mouseX, double mouseY) {
        Sign sign = getEditingDigitalSign();
        if (sign == null || sign.getTextLines().isEmpty()) return false;
        int controlsWidth = Math.min(188, Math.max(120, width / 4 - 16));
        int editPanelX = 16 + controlsWidth;
        int editPanelWidth = Math.max(96, width - editPanelX - 136);
        int previewSize = Math.max(16, Math.min(editPanelWidth - 16, height - 104));
        int previewX = editPanelX + editPanelWidth / 2 - previewSize / 2;
        int previewY = 42;
        if (mouseX < previewX || mouseX > previewX + previewSize
                || mouseY < previewY || mouseY > previewY + previewSize) return false;
        double fullScale = ((double) previewSize / font.lineHeight) / 16D;
        double localX = (mouseX - previewX) / fullScale;
        double localY = (mouseY - previewY) / fullScale;
        for (int i = 0; i < sign.getTextLines().size(); i++) {
            Sign.TextLine line = sign.getTextLines().get(i);
            double x = line.getX() * font.lineHeight;
            double y = line.getY() * font.lineHeight;
            double lineWidth = line.getScaleAdjustedWidth() * font.lineHeight * line.getXScale();
            double lineHeight = font.lineHeight * line.getYScale();
            if (line.gethAlign() == SignHorizontalAlignment.Center) x -= lineWidth / 2D;
            else if (line.gethAlign() == SignHorizontalAlignment.Right) x -= lineWidth;
            if (line.getvAlign() == SignVerticalAlignment.Center) y -= lineHeight / 2D;
            else if (line.getvAlign() == SignVerticalAlignment.Bottom) y -= lineHeight;
            if (localX >= x && localX <= x + lineWidth && localY >= y && localY <= y + lineHeight) {
                currentDigitalTextLine = i;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (display instanceof DigitalSignControllerBlockEntity controller) {
            Sign sign = getEditingDigitalSign();
            if (digitalTextEditMode && sign != null && !sign.getTextLines().isEmpty()) {
                if (currentDigitalTextLine < 0 || currentDigitalTextLine >= sign.getTextLines().size())
                    currentDigitalTextLine = 0;
                String text = getEditingDigitalText(currentDigitalTextLine);
                if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && !text.isEmpty()) {
                    setEditingDigitalText(currentDigitalTextLine, text.substring(0, text.length() - 1));
                    return true;
                }
                if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN) {
                    currentDigitalTextLine = (currentDigitalTextLine + 1) % sign.getTextLines().size();
                    return true;
                }
                if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_UP) {
                    currentDigitalTextLine = currentDigitalTextLine <= 0
                            ? sign.getTextLines().size() - 1 : currentDigitalTextLine - 1;
                    return true;
                }
                if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER
                        || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
                    if (currentDigitalTextLine == sign.getTextLines().size() - 1)
                        toggleDigitalTextEditor(controller);
                    else currentDigitalTextLine++;
                    return true;
                }
                if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                    toggleDigitalTextEditor(controller);
                    return true;
                }
            } else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_T
                    && (digitalSearch == null || !digitalSearch.isFocused())
                    && (interval == null || !interval.isFocused())
                    && (signTime == null || !signTime.isFocused())) {
                toggleDigitalTextEditor(controller);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (display instanceof DigitalSignControllerBlockEntity && digitalTextEditMode) {
            Sign sign = getEditingDigitalSign();
            if (sign != null && currentDigitalTextLine >= 0
                    && currentDigitalTextLine < sign.getTextLines().size()) {
                Sign.TextLine line = sign.getTextLines().get(currentDigitalTextLine);
                String text = getEditingDigitalText(currentDigitalTextLine);
                if (text.length() < line.getMaxLength() && !Character.isISOControl(codePoint)) {
                    setEditingDigitalText(currentDigitalTextLine, text + codePoint);
                    return true;
                }
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) { }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) { }
    @Override protected void renderSlot(GuiGraphics graphics, Slot slot) { }
}
