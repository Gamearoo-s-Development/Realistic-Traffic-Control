package com.gamearoosdevelopment.realistictrafficcontrol.scanner;

import java.util.HashSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import com.google.common.collect.ImmutableList;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Port of 1.12.2 {@code ScannerData} ({@code WorldSavedData}): tracks relay positions subscribed to
 * the IR train scanner for the current dimension.
 */
public class ScannerData extends SavedData {
    public static final String DATA_NAME = "tc_scanner_data";

    private final HashSet<BlockPos> tileEntitySubscriptions = new HashSet<>();
    private final ReentrantReadWriteLock tileEntitySubscriptionLock = new ReentrantReadWriteLock();

    public ScannerData() {
    }

    public static ScannerData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ScannerData::new, ScannerData::load, null),
                DATA_NAME);
    }

    private static ScannerData load(CompoundTag tag, HolderLookup.Provider registries) {
        ScannerData data = new ScannerData();
        data.readFromNBT(tag);
        return data;
    }

    private void readFromNBT(CompoundTag nbt) {
        WriteLock lock = tileEntitySubscriptionLock.writeLock();
        lock.lock();
        tileEntitySubscriptions.clear();
        int i = 0;
        while (nbt.contains("blockpos" + i)) {
            tileEntitySubscriptions.add(BlockPos.of(nbt.getLong("blockpos" + i)));
            i++;
        }
        lock.unlock();
    }

    @Override
    public CompoundTag save(CompoundTag compound, HolderLookup.Provider registries) {
        int i = 0;
        ReadLock lock = tileEntitySubscriptionLock.readLock();
        lock.lock();
        for (BlockPos pos : tileEntitySubscriptions) {
            compound.putLong("blockpos" + i, pos.asLong());
            i++;
        }
        lock.unlock();
        return compound;
    }

    public ImmutableList<BlockPos> getSubscribers() {
        ReadLock lock = tileEntitySubscriptionLock.readLock();
        lock.lock();
        ImmutableList<BlockPos> list = ImmutableList.copyOf(tileEntitySubscriptions);
        lock.unlock();
        return list;
    }

    public void addSubscriber(BlockPos pos) {
        WriteLock lock = tileEntitySubscriptionLock.writeLock();
        lock.lock();
        if (tileEntitySubscriptions.add(pos)) {
            setDirty();
        }
        lock.unlock();
    }

    public void removeSubscriber(BlockPos pos) {
        WriteLock lock = tileEntitySubscriptionLock.writeLock();
        lock.lock();
        if (tileEntitySubscriptions.remove(pos)) {
            setDirty();
        }
        lock.unlock();
    }
}
