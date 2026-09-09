package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;
import com.gamearoosdevelopment.realistictrafficcontrol.util.DisplaySchedule;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
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
        TEXT, ARROW_LEFT, ARROW_RIGHT, ARROW_BOTH, CAUTION, OFF;
        public DisplayMode next() { return values()[(ordinal() + 1) % values().length]; }
        public static DisplayMode parse(String value) {
            if (value != null) {
                String normalized = value.trim().toUpperCase().replace(' ', '_');
                if (normalized.equals("ARROW_MERGE_LEFT")) return ARROW_LEFT;
                if (normalized.equals("ARROW_MERGE_RIGHT")) return ARROW_RIGHT;
                if (normalized.equals("ARROW_LEFT_RIGHT") || normalized.equals("ARROW_COMBINED")
                        || normalized.equals("DOUBLE_ARROW") || normalized.equals("ARROW_MERGE_BOTH")) {
                    return ARROW_BOTH;
                }
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
    public static final int MAX_BOARDS = 16;
    public static final int MAX_ROTATION_PAGES = 32;
    private final String[] lines = {"", "", ""};
    private int color = 0xFFA000;
    private float brightness = 1F;
    private float textScale = 1F;
    private FontStyle fontStyle = FontStyle.REGULAR;
    private DisplayMode mode = DisplayMode.TEXT;
    private final ArrayList<BlockPos> boards = new ArrayList<>();
    private final ArrayList<CompoundTag> pages = new ArrayList<>();
    private final DisplaySchedule schedule = new DisplaySchedule();
    private int rotationIndex = -1;
    private boolean applyingRemote;

    public MessageBoardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MESSAGE_BOARD.get(), pos, state);
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
    public void setTextScale(float value) {
        textScale = Math.max(.5F, Math.min(1.5F, Math.round(value * 10F) / 10F));
        changed();
    }
    public FontStyle getFontStyle() { return fontStyle; }
    public void setFontStyle(FontStyle value) { fontStyle = value == null ? FontStyle.REGULAR : value; changed(); }
    public DisplayMode getMode() { return mode; }
    public void setMode(DisplayMode value) { mode = value == null ? DisplayMode.TEXT : value; changed(); }

    public List<BlockPos> getLinkedBoards() { return Collections.unmodifiableList(boards); }
    public int getRotationPageCount() { return pages.size(); }
    public int getRotationIndex() { return rotationIndex; }
    public DisplaySchedule.Mode getScheduleMode() { return schedule.getMode(); }
    public int getScheduleIntervalAmount() { return schedule.getIntervalAmount(); }
    public String getScheduleTimesText() { return schedule.getGameTimesText(); }
    public void setScheduleMode(DisplaySchedule.Mode value) { schedule.setMode(value); changed(); }
    public void setScheduleIntervalAmount(int value) { schedule.setIntervalAmount(value); changed(); }
    public void setScheduleTimes(String value) { schedule.setGameTimesFromText(value); changed(); }

    public boolean linkBoard(BlockPos pos) {
        if (level == null || pos == null || pos.equals(worldPosition) || boards.contains(pos)
                || boards.size() >= MAX_BOARDS
                || !(level.getBlockEntity(pos) instanceof MessageBoardBlockEntity board)) return false;
        boards.add(pos);
        applyTo(board);
        changed();
        return true;
    }
    public boolean unlinkBoard(BlockPos pos) {
        boolean result = boards.remove(pos);
        if (result) changed();
        return result;
    }
    public boolean addCurrentPage() {
        if (pages.size() >= MAX_ROTATION_PAGES) return false;
        pages.add(snapshot());
        rotationIndex = pages.size() - 1;
        changed();
        return true;
    }
    public boolean updateCurrentPage() {
        if (rotationIndex < 0 || rotationIndex >= pages.size()) return false;
        pages.set(rotationIndex, snapshot());
        changed();
        return true;
    }
    public boolean removeCurrentPage() {
        if (rotationIndex < 0 || rotationIndex >= pages.size()) return false;
        pages.remove(rotationIndex);
        rotationIndex = pages.isEmpty() ? -1 : Math.min(rotationIndex, pages.size() - 1);
        if (rotationIndex >= 0) applyPage(rotationIndex);
        changed();
        return true;
    }
    public boolean selectRotationPage(int index) {
        if (pages.isEmpty()) return false;
        rotationIndex = Math.floorMod(index, pages.size());
        applyPage(rotationIndex);
        return true;
    }
    public void clearRotationPages() { pages.clear(); rotationIndex = -1; changed(); }

    protected void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        if (level == null || level.isClientSide || applyingRemote) return;
        for (BlockPos pos : new ArrayList<>(boards)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MessageBoardBlockEntity board && !pos.equals(worldPosition)) applyTo(board);
            else boards.remove(pos);
        }
    }

    private void applyTo(MessageBoardBlockEntity board) {
        board.applyingRemote = true;
        try {
            for (int i = 0; i < MAX_LINES; i++) board.setLine(i, getLine(i));
            board.setMode(getMode());
            board.setColor(getColor());
            board.setBrightness(getBrightness());
            board.setTextScale(getTextScale());
            board.setFontStyle(getFontStyle());
        } finally {
            board.applyingRemote = false;
        }
    }

    private CompoundTag snapshot() {
        CompoundTag tag = new CompoundTag();
        writeData(tag);
        return tag;
    }
    private void applyPage(int index) {
        readData(pages.get(index));
        changed();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MessageBoardBlockEntity be) {
        if (be.schedule.update(level) && be.pages.size() > 1)
            be.selectRotationPage((be.rotationIndex + 1 + be.pages.size()) % be.pages.size());
    }

    protected void readData(CompoundTag tag) {
        for (int i = 0; i < MAX_LINES; i++) lines[i] = tag.getString("line" + i);
        if (tag.contains("color")) color = tag.getInt("color");
        if (tag.contains("brightness")) brightness = Math.max(.1F, Math.min(1F, tag.getFloat("brightness")));
        if (tag.contains("textScale")) {
            textScale = Math.max(.5F, Math.min(1.5F, Math.round(tag.getFloat("textScale") * 10F) / 10F));
        }
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

    private void readControllerData(CompoundTag tag) {
        boards.clear();
        for (long value : tag.getLongArray("boards")) boards.add(BlockPos.of(value));
        pages.clear();
        ListTag list = tag.getList("pages", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(MAX_ROTATION_PAGES, list.size()); i++) pages.add(list.getCompound(i).copy());
        rotationIndex = tag.getInt("rotationIndex");
        if (rotationIndex < 0 || rotationIndex >= pages.size()) rotationIndex = pages.isEmpty() ? -1 : 0;
        schedule.load(tag, "schedule");
    }

    private void writeControllerData(CompoundTag tag) {
        tag.putLongArray("boards", boards.stream().mapToLong(BlockPos::asLong).toArray());
        ListTag list = new ListTag();
        pages.forEach(page -> list.add(page.copy()));
        tag.put("pages", list);
        tag.putInt("rotationIndex", rotationIndex);
        schedule.save(tag, "schedule");
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        writeData(tag);
        writeControllerData(tag);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        readData(tag);
        readControllerData(tag);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider provider) { return saveWithoutMetadata(provider); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        readData(tag);
        readControllerData(tag);
    }
    @Override public CompoundTag getClientToServerUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }
    @Override public void handleClientToServerUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        readData(tag);
        readControllerData(tag);
        changed();
    }
}
