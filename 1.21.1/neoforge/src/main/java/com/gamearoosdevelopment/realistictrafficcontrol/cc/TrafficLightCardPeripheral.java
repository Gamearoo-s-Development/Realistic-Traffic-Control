package com.gamearoosdevelopment.realistictrafficcontrol.cc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightControlBoxBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.EnumTrafficLightBulbTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * CC:Tweaked peripheral logic for the traffic-light control box. Plain Java (no CC imports) so the mod
 * compiles without CC:T on the classpath; {@link TrafficLightPeripheralProvider} wraps this in a proxy.
 */
public class TrafficLightCardPeripheral {
    private final Level level;
    private final BlockPos pos;
    private final TrafficLightControlBoxBlockEntity tile;

    public TrafficLightCardPeripheral(Level level, BlockPos pos, TrafficLightControlBoxBlockEntity tile) {
        this.level = level;
        this.pos = pos;
        this.tile = tile;
    }

    public String getType() {
        return "traffic_light_card";
    }

    public String[] getMethodNames() {
        return new String[] {
                "listBlockPos",
                "listTrafficLights",
                "setTrafficLightState",
                "clearTrafficLightState",
                "listBulbTypes",
                "queueNorthSouthPed",
                "queueWestEastPed",
                "isNorthSouthPedQueued",
                "isWestEastPedQueued",
                "listPedButtons"
        };
    }

    public Object[] callMethod(int method, Object[] args) throws CcLuaException {
        return switch (method) {
            case 0 -> new Object[] { tile.getBlockPos().toString() };
            case 1 -> {
                List<BlockPos> all = new ArrayList<>();
                all.addAll(tile.getNorthSouthLights());
                all.addAll(tile.getWestEastLights());
                yield new Object[] { all.stream().map(BlockPos::toString).toArray(String[]::new) };
            }
            case 2 -> setTrafficLightState(args);
            case 3 -> clearTrafficLightState(args);
            case 4 -> listBulbTypes(args);
            case 5 -> {
                tile.getAutomator().setNorthSouthPedQueued(true);
                tile.setChanged();
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                yield new Object[] { true };
            }
            case 6 -> {
                tile.getAutomator().setWestEastPedQueued(true);
                tile.setChanged();
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                yield new Object[] { true };
            }
            case 7 -> new Object[] { tile.getAutomator().isNorthSouthPedQueued() };
            case 8 -> new Object[] { tile.getAutomator().isWestEastPedQueued() };
            case 9 -> new Object[] { listPedButtons() };
            default -> new Object[] {};
        };
    }

    private Object[] setTrafficLightState(Object[] args) throws CcLuaException {
        if (args.length != 4) {
            throw new CcLuaException("Usage: setTrafficLightState(posString, bulbType, on, flash)");
        }
        BlockPos target = parsePos(args[0].toString());
        EnumTrafficLightBulbTypes bulbType;
        try {
            bulbType = EnumTrafficLightBulbTypes.valueOf(args[1].toString());
        } catch (IllegalArgumentException ex) {
            throw new CcLuaException("Invalid bulb type: " + args[1]);
        }
        boolean on = (Boolean) args[2];
        boolean flash = (Boolean) args[3];
        return applyBulbState(target, bulbType, on, flash);
    }

    private Object[] applyBulbState(BlockPos target, EnumTrafficLightBulbTypes bulbType, boolean on, boolean flash) {
        BlockEntity te = level.getBlockEntity(target);
        if (!(te instanceof TrafficLightBlockEntity light)) {
            return new Object[] { false, "No traffic light at position" };
        }
        if (!light.hasBulb(bulbType)) {
            return new Object[] { false, "Bulb not found in light" };
        }
        light.setActive(bulbType, on, flash);
        return new Object[] { true };
    }

    private Object[] clearTrafficLightState(Object[] args) throws CcLuaException {
        if (args.length != 1) {
            throw new CcLuaException("Usage: clearTrafficLightState(posString)");
        }
        BlockPos target = parsePos(args[0].toString());
        BlockEntity te = level.getBlockEntity(target);
        if (te instanceof TrafficLightBlockEntity light) {
            light.powerOff();
            return new Object[] { true };
        }
        return new Object[] { false };
    }

    private Object[] listBulbTypes(Object[] args) throws CcLuaException {
        if (args.length != 1) {
            throw new CcLuaException("Usage: listBulbTypes(posString)");
        }
        BlockPos target = parsePos(args[0].toString());
        BlockEntity te = level.getBlockEntity(target);
        if (!(te instanceof TrafficLightBlockEntity light)) {
            return new Object[] { false, "No traffic light found" };
        }
        List<String> bulbs = new ArrayList<>();
        for (int i = 0; i < light.getBulbCount(); i++) {
            EnumTrafficLightBulbTypes type = light.getBulbTypeBySlot(i);
            if (type != null) {
                bulbs.add(type.toString());
            }
        }
        return new Object[] { bulbs.toArray(new String[0]) };
    }

    private Map<String, Object> listPedButtons() {
        HolderLookup.Provider registries = level.registryAccess();
        CompoundTag compound = tile.saveWithFullMetadata(registries);
        ListTag northSouth = compound.getList("northSouthPedButtons", Tag.TAG_LONG);
        ListTag westEast = compound.getList("westEastPedButtons", Tag.TAG_LONG);

        String[] northSouthPos = new String[northSouth.size()];
        for (int i = 0; i < northSouth.size(); i++) {
            long asLong = ((LongTag) northSouth.get(i)).getAsLong();
            northSouthPos[i] = BlockPos.of(asLong).toString();
        }

        String[] westEastPos = new String[westEast.size()];
        for (int i = 0; i < westEast.size(); i++) {
            long asLong = ((LongTag) westEast.get(i)).getAsLong();
            westEastPos[i] = BlockPos.of(asLong).toString();
        }

        Map<String, Object> out = new HashMap<>();
        out.put("northSouth", northSouthPos);
        out.put("westEast", westEastPos);
        return out;
    }

    private static BlockPos parsePos(String posStr) throws CcLuaException {
        try {
            if (posStr.startsWith("BlockPos{")) {
                String[] parts = posStr.replace("BlockPos{", "").replace("}", "").split(",");
                int x = Integer.parseInt(parts[0].split("=")[1].trim());
                int y = Integer.parseInt(parts[1].split("=")[1].trim());
                int z = Integer.parseInt(parts[2].split("=")[1].trim());
                return new BlockPos(x, y, z);
            }
            return BlockPos.of(Long.parseLong(posStr));
        } catch (Exception ex) {
            throw new CcLuaException("Invalid position format");
        }
    }

    /** Stand-in for CC {@code LuaException} without a compile-time CC dependency. */
    public static final class CcLuaException extends Exception {
        public CcLuaException(String message) {
            super(message);
        }
    }
}
