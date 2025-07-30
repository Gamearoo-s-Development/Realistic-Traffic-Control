package com.gamearoosdevelopment.realistictrafficcontrol.proxy;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.CC.TrafficLightCardPeripheral;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightControlBoxTileEntity;



import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TrafficLightPeripheralProvider {
    public static void register() {
        if (ModRealisticTrafficControl.CC_INSTALLED) {
            dan200.computercraft.api.ComputerCraftAPI.registerPeripheralProvider(
                new dan200.computercraft.api.peripheral.IPeripheralProvider() {
                    @Override
                    public dan200.computercraft.api.peripheral.IPeripheral getPeripheral(World world, BlockPos pos, EnumFacing side) {
                        TileEntity tile = world.getTileEntity(pos);
                        if (tile instanceof TrafficLightControlBoxTileEntity) {
                            return new TrafficLightCardPeripheral(world, pos, (TrafficLightControlBoxTileEntity) tile);
                        }
                        return null;
                    }
                }
            );
        }
    }
}
