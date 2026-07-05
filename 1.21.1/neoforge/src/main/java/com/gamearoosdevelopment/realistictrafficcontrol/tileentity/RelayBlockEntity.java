package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.gamearoosdevelopment.realistictrafficcontrol.Config;
import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;
import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.RelayBlockBase;
import com.gamearoosdevelopment.realistictrafficcontrol.crossing.ICrossingGateBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.crossing.ICrossingLampBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.crossing.IWigWagBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.scanner.IScannerSubscriber;
import com.gamearoosdevelopment.realistictrafficcontrol.scanner.ScanCompleteData;
import com.gamearoosdevelopment.realistictrafficcontrol.scanner.ScanRequest;
import com.gamearoosdevelopment.realistictrafficcontrol.scanner.Scanner;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CrossingLampState;
import com.gamearoosdevelopment.realistictrafficcontrol.util.Tuple;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of 1.12.2 {@code RelayTileEntity}: master/slave multiblock orchestration for crossing gates,
 * lamps, bells, and wig-wags. Gate/lamp/wig-wag block entities call the {@code ICrossing*} hook
 * interfaces when those subsystems are ported; bells are fully wired via {@link BellBlockEntity}.
 */
public class RelayBlockEntity extends SyncableBlockEntity implements IScannerSubscriber {

    private boolean isMaster;
    private boolean isPowered;
    private boolean automatedPowerOverride;
    private int masterX;
    private int masterY;
    private int masterZ;

    private boolean alreadyNotifiedGates;
    private int lastFlash = 19;
    private CrossingLampState state = CrossingLampState.Off;
    private boolean alreadyNotifiedBells;
    private boolean alreadyNotifiedWigWags;
    private boolean alreadyNotifiedVerticalWigWags;

    private final ArrayList<BlockPos> crossingLampLocations = new ArrayList<>();
    private final ArrayList<BlockPos> crossingGateLocations = new ArrayList<>();
    private final ArrayList<BlockPos> bellLocations = new ArrayList<>();
    private final ArrayList<BlockPos> wigWagLocations = new ArrayList<>();
    private final ArrayList<BlockPos> verticalWigWagLocations = new ArrayList<>();
    private final ArrayList<Tuple<BlockPos, Direction>> shuntBorderOriginsAndFacing = new ArrayList<>();
    private final ArrayList<Tuple<BlockPos, Direction>> shuntIslandOriginsAndFacing = new ArrayList<>();
    private final HashMap<BlockPos, Integer> invalidCrossingGates = new HashMap<>();
    private final HashMap<BlockPos, Integer> invalidBells = new HashMap<>();
    private int lastHeartbeat;
    private long crossingBellStartWorldTime = -1L;
    private int relayBellStopAfterSeconds;

