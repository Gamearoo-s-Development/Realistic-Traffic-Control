package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockCrossingGateGate;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties;
import com.gamearoosdevelopment.realistictrafficcontrol.crossing.ICrossingGateBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CrossingLampState;
import com.gamearoosdevelopment.realistictrafficcontrol.util.ILoopableSoundTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.NBTUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

/** Port of 1.12.2 {@code CrossingGateGateTileEntity}. */
public class CrossingGateGateBlockEntity extends SyncableBlockEntity
        implements ICrossingGateBlockEntity, ILoopableSoundTileEntity {

    public enum GateLightCount {
        ThreeLights(0),
        OneLight(1);

        private final int id;

        GateLightCount(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static GateLightCount byId(int id) {
            for (GateLightCount count : values()) {
                if (count.id == id) {
                    return count;
                }
            }
            return ThreeLights;
        }
    }

    private float gateRotation = -60;
    private float gateDelay = 0;
    private GateStatus status = GateStatus.Open;
    private CrossingLampState flashState = CrossingLampState.Off;
    private boolean soundPlaying = false;
    private float crossingGateLength = 4;
    private float upperRotationLimit = 60;
    private float lowerRotationLimit = 0;
    private float delay = 4;
    private float lightStartOffset = 1;
    private GateLightCount gateLightCount = GateLightCount.ThreeLights;

    private final Set<BlockPos> lastBarrierSlots = new HashSet<>();

    private static final float BARRIER_ARM_ON_THRESHOLD = -32.0F;
    private static final double BARRIER_SAMPLE_STEP_MODEL = 16.0;
    private static final boolean BARRIER_BOOM_ON_NEGATIVE_X_SIDE = true;

    public CrossingGateGateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CROSSING_GATE_GATE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrossingGateGateBlockEntity te) {
        te.tickGate(false);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, CrossingGateGateBlockEntity te) {
        te.tickGate(true);
    }

    private void tickGate(boolean client) {
        if (level == null) {
            return;
        }
        switch (status) {
            case Closing -> {
                if (gateDelay <= delay * 20) {
                    gateDelay++;
                    if (!client) {
                        setChanged();
                    }
                    return;
                }
                if (gateRotation >= -lowerRotationLimit) {
                    status = GateStatus.Closed;
                    if (!client) {
                        setChanged();
                    }
                    return;
                }
                if (client) {
                    handlePlaySound();
                }
                gateRotation += 0.5F;
            }
            case Opening -> {
                if (gateRotation <= -upperRotationLimit) {
                    gateDelay = 0;
                    status = GateStatus.Open;
                    if (!client) {
                        setChanged();
                    }
                }
                if (client) {
                    handlePlaySound();
                }
                gateRotation -= 0.5F;
            }
            case Open, Closed -> {
                float idealAngle = status == GateStatus.Open ? -upperRotationLimit : -lowerRotationLimit;
                if (gateRotation > idealAngle) {
                    gateRotation -= 0.5F;
                } else if (gateRotation < idealAngle) {
                    gateRotation += 0.5F;
                }
                if (client) {
                    soundPlaying = false;
                }
            }
        }

        if (!client) {
            syncGateBarrierBlocks();
        }
    }

    private void handlePlaySound() {
        if (!soundPlaying) {
            com.gamearoosdevelopment.realistictrafficcontrol.client.GateSoundHandler.play(this);
            soundPlaying = true;
        }
    }

    private void syncGateBarrierBlocks() {
        Set<BlockPos> desired = computeBarrierSlotsClosed();
        boolean armDown = gateRotation > BARRIER_ARM_ON_THRESHOLD;
        Set<BlockPos> previous = new HashSet<>(lastBarrierSlots);

        if (armDown) {
            for (BlockPos p : desired) {
                if (level.isEmptyBlock(p)) {
                    level.setBlock(p, Blocks.BARRIER.defaultBlockState(), 3);
                }
            }
        }

        for (BlockPos p : previous) {
            boolean keep = armDown && desired.contains(p);
            if (!keep && level.getBlockState(p).is(Blocks.BARRIER)) {
                level.removeBlock(p, false);
            }
        }

        lastBarrierSlots.clear();
        lastBarrierSlots.addAll(desired);
    }

    private Set<BlockPos> computeBarrierSlotsClosed() {
        Set<BlockPos> out = new HashSet<>();
        BlockState blockState = getBlockState();
        if (!(blockState.getBlock() instanceof BlockCrossingGateGate)) {
            return out;
        }

        int rot = blockState.getValue(RTCProperties.ROTATION);
        double facingRad = Math.toRadians(-rot * 22.5F);
        double cosF = Math.cos(facingRad);
        double sinF = Math.sin(facingRad);
        double cx = worldPosition.getX() + 0.5;
        double cz = worldPosition.getZ() + 0.5;
        int gateY = worldPosition.getY();

        double minXModel = -(crossingGateLength * 16.0) - 13.0;
        double maxXModel = (crossingGateLength * 16.0) + 5.5;
        double mxLo;
        double mxHi;
        if (BARRIER_BOOM_ON_NEGATIVE_X_SIDE) {
            mxLo = minXModel;
            mxHi = Math.min(maxXModel, 0.0);
        } else {
            mxLo = Math.max(minXModel, 0.0);
            mxHi = maxXModel;
        }

        for (double mx = mxLo; mx <= mxHi; mx += BARRIER_SAMPLE_STEP_MODEL) {
            double x3 = (mx + 3.0) * (1.0 / 16.0);
            double wx = x3 * cosF + cx;
            double wz = -x3 * sinF + cz;
            BlockPos bp = new BlockPos(Mth.floor(wx), gateY, Mth.floor(wz));
            if (!bp.equals(worldPosition)) {
                out.add(bp);
            }
        }
        return out;
    }

    public void onRemoved() {
        if (level != null && !level.isClientSide) {
            for (BlockPos p : lastBarrierSlots) {
                if (level.getBlockState(p).is(Blocks.BARRIER)) {
                    level.removeBlock(p, false);
                }
            }
            lastBarrierSlots.clear();
        }
    }

    public float getFacingRotation() {
        BlockState blockState = getBlockState();
        if (!(blockState.getBlock() instanceof BlockCrossingGateGate)) {
            return 0;
        }
        return -blockState.getValue(RTCProperties.ROTATION) * 22.5F;
    }

    public float getGateRotation() {
        return gateRotation;
    }

    @Override
    public GateStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(GateStatus status) {
        if ((status == GateStatus.Opening && this.status == GateStatus.Open)
                || (status == GateStatus.Closing && this.status == GateStatus.Closed)) {
            return;
        }
        this.status = status;
        sendUpdates(true);
    }

    @Override
    public void setFlashState(CrossingLampState state) {
        if (state != flashState) {
            flashState = state;
            sendUpdates(true);
        }
    }

    public CrossingLampState getFlashState() {
        return flashState;
    }

    public float getCrossingGateLength() {
        return crossingGateLength;
    }

    public void setCrossingGateLength(float length) {
        if (length != crossingGateLength) {
            crossingGateLength = length;
            sendUpdates(true);
        }
    }

    public float getUpperRotationLimit() {
        return upperRotationLimit;
    }

    public void setUpperRotationLimit(float upperRotationLimit) {
        if (upperRotationLimit != this.upperRotationLimit) {
            this.upperRotationLimit = upperRotationLimit;
            sendUpdates(true);
        }
    }

    public float getLowerRotationLimit() {
        return lowerRotationLimit;
    }

    public void setLowerRotationLimit(float lowerRotationLimit) {
        if (lowerRotationLimit != this.lowerRotationLimit) {
            this.lowerRotationLimit = lowerRotationLimit;
            sendUpdates(true);
        }
    }

    public float getDelay() {
        return delay;
    }

    public void setDelay(float delay) {
        if (delay != this.delay) {
            this.delay = delay;
            sendUpdates(true);
        }
    }

    public float getLightStartOffset() {
        return lightStartOffset;
    }

    public void setLightStartOffset(float lightStartOffset) {
        if (lightStartOffset != this.lightStartOffset) {
            this.lightStartOffset = lightStartOffset;
            sendUpdates(true);
        }
    }

    public GateLightCount getGateLightCount() {
        return gateLightCount;
    }

    public void setGateLightCount(GateLightCount count) {
        if (gateLightCount != count) {
            gateLightCount = count;
            sendUpdates(true);
        }
    }

    @Override
    public boolean isDonePlayingSound() {
        return !soundPlaying;
    }

    private void sendUpdates(boolean markDirty) {
        if (markDirty) {
            setChanged();
        }
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("gateRotation", gateRotation);
        tag.putFloat("gateDelay", gateDelay);
        tag.putInt("status", status.ordinal());
        tag.putInt("flashState", flashState.getId());
        tag.putFloat("length", crossingGateLength);
        tag.putFloat("upperRotation", upperRotationLimit);
        tag.putFloat("lowerRotation", lowerRotationLimit);
        tag.putFloat("delay", delay);
        tag.putFloat("lightStartOffset", lightStartOffset);
        tag.putInt("gateLightCount", gateLightCount.getId());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        gateRotation = tag.getFloat("gateRotation");
        gateDelay = tag.getFloat("gateDelay");
        status = GateStatus.values()[Mth.clamp(tag.getInt("status"), 0, GateStatus.values().length - 1)];
        flashState = CrossingLampState.byId(tag.getInt("flashState"));
        crossingGateLength = NBTUtils.getFloatOrDefault(tag, "length", 4);
        upperRotationLimit = NBTUtils.getFloatOrDefault(tag, "upperRotation", 60);
        lowerRotationLimit = NBTUtils.getFloatOrDefault(tag, "lowerRotation", 0);
        delay = NBTUtils.getFloatOrDefault(tag, "delay", 4);
        lightStartOffset = NBTUtils.getFloatOrDefault(tag, "lightStartOffset", 1);
        gateLightCount = GateLightCount.byId(NBTUtils.getIntOrDefault(tag, "gateLightCount", GateLightCount.ThreeLights.getId()));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Override
    public CompoundTag getClientToServerUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("length", crossingGateLength);
        tag.putFloat("upperRotation", upperRotationLimit);
        tag.putFloat("lowerRotation", lowerRotationLimit);
        tag.putFloat("delay", delay);
        tag.putFloat("lightStartOffset", lightStartOffset);
        tag.putInt("gateLightCount", gateLightCount.getId());
        return tag;
    }

    @Override
    public void handleClientToServerUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        setCrossingGateLength(tag.getFloat("length"));
        setUpperRotationLimit(tag.getFloat("upperRotation"));
        setLowerRotationLimit(tag.getFloat("lowerRotation"));
        setDelay(tag.getFloat("delay"));
        setLightStartOffset(tag.getFloat("lightStartOffset"));
        setGateLightCount(GateLightCount.byId(tag.getInt("gateLightCount")));
    }
}
