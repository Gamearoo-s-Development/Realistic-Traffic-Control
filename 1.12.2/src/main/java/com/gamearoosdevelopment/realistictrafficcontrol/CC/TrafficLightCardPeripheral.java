package com.gamearoosdevelopment.realistictrafficcontrol.CC;

import java.util.ArrayList;
import java.util.List;

import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.BaseTrafficLightTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightControlBoxTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.EnumTrafficLightBulbTypes;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TrafficLightCardPeripheral implements IPeripheral {
    private final World world;
    private final BlockPos pos;
    private final TrafficLightControlBoxTileEntity tile;

    public TrafficLightCardPeripheral(World world, BlockPos pos, TrafficLightControlBoxTileEntity tile) {
        this.world = world;
        this.pos = pos;
        this.tile = tile;
    }

    @Override
    public String getType() {
        return "traffic_light_card";
    }

    @Override
    public boolean equals(IPeripheral other) {
        return this == other;
    }

    @Override
    public String[] getMethodNames() {
    	  return new String[] {
    		        "listBlockPos",
    		        "listTrafficLights", 
    		        "setTrafficLightState",
    		        "clearTrafficLightState",
    		        "listBulbTypes"
    		    };
    }

   

	@Override
	public Object[] callMethod(IComputerAccess arg0, ILuaContext arg1, int arg2, Object[] arg3)
		    throws LuaException, InterruptedException {
	    switch (arg2) {
	        case 0:
	            return new Object[] { tile.getPos().toString() };
	        case 1:
	            // Combine both NS and WE lights into Lua table
	            List<BlockPos> all = new ArrayList<>();
	            all.addAll(tile.getNorthSouthLights());
	            all.addAll(tile.getWestEastLights());
	            String[] result = all.stream().map(BlockPos::toString).toArray(String[]::new);
	            return new Object[] { result };
	        case 2: {
	            if (arg3.length != 4)
	                throw new LuaException("Usage: setTrafficLightState(posString, bulbType, on, flash)");

	            String posStr = arg3[0].toString();
	            String bulbStr = arg3[1].toString();
	            boolean on = (Boolean) arg3[2];
	            boolean flash = (Boolean) arg3[3];

	            BlockPos target;
	            try {
	                if (posStr.startsWith("BlockPos{")) {
	                    // Parse BlockPos string
	                    String[] parts = posStr.replace("BlockPos{", "").replace("}", "").split(",");
	                    int x = Integer.parseInt(parts[0].split("=")[1].trim());
	                    int y = Integer.parseInt(parts[1].split("=")[1].trim());
	                    int z = Integer.parseInt(parts[2].split("=")[1].trim());
	                    target = new BlockPos(x, y, z);
	                } else {
	                    // Support long (Lua number) position values
	                    long longPos = Long.parseLong(posStr);
	                    target = BlockPos.fromLong(longPos);
	                }
	            } catch (Exception e) {
	                throw new LuaException("Invalid position format");
	            }

	            EnumTrafficLightBulbTypes bulbType;
	            try {
	                bulbType = EnumTrafficLightBulbTypes.valueOf(bulbStr);
	            } catch (IllegalArgumentException e) {
	                throw new LuaException("Invalid bulb type: " + bulbStr);
	            }

	            TileEntity te = world.getTileEntity(target);
	            if (!(te instanceof BaseTrafficLightTileEntity)) {
	                return new Object[] { false, "No traffic light at position" };
	            }

	            BaseTrafficLightTileEntity light = (BaseTrafficLightTileEntity) te;

	            if (!light.hasBulb(bulbType)) {
	                return new Object[] { false, "Bulb not found in light" };
	            }

	            light.setActive(bulbType, on, flash);
	            return new Object[] { true };
	        }

	        case 3: {
	            if (arg3.length != 1)
	                throw new LuaException("Usage: clearTrafficLightState(posString)");

	            String posStr = (String) arg3[0];
	            BlockPos target;
	            try {
	                String[] parts = posStr.replace("BlockPos{", "").replace("}", "").split(",");
	                int x = Integer.parseInt(parts[0].split("=")[1].trim());
	                int y = Integer.parseInt(parts[1].split("=")[1].trim());
	                int z = Integer.parseInt(parts[2].split("=")[1].trim());
	                target = new BlockPos(x, y, z);
	            } catch (Exception e) {
	                throw new LuaException("Invalid position format");
	            }

	            TileEntity te = world.getTileEntity(target);
	            if (te instanceof BaseTrafficLightTileEntity) {
	                BaseTrafficLightTileEntity light = (BaseTrafficLightTileEntity) te;
	                light.powerOff(); // Resets all bulb states
	                return new Object[] { true };
	            }

	            return new Object[] { false };
	        }
	        case 4: { // listBulbTypes
	            if (arg3.length != 1)
	                throw new LuaException("Usage: listBulbTypes(posString)");

	            String posStr = arg3[0].toString();
	            BlockPos target;

	            try {
	                String[] parts = posStr.replace("BlockPos{", "").replace("}", "").split(",");
	                int x = Integer.parseInt(parts[0].split("=")[1].trim());
	                int y = Integer.parseInt(parts[1].split("=")[1].trim());
	                int z = Integer.parseInt(parts[2].split("=")[1].trim());
	                target = new BlockPos(x, y, z);
	            } catch (Exception e) {
	                throw new LuaException("Invalid position format");
	            }

	            TileEntity te = world.getTileEntity(target);
	            if (!(te instanceof BaseTrafficLightTileEntity))
	                return new Object[] { false, "No traffic light found" };

	            BaseTrafficLightTileEntity light = (BaseTrafficLightTileEntity) te;
	            List<String> bulbs = new ArrayList<>();
	            for (int i = 0; i < light.getBulbCount(); i++) {
	                EnumTrafficLightBulbTypes type = light.getBulbTypeBySlot(i);
	                if (type != null) bulbs.add(type.toString());
	            }

	            return new Object[] { bulbs.toArray(new String[0]) };
	        }



	        default:
	            return new Object[] {};
	    }
	}
}
