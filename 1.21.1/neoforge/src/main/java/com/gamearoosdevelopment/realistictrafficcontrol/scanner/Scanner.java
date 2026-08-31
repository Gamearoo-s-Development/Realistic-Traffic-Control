package com.gamearoosdevelopment.realistictrafficcontrol.scanner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import com.gamearoosdevelopment.realistictrafficcontrol.Config;
import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.compat.CreateCompat;
import com.gamearoosdevelopment.realistictrafficcontrol.util.ImmersiveRailroadingHelper;
import com.gamearoosdevelopment.realistictrafficcontrol.util.Tuple;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Per-dimension train scanner used when Immersive Railroading or Create is installed.
 */
public class Scanner {
    private static final UUID ISLAND_REQUEST = UUID.fromString("da2e3487-9fe6-4369-80bc-4b5ce40f0530");
    private static final UUID BORDER_REQUEST = UUID.fromString("c4ba0fb7-3df0-4c18-9edf-491d825899d9");

    private final ScannerData data;
    public static final Map<ResourceKey<Level>, Scanner> scannersByWorld = new HashMap<>();

    private final ScanSession[] scansInProgress;
    private final HashSet<IScannerSubscriber> requestsHandledThisTick = new HashSet<>();
    private int lastIndex = 0;

    public Scanner(ServerLevel level) {
        data = ScannerData.get(level);
        scansInProgress = new ScanSession[Config.parallelScans];
        for (int i = 0; i < Config.parallelScans; i++) {
            scansInProgress[i] = new ScanSession();
        }
    }

    public void subscribe(BlockEntity subscriber) {
        if (subscriber instanceof IScannerSubscriber) {
            data.addSubscriber(subscriber.getBlockPos());
        }
    }

    public void tick(ServerLevel world) {
        if (!ModRealisticTrafficControl.IR_INSTALLED && !ModRealisticTrafficControl.CREATE_INSTALLED) {
            return;
        }
        try {
            if (data.getSubscribers().isEmpty()) {
                return;
            }

            for (ScanSession scanSession : scansInProgress) {
                if (scanSession.getScanSubscriber() == null) {
                    tryFindNextSubscriber(scanSession, world);
                }

                ScanRequest request = scanSession.getScanRequest();
                if (request == null) {
                    if (scanSession.getScanSubscriber() != null) {
                        scanSession.getScanSubscriber().onScanRequestsCompleted();
                        scanSession.setScanSubscriber(null);
                    }
                    continue;
                }

                if (scanSession.getBlocksScannedThisSession() == 0
                        && ModRealisticTrafficControl.CREATE_INSTALLED) {
                    CreateCompat.TrainScanResult createResult =
                            CreateCompat.scanForTrain(request, maxDistance(request), world);
                    if (createResult.trainFound()) {
                        scanSession.setFoundTrain(true);
                    }
                    if (createResult.movingTowardsDestination()) {
                        scanSession.setTrainMovingTowardsDestination(true);
                    }

                    if (!ModRealisticTrafficControl.IR_INSTALLED) {
                        completeRequest(scanSession, request, false);
                        continue;
                    }
                }

                int maxBlocksThisTick = maxBlocksPerTick(request);
                while (scanSession.getBlocksScannedThisSession() < maxBlocksThisTick) {
                    Vec3 lastPosition = scanSession.getLastPosition();
                    Vec3 motion = scanSession.getMotion();
                    if (lastPosition == null) {
                        lastPosition = Vec3.atCenterOf(request.getStartingPos());
                        motion = new Vec3(request.getStartDirection().getStepX(),
                                request.getStartDirection().getStepY(),
                                request.getStartDirection().getStepZ());
                        scanSession.setLastPosition(lastPosition);
                        scanSession.setMotion(motion);
                    }

                    Tuple<Boolean, Boolean> check = checkPosition(lastPosition, motion, world, request);
                    if (Boolean.TRUE.equals(check.getFirst())) {
                        scanSession.setFoundTrain(true);
                        if (Boolean.TRUE.equals(check.getSecond())) {
                            scanSession.setTrainMovingTowardsDestination(true);
                        }
                    }

                    for (BlockPos endingPos : request.getEndingPositions()) {
                        AABB endingBB = new AABB(endingPos).inflate(-1, -1, -1).expandTowards(1, 1, 1);
                        if (endingBB.contains(lastPosition)) {
                            completeRequest(scanSession, request, false);
                            request = scanSession.getScanRequest();
                            break;
                        }
                    }

                    if (scanSession.getLastPosition() == null) {
                        break;
                    }

                    if (request == null) {
                        break;
                    }

                    if (scanSession.getBlocksScannedThisSession() >= maxDistance(request)) {
                        completeRequest(scanSession, request, true);
                        request = scanSession.getScanRequest();
                        if (request == null) {
                            break;
                        }
                        break;
                    }

                    Vec3 nextPosition = ImmersiveRailroadingHelper.getNextPosition(lastPosition, motion, world);
                    if (nextPosition.distanceToSqr(lastPosition) < 1.0E-6) {
                        completeRequest(scanSession, request, true);
                        request = scanSession.getScanRequest();
                        if (request == null) {
                            break;
                        }
                        break;
                    }

                    scanSession.setLastPosition(nextPosition);
                    scanSession.addBlockScannedThisSession();
                }

                if (scanSession.getScanRequest() == null && scanSession.getScanSubscriber() != null) {
                    scanSession.getScanSubscriber().onScanRequestsCompleted();
                    tryFindNextSubscriber(scanSession, world);
                }
            }

            requestsHandledThisTick.clear();
        } catch (Exception ex) {
            ModRealisticTrafficControl.LOGGER.error("Error in scanner", ex);
        }
    }

