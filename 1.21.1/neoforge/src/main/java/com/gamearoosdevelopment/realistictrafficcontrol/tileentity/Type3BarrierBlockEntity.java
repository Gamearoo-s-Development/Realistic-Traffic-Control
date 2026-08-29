package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;
import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockType3BarrierBase;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.Sign;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.UUID;

/** Port of 1.12.2 {@code Type3BarrierTileEntity}. */
public class Type3BarrierBlockEntity extends SyncableBlockEntity {

    private boolean renderSign;
    private SignType signType = SignType.RoadClosed;
    private boolean renderThisSign;
    private int thisSignTypeLegacy = -1;
    private int thisSignVariantLegacy = -1;
    private UUID thisSignID;
    private ArrayList<String> thisSignTextLines;

    public Type3BarrierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TYPE_3_BARRIER.get(), pos, state);
    }

    public boolean getRenderSign() {
        return renderSign;
    }

    public SignType getSignType() {
        return signType;
    }

    public boolean getRenderThisSign() {
        return renderThisSign;
    }

    public int getThisSignType() {
        return thisSignTypeLegacy;
    }

    public int getThisSignVariant() {
        return thisSignVariantLegacy;
    }

    public UUID getThisSignID() {
        return thisSignID;
    }

    public void setRenderSign(boolean renderSign) {
        if (this.renderSign != renderSign) {
            this.renderSign = renderSign;
            markDirtyAndNotify();
        }
    }

    public void setSignType(SignType signType) {
        if (this.signType != signType) {
            this.signType = signType;
            markDirtyAndNotify();
        }
    }

    public void setRenderThisSign(boolean renderThisSign) {
        if (this.renderThisSign != renderThisSign) {
            this.renderThisSign = renderThisSign;
            markDirtyAndNotify();
        }
    }

    public void setThisSignTypeLegacy(int thisSignType) {
        if (this.thisSignTypeLegacy != thisSignType) {
            this.thisSignTypeLegacy = thisSignType;
            markDirtyAndNotify();
        }
    }

    public void setThisSignVariantLegacy(int thisSignVariant) {
        if (this.thisSignVariantLegacy != thisSignVariant) {
            this.thisSignVariantLegacy = thisSignVariant;
            markDirtyAndNotify();
        }
    }

    public void setThisSignID(UUID thisSignID) {
        if ((thisSignID == null && this.thisSignID != null)
                || (thisSignID != null && !thisSignID.equals(this.thisSignID))) {
            this.thisSignID = thisSignID;
            markDirtyAndNotify();
        }
    }

    public Sign getThisSign() {
        Sign sign = null;
        if (thisSignTypeLegacy >= 0) {
            sign = ModRealisticTrafficControl.signRepo.getSignByTypeVariant(
                    SignBlockEntity.getSignTypeName(thisSignTypeLegacy), thisSignVariantLegacy);
        } else if (thisSignID != null) {
            sign = ModRealisticTrafficControl.signRepo.getSignByID(thisSignID);
        }
        if (sign == null) {
            return ModRealisticTrafficControl.signRepo.getSignByID(Sign.DEFAULT_ERROR_SIGN);
        }
        return sign;
    }

    public String getThisSignTextLine(int index) {
        if (thisSignTextLines == null || index >= thisSignTextLines.size()) {
            return null;
        }
        return thisSignTextLines.get(index);
    }

    public void setThisSignTextLine(int index, String text) {
        if (thisSignTextLines == null) {
            thisSignTextLines = new ArrayList<>();
        }
        while (thisSignTextLines.size() <= index) {
            thisSignTextLines.add(null);
        }
        thisSignTextLines.set(index, text);
    }

    public void clearThisSignTextLines() {
        if (thisSignTextLines != null) {
            thisSignTextLines.clear();
        }
    }

    public void nextSignType() {
        int nextIndex = signType.index + 1;
        if (nextIndex > SignType.getMaxIndex()) {
            nextIndex = 0;
        }
        setSignType(SignType.getByIndex(nextIndex));
    }

    public void prevSignType() {
        int nextIndex = signType.index - 1;
        if (nextIndex < 0) {
            nextIndex = SignType.getMaxIndex();
        }
        setSignType(SignType.getByIndex(nextIndex));
    }

    public Type3BarrierBlockEntity findFurthestLeft() {
        BlockPos workingPos = worldPosition;
        BlockState currentState = level.getBlockState(workingPos);
        Block currentBlockInstance = ((BlockType3BarrierBase) currentState.getBlock()).getBlockInstance();
        Direction facing = currentState.getValue(BlockType3BarrierBase.FACING);

        while (currentState.getBlock() == currentBlockInstance
                && !currentState.getValue(RTCProperties.ISFURTHESTLEFT)) {
            workingPos = workingPos.relative(facing.getCounterClockWise());
            currentState = level.getBlockState(workingPos);
            if (!(currentState.getBlock() instanceof BlockType3BarrierBase)) {
                break;
            }
            currentState = getActualState(workingPos, currentState);
        }
        BlockEntity be = level.getBlockEntity(workingPos);
        return be instanceof Type3BarrierBlockEntity barrier ? barrier : this;
    }

    public void syncConnectedBarriers(boolean doClientToServerSync) {
        Type3BarrierBlockEntity left = findFurthestLeft();
        BlockPos workingPos = left.getBlockPos();
        BlockState currentState = level.getBlockState(workingPos);
        Block currentBlockInstance = ((BlockType3BarrierBase) currentState.getBlock()).getBlockInstance();
        Direction facing = currentState.getValue(BlockType3BarrierBase.FACING);

        while (currentState.getBlock() == currentBlockInstance) {
            BlockEntity te = level.getBlockEntity(workingPos);
            if (te instanceof Type3BarrierBlockEntity barrier) {
                barrier.setRenderSign(left.getRenderSign());
                barrier.setSignType(left.getSignType());
                if (doClientToServerSync) {
                    barrier.performClientToServerSync();
                }
            }
            if (currentState.getValue(RTCProperties.ISFURTHESTRIGHT)) {
                break;
            }
            workingPos = workingPos.relative(facing.getClockWise());
            currentState = level.getBlockState(workingPos);
            if (!(currentState.getBlock() instanceof BlockType3BarrierBase)) {
                break;
            }
            currentState = getActualState(workingPos, currentState);
        }
    }

    private BlockState getActualState(BlockPos pos, BlockState state) {
        if (level == null || !(state.getBlock() instanceof BlockType3BarrierBase base)) {
            return state;
        }
        return base.computeActualState(state, level, pos);
    }

    private void markDirtyAndNotify() {
        setChanged();
        if (level != null) {
            BlockState blockState = getBlockState();
            level.sendBlockUpdated(worldPosition, blockState, blockState, 3);
        }
    }

    private void readData(CompoundTag tag) {
        renderSign = tag.getBoolean("renderSign");
        signType = SignType.getByIndex(tag.getInt("signType"));
        renderThisSign = tag.getBoolean("renderThisSign");
        thisSignTypeLegacy = tag.getInt("thisSignType");
        thisSignVariantLegacy = tag.getInt("thisSignVariant");
        if (tag.hasUUID("thisSignID")) {
            thisSignID = tag.getUUID("thisSignID");
        } else if (tag.contains("thisSignID")) {
            thisSignID = NbtUtils.loadUUID(tag.getCompound("thisSignID"));
        }
        if (thisSignTextLines != null) {
            thisSignTextLines.clear();
        }
        if (tag.contains("text0")) {
            thisSignTextLines = new ArrayList<>();
            int i = 0;
            while (tag.contains("text" + i)) {
                thisSignTextLines.add(tag.getString("text" + i));
                i++;
            }
        }
    }

    private void writeData(CompoundTag tag) {
        tag.putBoolean("renderSign", renderSign);
        tag.putInt("signType", signType.index);
        tag.putBoolean("renderThisSign", renderThisSign);
        tag.putInt("thisSignType", thisSignTypeLegacy);
        tag.putInt("thisSignVariant", thisSignVariantLegacy);
        if (thisSignID != null) {
            tag.putUUID("thisSignID", thisSignID);
        }
        if (thisSignTextLines != null) {
            for (int i = 0; i < thisSignTextLines.size(); i++) {
                tag.putString("text" + i, thisSignTextLines.get(i) != null ? thisSignTextLines.get(i) : "");
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeData(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readData(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        readData(tag);
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    public CompoundTag getClientToServerUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        writeData(tag);
        return tag;
    }

    @Override
    public void handleClientToServerUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        readData(tag);
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    public enum SignType {
        RoadClosed(0), LaneClosed(1), RoadClosedThruTraffic(2);

        public final int index;

        SignType(int index) {
            this.index = index;
        }

        public static SignType getByIndex(int index) {
            for (SignType type : values()) {
                if (type.index == index) {
                    return type;
                }
            }
            return RoadClosed;
        }

        public static int getMaxIndex() {
            int max = -1;
            for (SignType type : values()) {
                max = Math.max(max, type.index);
            }
            return max;
        }
    }
}
