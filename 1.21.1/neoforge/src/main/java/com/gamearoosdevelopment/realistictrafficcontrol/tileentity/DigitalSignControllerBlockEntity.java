package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;
import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.Sign;
import com.gamearoosdevelopment.realistictrafficcontrol.util.DisplaySchedule;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class DigitalSignControllerBlockEntity extends SyncableBlockEntity {
    public static final int MAX_SIGNS = 16;
    public static final int MAX_ROTATION_SIGNS = 32;

    private static final class RotationPage {
        private UUID signId;
        private Integer gameTime;
        private final ArrayList<String> textLines = new ArrayList<>();

        private RotationPage(UUID signId) {
            this.signId = signId == null ? Sign.DEFAULT_BLANK_SIGN : signId;
            ensureTextLineCount(this);
        }

        private RotationPage(CompoundTag tag) {
            signId = tag.hasUUID("id") ? tag.getUUID("id")
                    : tag.hasUUID("signId") ? tag.getUUID("signId")
                    : tag.contains("signId") ? NbtUtils.loadUUID(tag.getCompound("signId"))
                    : Sign.DEFAULT_BLANK_SIGN;
            if (tag.contains("time")) gameTime = DisplaySchedule.normalizeDayTime(tag.getInt("time"));
            else if (tag.contains("gameTime")) gameTime = DisplaySchedule.normalizeDayTime(tag.getInt("gameTime"));
            ListTag lines = tag.getList("textLines", Tag.TAG_STRING);
            for (int i = 0; i < lines.size(); i++) textLines.add(lines.getString(i));
            if (lines.isEmpty()) {
                for (int i = 0; tag.contains("text" + i); i++) textLines.add(tag.getString("text" + i));
            }
            ensureTextLineCount(this);
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("id", signId == null ? Sign.DEFAULT_BLANK_SIGN : signId);
            if (gameTime != null) tag.putInt("time", gameTime);
            ListTag lines = new ListTag();
            for (String line : textLines) lines.add(StringTag.valueOf(line == null ? "" : line));
            tag.put("textLines", lines);
            return tag;
        }
    }

    private final ArrayList<BlockPos> signs = new ArrayList<>();
    private final ArrayList<BlockPos> syncedControllers = new ArrayList<>();
    private final ArrayList<RotationPage> rotationPages = new ArrayList<>();
    private final DisplaySchedule schedule = new DisplaySchedule();
    private UUID selectedSign = Sign.DEFAULT_BLANK_SIGN;
    private int rotationIndex = -1;
    private BlockPos syncMaster;
    private boolean applyingSync;

    public DigitalSignControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIGITAL_SIGN_CONTROLLER.get(), pos, state);
    }

    public List<BlockPos> getLinkedSigns() { return Collections.unmodifiableList(signs); }
    public List<BlockPos> getSyncedControllers() { return Collections.unmodifiableList(syncedControllers); }
    public BlockPos getSyncMaster() { return syncMaster; }
    public boolean isSyncFollower() { return syncMaster != null; }

    public boolean linkSign(BlockPos pos) {
        if (level == null || pos == null || signs.contains(pos) || signs.size() >= MAX_SIGNS
                || !(level.getBlockEntity(pos) instanceof DigitalSignBlockEntity sign)) return false;
        signs.add(pos);
        applyTo(sign);
        changed();
        return true;
    }

    public boolean unlinkSign(BlockPos pos) {
        boolean removed = signs.remove(pos);
        if (removed) changed();
        return removed;
    }

    public boolean linkSyncedController(BlockPos otherPos) {
        if (level == null || otherPos == null || otherPos.equals(worldPosition)
                || !(level.getBlockEntity(otherPos) instanceof DigitalSignControllerBlockEntity follower)) {
            return false;
        }
        if (syncedControllers.remove(otherPos)) {
            if (worldPosition.equals(follower.syncMaster)) follower.syncMaster = null;
            follower.changed();
            changed();
            return false;
        }
        if (wouldCreateSyncCycle(follower)) return false;
        if (follower.syncMaster != null
                && level.getBlockEntity(follower.syncMaster) instanceof DigitalSignControllerBlockEntity oldMaster) {
            oldMaster.syncedControllers.remove(otherPos);
            oldMaster.changed();
        }
        syncedControllers.add(otherPos);
        follower.syncMaster = worldPosition;
        follower.applySyncFrom(this);
        follower.changed();
        changed();
        return true;
    }

    private boolean wouldCreateSyncCycle(DigitalSignControllerBlockEntity follower) {
        Set<BlockPos> visited = new HashSet<>();
        DigitalSignControllerBlockEntity current = this;
        while (current != null && visited.add(current.worldPosition)) {
            if (current.worldPosition.equals(follower.worldPosition)) return true;
            current = current.syncMaster != null
                    && level.getBlockEntity(current.syncMaster) instanceof DigitalSignControllerBlockEntity master
                            ? master : null;
        }
        return ownsSyncOf(follower, worldPosition, new HashSet<>());
    }

    private boolean ownsSyncOf(DigitalSignControllerBlockEntity controller, BlockPos target, Set<BlockPos> visited) {
        if (!visited.add(controller.worldPosition)) return false;
        for (BlockPos childPos : controller.syncedControllers) {
            if (childPos.equals(target)) return true;
            if (level.getBlockEntity(childPos) instanceof DigitalSignControllerBlockEntity child
                    && ownsSyncOf(child, target, visited)) return true;
        }
        return false;
    }

    public int getRotationPageCount() { return rotationPages.size(); }

    public List<UUID> getRotationSigns() {
        ArrayList<UUID> ids = new ArrayList<>();
        for (RotationPage page : rotationPages) ids.add(page.signId);
        return Collections.unmodifiableList(ids);
    }

    public UUID getPageSignId(int index) {
        return index >= 0 && index < rotationPages.size() ? rotationPages.get(index).signId : selectedSign;
    }

    public String getPageTextLine(int pageIndex, int lineIndex) {
        if (pageIndex < 0 || pageIndex >= rotationPages.size()) return "";
        RotationPage page = rotationPages.get(pageIndex);
        return lineIndex >= 0 && lineIndex < page.textLines.size()
                ? Objects.requireNonNullElse(page.textLines.get(lineIndex), "") : "";
    }

    public void setPageTextLine(int pageIndex, int lineIndex, String text) {
        if (pageIndex < 0 || pageIndex >= rotationPages.size() || lineIndex < 0) return;
        RotationPage page = rotationPages.get(pageIndex);
        ensureTextLineCount(page);
        if (lineIndex >= page.textLines.size()) return;
        page.textLines.set(lineIndex, text == null ? "" : text);
        if (pageIndex == rotationIndex) applySelectedToLinkedSigns();
        changed();
    }

    public int getRotationIndex() { return rotationIndex; }
    public UUID getSelectedSign() { return selectedSign; }
    public DisplaySchedule.Mode getScheduleMode() { return schedule.getMode(); }
    public int getScheduleIntervalAmount() { return schedule.getIntervalAmount(); }

    public void setScheduleMode(DisplaySchedule.Mode mode) {
        if (isSyncFollower()) return;
        DisplaySchedule.Mode next = mode == null ? DisplaySchedule.Mode.MANUAL : mode;
        if (schedule.getMode() == next) return;
        schedule.setMode(next);
        changed();
        pushSyncToFollowers();
    }

    public void setScheduleIntervalAmount(int value) {
        if (isSyncFollower()) return;
        int old = schedule.getIntervalAmount();
        schedule.setIntervalAmount(value);
        if (old == schedule.getIntervalAmount()) return;
        changed();
        pushSyncToFollowers();
    }

    public boolean addRotationSign(UUID id) {
        if (id == null || rotationPages.size() >= MAX_ROTATION_SIGNS) return false;
        rotationPages.add(new RotationPage(id));
        applyRotationIndex(rotationPages.size() - 1);
        changed();
        pushSyncToFollowers();
        return true;
    }

    public boolean selectRotationSign(int index) {
        if (index < 0 || index >= rotationPages.size()) return false;
        applyRotationIndex(index);
        changed();
        pushSyncToFollowers();
        return true;
    }

    public boolean updateRotationSign(int index, UUID id) {
        if (id == null || index < 0 || index >= rotationPages.size()) return false;
        RotationPage page = rotationPages.get(index);
        if (!id.equals(page.signId)) {
            page.signId = id;
            page.textLines.clear();
            ensureTextLineCount(page);
        }
        applyRotationIndex(index);
        changed();
        pushSyncToFollowers();
        return true;
    }

    public void saveRotationPage(int index, UUID id, List<String> textLines) {
        if (index < 0 || index >= rotationPages.size()) return;
        RotationPage page = rotationPages.get(index);
        page.signId = id == null ? Sign.DEFAULT_BLANK_SIGN : id;
        page.textLines.clear();
        if (textLines != null) {
            for (String line : textLines) page.textLines.add(line == null ? "" : line);
        }
        ensureTextLineCount(page);
        applyRotationIndex(index);
        changed();
        pushSyncToFollowers();
    }

    public boolean removeRotationSign(int index) {
        if (index < 0 || index >= rotationPages.size()) return false;
        rotationPages.remove(index);
        if (rotationPages.isEmpty()) {
            rotationIndex = -1;
            selectedSign = Sign.DEFAULT_BLANK_SIGN;
            applySelectedToLinkedSigns();
        } else {
            applyRotationIndex(Math.min(index, rotationPages.size() - 1));
        }
        changed();
        pushSyncToFollowers();
        return true;
    }

    public void clearRotationSigns() {
        rotationPages.clear();
        rotationIndex = -1;
        selectedSign = Sign.DEFAULT_BLANK_SIGN;
        applySelectedToLinkedSigns();
        changed();
        pushSyncToFollowers();
    }

    public int setSelectedSign(UUID id) {
        selectedSign = id == null ? Sign.DEFAULT_BLANK_SIGN : id;
        if (rotationIndex >= 0 && rotationIndex < rotationPages.size())
            selectedSign = rotationPages.get(rotationIndex).signId;
        int updated = applySelectedToLinkedSigns();
        changed();
        pushSyncToFollowers();
        return updated;
    }

    public boolean setRotationSignTime(UUID id, String text) {
        if (id == null) return false;
        if (rotationIndex >= 0 && rotationIndex < rotationPages.size()
                && id.equals(rotationPages.get(rotationIndex).signId))
            return setRotationPageTime(rotationIndex, text);
        for (int i = 0; i < rotationPages.size(); i++)
            if (id.equals(rotationPages.get(i).signId)) return setRotationPageTime(i, text);
        return false;
    }

    public String getRotationSignTimeText(UUID id) {
        if (id == null) return "";
        if (rotationIndex >= 0 && rotationIndex < rotationPages.size()
                && id.equals(rotationPages.get(rotationIndex).signId))
            return getRotationPageTimeText(rotationIndex);
        for (int i = 0; i < rotationPages.size(); i++)
            if (id.equals(rotationPages.get(i).signId)) return getRotationPageTimeText(i);
        return "";
    }

    public String getRotationPageTimeText(int index) {
        if (index < 0 || index >= rotationPages.size()) return "";
        Integer time = rotationPages.get(index).gameTime;
        return time == null ? "" : DisplaySchedule.formatGameTime(time);
    }

    public boolean setRotationPageTime(int index, String text) {
        if (isSyncFollower() || index < 0 || index >= rotationPages.size()) return false;
        Integer next;
        if (text == null || text.isBlank()) next = null;
        else {
            int parsed = DisplaySchedule.parseGameTime(text);
            if (parsed < 0) return false;
            next = parsed;
        }
        RotationPage page = rotationPages.get(index);
        if (Objects.equals(page.gameTime, next)) return true;
        page.gameTime = next;
        changed();
        pushSyncToFollowers();
        return true;
    }

    private static void ensureTextLineCount(RotationPage page) {
        Sign sign = ModRealisticTrafficControl.signRepo.getSignByID(page.signId);
        int count = sign == null ? 0 : sign.getTextLines().size();
        while (page.textLines.size() < count) page.textLines.add("");
        while (page.textLines.size() > count) page.textLines.remove(page.textLines.size() - 1);
    }

    private RotationPage activePage() {
        return rotationIndex >= 0 && rotationIndex < rotationPages.size() ? rotationPages.get(rotationIndex) : null;
    }

    private void applyRotationIndex(int index) {
        if (rotationPages.isEmpty()) {
            rotationIndex = -1;
            return;
        }
        rotationIndex = Math.floorMod(index < 0 ? 0 : index, rotationPages.size());
        selectedSign = rotationPages.get(rotationIndex).signId;
        applySelectedToLinkedSigns();
    }

    private int applySelectedToLinkedSigns() {
        if (level == null) return 0;
        int updated = 0;
        for (BlockPos pos : new ArrayList<>(signs)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DigitalSignBlockEntity sign) {
                applyTo(sign);
                updated++;
            } else {
                signs.remove(pos);
            }
        }
        return updated;
    }

    private void applyTo(DigitalSignBlockEntity sign) {
        RotationPage page = activePage();
        sign.setTypeLegacy(-1);
        sign.setVariantLegacy(-1);
        sign.setID(page == null ? selectedSign : page.signId);
        sign.clearTextLines();
        if (page != null) {
            ensureTextLineCount(page);
            for (int i = 0; i < page.textLines.size(); i++)
                sign.setTextLine(i, Objects.requireNonNullElse(page.textLines.get(i), ""));
        }
        sign.setChanged();
        if (level != null)
            level.sendBlockUpdated(sign.getBlockPos(), sign.getBlockState(), sign.getBlockState(), 3);
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
            DigitalSignControllerBlockEntity controller) {
        if (controller.syncMaster != null) {
            if (level.getBlockEntity(controller.syncMaster) instanceof DigitalSignControllerBlockEntity master) {
                controller.followMaster(master);
            } else {
                controller.syncMaster = null;
                controller.changed();
            }
            return;
        }
        boolean advanced = false;
        if (controller.schedule.getMode() == DisplaySchedule.Mode.GAME_TIMES) {
            int now = DisplaySchedule.normalizeDayTime(level.getDayTime());
            int best = -1;
            int elapsed = Integer.MAX_VALUE;
            for (int i = 0; i < controller.rotationPages.size(); i++) {
                Integer configured = controller.rotationPages.get(i).gameTime;
                if (configured != null) {
                    int candidate = (now - configured + 24000) % 24000;
                    if (candidate < elapsed) {
                        elapsed = candidate;
                        best = i;
                    }
                }
            }
            if (best >= 0 && best != controller.rotationIndex) {
                controller.applyRotationIndex(best);
                controller.changed();
                advanced = true;
            }
        } else if (controller.schedule.getMode().isInterval() && controller.rotationPages.size() > 1
                && controller.schedule.update(level)) {
            controller.applyRotationIndex(controller.rotationIndex + 1);
            controller.changed();
            advanced = true;
        }
        if (advanced) controller.pushSyncToFollowers();
    }

    private void applySyncFrom(DigitalSignControllerBlockEntity master) {
        if (master == null || master == this) return;
        applyingSync = true;
        try {
            schedule.copyTimingFrom(master.schedule);
            applyRotationIndex(master.rotationIndex);
        } finally {
            applyingSync = false;
        }
    }

    private void followMaster(DigitalSignControllerBlockEntity master) {
        int target = rotationPages.isEmpty() ? -1
                : Math.floorMod(master.rotationIndex < 0 ? 0 : master.rotationIndex, rotationPages.size());
        boolean scheduleDiff = schedule.getMode() != master.schedule.getMode()
                || schedule.getIntervalAmount() != master.schedule.getIntervalAmount()
                || !schedule.getGameTimes().equals(master.schedule.getGameTimes());
        if (!scheduleDiff && rotationIndex == target) return;
        applySyncFrom(master);
        changed();
    }

    private void pushSyncToFollowers() {
        if (applyingSync || level == null || level.isClientSide) return;
        for (BlockPos followerPos : new ArrayList<>(syncedControllers)) {
            if (level.getBlockEntity(followerPos) instanceof DigitalSignControllerBlockEntity follower) {
                follower.syncMaster = worldPosition;
                follower.applySyncFrom(this);
                follower.changed();
            } else {
                syncedControllers.remove(followerPos);
                changed();
            }
        }
    }

    private void readData(CompoundTag tag) {
        signs.clear();
        for (long value : tag.getLongArray("signs")) signs.add(BlockPos.of(value));
        if (signs.isEmpty()) {
            for (int i = 0; i < MAX_SIGNS && tag.contains("digitalSign" + i); i++) {
                signs.add(BlockPos.of(tag.getLong("digitalSign" + i)));
            }
        }
        syncedControllers.clear();
        for (long value : tag.getLongArray("syncedControllers")) syncedControllers.add(BlockPos.of(value));
        if (syncedControllers.isEmpty()) {
            int count = Math.max(0, tag.getInt("syncedControllerCount"));
            for (int i = 0; i < count; i++) {
                if (tag.contains("syncedController" + i)) {
                    syncedControllers.add(BlockPos.of(tag.getLong("syncedController" + i)));
                }
            }
        }
        syncMaster = tag.contains("syncMaster") ? BlockPos.of(tag.getLong("syncMaster")) : null;
        selectedSign = tag.hasUUID("selectedSign") ? tag.getUUID("selectedSign")
                : tag.contains("selectedSign") ? NbtUtils.loadUUID(tag.getCompound("selectedSign"))
                : Sign.DEFAULT_BLANK_SIGN;
        rotationPages.clear();
        ListTag list = tag.getList(tag.contains("rotationPages") ? "rotationPages" : "rotationSigns",
                Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(MAX_ROTATION_SIGNS, list.size()); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("id") || entry.contains("signId")) rotationPages.add(new RotationPage(entry));
        }
        if (rotationPages.isEmpty()) {
            int pageCount = Math.min(MAX_ROTATION_SIGNS, Math.max(0, tag.getInt("rotationPageCount")));
            for (int i = 0; i < pageCount; i++) {
                if (tag.contains("rotationPage" + i, Tag.TAG_COMPOUND)) {
                    rotationPages.add(new RotationPage(tag.getCompound("rotationPage" + i)));
                }
            }
        }
        rotationIndex = tag.getInt("rotationIndex");
        if (rotationIndex < 0 || rotationIndex >= rotationPages.size())
            rotationIndex = rotationPages.isEmpty() ? -1 : 0;
        if (rotationIndex >= 0) selectedSign = rotationPages.get(rotationIndex).signId;
        schedule.load(tag, "schedule");
    }

    private void writeData(CompoundTag tag) {
        tag.putLongArray("signs", signs.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putLongArray("syncedControllers", syncedControllers.stream().mapToLong(BlockPos::asLong).toArray());
        if (syncMaster != null) tag.putLong("syncMaster", syncMaster.asLong());
        if (selectedSign != null) tag.putUUID("selectedSign", selectedSign);
        ListTag list = new ListTag();
        for (RotationPage page : rotationPages) list.add(page.save());
        tag.put("rotationPages", list);
        tag.putInt("rotationIndex", rotationIndex);
        schedule.save(tag, "schedule");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        writeData(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        readData(tag);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider provider) { return saveWithoutMetadata(provider); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) { readData(tag); }
    @Override public CompoundTag getClientToServerUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }
    @Override
    public void handleClientToServerUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        readData(tag);
        applySelectedToLinkedSigns();
        changed();
        pushSyncToFollowers();
    }
}
