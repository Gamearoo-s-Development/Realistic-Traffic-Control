package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import java.util.HashMap;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.ITrafficLightBlock;
import com.gamearoosdevelopment.realistictrafficcontrol.util.EnumTrafficLightBulbTypes;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Single block entity shared by every traffic-light frame block. Ported from the 1.12.2
 * {@code BaseTrafficLightTileEntity} (which had one subclass per bulb count); here the bulb count is read
 * from the {@link ITrafficLightBlock}, so one class + one {@code BlockEntityType} covers all frame types.
 *
 * <p>1.12.2 mapping: {@code writeToNBT}/{@code readFromNBT} -&gt; {@link #saveAdditional}/
 * {@link #loadAdditional}; {@code getUpdateTag}/{@code getUpdatePacket} kept; {@code ITickable.update}
 * (client flashing) -&gt; {@link #clientTick}. The item-share tag handshake is replaced by
 * {@link #writeFrameTag}/{@link #applyFrameTag} against the {@code frame_data} data component.
 */
public class TrafficLightBlockEntity extends BlockEntity {

    private int bulbCount;
    private HashMap<Integer, EnumTrafficLightBulbTypes> bulbsBySlot = new HashMap<>();
    private HashMap<Integer, EnumTrafficLightBulbTypes> secondaryBulbsBySlot = new HashMap<>();
    private HashMap<Integer, EnumTrafficLightBulbTypes> activeBulbSelectionBySlot = new HashMap<>();
    private HashMap<Integer, Boolean> activeBySlot = new HashMap<>();
    private HashMap<Integer, Boolean> flashBySlot = new HashMap<>();
    private HashMap<Integer, Integer> flashTimeBySlot = new HashMap<>();
    private HashMap<Integer, Boolean> flashCurrent = new HashMap<>();
    private HashMap<Integer, Boolean> allowFlashBySlot = new HashMap<>();

    private boolean hasCover = true;
    private boolean hasPole = false;
    private boolean suppressHorizontalBar = false;
    private Direction configuredApproachFacing = null;

    public TrafficLightBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRAFFIC_LIGHT.get(), pos, state);
        this.bulbCount = resolveBulbCount(state);
        for (int i = 0; i < bulbCount; i++) {
            activeBySlot.put(i, false);
            flashBySlot.put(i, false);
            allowFlashBySlot.put(i, true);
        }
    }

    private static int resolveBulbCount(BlockState state) {
        if (state != null && state.getBlock() instanceof ITrafficLightBlock tl) {
            return tl.getBulbCount();
        }
        return 3;
    }

    public int getBulbCount() {
        return bulbCount;
    }

    public boolean hasCover() {
        return hasCover;
    }

    public boolean hasPole() {
        return hasPole;
    }

    public void setCover(boolean hasCover) {
        this.hasCover = hasCover;
        markDirtyAndSync();
    }

    public void setPole(boolean hasPole) {
        this.hasPole = hasPole;
        markDirtyAndSync();
    }

    public boolean isHorizontalBarSuppressed() {
        return suppressHorizontalBar;
    }

    public void setHorizontalBarSuppressed(boolean suppress) {
        this.suppressHorizontalBar = suppress;
        markDirtyAndSync();
    }

    public Direction getConfiguredApproachFacing() {
        return configuredApproachFacing;
    }

    public void setConfiguredApproachFacing(Direction facing) {
        if (facing != null && !facing.getAxis().isHorizontal()) {
            return;
        }
        this.configuredApproachFacing = facing;
        markDirtyAndSync();
    }

    public void cycleConfiguredApproachFacing() {
        if (configuredApproachFacing == null) {
            configuredApproachFacing = Direction.NORTH;
        } else if (configuredApproachFacing == Direction.NORTH) {
            configuredApproachFacing = Direction.SOUTH;
        } else if (configuredApproachFacing == Direction.SOUTH) {
            configuredApproachFacing = Direction.EAST;
        } else if (configuredApproachFacing == Direction.EAST) {
            configuredApproachFacing = Direction.WEST;
        } else {
            configuredApproachFacing = null;
        }
        markDirtyAndSync();
    }

    public void setBulbType(int slot, EnumTrafficLightBulbTypes newType) {
        setBulbType(slot, 0, newType);
    }

    public void setBulbType(int slot, int layer, EnumTrafficLightBulbTypes newType) {
        if (slot < 0 || slot >= bulbCount) {
            return;
        }
        HashMap<Integer, EnumTrafficLightBulbTypes> target = layer == 1 ? secondaryBulbsBySlot : bulbsBySlot;
        if (newType == null) {
            target.remove(slot);
        } else {
            target.put(slot, newType);
        }
        EnumTrafficLightBulbTypes selection = activeBulbSelectionBySlot.get(slot);
        if (selection != null && !matchesSlotBulb(slot, selection)) {
            activeBulbSelectionBySlot.remove(slot);
        }
        markDirtyAndSync();
    }

    public void setBulbsBySlot(HashMap<Integer, EnumTrafficLightBulbTypes> primary,
            HashMap<Integer, EnumTrafficLightBulbTypes> secondary) {
        bulbsBySlot = new HashMap<>(primary);
        secondaryBulbsBySlot = new HashMap<>(secondary);
        activeBulbSelectionBySlot.clear();
        for (int i = 0; i < bulbCount; i++) {
            activeBySlot.put(i, false);
            flashBySlot.put(i, false);
        }
        markDirtyAndSync();
    }

    public void setAllowFlashBySlot(HashMap<Integer, Boolean> allowFlash) {
        this.allowFlashBySlot = new HashMap<>();
        for (int i = 0; i < bulbCount; i++) {
            this.allowFlashBySlot.put(i, allowFlash.getOrDefault(i, true));
        }
        markDirtyAndSync();
    }

    public boolean getAllowFlashBySlot(int slot) {
        return allowFlashBySlot.getOrDefault(slot, true);
    }

    public void setActive(EnumTrafficLightBulbTypes bulbType, boolean active, boolean flash) {
        boolean changed = false;
        for (int slot = 0; slot < bulbCount; slot++) {
            EnumTrafficLightBulbTypes primary = bulbsBySlot.get(slot);
            EnumTrafficLightBulbTypes secondary = secondaryBulbsBySlot.get(slot);
            if (primary == bulbType || secondary == bulbType) {
                activeBySlot.put(slot, active);
                flashBySlot.put(slot, flash);
                if (active) {
                    activeBulbSelectionBySlot.put(slot, bulbType);
                } else if (activeBulbSelectionBySlot.get(slot) == bulbType) {
                    activeBulbSelectionBySlot.remove(slot);
                }
                changed = true;
            }
        }
        if (changed) {
            markDirtyAndSync();
        }
    }

    public void powerOff() {
        for (int i = 0; i < bulbCount; i++) {
            activeBySlot.put(i, false);
            flashBySlot.put(i, false);
            activeBulbSelectionBySlot.remove(i);
        }
        setActive(EnumTrafficLightBulbTypes.DontCross, true, false);
        markDirtyAndSync();
    }

    public boolean hasBulb(EnumTrafficLightBulbTypes bulbType) {
        if (bulbType == null) {
            return false;
        }
        for (int slot = 0; slot < bulbCount; slot++) {
            if (matchesSlotBulb(slot, bulbType)) {
                return true;
            }
        }
        return false;
    }

    public EnumTrafficLightBulbTypes getBulbTypeBySlot(int slot) {
        return getDisplayedBulbForSlot(slot);
    }

    public EnumTrafficLightBulbTypes getBulbTypeBySlot(int slot, int layer) {
        if (layer == 0) {
            return bulbsBySlot.get(slot);
        }
        if (layer == 1) {
            return secondaryBulbsBySlot.get(slot);
        }
        return null;
    }

    public EnumTrafficLightBulbTypes[] getBulbTypesBySlot(int slot) {
        return new EnumTrafficLightBulbTypes[] { bulbsBySlot.get(slot), secondaryBulbsBySlot.get(slot) };
    }

    public EnumTrafficLightBulbTypes getDisplayedBulbForSlot(int slot) {
        EnumTrafficLightBulbTypes selected = activeBulbSelectionBySlot.get(slot);
        if (selected != null) {
            return selected;
        }
        return getFirstAvailableBulb(slot);
    }

    public boolean getActiveBySlot(int slot) {
        return activeBySlot.getOrDefault(slot, false);
    }

    public boolean getFlashBySlot(int slot) {
        return flashBySlot.getOrDefault(slot, false);
    }

    public boolean getFlashCurrentBySlot(int slot) {
        return flashCurrent.getOrDefault(slot, true);
    }

    public boolean anyActive() {
        for (int i = 0; i < bulbCount; i++) {
            if (getActiveBySlot(i) && (!getFlashBySlot(i) || getFlashCurrentBySlot(i))) {
                return true;
            }
        }
        return false;
    }

    /** Client ticker: advances the flashing animation. Registered via the block's ticker. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, TrafficLightBlockEntity be) {
        for (int i = 0; i < be.bulbCount; i++) {
            if (be.getFlashBySlot(i) && be.getAllowFlashBySlot(i)) {
                be.flashTimeBySlot.merge(i, 1, Integer::sum);
                be.flashCurrent.putIfAbsent(i, false);
                if (be.flashTimeBySlot.get(i) > 20) {
                    be.flashCurrent.put(i, !be.flashCurrent.get(i));
                    be.flashTimeBySlot.put(i, 0);
                }
            } else if (be.getFlashBySlot(i) && !be.getAllowFlashBySlot(i)) {
                be.flashCurrent.put(i, false);
            }
        }
    }

    private void markDirtyAndSync() {
        setChanged();
        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    // --- persistence ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeStateToTag(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readStateFromTag(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        writeStateToTag(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Serialize the placeable state for storage inside a frame item's {@code frame_data} component. */
    public CompoundTag writeFrameTag() {
        CompoundTag tag = new CompoundTag();
        writeStateToTag(tag);
        return tag;
    }

    /** Apply a frame item's stored {@code frame_data} to this freshly placed block entity. */
    public void applyFrameTag(CompoundTag tag) {
        if (tag != null && !tag.isEmpty()) {
            readStateFromTag(tag);
        }
        markDirtyAndSync();
    }

    private void writeStateToTag(CompoundTag tag) {
        int[] primaryBulbTypes = new int[bulbCount];
        int[] secondaryBulbTypes = new int[bulbCount];
        int[] activeSelections = new int[bulbCount];
        for (int i = 0; i < bulbCount; i++) {
            EnumTrafficLightBulbTypes primary = bulbsBySlot.get(i);
            EnumTrafficLightBulbTypes secondary = secondaryBulbsBySlot.get(i);
            EnumTrafficLightBulbTypes selection = activeBulbSelectionBySlot.get(i);
            primaryBulbTypes[i] = primary != null ? primary.getIndex() : -1;
            secondaryBulbTypes[i] = secondary != null ? secondary.getIndex() : -1;
            activeSelections[i] = selection != null ? selection.getIndex() : -1;
            tag.putBoolean("active" + i, getActiveBySlot(i));
            tag.putBoolean("flash" + i, getFlashBySlot(i));
            tag.putBoolean("allowflash" + i, getAllowFlashBySlot(i));
        }
        tag.putIntArray("bulbTypes", primaryBulbTypes);
        tag.putIntArray("secondaryBulbTypes", secondaryBulbTypes);
        tag.putIntArray("activeBulbSelections", activeSelections);
        tag.putBoolean("cover", hasCover);
        tag.putBoolean("pole", hasPole);
        tag.putBoolean("suppressHorizontalBar", suppressHorizontalBar);
        tag.putInt("configuredApproachFacing", configuredApproachFacing != null ? configuredApproachFacing.get2DDataValue() : -1);
    }

    private void readStateFromTag(CompoundTag tag) {
        bulbsBySlot = new HashMap<>();
        secondaryBulbsBySlot = new HashMap<>();
        activeBulbSelectionBySlot = new HashMap<>();
        activeBySlot = new HashMap<>();
        flashBySlot = new HashMap<>();
        allowFlashBySlot = new HashMap<>();

        int[] primaryBulbTypes = tag.getIntArray("bulbTypes");
        int[] secondaryBulbTypes = tag.contains("secondaryBulbTypes") ? tag.getIntArray("secondaryBulbTypes") : new int[0];
        int[] activeSelections = tag.contains("activeBulbSelections") ? tag.getIntArray("activeBulbSelections") : new int[0];
        for (int i = 0; i < bulbCount; i++) {
            if (i < primaryBulbTypes.length) {
                EnumTrafficLightBulbTypes primary = EnumTrafficLightBulbTypes.get(primaryBulbTypes[i]);
                if (primary != null) {
                    bulbsBySlot.put(i, primary);
                }
            }
            if (i < secondaryBulbTypes.length) {
                EnumTrafficLightBulbTypes secondary = EnumTrafficLightBulbTypes.get(secondaryBulbTypes[i]);
                if (secondary != null) {
                    secondaryBulbsBySlot.put(i, secondary);
                }
            }
            if (i < activeSelections.length) {
                EnumTrafficLightBulbTypes selection = EnumTrafficLightBulbTypes.get(activeSelections[i]);
                if (selection != null) {
                    activeBulbSelectionBySlot.put(i, selection);
                }
            }
            activeBySlot.put(i, tag.getBoolean("active" + i));
            flashBySlot.put(i, tag.getBoolean("flash" + i));
            allowFlashBySlot.put(i, tag.contains("allowflash" + i) ? tag.getBoolean("allowflash" + i) : true);
        }
        hasCover = tag.getBoolean("cover");
        hasPole = tag.getBoolean("pole");
        suppressHorizontalBar = tag.getBoolean("suppressHorizontalBar");
        if (tag.contains("configuredApproachFacing")) {
            int facingIndex = tag.getInt("configuredApproachFacing");
            configuredApproachFacing = facingIndex < 0 ? null : Direction.from2DDataValue(facingIndex);
        }
    }

    private boolean matchesSlotBulb(int slot, EnumTrafficLightBulbTypes type) {
        if (type == null) {
            return false;
        }
        EnumTrafficLightBulbTypes primary = bulbsBySlot.get(slot);
        EnumTrafficLightBulbTypes secondary = secondaryBulbsBySlot.get(slot);
        return type == primary || type == secondary;
    }

    private EnumTrafficLightBulbTypes getFirstAvailableBulb(int slot) {
        EnumTrafficLightBulbTypes primary = bulbsBySlot.get(slot);
        if (primary != null) {
            return primary;
        }
        return secondaryBulbsBySlot.get(slot);
    }
}
