package com.gamearoosdevelopment.realistictrafficcontrol.compat;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Optional Create: Power Grid / Power Grid Essentials bridge. Uses reflection so RTC loads
 * without those jars. When Power Grid is present, lamps and relays can see live voltage.
 */
public final class PowerGridCompat {

    private static final String I_ELECTRIC = "org.patryk3211.powergrid.electricity.base.IElectric";
    private static final String I_NODE = "org.patryk3211.powergrid.electricity.sim.node.IElectricNode";
    private static final double LIVE_VOLTS = 1.0;

    private PowerGridCompat() {
    }

    public static boolean isAvailable() {
        return ModRealisticTrafficControl.POWERGRID_INSTALLED;
    }

    /**
     * Street lights stay on by default. Redstone still kills them. If a Power Grid
     * device is wired next to the fixture, it also needs live voltage.
     */
    public static boolean streetLightShouldBeOff(Level level, BlockPos pos) {
        if (level.hasNeighborSignal(pos)) {
            return true;
        }
        return requiresLivePower(level, pos) && !isEnergizedNearby(level, pos);
    }

    /** True when a Power Grid device is actually wired to this block or a neighbor. */
    public static boolean requiresLivePower(Level level, BlockPos pos) {
        return isAvailable() && hasElectricNearby(level, pos);
    }

    public static boolean hasElectricNearby(Level level, BlockPos pos) {
        if (!isAvailable() || level == null || pos == null || !level.isLoaded(pos)) {
            return false;
        }
        if (electricAt(level, pos) != null) {
            return true;
        }
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (level.isLoaded(neighbor) && electricAt(level, neighbor) != null) {
                return true;
            }
        }
        return false;
    }

    /** True if this block or an immediate neighbor is on a live Power Grid circuit. */
    public static boolean isEnergizedNearby(Level level, BlockPos pos) {
        if (!isAvailable() || level == null || pos == null || !level.isLoaded(pos)) {
            return false;
        }
        if (voltageAt(level, pos) >= LIVE_VOLTS) {
            return true;
        }
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (level.isLoaded(neighbor) && voltageAt(level, neighbor) >= LIVE_VOLTS) {
                return true;
            }
        }
        return false;
    }

    public static double voltageAt(Level level, BlockPos pos) {
        if (!isAvailable() || level == null || !level.isLoaded(pos)) {
            return 0.0;
        }
        Object electric = electricAt(level, pos);
        if (electric == null) {
            return voltageFromBehaviour(level.getBlockEntity(pos));
        }
        BlockState state = level.getBlockState(pos);
        Object behaviour = invoke(electric, "getBehaviour", level, pos, state);
        if (behaviour == null) {
            behaviour = invoke(electric, "getElectricBehaviour");
        }
        return voltageFromBehaviourObject(behaviour);
    }

    private static Object electricAt(Level level, BlockPos pos) {
        try {
            Class<?> electricType = Class.forName(I_ELECTRIC, false, PowerGridCompat.class.getClassLoader());
            Object electric = electricType.getMethod("getAt", Level.class, BlockPos.class).invoke(null, level, pos);
            if (electric != null) {
                return electric;
            }
            BlockEntity be = level.getBlockEntity(pos);
            return be != null && electricType.isInstance(be) ? be : null;
        } catch (ReflectiveOperationException | LinkageError ex) {
            return null;
        }
    }

    private static double voltageFromBehaviour(BlockEntity be) {
        if (be == null) {
            return 0.0;
        }
        Object behaviour = invoke(be, "getElectricBehaviour");
        if (behaviour == null) {
            behaviour = invoke(be, "getBehaviour");
        }
        return voltageFromBehaviourObject(behaviour);
    }

    private static double voltageFromBehaviourObject(Object behaviour) {
        if (behaviour == null) {
            return 0.0;
        }
        double best = 0.0;
        for (int i = 0; i < 8; i++) {
            Object terminal = invoke(behaviour, "getTerminal", i);
            Double volts = voltageOf(terminal);
            if (volts != null) {
                best = Math.max(best, Math.abs(volts));
            }
        }
        return best;
    }

    private static Double voltageOf(Object node) {
        if (node == null) {
            return null;
        }
        try {
            Class<?> nodeType = Class.forName(I_NODE, false, PowerGridCompat.class.getClassLoader());
            if (nodeType.isInstance(node)) {
                Object value = nodeType.getMethod("getVoltage").invoke(node);
                if (value instanceof Number number) {
                    return number.doubleValue();
                }
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
        Object value = invoke(node, "getVoltage");
        return value instanceof Number number ? number.doubleValue() : null;
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
