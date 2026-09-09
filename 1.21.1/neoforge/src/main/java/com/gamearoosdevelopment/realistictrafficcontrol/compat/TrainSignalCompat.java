package com.gamearoosdevelopment.realistictrafficcontrol.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Create / Extended Signals / redstone "ghost signal" for distant crossing approaches.
 * A powered or red train signal next to a shunt is treated as occupancy so relays do not
 * have to scan hundreds of blocks of rolling stock.
 */
public final class TrainSignalCompat {

    private static final String CREATE_SIGNAL_BE =
            "com.simibubi.create.content.trains.signal.SignalBlockEntity";
    private static final int SEARCH = 2;

    private TrainSignalCompat() {
    }

    public static boolean isOccupied(Level level, BlockPos origin) {
        return isOccupied(level, origin, null);
    }

    /** Island / cutoff: the section itself is occupied. */
    public static boolean isOccupied(Level level, BlockPos origin, Direction facing) {
        return isOccupied(level, origin, facing, false);
    }

    /** Border / approach: red, yellow, reserved, or wired redstone. */
    public static boolean isApproach(Level level, BlockPos origin, Direction facing) {
        return isOccupied(level, origin, facing, true);
    }

    private static boolean isOccupied(Level level, BlockPos origin, Direction facing, boolean approach) {
        if (level == null || origin == null || origin.getY() < 0) {
            return false;
        }
        if (!level.isLoaded(origin)) {
            return false;
        }
        if (level.hasNeighborSignal(origin)) {
            return true;
        }
        for (int dy = -SEARCH; dy <= SEARCH; dy++) {
            for (int dx = -SEARCH; dx <= SEARCH; dx++) {
                for (int dz = -SEARCH; dz <= SEARCH; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!level.isLoaded(pos)) {
                        continue;
                    }
                    if (isSignalOccupied(level, pos, approach)) {
                        return true;
                    }
                }
            }
        }
        if (facing != null) {
            BlockPos ahead = origin.relative(facing);
            if (level.isLoaded(ahead) && (level.hasNeighborSignal(ahead)
                    || isSignalOccupied(level, ahead, approach))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSignalOccupied(Level level, BlockPos pos, boolean approach) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            return false;
        }
        if (invokeBoolean(be, "isPowered") || invokeBoolean(be, "getReportedPower")) {
            return true;
        }
        Object node = invoke(be, "currentSignalState");
        if (node != null && (invokeBoolean(node, "isStop") || invokeBoolean(node, "isReserved")
                || (approach && createStateOccupied(invoke(node, "getCreateSignalState", true), true)))) {
            return true;
        }
        Object state = invoke(be, "getState");
        if (createStateOccupied(state, approach)) {
            return true;
        }
        var blockState = level.getBlockState(pos);
        if (blockState.hasProperty(BlockStateProperties.POWERED) && blockState.getValue(BlockStateProperties.POWERED)) {
            String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(blockState.getBlock())
                    .getPath();
            if (path.contains("signal")) {
                return true;
            }
        }
        try {
            Class<?> createSignal = Class.forName(CREATE_SIGNAL_BE, false, TrainSignalCompat.class.getClassLoader());
            if (createSignal.isInstance(be)) {
                return createStateOccupied(invoke(be, "getState"), approach);
            }
        } catch (ClassNotFoundException | LinkageError ignored) {
        }
        return false;
    }

    private static boolean createStateOccupied(Object state, boolean approach) {
        if (state == null) {
            return false;
        }
        String name = enumName(state);
        if ("RED".equals(name)) {
            return true;
        }
        if (approach && "YELLOW".equals(name)) {
            return true;
        }
        if (invokeBoolean(state, "isRedLight", 0.0F)) {
            return true;
        }
        return approach && invokeBoolean(state, "isYellowLight", 0.0F);
    }

    private static String enumName(Object value) {
        return value instanceof Enum<?> e ? e.name() : String.valueOf(value);
    }

    private static boolean invokeBoolean(Object target, String method, Object... args) {
        Object result = invoke(target, method, args);
        return result instanceof Boolean flag && flag;
    }

    private static Object invoke(Object target, String method, Object... args) {
        if (target == null) {
            return null;
        }
        try {
            for (var candidate : target.getClass().getMethods()) {
                if (!candidate.getName().equals(method) || candidate.getParameterCount() != args.length) {
                    continue;
                }
                return candidate.invoke(target, args);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }
}
