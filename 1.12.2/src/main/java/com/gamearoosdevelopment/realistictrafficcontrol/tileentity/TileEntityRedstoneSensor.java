// --- TileEntityRedstoneSensor.java ---
package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import java.util.List;

public class TileEntityRedstoneSensor extends TileEntity implements ITickable {

    private boolean triggered = false;
    private int tickCounter = 0;

    public boolean isTriggered() {
        return triggered;
    }

    @Override
    public void update() {
        if (world == null || world.isRemote) return;

        tickCounter++;
        if (tickCounter >= 5) { // check every 5 ticks
            tickCounter = 0;
            boolean wasTriggered = triggered;
            triggered = detectEntities(world, pos);

            if (wasTriggered != triggered) {
                world.notifyNeighborsOfStateChange(pos, getBlockType(), true);
                markDirty();
            }
        }
    }

    private boolean detectEntities(World world, BlockPos pos) {
        AxisAlignedBB box = new AxisAlignedBB(
                pos.getX() - 1, pos.getY() - 6, pos.getZ() - 1,
                pos.getX() + 2, pos.getY() + 7, pos.getZ() + 2
        );

        List<Entity> entities = world.getEntitiesWithinAABB(Entity.class, box);
        return !entities.isEmpty();
    }

    @Override
    public double getMaxRenderDistanceSquared() {
        return 4096.0; // 64 block render distance
    }
} 
