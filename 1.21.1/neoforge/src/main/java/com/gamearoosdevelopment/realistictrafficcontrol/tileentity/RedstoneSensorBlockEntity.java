package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Port of 1.12.2 {@code TileEntityRedstoneSensor}: emits redstone when entities are nearby. */
public class RedstoneSensorBlockEntity extends BlockEntity {
    private boolean triggered;
    private int tickCounter;

    public RedstoneSensorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REDSTONE_SENSOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RedstoneSensorBlockEntity be) {
        be.tickServer(level, pos, state);
    }

    private void tickServer(Level level, BlockPos pos, BlockState state) {
        tickCounter++;
        if (tickCounter < 5) {
            return;
        }
        tickCounter = 0;
        boolean wasTriggered = triggered;
        triggered = !level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,
                new AABB(pos).inflate(1, 6, 1), e -> true).isEmpty();
        if (wasTriggered != triggered) {
            setChanged();
            level.updateNeighborsAt(pos, state.getBlock());
        }
    }

    public boolean isTriggered() {
        return triggered;
    }
}
