package com.gamearoosdevelopment.realistictrafficcontrol.util;

import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.BaseTrafficLightTileEntity;

import net.minecraft.util.EnumFacing;

public final class ApproachMovementBulbHelper {
	private ApproachMovementBulbHelper() {}

	public static EnumFacing getApproachFacing(BaseTrafficLightTileEntity tl) {
		return TrafficLightFacingResolver.resolveApproachFacing(tl);
	}

	public static void forceAllRed(BaseTrafficLightTileEntity tl) {
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
	 * Enabled movements are left to normal phase logic.
	 * When sharedTurns is on, left/right ride with straight — do not apply turn idle.
	 */
	public static void applyOverrides(BaseTrafficLightTileEntity tl, ApproachMovementSettings settings) {
		if (!settings.sharedTurns && !settings.leftEnabled) {
			clearLeftBulbs(tl);
			applyLeftIdle(tl, settings, settings.leftIdle);
		}
		if (!settings.sharedTurns && !settings.rightEnabled) {
			clearRightBulbs(tl);
			applyRightIdle(tl, settings, settings.rightIdle);
		}
		// Apply straight idle last so shared solid-ball hardware is not overridden by turn-idle red aliases.
		if (!settings.straightEnabled) {
			clearStraightBulbs(tl);
			clearStraightConflictingArrows(tl, settings.straightIdle);
			applyStraightIdle(tl, settings.straightIdle);
		} else {
			// Straight is in phase control — never leave solid red from turn-idle aliases
			// while green/yellow balls are showing.
			tl.clearConflictingSolidRedsIfProceeding();
		}
	}

	/** Left + U-turn + right greens with straight (T / ramp / shared / split). */
	public static void applySharedTurnGreens(BaseTrafficLightTileEntity tl) {
		LeftTurnBulbHelper.setGreen(tl, false);
		tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight, true, false);
		tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight2, true, false);
	}

	/** Left + U-turn + right yellows with straight. */
	public static void applySharedTurnYellows(BaseTrafficLightTileEntity tl) {
		LeftTurnBulbHelper.setYellow(tl, false);
		tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft3, true, false);
		tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight, true, false);
		tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight2, true, false);
		tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight3, true, false);
	}

	private static void clearStraightConflictingArrows(BaseTrafficLightTileEntity tl, IdleBulbMode idleMode) {
		if (idleMode == IdleBulbMode.SOLID_RED || idleMode == IdleBulbMode.ARROW_RED) {
			return;
		}
		tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, false, false);
		tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, false, false);
	}

	private static void clearStraightBulbs(BaseTrafficLightTileEntity tl) {
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

	private static void clearLeftBulbs(BaseTrafficLightTileEntity tl) {
		LeftTurnBulbHelper.clear(tl);
	}

	private static void clearRightBulbs(BaseTrafficLightTileEntity tl) {
		tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight, false, false);
		tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight2, false, false);
		tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight, false, false);
		tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight2, false, false);
		tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, false, false);
		tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, false, false);
		tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, false, false);
	}

	private static void applyStraightIdle(BaseTrafficLightTileEntity tl, IdleBulbMode mode) {
		applySolidBalls(tl, mode);
	}

	private static void applyLeftIdle(BaseTrafficLightTileEntity tl, ApproachMovementSettings settings, IdleBulbMode mode) {
		IdleBulbMode effectiveMode = mode;
		if (mode.isSolid() && settings.straightEnabled) {
			effectiveMode = mode.toArrowEquivalent();
		}
		if (effectiveMode.isSolid()) {
			applySolidBalls(tl, effectiveMode);
			return;
		}
		switch (effectiveMode) {
			case ARROW_GREEN:
				LeftTurnBulbHelper.setGreen(tl, false);
				break;
			case ARROW_YELLOW:
				LeftTurnBulbHelper.setYellow(tl, false);
				break;
			case ARROW_RED:
			default:
				LeftTurnBulbHelper.setRed(tl, false);
				tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
				break;
		}
	}

	private static void applyRightIdle(BaseTrafficLightTileEntity tl, ApproachMovementSettings settings, IdleBulbMode mode) {
		IdleBulbMode effectiveMode = mode;
		if (mode.isSolid() && settings.straightEnabled) {
			effectiveMode = mode.toArrowEquivalent();
		}
		if (effectiveMode.isSolid()) {
			applySolidBalls(tl, effectiveMode);
			return;
		}
		switch (effectiveMode) {
			case ARROW_GREEN:
				tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight, true, false);
				tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight2, true, false);
				break;
			case ARROW_YELLOW:
				tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight, true, false);
				tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight2, true, false);
				break;
			case ARROW_RED:
			default:
				tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
				tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
				tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
				break;
		}
	}

	private static void applySolidBalls(BaseTrafficLightTileEntity tl, IdleBulbMode mode) {
		switch (mode) {
			case SOLID_GREEN:
				tl.setActive(EnumTrafficLightBulbTypes.Green, true, false);
				tl.setActive(EnumTrafficLightBulbTypes.StraightGreen, true, false);
				tl.setActive(EnumTrafficLightBulbTypes.GreenDownArrow, true, false);
				break;
			case ARROW_GREEN:
				tl.setActive(EnumTrafficLightBulbTypes.Green, true, false);
				tl.setActive(EnumTrafficLightBulbTypes.StraightGreen, true, false);
				break;
			case SOLID_YELLOW:
			case ARROW_YELLOW:
				tl.setActive(EnumTrafficLightBulbTypes.Yellow, true, false);
				tl.setActive(EnumTrafficLightBulbTypes.StraightYellow, true, false);
				break;
			case SOLID_RED:
			case ARROW_RED:
			default:
				tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
				tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
				tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
				break;
		}
	}
}
