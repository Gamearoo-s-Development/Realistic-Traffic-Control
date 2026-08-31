package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;

public class MessageBoardBlockEntity extends SyncableBlockEntity {
    public enum FontStyle {
        REGULAR, BOLD, ITALIC, BOLD_ITALIC;
        public FontStyle next() { return values()[(ordinal() + 1) % values().length]; }
        public static FontStyle parse(String value) {
            if (value != null) {
                try {
                    return valueOf(value.trim().toUpperCase().replace(' ', '_'));
                } catch (IllegalArgumentException ignored) {
                }
            }
            return REGULAR;
        }
    }
    public enum DisplayMode {
        TEXT, ARROW_LEFT, ARROW_RIGHT, CAUTION, OFF;
        public DisplayMode next() { return values()[(ordinal() + 1) % values().length]; }
        public static DisplayMode parse(String value) {
            if (value != null) {
                String normalized = value.trim().toUpperCase();
                if (normalized.equals("ARROW_MERGE_LEFT")) return ARROW_LEFT;
                if (normalized.equals("ARROW_MERGE_RIGHT")) return ARROW_RIGHT;
                try {
                    return valueOf(normalized);
                } catch (IllegalArgumentException ignored) {
                }
            }
            return TEXT;
        }
    }

    public static final int MAX_LINES = 3;
    public static final int MAX_LINE_LENGTH = 32;
    private final String[] lines = {"", "", ""};
    private int color = 0xFFA000;
    private float brightness = 1F;
    private float textScale = 1F;
    private FontStyle fontStyle = FontStyle.REGULAR;
    private DisplayMode mode = DisplayMode.TEXT;

    public MessageBoardBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.MESSAGE_BOARD.get(), pos, state);
    }
    protected MessageBoardBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,
            BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public String getLine(int i) { return i >= 0 && i < MAX_LINES ? lines[i] : ""; }
    public void setLine(int i, String value) {
        if (i >= 0 && i < MAX_LINES) {
            value = value == null ? "" : value;
            lines[i] = value.substring(0, Math.min(MAX_LINE_LENGTH, value.length()));
            changed();
        }
    }
    public int getColor() { return color; }
    public void setColor(int value) { color = value & 0xFFFFFF; changed(); }
    public float getBrightness() { return brightness; }
    public void setBrightness(float value) { brightness = Math.max(.1F, Math.min(1F, value)); changed(); }
    public float getTextScale() { return textScale; }
    public void setTextScale(float value) { textScale = Math.max(.5F, Math.min(1.5F, value)); changed(); }
    public FontStyle getFontStyle() { return fontStyle; }
    public void setFontStyle(FontStyle value) { fontStyle = value == null ? FontStyle.REGULAR : value; changed(); }
    public DisplayMode getMode() { return mode; }
    public void setMode(DisplayMode value) { mode = value == null ? DisplayMode.TEXT : value; changed(); }

    protected void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    protected void readData(CompoundTag tag) {
        for (int i = 0; i < MAX_LINES; i++) lines[i] = tag.getString("line" + i);
        if (tag.contains("color")) color = tag.getInt("color");
        if (tag.contains("brightness")) brightness = Math.max(.1F, Math.min(1F, tag.getFloat("brightness")));
        if (tag.contains("textScale")) textScale = Math.max(.5F, Math.min(1.5F, tag.getFloat("textScale")));
        fontStyle = FontStyle.parse(tag.getString("fontStyle"));
        mode = DisplayMode.parse(tag.getString("mode"));
    }

    protected void writeData(CompoundTag tag) {
        for (int i = 0; i < MAX_LINES; i++) tag.putString("line" + i, lines[i]);
        tag.putInt("color", color);
        tag.putFloat("brightness", brightness);
        tag.putFloat("textScale", textScale);
        tag.putString("fontStyle", fontStyle.name());
        tag.putString("mode", mode.name());
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        writeData(tag);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        readData(tag);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider provider) { return saveWithoutMetadata(provider); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) { readData(tag); }
    @Override public CompoundTag getClientToServerUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }
    @Override public void handleClientToServerUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        readData(tag);
        changed();
    }
}