    public RelayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RELAY.get(), pos, state);
        relayBellStopAfterSeconds = Config.crossingBellStopAfterSeconds;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RelayBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        if (level == null || level.isClientSide || !isMaster) {
            return;
        }

        if (getPowered()) {
            if (crossingBellStartWorldTime < 0L) {
                crossingBellStartWorldTime = level.getGameTime();
                setChanged();
            }
        } else if (crossingBellStartWorldTime >= 0L) {
            crossingBellStartWorldTime = -1L;
            setChanged();
        }

        if (lastHeartbeat >= 20) {
            alreadyNotifiedBells = false;
            alreadyNotifiedGates = false;
            alreadyNotifiedWigWags = false;
            lastHeartbeat = 0;
        } else {
            lastHeartbeat++;
        }

        boolean markDirty = !alreadyNotifiedBells || !alreadyNotifiedGates || !alreadyNotifiedWigWags;
        markDirty |= notifyGates();
        updateLamps();
        markDirty |= updateBells();
        markDirty |= notifyWigWags();
        markDirty |= notifyVerticalWigWags();
        markDirty |= checkRemoveInvalidItems();

        if (markDirty) {
            setChanged();
        }

        if (ModRealisticTrafficControl.IR_INSTALLED && level instanceof ServerLevel serverLevel) {
            Scanner scanner = Scanner.scannersByWorld.get(serverLevel.dimension());
            if (scanner != null) {
                scanner.subscribe(this);
            }
        }
    }

    public void onPlaced(Level level) {
        // no-op hook for future placement logic
    }

    private boolean notifyGates() {
        boolean markDirty = false;
        for (BlockPos gatePos : crossingGateLocations) {
            BlockEntity te = level.getBlockEntity(gatePos);
            if (!(te instanceof ICrossingGateBlockEntity gate) || te.isRemoved()) {
                invalidCrossingGates.merge(gatePos, 1, Integer::sum);
                continue;
            }
            invalidCrossingGates.remove(gatePos);
            if (!alreadyNotifiedGates) {
                gate.setStatus(getPowered()
                        ? ICrossingGateBlockEntity.GateStatus.Closing
                        : ICrossingGateBlockEntity.GateStatus.Opening);
                markDirty = true;
            }
        }
        alreadyNotifiedGates = true;
        return markDirty;
    }

    private void updateLamps() {
        if (!getPowered() && state != CrossingLampState.Off) {
            if (crossingGateLocations.stream().noneMatch(cgl -> !invalidCrossingGates.containsKey(cgl))) {
                lastFlash = 19;
                state = CrossingLampState.Off;
                notifyLamps();
                setChanged();
            } else {
                BlockPos firstValidGate = crossingGateLocations.stream()
                        .filter(cgl -> !invalidCrossingGates.containsKey(cgl))
                        .findFirst()
                        .orElse(null);
                if (firstValidGate != null
                        && level.getBlockEntity(firstValidGate) instanceof ICrossingGateBlockEntity gate
                        && gate.getStatus() == ICrossingGateBlockEntity.GateStatus.Open) {
                    lastFlash = 19;
                    state = CrossingLampState.Off;
                    notifyLamps();
                    setChanged();
                }
            }
        } else if (!getPowered() && state == CrossingLampState.Off) {
            return;
        }

        if (lastFlash < 20) {
            lastFlash++;
            setChanged();
            return;
        }

        lastFlash = 0;
        state = (state == CrossingLampState.Flash2 || state == CrossingLampState.Off)
                ? CrossingLampState.Flash1
                : CrossingLampState.Flash2;
        notifyLamps();
        setChanged();
    }

    private void notifyLamps() {
        ArrayList<BlockPos> positionsToRemove = new ArrayList<>();
        for (BlockPos lampLocation : crossingLampLocations) {
            try {
                if (level.getBlockEntity(lampLocation) instanceof ICrossingLampBlockEntity lamp) {
                    lamp.setState(state);
                    level.sendBlockUpdated(lampLocation, level.getBlockState(lampLocation),
                            level.getBlockState(lampLocation), 3);
                } else {
                    positionsToRemove.add(lampLocation);
                }
            } catch (Exception ex) {
                positionsToRemove.add(lampLocation);
            }
        }

        for (BlockPos gateLocation : crossingGateLocations) {
            try {
                if (level.getBlockEntity(gateLocation) instanceof ICrossingGateBlockEntity gate) {
                    gate.setFlashState(state);
                    level.sendBlockUpdated(gateLocation, level.getBlockState(gateLocation),
                            level.getBlockState(gateLocation), 3);
                }
            } catch (Exception ignored) {
            }
        }

        for (BlockPos positionToRemove : positionsToRemove) {
            crossingLampLocations.remove(positionToRemove);
            ModRealisticTrafficControl.LOGGER.error(
                    "Crossing Lamp at {} has been unpaired due to an error", positionToRemove);
        }
    }

    private boolean shouldBellsRing() {
        if (!getPowered()) {
            return false;
        }
        if (relayBellStopAfterSeconds <= 0) {
            return true;
        }
        if (crossingBellStartWorldTime < 0L) {
            return true;
        }
        return level.getGameTime() - crossingBellStartWorldTime < (long) relayBellStopAfterSeconds * 20L;
    }

    private boolean updateBells() {
        boolean markDirty = false;
        boolean ring = shouldBellsRing();
        boolean bellNeedsEveryTick = relayBellStopAfterSeconds > 0;
        for (BlockPos bellPos : bellLocations) {
            BlockEntity te = level.getBlockEntity(bellPos);
            if (!(te instanceof BellBlockEntity bell) || te.isRemoved()) {
                invalidBells.merge(bellPos, 1, Integer::sum);
                continue;
            }
            invalidBells.remove(bellPos);
            if (bellNeedsEveryTick || !alreadyNotifiedBells) {
                boolean wantRing = ring;
                if (getPowered() && relayBellStopAfterSeconds > 0 && !ring && !bell.isAffectedByRelayBellStopTimer()) {
                    wantRing = true;
                }
                if (bell.getIsRinging() != wantRing) {
                    bell.setIsRinging(wantRing);
                    markDirty = true;
                }
            }
        }
        alreadyNotifiedBells = true;
        return markDirty;
    }

    private boolean notifyWigWags() {
        if (!alreadyNotifiedWigWags) {
            ArrayList<BlockPos> positionsToRemove = new ArrayList<>();
            for (BlockPos pos : wigWagLocations) {
                try {
                    if (level.getBlockEntity(pos) instanceof IWigWagBlockEntity wigWag) {
                        wigWag.setActive(getPowered());
                        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                    } else {
                        positionsToRemove.add(pos);
                    }
                } catch (Exception ex) {
                    positionsToRemove.add(pos);
                }
            }
            for (BlockPos pos : positionsToRemove) {
                wigWagLocations.remove(pos);
                ModRealisticTrafficControl.LOGGER.error("Wig Wag at {} has been unpaired due to an error", pos);
            }
            alreadyNotifiedWigWags = true;
            return true;
        }
        return false;
    }

    private boolean notifyVerticalWigWags() {
        if (!alreadyNotifiedVerticalWigWags) {
            ArrayList<BlockPos> positionsToRemove = new ArrayList<>();
            for (BlockPos pos : verticalWigWagLocations) {
                try {
                    if (level.getBlockEntity(pos) instanceof IWigWagBlockEntity wigWag) {
                        wigWag.setActive(getPowered());
                        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                    } else {
                        positionsToRemove.add(pos);
                    }
                } catch (Exception ex) {
                    positionsToRemove.add(pos);
                }
            }
            for (BlockPos pos : positionsToRemove) {
                verticalWigWagLocations.remove(pos);
                ModRealisticTrafficControl.LOGGER.error("Vertical Wig Wag at {} has been unpaired due to an error", pos);
            }
            alreadyNotifiedVerticalWigWags = true;
            return true;
        }
        return false;
    }

    private boolean checkRemoveInvalidItems() {
        boolean didRemove = false;
        ArrayList<BlockPos> itemsRemoved = new ArrayList<>();

        for (BlockPos invalidGate : invalidCrossingGates.entrySet().stream()
                .filter(set -> set.getValue() > 200)
                .map(set -> set.getKey())
                .collect(Collectors.toList())) {
            didRemove = true;
            ModRealisticTrafficControl.LOGGER.warn(
                    "Crossing Relay at {} found that a crossing gate at {} did not load after 10 seconds. Removing from paired list.",
                    getBlockPos(), invalidGate);
            crossingGateLocations.remove(invalidGate);
            itemsRemoved.add(invalidGate);
        }
        for (BlockPos removed : itemsRemoved) {
            invalidCrossingGates.remove(removed);
        }
        itemsRemoved.clear();

        for (BlockPos invalidBell : invalidBells.entrySet().stream()
                .filter(set -> set.getValue() > 200)
                .map(set -> set.getKey())
                .collect(Collectors.toList())) {
            didRemove = true;
            ModRealisticTrafficControl.LOGGER.warn(
                    "Crossing Relay at {} found that a bell at {} did not load after 10 seconds. Removing from paired list.",
                    getBlockPos(), invalidBell);
            bellLocations.remove(invalidBell);
            itemsRemoved.add(invalidBell);
        }
        for (BlockPos removed : itemsRemoved) {
            invalidBells.remove(removed);
        }
        return didRemove;
    }

    public void setMaster() {
        isMaster = true;
        setChanged();
    }

    public void setMasterLocation(BlockPos pos) {
        masterX = pos.getX();
        masterY = pos.getY();
        masterZ = pos.getZ();
    }

    public RelayBlockEntity getMaster(Level world) {
        if (isMaster) {
            return this;
        }
        BlockEntity master = world.getBlockEntity(getMasterBlockPos());
        if (master instanceof RelayBlockEntity relay) {
            return relay;
        }
        return null;
    }

    public boolean isMasterRelay() {
        return isMaster;
    }

    public int getRelayBellStopAfterSeconds() {
        RelayBlockEntity master = isMaster ? this : getMaster(level);
        return master != null ? master.relayBellStopAfterSeconds : relayBellStopAfterSeconds;
    }

    public void setRelayBellStopAfterSeconds(int sec) {
        sec = Math.max(0, Math.min(3600, sec));
        RelayBlockEntity master = isMaster ? this : getMaster(level);
        if (master != null) {
            master.applyRelayBellStopToFullMultiblock(sec);
        } else {
            applyRelayBellStopLocal(sec);
        }
    }

    public static List<BlockPos> enumerateMultiblockParts(BlockPos origin, BlockState originState) {
        List<BlockPos> list = new ArrayList<>(8);
        if (!(originState.getBlock() instanceof RelayBlockBase)) {
            list.add(origin);
            return list;
        }
        Direction facing = originState.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        BlockPos p = origin;
        Direction lastFacing = facing;
        list.add(p);
        lastFacing = relayMultiblockRotateLeft(lastFacing);
        p = p.relative(lastFacing);
        list.add(p);
        lastFacing = relayMultiblockRotateRight(lastFacing);
        p = p.relative(lastFacing);
        list.add(p);
        lastFacing = relayMultiblockRotateRight(lastFacing);
        p = p.relative(lastFacing);
        list.add(p);
        p = p.relative(Direction.UP);
        list.add(p);
        lastFacing = relayMultiblockRotateRight(lastFacing);
        p = p.relative(lastFacing);
        list.add(p);
        lastFacing = relayMultiblockRotateRight(lastFacing);
        p = p.relative(lastFacing);
        list.add(p);
        lastFacing = relayMultiblockRotateRight(lastFacing);
        p = p.relative(lastFacing);
        list.add(p);
        return list;
    }

    private List<BlockPos> enumerateRelayMultiblockParts() {
        if (level == null) {
            return List.of(getBlockPos());
        }
        return enumerateMultiblockParts(getBlockPos(), level.getBlockState(getBlockPos()));
    }

    private static Direction relayMultiblockRotateLeft(Direction in) {
        return switch (in) {
            case NORTH -> Direction.WEST;
            case WEST -> Direction.SOUTH;
            case SOUTH -> Direction.EAST;
            case EAST -> Direction.NORTH;
            default -> in;
        };
    }

    private static Direction relayMultiblockRotateRight(Direction in) {
        return switch (in) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> in;
        };
    }

    private void applyRelayBellStopToFullMultiblock(int sec) {
        if (!isMaster) {
            RelayBlockEntity m = getMaster(level);
            if (m != null) {
                m.applyRelayBellStopToFullMultiblock(sec);
            } else {
                applyRelayBellStopLocal(sec);
            }
            return;
        }
        for (BlockPos partPos : enumerateRelayMultiblockParts()) {
            if (level.getBlockEntity(partPos) instanceof RelayBlockEntity relay) {
                relay.applyRelayBellStopLocal(sec);
            }
        }
    }

    private void applyRelayBellStopLocal(int sec) {
        if (relayBellStopAfterSeconds == sec) {
            return;
        }
        relayBellStopAfterSeconds = sec;
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState st = level.getBlockState(getBlockPos());
            level.sendBlockUpdated(getBlockPos(), st, st, 3);
        }
    }

    private BlockPos getMasterBlockPos() {
        return new BlockPos(masterX, masterY, masterZ);
    }

    public boolean addOrRemoveCrossingGateLamp(BlockPos lampPos) {
        if (crossingLampLocations.contains(lampPos)) {
            crossingLampLocations.remove(lampPos);
            setChanged();
            return false;
        }
        crossingLampLocations.add(lampPos);
        setChanged();
        return true;
    }

    public boolean addOrRemoveCrossingGateGate(BlockPos gatePos) {
        if (crossingGateLocations.contains(gatePos)) {
            crossingGateLocations.remove(gatePos);
            setChanged();
            return false;
        }
        crossingGateLocations.add(gatePos);
        setChanged();
        return true;
    }

    public boolean addOrRemoveBell(BlockPos bellPos) {
        if (bellLocations.contains(bellPos)) {
            bellLocations.remove(bellPos);
            setChanged();
            return false;
        }
        bellLocations.add(bellPos);
        setChanged();
        return true;
    }

    public boolean addOrRemoveWigWag(BlockPos wigWagPos) {
        if (wigWagLocations.contains(wigWagPos)) {
            wigWagLocations.remove(wigWagPos);
            setChanged();
            return false;
        }
        wigWagLocations.add(wigWagPos);
        setChanged();
        return true;
    }

    public boolean addOrRemoveVerticalWigWag(BlockPos wigWagPos) {
        if (verticalWigWagLocations.contains(wigWagPos)) {
            verticalWigWagLocations.remove(wigWagPos);
            setChanged();
            return false;
        }
        verticalWigWagLocations.add(wigWagPos);
        setChanged();
        return true;
    }

    public boolean addOrRemoveShuntBorder(BlockPos trackOrigin, Direction shuntFacing) {
        Tuple<BlockPos, Direction> value = new Tuple<>(trackOrigin, shuntFacing);
        if (shuntBorderOriginsAndFacing.contains(value)) {
            shuntBorderOriginsAndFacing.remove(value);
            setChanged();
            return false;
        }
        shuntBorderOriginsAndFacing.add(value);
        setChanged();
        return true;
    }

    public boolean addOrRemoveShuntIsland(BlockPos trackOrigin, Direction shuntFacing) {
        Tuple<BlockPos, Direction> value = new Tuple<>(trackOrigin, shuntFacing);
        if (shuntIslandOriginsAndFacing.contains(value)) {
            shuntIslandOriginsAndFacing.remove(value);
            setChanged();
            return false;
        }
        shuntIslandOriginsAndFacing.add(value);
        setChanged();
        return true;
    }

    public void setPowered(boolean powered) {
        this.isPowered = powered;
        alreadyNotifiedGates = false;
        alreadyNotifiedBells = false;
        alreadyNotifiedWigWags = false;
        alreadyNotifiedVerticalWigWags = false;
        setChanged();
        updateComparator();
    }

    private void setAutomatedPowered(boolean automatedPowerOverride) {
        this.automatedPowerOverride = automatedPowerOverride;
        updateComparator();
    }

    private void updateComparator() {
        if (level == null) {
            return;
        }
        Direction myFacing = level.getBlockState(getBlockPos())
                .getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        Direction workingFacing = myFacing.getClockWise();
        BlockPos workingPos = getBlockPos();
        for (int i = 0; i < 4; i++) {
            level.updateNeighbourForOutputSignal(workingPos, level.getBlockState(workingPos).getBlock());
            workingFacing = workingFacing.getCounterClockWise();
            workingPos = workingPos.relative(workingFacing);
        }
        workingFacing = myFacing.getClockWise();
        workingPos = getBlockPos().above();
        for (int i = 0; i < 4; i++) {
            level.updateNeighbourForOutputSignal(workingPos, level.getBlockState(workingPos).getBlock());
            workingFacing = workingFacing.getCounterClockWise();
            workingPos = workingPos.relative(workingFacing);
        }
    }

    public boolean getPowered() {
        return isPowered || automatedPowerOverride;
    }

    private final UUID islandRequest = UUID.fromString("da2e3487-9fe6-4369-80bc-4b5ce40f0530");
    private final UUID borderRequest = UUID.fromString("c4ba0fb7-3df0-4c18-9edf-491d825899d9");
    private long lastMovementWorldTime = 0;

    @Override
    public List<ScanRequest> getScanRequests() {
        ArrayList<ScanRequest> scanRequestList = new ArrayList<>();
        for (Tuple<BlockPos, Direction> islandOrigin : shuntIslandOriginsAndFacing) {
            scanRequestList.add(new ScanRequest(
                    islandRequest,
                    islandOrigin.getFirst(),
                    shuntIslandOriginsAndFacing.stream()
                            .map(Tuple::getFirst)
                            .filter(pos -> !pos.equals(islandOrigin.getFirst()))
                            .collect(Collectors.toList()),
                    islandOrigin.getSecond()));
        }
        for (Tuple<BlockPos, Direction> borderOrigin : shuntBorderOriginsAndFacing) {
            scanRequestList.add(new ScanRequest(
                    borderRequest,
                    borderOrigin.getFirst(),
                    shuntIslandOriginsAndFacing.stream().map(Tuple::getFirst).collect(Collectors.toList()),
                    borderOrigin.getSecond()));
        }
        return scanRequestList;
    }

    @Override
    public void onScanComplete(ScanCompleteData scanCompleteData) {
        UUID scanRequestID = scanCompleteData.getScanRequest().getRequestID();
        if (scanRequestID.equals(islandRequest)) {
            if (scanCompleteData.getTrainFound()) {
                if (!automatedPowerOverride) {
                    alreadyNotifiedBells = false;
                    alreadyNotifiedGates = false;
                    alreadyNotifiedWigWags = false;
                    alreadyNotifiedVerticalWigWags = false;
                    setAutomatedPowered(true);
                }
                scanCompleteData.cancelScanningForTileEntity();
            }
        } else {
            if (scanCompleteData.getTimedOut()) {
                return;
            }
            if (scanCompleteData.getTrainFound()) {
                if (scanCompleteData.getTrainMovingTowardsDestination()) {
                    lastMovementWorldTime = level.getGameTime();
                }
                if (!automatedPowerOverride && scanCompleteData.getTrainMovingTowardsDestination()) {
                    alreadyNotifiedBells = false;
                    alreadyNotifiedGates = false;
                    alreadyNotifiedWigWags = false;
                    alreadyNotifiedVerticalWigWags = false;
                    setAutomatedPowered(true);
                }
                if (!(automatedPowerOverride && !scanCompleteData.getTrainMovingTowardsDestination()
                        && level.getGameTime() - lastMovementWorldTime > 200)) {
                    scanCompleteData.cancelScanningForTileEntity();
                }
            }
        }
    }

    @Override
    public void onScanRequestsCompleted() {
        if (automatedPowerOverride) {
            alreadyNotifiedBells = false;
            alreadyNotifiedGates = false;
            alreadyNotifiedWigWags = false;
            alreadyNotifiedVerticalWigWags = false;
            setAutomatedPowered(false);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("ismaster", isMaster);
        tag.putBoolean("ispowered", isPowered);
        tag.putInt("masterx", masterX);
        tag.putInt("mastery", masterY);
        tag.putInt("masterz", masterZ);
        tag.putBoolean("alreadynotifiedgates", alreadyNotifiedGates);
        tag.putInt("lastflash", lastFlash);
        tag.putInt("state", state.getId());
        tag.putBoolean("alreadynotifiedbells", alreadyNotifiedBells);
        tag.putLong("crossingbellstart", crossingBellStartWorldTime);
        tag.putInt("relaybellstop", relayBellStopAfterSeconds);
        tag.putBoolean("alreadynotifiedwigwags", alreadyNotifiedWigWags);
        tag.putBoolean("alreadynotifiedverticalwigwags", alreadyNotifiedVerticalWigWags);
        writePosList(tag, "lamps", crossingLampLocations);
        writePosList(tag, "gate", crossingGateLocations);
        writePosList(tag, "bell", bellLocations);
        writePosList(tag, "wigwags", wigWagLocations);
        writePosList(tag, "verticalwigwags", verticalWigWagLocations);
        writeShuntList(tag, "island", shuntIslandOriginsAndFacing);
        writeShuntList(tag, "border", shuntBorderOriginsAndFacing);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        isMaster = tag.getBoolean("ismaster");
        isPowered = tag.getBoolean("ispowered");
        masterX = tag.getInt("masterx");
        masterY = tag.getInt("mastery");
        masterZ = tag.getInt("masterz");
        alreadyNotifiedGates = tag.getBoolean("alreadynotifiedgates");
        lastFlash = tag.getInt("lastflash");
        state = CrossingLampState.byId(tag.getInt("state"));
        alreadyNotifiedBells = tag.getBoolean("alreadynotifiedbells");
        crossingBellStartWorldTime = tag.contains("crossingbellstart") ? tag.getLong("crossingbellstart") : -1L;
        relayBellStopAfterSeconds = tag.contains("relaybellstop")
                ? tag.getInt("relaybellstop")
                : Config.crossingBellStopAfterSeconds;
        alreadyNotifiedWigWags = tag.getBoolean("alreadynotifiedwigwags");
        alreadyNotifiedVerticalWigWags = tag.getBoolean("alreadynotifiedverticalwigwags");
        readPosList(tag, "lamps", crossingLampLocations);
        readPosList(tag, "gate", crossingGateLocations);
        readPosList(tag, "bell", bellLocations);
        readPosList(tag, "wigwags", wigWagLocations);
        readPosList(tag, "verticalwigwags", verticalWigWagLocations);
        readShuntList(tag, "island", shuntIslandOriginsAndFacing);
        readShuntList(tag, "border", shuntBorderOriginsAndFacing);
    }

    private static void writePosList(CompoundTag tag, String key, List<BlockPos> list) {
        for (int i = 0; i < list.size(); i++) {
            BlockPos pos = list.get(i);
            tag.putIntArray(key + i, new int[] { pos.getX(), pos.getY(), pos.getZ() });
        }
    }

    private static void readPosList(CompoundTag tag, String key, List<BlockPos> list) {
        list.clear();
        int i = 0;
        while (tag.contains(key + i)) {
            int[] blockPos = tag.getIntArray(key + i);
            list.add(new BlockPos(blockPos[0], blockPos[1], blockPos[2]));
            i++;
        }
    }

    private static void writeShuntList(CompoundTag tag, String prefix, List<Tuple<BlockPos, Direction>> list) {
        for (int i = 0; i < list.size(); i++) {
            Tuple<BlockPos, Direction> entry = list.get(i);
            tag.putLong(prefix + "_pos_" + i, entry.getFirst().asLong());
            tag.putInt(prefix + "_facing_" + i, entry.getSecond().get3DDataValue());
        }
    }

    private static void readShuntList(CompoundTag tag, String prefix, List<Tuple<BlockPos, Direction>> list) {
        list.clear();
        int i = 0;
        while (tag.contains(prefix + "_pos_" + i)) {
            BlockPos pos = BlockPos.of(tag.getLong(prefix + "_pos_" + i));
            Direction facing = Direction.from3DDataValue(tag.getInt(prefix + "_facing_" + i));
            list.add(new Tuple<>(pos, facing));
            i++;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("relaybellstop", relayBellStopAfterSeconds);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (tag.contains("relaybellstop")) {
            relayBellStopAfterSeconds = tag.getInt("relaybellstop");
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        handleUpdateTag(pkt.getTag(), registries);
    }

    @Override
    public CompoundTag getClientToServerUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("relaybellstop", relayBellStopAfterSeconds);
        return tag;
    }

    @Override
    public void handleClientToServerUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        if (!tag.contains("relaybellstop") || level == null) {
            return;
        }
        RelayBlockEntity master = getMaster(level);
        if (master != null) {
            master.setRelayBellStopAfterSeconds(tag.getInt("relaybellstop"));
        }
    }
}
