package com.gamearoosdevelopment.realistictrafficcontrol.blocks;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Extra-Frames / 1.12 {@code IHorizontalPoleConnectable}: neighbor check for auto-connect pole bars. */
public interface IHorizontalPoleConnectable {

    boolean canConnectHorizontalPole(BlockState state, Direction fromFacing);
}
