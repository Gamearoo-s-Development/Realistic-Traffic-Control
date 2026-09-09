package com.gamearoosdevelopment.realistictrafficcontrol.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.scanner.ScanRequest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Immersive Railroading / Track API bridge. Uses reflection so RTC compiles without those jars.
 * 1.21 IR routes through Track API ({@code trackapi.lib}) with Minecraft {@link Vec3}; older UMC
 * {@code cam72cam.mod.math.Vec3d} field access is kept as a fallback.
 */
public final class ImmersiveRailroadingHelper {

    private static final String TRACK_API_UTIL = "trackapi.lib.Util";
    private static final String TRACK_API_BLOCK = "trackapi.lib.ITrackBlock";
    private static final String IR_ITRACK = "cam72cam.immersiverailroading.thirdparty.trackapi.ITrack";
    private static final String UMC_WORLD = "cam72cam.mod.world.World";
    private static final String UMC_VEC3D = "cam72cam.mod.math.Vec3d";
    private static final String UMC_ENTITY = "cam72cam.mod.entity.ModdedEntity";
    private static final String IR_STOCK = "cam72cam.immersiverailroading.entity.EntityMoveableRollingStock";
    private static final String IR_NAMESPACE = "immersiverailroading";
    private static final String IR_NAMESPACE_ALT = "immersive_railroading";

    private static boolean loggedFindOriginFailure;
    private static boolean loggedNextFailure;
    private static boolean loggedStockFailure;

    private ImmersiveRailroadingHelper() {
    }

    public static boolean isAvailable() {
        return ModRealisticTrafficControl.IR_INSTALLED || ModRealisticTrafficControl.TRACK_API_INSTALLED;
    }

