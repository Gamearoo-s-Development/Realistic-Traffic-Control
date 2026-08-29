package com.gamearoosdevelopment.realistictrafficcontrol.util;

import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightBlockEntity;

import net.minecraft.core.Direction;

public final class ApproachMovementBulbHelper {
    private ApproachMovementBulbHelper() {
    }

    public static Direction getApproachFacing(TrafficLightBlockEntity tl) {
        return TrafficLightFacingResolver.resolveApproachFacing(tl);
    }

    public static void forceAllRed(TrafficLightBlockEntity tl) {
        tl.powerOff();
        tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
        tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
        tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
        tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
        tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
        tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
        tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
    }

    /**
     * Applies idle display only for movements that are turned OFF.
     * When sharedTurns is on, left/right ride with straight — do not apply turn idle.
     */
    public static void applyOverrides(TrafficLightBlockEntity tl, ApproachMovementSettings settings) {
        if (!settings.sharedTurns && !settings.leftEnabled) {
            clearLeftBulbs(tl);
            applyLeftIdle(tl, settings, settings.leftIdle);
        }
        if (!settings.sharedTurns && !settings.rightEnabled) {
            clearRightBulbs(tl);
            applyRightIdle(tl, settings, settings.rightIdle);
        }
        if (!settings.straightEnabled) {
            clearStraightBulbs(tl);
            clearStraightConflictingArrows(tl, settings.straightIdle);
            applyStraightIdle(tl, settings.straightIdle);
        } else {
            tl.clearConflictingSolidRedsIfProceeding();
        }
    }

    public static void applySharedTurnGreens(TrafficLightBlockEntity tl) {
        LeftTurnBulbHelper.setGreen(tl, false);
        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight, true, false);
        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight2, true, false);
    }

    public static void applySharedTurnYellows(TrafficLightBlockEntity tl) {
        LeftTurnBulbHelper.setYellow(tl, false);
        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft3, true, false);
        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight, true, false);
        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight2, true, false);
        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight3, true, false);
    }

    private static void clearStraightConflictingArrows(TrafficLightBlockEntity tl, IdleBulbMode idleMode) {
        if (idleMode == IdleBulbMode.SOLID_RED || idleMode == IdleBulbMode.ARROW_RED) {
            return;
        }
        tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, false, false);
        tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, false, false);
    }

    private static void clearStraightBulbs(TrafficLightBlockEntity tl) {
        tl.setActive(EnumTrafficLightBulbTypes.Green, false, false);
        tl.setActive(EnumTrafficLightBulbTypes.StraightGreen, false, false);
        tl.setActive(EnumTrafficLightBulbTypes.Yellow, false, false);
        tl.setActive(EnumTrafficLightBulbTypes.YellowX, false, false);
        tl.setActive(EnumTrafficLightBulbTypes.StraightYellow, false, false);
        tl.setActive(EnumTrafficLightBulbTypes.Red, false, false);
        tl.setActive(EnumTrafficLightBulbTypes.Red2, false, false);
        tl.setActive(EnumTrafficLightBulbTypes.StraightRed, false, false);
        tl.setActive(EnumTrafficLightBulbTypes.GreenDownArrow, false, false);
    }

    private static void clearLeftBulbs(TrafficLightBlockEntity tl) {
        LeftTurnBulbHelper.clear(tl);
    }

    private static void clearRightBulbs(TrafficLightBlockEntity tl) {
        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight, false, false);
        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight2, false, false);
        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight, false, false);
        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight2, false, false);
        tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, false, false);
        tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, false, false);
        tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, false, false);
    }

    private static void applyStraightIdle(TrafficLightBlockEntity tl, IdleBulbMode mode) {
        applySolidBalls(tl, mode);
    }

    private static void applyLeftIdle(TrafficLightBlockEntity tl, ApproachMovementSettings settings, IdleBulbMode mode) {
        IdleBulbMode effectiveMode = mode;
        if (mode.isSolid() && settings.straightEnabled) {
            effectiveMode = mode.toArrowEquivalent();
        }
        if (effectiveMode.isSolid()) {
            applySolidBalls(tl, effectiveMode);
            return;
        }
        switch (effectiveMode) {
            case ARROW_GREEN -> LeftTurnBulbHelper.setGreen(tl, false);
            case ARROW_YELLOW -> LeftTurnBulbHelper.setYellow(tl, false);
            default -> {
                LeftTurnBulbHelper.setRed(tl, false);
                tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
            }
        }
    }

    private static void applyRightIdle(TrafficLightBlockEntity tl, ApproachMovementSettings settings, IdleBulbMode mode) {
        IdleBulbMode effectiveMode = mode;
        if (mode.isSolid() && settings.straightEnabled) {
            effectiveMode = mode.toArrowEquivalent();
        }
        if (effectiveMode.isSolid()) {
            applySolidBalls(tl, effectiveMode);
            return;
        }
        switch (effectiveMode) {
            case ARROW_GREEN -> {
                tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight, true, false);
                tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight2, true, false);
            }
            case ARROW_YELLOW -> {
                tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight, true, false);
                tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight2, true, false);
            }
            default -> {
                tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
                tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
                tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
            }
        }
    }

    private static void applySolidBalls(TrafficLightBlockEntity tl, IdleBulbMode mode) {
        switch (mode) {
            case SOLID_GREEN -> {
                tl.setActive(EnumTrafficLightBulbTypes.Green, true, false);
                tl.setActive(EnumTrafficLightBulbTypes.StraightGreen, true, false);
                tl.setActive(EnumTrafficLightBulbTypes.GreenDownArrow, true, false);
            }
            case ARROW_GREEN -> {
                tl.setActive(EnumTrafficLightBulbTypes.Green, true, false);
                tl.setActive(EnumTrafficLightBulbTypes.StraightGreen, true, false);
            }
            case SOLID_YELLOW, ARROW_YELLOW -> {
                tl.setActive(EnumTrafficLightBulbTypes.Yellow, true, false);
                tl.setActive(EnumTrafficLightBulbTypes.StraightYellow, true, false);
            }
            default -> {
                tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
                tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
                tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
            }
        }
    }
}
