package com.gamearoosdevelopment.realistictrafficcontrol.util;

import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightBlockEntity;

import net.minecraft.core.Direction;

/**
 * Applies per-approach movement overrides (disable straight/left/right and force an idle bulb) to a
 * traffic light. Ported verbatim from 1.12.2 ({@code BaseTrafficLightTileEntity} -&gt;
 * {@link TrafficLightBlockEntity}).
 */
public final class ApproachMovementBulbHelper {
    private ApproachMovementBulbHelper() {
    }

    public static Direction getApproachFacing(TrafficLightBlockEntity tl) {
        return TrafficLightFacingResolver.resolveApproachFacing(tl);
    }

    public static void applyOverrides(TrafficLightBlockEntity tl, ApproachMovementSettings settings) {
        if (!settings.straightEnabled) {
            clearStraightBulbs(tl);
            applyStraightIdle(tl, settings.straightIdle);
        }
        if (!settings.leftEnabled) {
            clearLeftBulbs(tl);
            applyLeftIdle(tl, settings.leftIdle);
        }
        if (!settings.rightEnabled) {
            clearRightBulbs(tl);
            applyRightIdle(tl, settings.rightIdle);
        }
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

    private static void applyStraightIdle(TrafficLightBlockEntity tl, IdleBulbState state) {
        if (state == IdleBulbState.GREEN) {
            tl.setActive(EnumTrafficLightBulbTypes.Green, true, false);
            tl.setActive(EnumTrafficLightBulbTypes.StraightGreen, true, false);
        } else {
            tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
            tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
            tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
        }
    }

    private static void clearLeftBulbs(TrafficLightBlockEntity tl) {
        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft, false, false);
        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft2, false, false);
        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowUTurn, false, false);
        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowUTurn2, false, false);
        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft, false, false);
        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft2, false, false);
        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn, false, false);
        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn2, false, false);
        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, false, false);
        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, false, false);
        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, false, false);
        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, false, false);
        tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, false, false);
    }

    private static void applyLeftIdle(TrafficLightBlockEntity tl, IdleBulbState state) {
        if (state == IdleBulbState.GREEN) {
            tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft, true, false);
            tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft2, true, false);
            tl.setActive(EnumTrafficLightBulbTypes.GreenArrowUTurn, true, false);
            tl.setActive(EnumTrafficLightBulbTypes.GreenArrowUTurn2, true, false);
        } else {
            tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
            tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
            tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
            tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
            tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
        }
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

    private static void applyRightIdle(TrafficLightBlockEntity tl, IdleBulbState state) {
        if (state == IdleBulbState.GREEN) {
            tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight, true, false);
            tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight2, true, false);
        } else {
            tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
            tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
            tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
        }
    }
}
