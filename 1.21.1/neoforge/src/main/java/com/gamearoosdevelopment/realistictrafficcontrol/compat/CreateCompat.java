package com.gamearoosdevelopment.realistictrafficcontrol.compat;

import java.util.List;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.scanner.ScanRequest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Optional Create integration. This bridge intentionally has no compile-time references to Create
 * classes, so loading RTC does not ask the JVM to resolve Create when the mod is absent.
 */
public final class CreateCompat {
    private static final String CONTRAPTION_ENTITY =
            "com.simibubi.create.content.contraptions.AbstractContraptionEntity";
    private static final String CARRIAGE_ENTITY =
            "com.simibubi.create.content.trains.entity.CarriageContraptionEntity";
    private static final String TRACK_BLOCK =
            "com.simibubi.create.content.trains.track.ITrackBlock";

    private static volatile Class<?> contraptionEntityClass;
    private static volatile Class<?> carriageEntityClass;
    private static volatile Class<?> trackBlockClass;

    private CreateCompat() {
    }

    public static boolean isContraption(Entity entity) {
        Class<?> type = resolve(CONTRAPTION_ENTITY, contraptionEntityClass);
        if (type != null) {
            contraptionEntityClass = type;
        }
        return type != null && type.isInstance(entity);
    }

    /**
     * Finds the nearest Create track block around a newly placed RTC shunt.
     */
    public static Vec3 findTrackOrigin(BlockPos shuntPos, Level level) {
        Class<?> type = resolve(TRACK_BLOCK, trackBlockClass);
        if (type == null) {
            return null;
        }
        trackBlockClass = type;

        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dy = -3; dy <= 2; dy++) {
            for (int dx = -10; dx <= 10; dx++) {
                for (int dz = -10; dz <= 10; dz++) {
                    BlockPos candidate = shuntPos.offset(dx, dy, dz);
                    Block block = level.getBlockState(candidate).getBlock();
                    if (!type.isInstance(block)) {
                        continue;
                    }
                    double distance = candidate.distSqr(shuntPos);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = candidate.immutable();
                    }
                }
            }
        }
        return best == null ? null : Vec3.atCenterOf(best);
    }

    /**
     * Searches the corridor represented by a relay scan request for Create train carriages.
     */
    public static TrainScanResult scanForTrain(ScanRequest request, int maxDistance, Level level) {
        Class<?> type = resolve(CARRIAGE_ENTITY, carriageEntityClass);
        if (type == null) {
            return TrainScanResult.NONE;
        }
        carriageEntityClass = type;

        List<BlockPos> destinations = request.getEndingPositions();
        AABB scanVolume = buildScanVolume(request, destinations, maxDistance);
        boolean found = false;
        boolean movingTowardsDestination = false;

        for (Entity entity : level.getEntities((Entity) null, scanVolume,
                candidate -> type.isInstance(candidate) && isInRequestCorridor(candidate, request, maxDistance))) {
            found = true;
            if (isMovingTowardsDestination(entity, destinations)) {
                movingTowardsDestination = true;
            }
        }
        return found ? new TrainScanResult(true, movingTowardsDestination) : TrainScanResult.NONE;
    }

    private static AABB buildScanVolume(ScanRequest request, List<BlockPos> destinations, int maxDistance) {
        Vec3 start = Vec3.atCenterOf(request.getStartingPos());
        AABB volume = new AABB(start, start);
        if (destinations.isEmpty()) {
            Direction direction = request.getStartDirection();
            Vec3 end = start.add(
                    direction.getStepX() * maxDistance,
                    direction.getStepY() * maxDistance,
                    direction.getStepZ() * maxDistance);
            volume = volume.minmax(new AABB(end, end));
        } else {
            for (BlockPos destination : destinations) {
                Vec3 end = Vec3.atCenterOf(destination);
                volume = volume.minmax(new AABB(end, end));
            }
        }
        // Create carriage entity positions are near their anchors, not necessarily at the body edge.
        return volume.inflate(6.0);
    }

    private static boolean isInRequestCorridor(Entity entity, ScanRequest request, int maxDistance) {
        Vec3 start = Vec3.atCenterOf(request.getStartingPos());
        Vec3 position = entity.position();
        Vec3 fromStart = position.subtract(start);
        Direction direction = request.getStartDirection();
        Vec3 forward = new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
        if (fromStart.dot(forward) < -6.0 || fromStart.lengthSqr() > square(maxDistance + 6.0)) {
            return false;
        }

        List<BlockPos> destinations = request.getEndingPositions();
        if (destinations.isEmpty()) {
            double forwardDistance = fromStart.dot(forward);
            Vec3 nearest = start.add(forward.scale(Math.max(0.0, Math.min(maxDistance, forwardDistance))));
            return position.distanceToSqr(nearest) <= square(6.0);
        }

        for (BlockPos destination : destinations) {
            if (distanceToSegmentSqr(position, start, Vec3.atCenterOf(destination)) <= square(6.0)) {
                return true;
            }
        }
        return false;
    }

    private static double distanceToSegmentSqr(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSqr = segment.lengthSqr();
        if (lengthSqr < 1.0E-6) {
            return point.distanceToSqr(start);
        }
        double projection = Math.max(0.0, Math.min(1.0, point.subtract(start).dot(segment) / lengthSqr));
        return point.distanceToSqr(start.add(segment.scale(projection)));
    }

    private static double square(double value) {
        return value * value;
    }

    private static boolean isMovingTowardsDestination(Entity entity, List<BlockPos> destinations) {
        if (destinations.isEmpty()) {
            return false;
        }

        Vec3 movement = entity.getDeltaMovement();
        if (movement.lengthSqr() < 1.0E-6) {
            movement = entity.position().subtract(entity.xOld, entity.yOld, entity.zOld);
        }
        if (movement.lengthSqr() < 1.0E-6) {
            return false;
        }

        Vec3 position = entity.position();
        BlockPos nearest = destinations.get(0);
        double nearestDistance = position.distanceToSqr(Vec3.atCenterOf(nearest));
        for (int i = 1; i < destinations.size(); i++) {
            BlockPos candidate = destinations.get(i);
            double distance = position.distanceToSqr(Vec3.atCenterOf(candidate));
            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }

        Vec3 toDestination = Vec3.atCenterOf(nearest).subtract(position);
        return toDestination.lengthSqr() > 1.0E-6 && movement.dot(toDestination) > 0.0;
    }

    private static Class<?> resolve(String name, Class<?> cached) {
        if (!ModRealisticTrafficControl.CREATE_INSTALLED) {
            return null;
        }
        if (cached != null) {
            return cached;
        }
        try {
            return Class.forName(name, false, CreateCompat.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError ex) {
            ModRealisticTrafficControl.LOGGER.debug("Create compatibility class unavailable: {}", name, ex);
            return null;
        }
    }

    public record TrainScanResult(boolean trainFound, boolean movingTowardsDestination) {
        private static final TrainScanResult NONE = new TrainScanResult(false, false);
    }
}
