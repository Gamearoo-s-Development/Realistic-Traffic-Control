package com.gamearoosdevelopment.realistictrafficcontrol.util;

import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockBaseTrafficLight;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.BaseTrafficLightTileEntity;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;

public final class TrafficLightFacingResolver {
	private TrafficLightFacingResolver() {}

	public static boolean isFacing(BaseTrafficLightTileEntity tl, EnumFacing approach) {
		if (tl == null || approach == null) {
			return false;
		}

		EnumFacing configured = tl.getConfiguredApproachFacing();
		if (configured != null) {
			return configured == approach;
		}

		IBlockState state = tl.getWorld().getBlockState(tl.getPos());
		if (!(state.getBlock() instanceof BlockBaseTrafficLight)) {
			return false;
		}

		int rotation = state.getValue(BlockBaseTrafficLight.ROTATION);
		return CustomAngleCalculator.isRotationFacing(rotation, approach);
	}

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
}