    public static Vec3 findOrigin(BlockPos currentPos, Level world) {
        if (!isAvailable()) {
            return invalidOrigin();
        }
        try {
            double bestDistSq = Double.MAX_VALUE;
            Vec3 bestCenter = null;
            double refX = currentPos.getX() + 0.5;
            double refY = currentPos.getY() + 0.5;
            double refZ = currentPos.getZ() + 0.5;

            for (int dy = -3; dy <= 2; dy++) {
                BlockPos columnBase = new BlockPos(currentPos.getX(), currentPos.getY() + dy, currentPos.getZ());
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    for (int i = 0; i <= 10; i++) {
                        BlockPos workingPos = columnBase.relative(dir, i);
                        Vec3 working = Vec3.atLowerCornerOf(workingPos);
                        Object tile = getTrack(world, working.add(0.5, 0.0, 0.5), false);
                        if (tile == null) {
                            continue;
                        }
                        Vec3 center = nextPosition(tile, working, Vec3.ZERO);
                        if (center == null) {
                            continue;
                        }
                        double dx = center.x - refX;
                        double dyv = center.y - refY;
                        double dz = center.z - refZ;
                        double distSq = dx * dx + dyv * dyv + dz * dz;
                        if (distSq < bestDistSq) {
                            bestDistSq = distSq;
                            bestCenter = center;
                        }
                    }
                }
            }

            return bestCenter != null ? bestCenter : invalidOrigin();
        } catch (ReflectiveOperationException ex) {
            warnOnce("IR findOrigin failed", ex, () -> loggedFindOriginFailure = true, loggedFindOriginFailure);
            return invalidOrigin();
        }
    }

    public static Vec3 getNextPosition(Vec3 currentPosition, Vec3 motion, Level world) {
        if (!isAvailable()) {
            return currentPosition;
        }
        try {
            BlockPos currentBlockPos = BlockPos.containing(currentPosition);
            Object te = getTrack(world, new Vec3(currentBlockPos.getX(), currentBlockPos.getY(),
                    currentBlockPos.getZ()), false);

            int attempt = 0;
            while (te == null && attempt < 8) {
                switch (attempt) {
                    case 0 -> currentBlockPos = currentBlockPos.above();
                    case 1 -> currentBlockPos = currentBlockPos.below(2);
                    case 2 -> {
                        Direction direction = Direction.getNearest(motion.x, motion.y, motion.z).getClockWise();
                        currentBlockPos = currentBlockPos.relative(direction);
                    }
                    case 3 -> {
                        Direction direction = Direction.getNearest(motion.x, motion.y, motion.z).getClockWise()
                                .getCounterClockWise().getCounterClockWise();
                        currentBlockPos = currentBlockPos.relative(direction, 2);
                    }
                    case 4 -> currentBlockPos = currentBlockPos.above();
                    case 5 -> currentBlockPos = currentBlockPos.below(2);
                    case 6 -> {
                        Direction direction = Direction.getNearest(motion.x, motion.y, motion.z).getClockWise();
                        currentBlockPos = currentBlockPos.relative(direction, 2);
                    }
                    case 7 -> currentBlockPos = currentBlockPos.above(2);
                    default -> {
                    }
                }
                te = getTrack(world, new Vec3(currentBlockPos.getX(), currentBlockPos.getY(),
                        currentBlockPos.getZ()), false);
                attempt++;
            }

            if (te == null) {
                return currentPosition;
            }

            Vec3 next = nextPosition(te, currentPosition, motion);
            return next != null ? next : currentPosition;
        } catch (ReflectiveOperationException ex) {
            warnOnce("IR getNextPosition failed", ex, () -> loggedNextFailure = true, loggedNextFailure);
            return currentPosition;
        }
    }

    public static Tuple<UUID, Vec3> getStockNearby(Vec3 currentPosition, Level world) {
        if (!isAvailable()) {
            return null;
        }
        try {
            BlockPos currentBlockPos = BlockPos.containing(currentPosition);
            BlockPos min = currentBlockPos.below(2).south(4).west(4);
            BlockPos max = currentBlockPos.above(4).east(4).north(4);
            AABB bb = new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);
            return firstStockIn(world, bb);
        } catch (RuntimeException ex) {
            warnOnce("IR getStockNearby failed", ex, () -> loggedStockFailure = true, loggedStockFailure);
            return null;
        }
    }

    /**
     * Create-style corridor scan for IR rolling stock, so relays work even when track walking cannot
     * resolve a next position.
     */
    public static TrainScanResult scanForTrain(ScanRequest request, int maxDistance, Level world) {
        if (!isAvailable()) {
            return TrainScanResult.NONE;
        }
        try {
            AABB volume = buildScanVolume(request, maxDistance);
            boolean found = false;
            boolean towards = false;
            for (Entity entity : world.getEntities((Entity) null, volume, e -> true)) {
                Tuple<UUID, Vec3> stock = stockFromEntity(entity);
                if (stock == null) {
                    continue;
                }
                if (!isInRequestCorridor(entity.position(), request, maxDistance)) {
                    continue;
                }
                found = true;
                if (isMovingTowardsDestination(entity.position(), stock.getSecond(), request)) {
                    towards = true;
                }
            }
            return found ? new TrainScanResult(true, towards) : TrainScanResult.NONE;
        } catch (RuntimeException ex) {
            warnOnce("IR scanForTrain failed", ex, () -> loggedStockFailure = true, loggedStockFailure);
            return TrainScanResult.NONE;
        }
    }

    public record TrainScanResult(boolean trainFound, boolean movingTowardsDestination) {
        public static final TrainScanResult NONE = new TrainScanResult(false, false);
    }

    private static Tuple<UUID, Vec3> firstStockIn(Level world, AABB bb) {
        for (Entity entity : world.getEntitiesOfClass(Entity.class, bb, e -> true)) {
            Tuple<UUID, Vec3> stock = stockFromEntity(entity);
            if (stock == null) {
                continue;
            }
            return stock;
        }
        return null;
    }

    private static Tuple<UUID, Vec3> stockFromEntity(Entity entity) {
        if (entity == null) {
            return null;
        }
        Class<?> moddedEntityClass = optionalClass(UMC_ENTITY);
        Class<?> stockClass = optionalClass(IR_STOCK);
        Object self = entity;
        if (moddedEntityClass != null && moddedEntityClass.isInstance(entity)) {
            Object wrapped = tryInvoke(entity, "getSelf");
            if (wrapped != null) {
                self = wrapped;
            }
        }
        boolean rollingStock = stockClass != null && stockClass.isInstance(self);
        if (!rollingStock) {
            rollingStock = isRollingStockName(self.getClass().getName()) || isIrEntityType(entity);
        }
        if (!rollingStock) {
            return null;
        }
        UUID uuid = entity.getUUID();
        Object id = tryInvoke(self, "getUUID");
        if (id instanceof UUID parsed) {
            uuid = parsed;
        }
        Vec3 velocity = toMcVec3(tryInvoke(self, "getVelocity"));
        if (velocity == null) {
            velocity = entity.getDeltaMovement();
        }
        return new Tuple<>(uuid, velocity);
    }

    private static boolean isRollingStockName(String className) {
        return className.contains("EntityMoveableRollingStock")
                || className.contains("EntityRidableRollingStock")
                || className.contains("EntityCoupleableRollingStock")
                || className.contains("EntityBuildableRollingStock")
                || className.contains("EntityRollingStock")
                || className.contains("Locomotive")
                || className.contains("FreightCar")
                || className.contains("PassengerCar")
                || className.contains("Tender")
                || className.contains("Caboose");
    }

    private static boolean isIrEntityType(Entity entity) {
        var key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (key == null) {
            return false;
        }
        String namespace = key.getNamespace();
        if (!IR_NAMESPACE.equals(namespace) && !IR_NAMESPACE_ALT.equals(namespace) && !"trackapi".equals(namespace)) {
            return false;
        }
        String path = key.getPath();
        return !path.contains("seat") && !path.contains("particle") && !path.contains("dummy");
    }

    private static AABB buildScanVolume(ScanRequest request, int maxDistance) {
        Vec3 start = Vec3.atCenterOf(request.getStartingPos());
        AABB volume = new AABB(start, start);
        if (request.getEndingPositions().isEmpty()) {
            Direction direction = request.getStartDirection();
            Vec3 end = start.add(
                    direction.getStepX() * maxDistance,
                    direction.getStepY() * maxDistance,
                    direction.getStepZ() * maxDistance);
            volume = volume.minmax(new AABB(end, end));
        } else {
            for (BlockPos destination : request.getEndingPositions()) {
                volume = volume.minmax(new AABB(Vec3.atCenterOf(destination), Vec3.atCenterOf(destination)));
            }
        }
        return volume.inflate(10.0);
    }

    private static boolean isInRequestCorridor(Vec3 position, ScanRequest request, int maxDistance) {
        Vec3 start = Vec3.atCenterOf(request.getStartingPos());
        Vec3 fromStart = position.subtract(start);
        Direction direction = request.getStartDirection();
        Vec3 forward = new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
        if (fromStart.dot(forward) < -10.0 || fromStart.lengthSqr() > square(maxDistance + 10.0)) {
            return false;
        }
        if (request.getEndingPositions().isEmpty()) {
            double forwardDistance = fromStart.dot(forward);
            Vec3 nearest = start.add(forward.scale(Math.max(0.0, Math.min(maxDistance, forwardDistance))));
            return position.distanceToSqr(nearest) <= square(10.0);
        }
        for (BlockPos destination : request.getEndingPositions()) {
            if (distanceToSegmentSqr(position, start, Vec3.atCenterOf(destination)) <= square(10.0)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMovingTowardsDestination(Vec3 position, Vec3 velocity, ScanRequest request) {
        if (request.getEndingPositions().isEmpty() || velocity == null || velocity.lengthSqr() < 1.0E-6) {
            return false;
        }
        BlockPos nearest = request.getEndingPositions().get(0);
        double nearestDistance = position.distanceToSqr(Vec3.atCenterOf(nearest));
        for (int i = 1; i < request.getEndingPositions().size(); i++) {
            BlockPos candidate = request.getEndingPositions().get(i);
            double distance = position.distanceToSqr(Vec3.atCenterOf(candidate));
            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        Vec3 toDestination = Vec3.atCenterOf(nearest).subtract(position);
        return toDestination.lengthSqr() > 1.0E-6 && velocity.dot(toDestination) > 0.0;
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

    private static Vec3 invalidOrigin() {
        return new Vec3(0, -1, 0);
    }

    private static Vec3 nextPosition(Object tile, Vec3 current, Vec3 motion) {
        if (tile instanceof BlockTrack track) {
            Object result = tryInvoke(track.block(), "getNextPosition", track.world(), track.pos(), current, motion);
            return toMcVec3(result);
        }
        Object result = tryInvoke(tile, "getNextPosition", current, motion);
        if (result == null) {
            Object camPos = newCamVec3d(current);
            Object camMotion = newCamVec3d(motion);
            if (camPos != null && camMotion != null) {
                result = tryInvoke(tile, "getNextPosition", camPos, camMotion);
            }
        }
        return toMcVec3(result);
    }

    private record BlockTrack(Level world, BlockPos pos, Object block) {
    }

    private static Object getTrack(Level world, Vec3 pos, boolean acceptMinecraftRails)
            throws ReflectiveOperationException {
        Object trackApi = tryStatic(TRACK_API_UTIL, "getTileEntity", world, pos, acceptMinecraftRails);
        if (trackApi != null) {
            return trackApi;
        }
        BlockPos blockPos = BlockPos.containing(pos);
        Object trackBlock = world.getBlockState(blockPos).getBlock();
        Class<?> trackBlockClass = optionalClass(TRACK_API_BLOCK);
        if (trackBlockClass != null && trackBlockClass.isInstance(trackBlock)) {
            return new BlockTrack(world, blockPos, trackBlock);
        }
        Object camWorld = tryStatic(UMC_WORLD, "get", world);
        Object camPos = newCamVec3d(pos);
        if (camWorld == null || camPos == null) {
            return null;
        }
        return tryStatic(IR_ITRACK, "get", camWorld, camPos, false);
    }

    private static Object newCamVec3d(Vec3 vec) {
        return newCamVec3d(vec.x, vec.y, vec.z);
    }

    private static Object newCamVec3d(double x, double y, double z) {
        try {
            Class<?> clazz = Class.forName(UMC_VEC3D);
            return clazz.getConstructor(double.class, double.class, double.class).newInstance(x, y, z);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private static Vec3 toMcVec3(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Vec3 vec) {
            return vec;
        }
        if (value instanceof BlockPos pos) {
            return Vec3.atCenterOf(pos);
        }
        Object internal = tryInvoke(value, "internal");
        if (internal instanceof Vec3 vec) {
            return vec;
        }
        if (internal instanceof BlockPos pos) {
            return Vec3.atCenterOf(pos);
        }
        Double x = vecComponent(value, "x");
        Double y = vecComponent(value, "y");
        Double z = vecComponent(value, "z");
        if (x != null && y != null && z != null) {
            return new Vec3(x, y, z);
        }
        return null;
    }

    private static Double vecComponent(Object vec, String name) {
        try {
            Field field = vec.getClass().getField(name);
            Object value = field.get(vec);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
        } catch (ReflectiveOperationException ignored) {
        }
        Object invoked = tryInvoke(vec, name);
        if (invoked instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }

    private static Class<?> optionalClass(String name) {
        try {
            return Class.forName(name, false, ImmersiveRailroadingHelper.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError ex) {
            return null;
        }
    }

    private static Object tryStatic(String className, String method, Object... args) {
        try {
            Class<?> clazz = Class.forName(className);
            return invoke(clazz, null, method, args);
        } catch (ReflectiveOperationException | LinkageError ex) {
            return null;
        }
    }

    private static Object tryInvoke(Object target, String method, Object... args) {
        if (target == null) {
            return null;
        }
        try {
            return invoke(target, method, args);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private static Object invoke(Object target, String method, Object... args) throws ReflectiveOperationException {
        return invoke(target.getClass(), target, method, args);
    }

    private static Object invoke(Class<?> clazz, Object target, String method, Object... args)
            throws ReflectiveOperationException {
        for (Method candidate : clazz.getMethods()) {
            if (!candidate.getName().equals(method) || candidate.getParameterCount() != args.length) {
                continue;
            }
            boolean match = true;
            Class<?>[] paramTypes = candidate.getParameterTypes();
            for (int i = 0; i < args.length; i++) {
                if (args[i] == null) {
                    continue;
                }
                if (!wrap(paramTypes[i]).isInstance(args[i]) && !isPrimitiveCompatible(paramTypes[i], args[i])) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return candidate.invoke(target, args);
            }
        }
        throw new NoSuchMethodException(clazz.getName() + "." + method);
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        return type;
    }

    private static boolean isPrimitiveCompatible(Class<?> paramType, Object arg) {
        if (paramType == boolean.class && arg instanceof Boolean) {
            return true;
        }
        if (paramType == int.class && arg instanceof Integer) {
            return true;
        }
        if (paramType == long.class && arg instanceof Long) {
            return true;
        }
        if (paramType == double.class && arg instanceof Number) {
            return true;
        }
        if (paramType == float.class && arg instanceof Number) {
            return true;
        }
        return paramType == arg.getClass();
    }

    private static void warnOnce(String message, Exception ex, Runnable mark, boolean already) {
        if (already) {
            return;
        }
        mark.run();
        ModRealisticTrafficControl.LOGGER.warn(message, ex);
    }
}
