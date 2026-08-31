package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.Sign;
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

public final class DigitalSignControllerBlockEntity extends SyncableBlockEntity {
    public static final int MAX_SIGNS = 16;
    public static final int MAX_ROTATION_SIGNS = 32;
    private final ArrayList<BlockPos> signs = new ArrayList<>();
    private final ArrayList<UUID> rotationSigns = new ArrayList<>();
    private final HashMap<UUID, Integer> rotationSignTimes = new HashMap<>();
    private final DisplaySchedule schedule = new DisplaySchedule();
    private UUID selectedSign = Sign.DEFAULT_BLANK_SIGN;
    private int rotationIndex = -1;

    public DigitalSignControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIGITAL_SIGN_CONTROLLER.get(), pos, state);
    }

    public List<BlockPos> getLinkedSigns() { return Collections.unmodifiableList(signs); }
    public boolean linkSign(BlockPos pos) {
        if (level == null || pos == null || signs.contains(pos) || signs.size() >= MAX_SIGNS
                || !(level.getBlockEntity(pos) instanceof DigitalSignBlockEntity sign)) return false;
        signs.add(pos);
        applyTo(sign);
        changed();
        return true;
    }
    public boolean unlinkSign(BlockPos pos) { boolean result = signs.remove(pos); if (result) changed(); return result; }
    public List<UUID> getRotationSigns() { return Collections.unmodifiableList(rotationSigns); }
    public Map<UUID, Integer> getRotationSignTimes() { return Collections.unmodifiableMap(rotationSignTimes); }
    public int getRotationIndex() { return rotationIndex; }
    public UUID getSelectedSign() { return selectedSign; }
    public DisplaySchedule.Mode getScheduleMode() { return schedule.getMode(); }
    public int getScheduleIntervalAmount() { return schedule.getIntervalAmount(); }
    public void setScheduleMode(DisplaySchedule.Mode mode) { schedule.setMode(mode); changed(); }
    public void setScheduleIntervalAmount(int value) { schedule.setIntervalAmount(value); changed(); }

    public boolean addRotationSign(UUID id) {
        if (id == null || rotationSigns.size() >= MAX_ROTATION_SIGNS) return false;
        int existing = rotationSigns.indexOf(id);
        if (existing < 0) rotationSigns.add(id);
        rotationIndex = existing < 0 ? rotationSigns.size() - 1 : existing;
        setSelectedSign(id);
        return existing < 0;
    }
    public boolean selectRotationSign(int index) {
        if (index < 0 || index >= rotationSigns.size()) return false;
        rotationIndex = index;
        setSelectedSign(rotationSigns.get(index));
        return true;
    }
    public boolean updateRotationSign(int index, UUID id) {
        if (id == null || index < 0 || index >= rotationSigns.size()) return false;
        int existing = rotationSigns.indexOf(id);
        if (existing >= 0 && existing != index) return false;
        rotationSigns.set(index, id);
        rotationIndex = index;
        setSelectedSign(id);
        return true;
    }
    public boolean removeRotationSign(int index) {
        if (index < 0 || index >= rotationSigns.size()) return false;
        rotationSignTimes.remove(rotationSigns.remove(index));
        rotationIndex = rotationSigns.isEmpty() ? -1 : Math.min(index, rotationSigns.size() - 1);
        setSelectedSign(rotationIndex < 0 ? Sign.DEFAULT_BLANK_SIGN : rotationSigns.get(rotationIndex));
        return true;
    }
    public void clearRotationSigns() {
        rotationSigns.clear(); rotationSignTimes.clear(); rotationIndex = -1; changed();
    }
    public void setSelectedSign(UUID id) {
        selectedSign = id == null ? Sign.DEFAULT_BLANK_SIGN : id;
        for (BlockPos pos : new ArrayList<>(signs)) {
            BlockEntity be = level == null ? null : level.getBlockEntity(pos);
            if (be instanceof DigitalSignBlockEntity sign) applyTo(sign); else signs.remove(pos);
        }
        changed();
    }
    public boolean setRotationSignTime(UUID id, String text) {
        if (id == null || !rotationSigns.contains(id)) return false;
        if (text == null || text.isBlank()) rotationSignTimes.remove(id);
        else {
            int time = DisplaySchedule.parseGameTime(text);
            if (time < 0) return false;
            rotationSignTimes.put(id, time);
        }
        changed();
        return true;
    }
    public String getRotationSignTimeText(UUID id) {
        Integer time = rotationSignTimes.get(id);
        return time == null ? "" : DisplaySchedule.formatGameTime(time);
    }

    private void applyTo(DigitalSignBlockEntity sign) {
        sign.setTypeLegacy(-1); sign.setVariantLegacy(-1); sign.setID(selectedSign);
        sign.setChanged();
        if (level != null) level.sendBlockUpdated(sign.getBlockPos(), sign.getBlockState(), sign.getBlockState(), 3);
    }
    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DigitalSignControllerBlockEntity be) {
        if (be.schedule.getMode() == DisplaySchedule.Mode.GAME_TIMES) {
            if (be.rotationSigns.isEmpty() || be.rotationSignTimes.isEmpty()) return;
            int now = DisplaySchedule.normalizeDayTime(level.getDayTime());
            int best = -1, elapsed = Integer.MAX_VALUE;
            for (int i = 0; i < be.rotationSigns.size(); i++) {
                Integer configured = be.rotationSignTimes.get(be.rotationSigns.get(i));
                if (configured != null) {
                    int candidate = (now - configured + 24000) % 24000;
                    if (candidate < elapsed) { elapsed = candidate; best = i; }
                }
            }
            if (best >= 0 && (best != be.rotationIndex
                    || !be.rotationSigns.get(best).equals(be.selectedSign))) {
                be.selectRotationSign(best);
            }
        } else if (be.schedule.update(level) && be.rotationSigns.size() > 1) {
            be.selectRotationSign((be.rotationIndex + 1 + be.rotationSigns.size()) % be.rotationSigns.size());
        }
    }

    private void readData(CompoundTag tag) {
        signs.clear();
        for (long value : tag.getLongArray("signs")) signs.add(BlockPos.of(value));
        selectedSign = tag.hasUUID("selectedSign") ? tag.getUUID("selectedSign") : Sign.DEFAULT_BLANK_SIGN;
        rotationSigns.clear(); rotationSignTimes.clear();
        ListTag list = tag.getList("rotationSigns", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(MAX_ROTATION_SIGNS, list.size()); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("id")) {
                UUID id = entry.getUUID("id");
                rotationSigns.add(id);
                if (entry.contains("time")) rotationSignTimes.put(id, entry.getInt("time"));
            }
        }
        rotationIndex = tag.getInt("rotationIndex");
        if (rotationIndex < 0 || rotationIndex >= rotationSigns.size()) rotationIndex = rotationSigns.isEmpty() ? -1 : 0;
        schedule.load(tag, "schedule");
    }
    private void writeData(CompoundTag tag) {
        tag.putLongArray("signs", signs.stream().mapToLong(BlockPos::asLong).toArray());
        if (selectedSign != null) tag.putUUID("selectedSign", selectedSign);
        ListTag list = new ListTag();
        for (UUID id : rotationSigns) {
            CompoundTag entry = new CompoundTag(); entry.putUUID("id", id);
            if (rotationSignTimes.containsKey(id)) entry.putInt("time", rotationSignTimes.get(id));
            list.add(entry);
        }
        tag.put("rotationSigns", list); tag.putInt("rotationIndex", rotationIndex); schedule.save(tag, "schedule");
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) { super.saveAdditional(tag, provider); writeData(tag); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) { super.loadAdditional(tag, provider); readData(tag); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider provider) { return saveWithoutMetadata(provider); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) { readData(tag); }
    @Override public CompoundTag getClientToServerUpdateTag(HolderLookup.Provider provider) { return saveWithoutMetadata(provider); }
    @Override public void handleClientToServerUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        readData(tag); setSelectedSign(selectedSign);
    }
}
