package com.gamearoosdevelopment.realistictrafficcontrol.util;

import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockBaseTrafficLight;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.BaseTrafficLightTileEntity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;

public final class TrafficLightFacingResolver {
	private TrafficLightFacingResolver() {}

	public static EnumFacing resolveApproachFacing(BaseTrafficLightTileEntity tl) {
		if (tl == null) {
			return EnumFacing.NORTH;
		}

		EnumFacing configured = tl.getConfiguredApproachFacing();
		if (configured != null) {
			return configured;
		}

		IBlockState state = tl.getWorld().getBlockState(tl.getPos());
		if (!(state.getBlock() instanceof BlockBaseTrafficLight)) {
			return EnumFacing.NORTH;
		}

		int rotation = state.getValue(BlockBaseTrafficLight.ROTATION);
		for (EnumFacing facing : new EnumFacing[] { EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST }) {
			if (CustomAngleCalculator.isRotationFacing(rotation, facing)) {
				return facing;
			}
		}

		return CustomAngleCalculator.rotationToFacing(rotation);
	}

	public static boolean isFacing(BaseTrafficLightTileEntity tl, EnumFacing approach) {
		return resolveApproachFacing(tl) == approach;
	}

	public static EnumFacing getOppositeApproach(EnumFacing approach) {
		if (approach == null) {
			return EnumFacing.NORTH;
		}
		switch (approach) {
			case NORTH:
				return EnumFacing.SOUTH;
			case SOUTH:
				return EnumFacing.NORTH;
			case EAST:
				return EnumFacing.WEST;
			case WEST:
				return EnumFacing.EAST;
			default:
				return approach;
		}
	}

	public static EnumFacing getClockwiseApproach(EnumFacing approach) {
		return approach == null ? EnumFacing.EAST : approach.rotateY();
	}

	/** Left phase that serves a right-only approach's protected right (e.g. E right -> N left). */
	public static EnumFacing getCoupledLeftApproachForRightOnly(EnumFacing rightOnlyApproach) {
		return rightOnlyApproach == null ? EnumFacing.NORTH : rightOnlyApproach.rotateYCCW();
	}
}
