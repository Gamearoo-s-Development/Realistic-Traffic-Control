package com.gamearoosdevelopment.realistictrafficcontrol.proxy;

import com.gamearoosdevelopment.realistictrafficcontrol.CC.TrafficLightCardPeripheral;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightControlBoxTileEntity;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TrafficLightPeripheralProvider implements IPeripheralProvider {

    @Override
    public IPeripheral getPeripheral(World world, BlockPos pos, EnumFacing side) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TrafficLightControlBoxTileEntity) {
            return new TrafficLightCardPeripheral(world, pos, (TrafficLightControlBoxTileEntity) tile);
        }
        return null;
    }
}
