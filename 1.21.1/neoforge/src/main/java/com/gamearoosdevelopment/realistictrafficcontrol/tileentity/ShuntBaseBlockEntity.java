package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import java.util.ArrayList;
import java.util.function.Consumer;

import com.gamearoosdevelopment.realistictrafficcontrol.blocks.ShuntBlockBase;
import com.gamearoosdevelopment.realistictrafficcontrol.util.ImmersiveRailroadingHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public abstract class ShuntBaseBlockEntity extends BlockEntity {
    private BlockPos trackOrigin = new BlockPos(0, -1, 0);
    private final ArrayList<BlockPos> relayBoxes = new ArrayList<>();

    protected ShuntBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected abstract Consumer<BlockState> getRelayAddOrRemoveShuntMethod(RelayBlockEntity relay);

    public void onBreak(BlockState state) {
        if (level == null) {
            return;
        }
        for (BlockPos relayPos : relayBoxes) {
            BlockEntity entity = level.getBlockEntity(relayPos);
            if (!(entity instanceof RelayBlockEntity relay)) {
                continue;
            }
            getRelayAddOrRemoveShuntMethod(relay).accept(state);
        }
    }

    public boolean setOrigin() {
        if (level == null) {
            return false;
        }
        Vec3 origin = ImmersiveRailroadingHelper.findOrigin(worldPosition, level);
        trackOrigin = BlockPos.containing(origin);
        setChanged();
        return trackOrigin.getY() != -1;
    }

    public BlockPos getTrackOrigin() {
        return trackOrigin;
    }

    public void addPairedRelayBox(BlockPos relayPos) {
        relayBoxes.add(relayPos);
        setChanged();
    }

    public void removePairedRelayBox(BlockPos relayPos) {
        relayBoxes.remove(relayPos);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putIntArray("origin", new int[] { trackOrigin.getX(), trackOrigin.getY(), trackOrigin.getZ() });
        for (int i = 0; i < relayBoxes.size(); i++) {
            BlockPos relayPos = relayBoxes.get(i);
            tag.putIntArray("relayBox" + i, new int[] { relayPos.getX(), relayPos.getY(), relayPos.getZ() });
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        trackOrigin = arrayToPos(tag.getIntArray("origin"));
        relayBoxes.clear();
        int counter = 0;
        while (tag.contains("relayBox" + counter)) {
            relayBoxes.add(arrayToPos(tag.getIntArray("relayBox" + counter)));
            counter++;
        }
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        return false;
    }

    public static boolean shouldRefresh(Level level, BlockPos pos, BlockState oldState, BlockState newState) {
        return !(newState.getBlock() instanceof ShuntBlockBase);
    }

    private static BlockPos arrayToPos(int[] array) {
        if (array.length < 3) {
            return new BlockPos(0, -1, 0);
        }
        return new BlockPos(array[0], array[1], array[2]);
    }
}
