package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Port of 1.12.2 {@code BlockPedCrossingLamps} (no configuration GUI). */
public class BlockPedCrossingLamps extends BlockRotatableCrossingLamps {

    public BlockPedCrossingLamps(Properties properties) {
        super(properties);
    }

    @Override
    public String getLampRegistryName() {
        return "ped_crossing_lamps";
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        return InteractionResult.PASS;
    }
}
