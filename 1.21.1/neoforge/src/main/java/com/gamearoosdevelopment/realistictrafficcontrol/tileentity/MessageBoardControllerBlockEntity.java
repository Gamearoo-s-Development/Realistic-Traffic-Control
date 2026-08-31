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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class MessageBoardControllerBlockEntity extends MessageBoardBlockEntity {
    public static final int MAX_BOARDS = 16;
    public static final int MAX_ROTATION_PAGES = 32;
    private final ArrayList<BlockPos> boards = new ArrayList<>();
    private final ArrayList<CompoundTag> pages = new ArrayList<>();
    private final DisplaySchedule schedule = new DisplaySchedule();
    private int rotationIndex = -1;

    public MessageBoardControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MESSAGE_BOARD_CONTROLLER.get(), pos, state);
    }
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
        if (level == null || pos == null || boards.contains(pos) || boards.size() >= MAX_BOARDS
                || !(level.getBlockEntity(pos) instanceof MessageBoardBlockEntity board)
                || board instanceof MessageBoardControllerBlockEntity) return false;
        boards.add(pos); applyTo(board); changed(); return true;
    }
    public boolean unlinkBoard(BlockPos pos) { boolean result = boards.remove(pos); if (result) changed(); return result; }
    public boolean addCurrentPage() {
        if (pages.size() >= MAX_ROTATION_PAGES) return false;
        pages.add(snapshot()); rotationIndex = pages.size() - 1; changed(); return true;
    }
    public boolean updateCurrentPage() {
        if (rotationIndex < 0 || rotationIndex >= pages.size()) return false;
        pages.set(rotationIndex, snapshot()); changed(); return true;
    }
    public boolean removeCurrentPage() {
        if (rotationIndex < 0 || rotationIndex >= pages.size()) return false;
        pages.remove(rotationIndex);
        rotationIndex = pages.isEmpty() ? -1 : Math.min(rotationIndex, pages.size() - 1);
        if (rotationIndex >= 0) applyPage(rotationIndex);
        changed(); return true;
    }
    public boolean selectRotationPage(int index) {
        if (pages.isEmpty()) return false;
        rotationIndex = Math.floorMod(index, pages.size()); applyPage(rotationIndex); return true;
    }
    public void clearRotationPages() { pages.clear(); rotationIndex = -1; changed(); }

    @Override protected void changed() {
        super.changed();
        if (level == null || level.isClientSide) return;
        for (BlockPos pos : new ArrayList<>(boards)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MessageBoardBlockEntity board && !(board instanceof MessageBoardControllerBlockEntity))
                applyTo(board);
            else boards.remove(pos);
        }
    }
    private void applyTo(MessageBoardBlockEntity board) {
        for (int i = 0; i < MAX_LINES; i++) board.setLine(i, getLine(i));
        board.setMode(getMode()); board.setColor(getColor()); board.setBrightness(getBrightness());
        board.setTextScale(getTextScale()); board.setFontStyle(getFontStyle());
    }
    private CompoundTag snapshot() { CompoundTag tag = new CompoundTag(); writeData(tag); return tag; }
    private void applyPage(int index) { readData(pages.get(index)); changed(); }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MessageBoardControllerBlockEntity be) {
        if (be.schedule.update(level) && be.pages.size() > 1)
            be.selectRotationPage((be.rotationIndex + 1 + be.pages.size()) % be.pages.size());
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putLongArray("boards", boards.stream().mapToLong(BlockPos::asLong).toArray());
        ListTag list = new ListTag(); pages.forEach(page -> list.add(page.copy()));
        tag.put("pages", list); tag.putInt("rotationIndex", rotationIndex); schedule.save(tag, "schedule");
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        readControllerData(tag);
    }
    private void readControllerData(CompoundTag tag) {
        boards.clear(); for (long value : tag.getLongArray("boards")) boards.add(BlockPos.of(value));
        pages.clear(); ListTag list = tag.getList("pages", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(MAX_ROTATION_PAGES, list.size()); i++) pages.add(list.getCompound(i).copy());
        rotationIndex = tag.getInt("rotationIndex");
        if (rotationIndex < 0 || rotationIndex >= pages.size()) rotationIndex = pages.isEmpty() ? -1 : 0;
        schedule.load(tag, "schedule");
    }
    @Override public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        super.handleUpdateTag(tag, provider); readControllerData(tag);
    }
    @Override public void handleClientToServerUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        readData(tag); readControllerData(tag); changed();
    }
}