    private void completeRequest(ScanSession scanSession, ScanRequest request, boolean timedOut) {
        IScannerSubscriber subscriber = scanSession.getScanSubscriber();
        if (subscriber != null) {
            ScanCompleteData result = new ScanCompleteData(request, timedOut, scanSession.isFoundTrain(),
                    scanSession.isTrainMovingTowardsDestination());
            subscriber.onScanComplete(result);
            if (!result.getContinueScanningForTileEntity()) {
                scanSession.setScanSubscriber(null);
                return;
            }
        }
        scanSession.popRequest();
    }

    private static int maxBlocksPerTick(ScanRequest request) {
        return request.getRequestID().equals(BORDER_REQUEST) ? Config.borderTick : 1;
    }

    private static int maxDistance(ScanRequest request) {
        return request.getRequestID().equals(BORDER_REQUEST) ? Config.borderTimeout : Config.islandTimeout;
    }

    private Tuple<Boolean, Boolean> checkPosition(Vec3 position, Vec3 motion, Level world, ScanRequest request) {
        Tuple<java.util.UUID, Vec3> stock = ImmersiveRailroadingHelper.getStockNearby(position, world);
        if (stock == null) {
            return new Tuple<>(false, false);
        }
        boolean towardsDestination = false;
        if (!request.getEndingPositions().isEmpty()) {
            BlockPos dest = request.getEndingPositions().get(0);
            Vec3 destVec = Vec3.atCenterOf(dest);
            Vec3 toDest = destVec.subtract(position).normalize();
            towardsDestination = motion.dot(toDest) > 0;
        }
        return new Tuple<>(true, towardsDestination);
    }

    private void tryFindNextSubscriber(ScanSession scan, ServerLevel world) {
        lastIndex++;
        if (lastIndex >= data.getSubscribers().size()) {
            lastIndex = 0;
        }

        HashSet<BlockPos> invalidScanSubscribers = new HashSet<>();
        IScannerSubscriber thisScanSubscriber = null;

        do {
            if (data.getSubscribers().isEmpty()) {
                break;
            }
            BlockPos subscriberPos = data.getSubscribers().get(lastIndex);
            if (world.isLoaded(subscriberPos)) {
                BlockEntity te = world.getBlockEntity(subscriberPos);

                if (te == null) {
                    invalidScanSubscribers.add(subscriberPos);
                    lastIndex++;
                    continue;
                }

                if (te instanceof IScannerSubscriber subscriber) {
                    thisScanSubscriber = subscriber;

                    if (!thisScanSubscriber.getScanRequests().isEmpty()) {
                        final IScannerSubscriber finalThisScanSubscriber = thisScanSubscriber;
                        if (!requestsHandledThisTick.add(thisScanSubscriber)
                                || Arrays.stream(scansInProgress)
                                        .anyMatch(ss -> ss.getScanSubscriber() == finalThisScanSubscriber)) {
                            thisScanSubscriber = null;
                        }
                    }
                }
            }

            if (thisScanSubscriber == null) {
                lastIndex++;
            }
        } while (lastIndex < data.getSubscribers().size() && thisScanSubscriber == null);

        for (BlockPos invalidSub : invalidScanSubscribers) {
            data.removeSubscriber(invalidSub);
        }

        scan.setScanSubscriber(thisScanSubscriber);
    }

    private static class ScanSession {
        private IScannerSubscriber scanSubscriber = null;
        private Queue<ScanRequest> scanRequestsToDo;
        private int blocksScannedThisSession = 0;
        private Vec3 lastPosition = null;
        private Vec3 motion = null;
        private boolean foundTrain = false;
        private boolean trainMovingTowardsDestination = false;

        public IScannerSubscriber getScanSubscriber() {
            return scanSubscriber;
        }

        public void setScanSubscriber(IScannerSubscriber scanSubscriber) {
            this.scanSubscriber = scanSubscriber;

            if (this.scanSubscriber != null) {
                scanRequestsToDo = new LinkedList<>(scanSubscriber.getScanRequests());
            } else {
                scanRequestsToDo = new LinkedList<>();
            }

            lastPosition = null;
            motion = null;
            blocksScannedThisSession = 0;
            foundTrain = false;
            trainMovingTowardsDestination = false;
        }

        public ScanRequest getScanRequest() {
            return scanRequestsToDo == null ? null : scanRequestsToDo.peek();
        }

        public void popRequest() {
            if (scanRequestsToDo != null) {
                scanRequestsToDo.poll();
            }
            lastPosition = null;
            motion = null;
            blocksScannedThisSession = 0;
            foundTrain = false;
            trainMovingTowardsDestination = false;
        }

        public void addBlockScannedThisSession() {
            blocksScannedThisSession++;
        }

        public int getBlocksScannedThisSession() {
            return blocksScannedThisSession;
        }

        public Vec3 getLastPosition() {
            return lastPosition;
        }

        public void setLastPosition(Vec3 lastPosition) {
            this.lastPosition = lastPosition;
        }

        public Vec3 getMotion() {
            return motion;
        }

        public void setMotion(Vec3 motion) {
            this.motion = motion;
        }

        public boolean isFoundTrain() {
            return foundTrain;
        }

        public void setFoundTrain(boolean foundTrain) {
            this.foundTrain = foundTrain;
        }

        public boolean isTrainMovingTowardsDestination() {
            return trainMovingTowardsDestination;
        }

        public void setTrainMovingTowardsDestination(boolean trainMovingTowardsDestination) {
            this.trainMovingTowardsDestination = trainMovingTowardsDestination;
        }
    }
}
