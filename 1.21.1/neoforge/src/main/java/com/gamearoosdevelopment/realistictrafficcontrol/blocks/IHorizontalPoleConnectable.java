package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Port of 1.12.2 {@code IHorizontalPoleConnectable}: horizontal-pole attachment hints for lamp blocks. */
public interface IHorizontalPoleConnectable {

    boolean canConnectHorizontalPole(BlockState state, Direction fromFacing);
}
