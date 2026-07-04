package com.gamearoosdevelopment.realistictrafficcontrol.util;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

import cam72cam.immersiverailroading.entity.EntityMoveableRollingStock;
import cam72cam.immersiverailroading.thirdparty.trackapi.ITrack;
import cam72cam.mod.entity.ModdedEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ImmersiveRailroadingHelper {
	public static Vec3d findOrigin(BlockPos currentPos, World world)
	{
		cam72cam.mod.world.World camWorld = cam72cam.mod.world.World.get(world);
		
		double bestDistSq = Double.MAX_VALUE;
		Vec3d bestCenter = null;
		double refX = currentPos.getX() + 0.5;
		double refY = currentPos.getY() + 0.5;
		double refZ = currentPos.getZ() + 0.5;
		
		for (int dy = -3; dy <= 2; dy++)
		{
			BlockPos columnBase = new BlockPos(currentPos.getX(), currentPos.getY() + dy, currentPos.getZ());
			for (EnumFacing dir : EnumFacing.Plane.HORIZONTAL)
			{
				for (int i = 0; i <= 10; i++)
				{
					BlockPos workingPos = columnBase.offset(dir, i);
					cam72cam.mod.math.Vec3d workingPosVec3d = new cam72cam.mod.math.Vec3d(workingPos.getX(), workingPos.getY(), workingPos.getZ());
					ITrack tile = ITrack.get(camWorld, workingPosVec3d, false);
					if (tile == null)
					{
						continue;
					}
					
					cam72cam.mod.math.Vec3d center = tile.getNextPosition(workingPosVec3d, new cam72cam.mod.math.Vec3d(0, 0, 0));
					double cx = center.x;
					double cy = center.y;
					double cz = center.z;
					double dx = cx - refX;
					double dyv = cy - refY;
					double dz = cz - refZ;
					double distSq = dx * dx + dyv * dyv + dz * dz;
					if (distSq < bestDistSq)
					{
						bestDistSq = distSq;
						bestCenter = new Vec3d(cx, cy, cz);
					}
				}
			}
		}
		
		if (bestCenter == null)
		{
			return new Vec3d(0, -1, 0);
		}
		
		return bestCenter;
	}
	
	public static Vec3d getNextPosition(Vec3d currentPosition, Vec3d motion, World world)
	{
		BlockPos currentBlockPos = new BlockPos(currentPosition.x, currentPosition.y, currentPosition.z);
		cam72cam.mod.world.World camWorld = cam72cam.mod.world.World.get(world);
		ITrack te = ITrack.get(camWorld, new cam72cam.mod.math.Vec3d(currentBlockPos.getX(), currentBlockPos.getY(), currentBlockPos.getZ()), false);
		
		int attempt = 0;
		while(te == null && attempt < 8)
		{
			switch(attempt)
			{
				case 0:
					currentBlockPos = currentBlockPos.up();
					break;
				case 1:
					currentBlockPos = currentBlockPos.down(2);
					break;
				case 2:
					currentBlockPos = currentBlockPos.up();
					EnumFacing direction = EnumFacing.getFacingFromVector((float)motion.x, (float)motion.y, (float)motion.z).rotateY();
					currentBlockPos = currentBlockPos.offset(direction);
					break;
				case 3:
					direction = EnumFacing.getFacingFromVector((float)motion.x, (float)motion.y, (float)motion.z).rotateY().rotateY().rotateY();
					currentBlockPos = currentBlockPos.offset(direction, 2);
					break;
				case 4:
					currentBlockPos = currentBlockPos.up();
					break;
				case 5:
					currentBlockPos = currentBlockPos.down(2);
					break;
				case 6:
					direction = EnumFacing.getFacingFromVector((float)motion.x, (float)motion.y, (float)motion.z).rotateY();
					currentBlockPos = currentBlockPos.offset(direction, 2);
					break;
				case 7:
					currentBlockPos = currentBlockPos.up(2);
					break;
			}
			
			te = ITrack.get(camWorld, new cam72cam.mod.math.Vec3d(currentBlockPos.getX(), currentBlockPos.getY(), currentBlockPos.getZ()), false);
			attempt++;
		}
		
		if (te == null)
		{
			return currentPosition;
		}

		cam72cam.mod.math.Vec3d retVal = te.getNextPosition(new cam72cam.mod.math.Vec3d(currentPosition), new cam72cam.mod.math.Vec3d(motion));
		return retVal.internal();
	}
	
	public static Tuple<UUID, Vec3d> getStockNearby(Vec3d currentPosition, World world)
	{
		BlockPos currentBlockPos = new BlockPos(currentPosition.x, currentPosition.y, currentPosition.z);

		AxisAlignedBB bb = new AxisAlignedBB(currentBlockPos.down().south(2).west(2), currentBlockPos.up(3).east(2).north(2));
		
		List<EntityMoveableRollingStock> stocks = ImmutableList.copyOf(world.loadedEntityList)
						.stream()
						.map(x -> x instanceof ModdedEntity ? (ModdedEntity)x : null)
						.filter(Objects::nonNull)
						.map(x -> x.getSelf() instanceof EntityMoveableRollingStock ? (EntityMoveableRollingStock)x.getSelf() : null)
						.filter(Objects::nonNull)
						.filter(emrs -> bb.contains(new Vec3d(emrs.getBlockPosition().internal())))
						.collect(Collectors.toList());

		if (!stocks.isEmpty())
		{
			EntityMoveableRollingStock stock = stocks.get(0);
			return new Tuple<UUID, Vec3d>(stock.getUUID(), stock.getVelocity().internal());
		}
		else
		{
			return null;
		}
	}

	// Methods for finding next position from Track API (to avoid referencing "world")
	
}