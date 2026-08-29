package com.gamearoosdevelopment.realistictrafficcontrol.util;

public final class ApproachMovementPhaseHelper {
	private ApproachMovementPhaseHelper() {}

	public static boolean hasAnyMovementEnabled(ApproachMovementSettings settings) {
		return settings.straightEnabled || settings.leftEnabled || settings.rightEnabled;
	}

	public static boolean isStraightOnly(ApproachMovementSettings settings) {
		return settings.straightEnabled && !settings.leftEnabled && !settings.rightEnabled;
	}

	public static boolean isLeftOnly(ApproachMovementSettings settings) {
		return settings.leftEnabled && !settings.straightEnabled && !settings.rightEnabled;
	}

	public static boolean isRightOnly(ApproachMovementSettings settings) {
		return settings.rightEnabled && !settings.straightEnabled && !settings.leftEnabled;
	}

	public static boolean isLeftAndStraight(ApproachMovementSettings settings) {
		return settings.leftEnabled && settings.straightEnabled;
	}

	public static boolean shouldServeRightAfterLeft(ApproachMovementSettings settings, double arrowMinimum) {
		return isLeftAndStraight(settings) && settings.rightEnabled && arrowMinimum > 0;
	}
}
