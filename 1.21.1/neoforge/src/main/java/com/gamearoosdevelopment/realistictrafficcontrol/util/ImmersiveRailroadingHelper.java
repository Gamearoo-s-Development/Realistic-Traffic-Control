package com.gamearoosdevelopment.realistictrafficcontrol.util;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Port of 1.12.2 {@code ImmersiveRailroadingHelper}. Uses reflection so the mod compiles without the IR
 * jar in {@code deps/}; calls are no-ops when Immersive Railroading is not installed.
 */
public final class ImmersiveRailroadingHelper {

    private ImmersiveRailroadingHelper() {
    }

    public static Vec3 findOrigin(BlockPos currentPos, Level world) {
        if (!ModRealisticTrafficControl.IR_INSTALLED) {
            return invalidOrigin();
        }
        try {
            double bestDistSq = Double.MAX_VALUE;
            Vec3 bestCenter = null;
            double refX = currentPos.getX() + 0.5;
            double refY = currentPos.getY() + 0.5;
            double refZ = currentPos.getZ() + 0.5;

            Object camWorld = invokeStatic("cam72cam.mod.world.World", "get", world);
            if (camWorld == null) {
                return invalidOrigin();
            }

            for (int dy = -3; dy <= 2; dy++) {
                BlockPos columnBase = new BlockPos(currentPos.getX(), currentPos.getY() + dy, currentPos.getZ());
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    for (int i = 0; i <= 10; i++) {
                        BlockPos workingPos = columnBase.relative(dir, i);
                        Object workingPosVec3d = newCamVec3d(workingPos.getX(), workingPos.getY(), workingPos.getZ());
                        Object tile = invokeStatic("cam72cam.immersiverailroading.thirdparty.trackapi.ITrack", "get",
                                camWorld, workingPosVec3d, false);
                        if (tile == null) {
                            continue;
                        }
                        Object center = invoke(tile, "getNextPosition", workingPosVec3d, newCamVec3d(0, 0, 0));
                        if (center == null) {
                            continue;
                        }
                        double cx = ((Number) invoke(center, "x")).doubleValue();
                        double cy = ((Number) invoke(center, "y")).doubleValue();
                        double cz = ((Number) invoke(center, "z")).doubleValue();
                        double dx = cx - refX;
                        double dyv = cy - refY;
                        double dz = cz - refZ;
                        double distSq = dx * dx + dyv * dyv + dz * dz;
                        if (distSq < bestDistSq) {
                            bestDistSq = distSq;
                            bestCenter = new Vec3(cx, cy, cz);
                        }
                    }
                }
            }

            return bestCenter != null ? bestCenter : invalidOrigin();
        } catch (ReflectiveOperationException ex) {
            ModRealisticTrafficControl.LOGGER.debug("IR findOrigin failed", ex);
            return invalidOrigin();
        }
    }

    public static Vec3 getNextPosition(Vec3 currentPosition, Vec3 motion, Level world) {
        if (!ModRealisticTrafficControl.IR_INSTALLED) {
            return currentPosition;
        }
        try {
            BlockPos currentBlockPos = BlockPos.containing(currentPosition);
            Object camWorld = invokeStatic("cam72cam.mod.world.World", "get", world);
            if (camWorld == null) {
                return currentPosition;
            }

            Object te = invokeStatic("cam72cam.immersiverailroading.thirdparty.trackapi.ITrack", "get", camWorld,
                    newCamVec3d(currentBlockPos.getX(), currentBlockPos.getY(), currentBlockPos.getZ()), false);

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
                te = invokeStatic("cam72cam.immersiverailroading.thirdparty.trackapi.ITrack", "get", camWorld,
                        newCamVec3d(currentBlockPos.getX(), currentBlockPos.getY(), currentBlockPos.getZ()), false);
                attempt++;
            }

            if (te == null) {
                return currentPosition;
            }

            Object retVal = invoke(te, "getNextPosition", newCamVec3d(currentPosition), newCamVec3d(motion));
            if (retVal == null) {
                return currentPosition;
            }
            Object internal = invoke(retVal, "internal");
            return internal instanceof Vec3 vec ? vec : currentPosition;
        } catch (ReflectiveOperationException ex) {
            ModRealisticTrafficControl.LOGGER.debug("IR getNextPosition failed", ex);
            return currentPosition;
        }
    }

    public static Tuple<UUID, Vec3> getStockNearby(Vec3 currentPosition, Level world) {
        if (!ModRealisticTrafficControl.IR_INSTALLED) {
            return null;
        }
        try {
            BlockPos currentBlockPos = BlockPos.containing(currentPosition);
            BlockPos min = currentBlockPos.below().south(2).west(2);
            BlockPos max = currentBlockPos.above(3).east(2).north(2);
            AABB bb = new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);

            Class<?> moddedEntityClass = Class.forName("cam72cam.mod.entity.ModdedEntity");
            Class<?> stockClass = Class.forName("cam72cam.immersiverailroading.entity.EntityMoveableRollingStock");

            for (var entity : world.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, bb, e -> true)) {
                if (!moddedEntityClass.isInstance(entity)) {
                    continue;
                }
                Object self = invoke(entity, "getSelf");
                if (!stockClass.isInstance(self)) {
                    continue;
                }
                Object blockPos = invoke(self, "getBlockPosition");
                Object internalPos = invoke(blockPos, "internal");
                if (!(internalPos instanceof Vec3 stockPos) || !bb.contains(stockPos)) {
                    continue;
                }
                Object velocity = invoke(self, "getVelocity");
                Object internalVel = invoke(velocity, "internal");
                UUID uuid = (UUID) invoke(self, "getUUID");
                return new Tuple<>(uuid, internalVel instanceof Vec3 vel ? vel : Vec3.ZERO);
            }
            return null;
        } catch (ReflectiveOperationException ex) {
            ModRealisticTrafficControl.LOGGER.debug("IR getStockNearby failed", ex);
            return null;
        }
    }

    private static Vec3 invalidOrigin() {
        return new Vec3(0, -1, 0);
    }

    private static Object newCamVec3d(double x, double y, double z) throws ReflectiveOperationException {
        return Class.forName("cam72cam.mod.math.Vec3d").getConstructor(double.class, double.class, double.class)
                .newInstance(x, y, z);
    }

    private static Object newCamVec3d(Vec3 vec) throws ReflectiveOperationException {
        return newCamVec3d(vec.x, vec.y, vec.z);
    }

    private static Object invokeStatic(String className, String method, Object... args) throws ReflectiveOperationException {
        Class<?> clazz = Class.forName(className);
        return invoke(clazz, null, method, args);
    }

    private static Object invoke(Object target, String method, Object... args) throws ReflectiveOperationException {
        return invoke(target.getClass(), target, method, args);
    }

    private static Object invoke(Class<?> clazz, Object target, String method, Object... args)
            throws ReflectiveOperationException {
        for (var m : clazz.getMethods()) {
            if (!m.getName().equals(method) || m.getParameterCount() != args.length) {
                continue;
            }
            boolean match = true;
            Class<?>[] paramTypes = m.getParameterTypes();
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
                return m.invoke(target, args);
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
        return Objects.equals(paramType, arg.getClass());
    }
}
