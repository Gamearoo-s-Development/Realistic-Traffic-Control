package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.gamearoosdevelopment.realistictrafficcontrol.Config;
import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;
import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.TrafficSensorBlock;
import com.gamearoosdevelopment.realistictrafficcontrol.util.ApproachMovementBulbHelper;
import com.gamearoosdevelopment.realistictrafficcontrol.util.ApproachMovementSettings;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;
import com.gamearoosdevelopment.realistictrafficcontrol.util.EnumTrafficLightBulbTypes;
import com.gamearoosdevelopment.realistictrafficcontrol.util.TrafficLightFacingResolver;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Full port of the 1.12.2 {@code TrafficLightControlBoxTileEntity}: the automation engine driving grouped
 * traffic lights (protected/permissive arrows, splits, FYA, HAWK beacon, night flash, pedestrian phases,
 * vehicle sensors). Ported to NeoForge 1.21.1 APIs:
 * {@code ITickable.update} -&gt; {@link #serverTick}, {@code EnumFacing} -&gt; {@link Direction},
 * {@code World} -&gt; {@link Level}, {@code NBTTagCompound} -&gt; {@link CompoundTag}. OpenComputers hooks
 * are omitted; ComputerCraft is bound externally via a peripheral provider.
 */
public class TrafficLightControlBoxBlockEntity extends SyncableBlockEntity {

    private ArrayList<BlockPos> westEastLights = new ArrayList<>();
    private ArrayList<BlockPos> northSouthLights = new ArrayList<>();
    private HashMap<EnumTrafficLightBulbTypes, Boolean> manualNorthSouthActive = new HashMap<>();
    private HashMap<EnumTrafficLightBulbTypes, Boolean> manualWestEastActive = new HashMap<>();
    private HashMap<EnumTrafficLightBulbTypes, Boolean> manualNorthSouthInactive = new HashMap<>();
    private HashMap<EnumTrafficLightBulbTypes, Boolean> manualWestEastInactive = new HashMap<>();
    private ArrayList<BlockPos> sensors = new ArrayList<>();
    private ArrayList<BlockPos> northSouthPedButtons = new ArrayList<>();
    private ArrayList<BlockPos> westEastPedButtons = new ArrayList<>();
    private boolean isAutoMode = false; // client only property
    private boolean powered = false;
    private Automator automator = null;
    public boolean hasNorth = true;
    public boolean hasSouth = true;
    public boolean hasEast = true;
    public boolean hasWest = true;
    private int ticksInCurrentStage = 0;
    private boolean nightFlashEnabled = false;
    private long nightFlashStart = 13000; // 7 PM
    private long nightFlashEnd = 0; // 5 AM
    private boolean inNightFlash = false;
    private boolean fyaNightOnlyEnabled = false;
    private boolean lastInNightFlash = false;
    private int fyaDayTransitionTicksRemaining = 0;
    private boolean northMain = true;
    private boolean hawkBeaconEnabled = false;
    private boolean splitDirectionsEnabled = false;
    private boolean splitNorthSouthEnabled = true;
    private boolean splitWestEastEnabled = true;
    private final EnumMap<Direction, ApproachMovementSettings> approachMovementSettings = new EnumMap<>(Direction.class);

    private boolean isInDarkMode = true;
    private boolean isFlashingEmergency = false;
    public boolean isFlashOn;
    // Replaces the 1.12.2 raw Thread + 15s sleep recovery flash with a server-tick countdown.
    private int emergencyFlashTicksRemaining = 0;

    public TrafficLightControlBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRAFFIC_LIGHT_CONTROL_BOX.get(), pos, state);
        for (Direction facing : new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST }) {
            approachMovementSettings.put(facing, new ApproachMovementSettings());
        }
    }

    private void markDirty() {
        setChanged();
    }

    private void notifyUpdate() {
        if (level != null) {
            BlockState s = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, s, s, 3);
        }
    }

    public ApproachMovementSettings getMovementSettings(Direction facing) {
        ApproachMovementSettings settings = approachMovementSettings.get(facing);
        if (settings == null) {
            settings = new ApproachMovementSettings();
            approachMovementSettings.put(facing, settings);
        }
        return settings;
    }

    public void setMovementSettings(Direction facing, ApproachMovementSettings settings) {
        approachMovementSettings.put(facing, settings == null ? new ApproachMovementSettings() : settings.copy());
        markDirty();
    }

    private void writeMovementSettingsToNBT(CompoundTag compound) {
        for (Direction facing : new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST }) {
            compound.put("movement" + facing.getName(), getMovementSettings(facing).writeToNBT());
        }
    }

    private void readMovementSettingsFromNBT(CompoundTag compound) {
        for (Direction facing : new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST }) {
            String key = "movement" + facing.getName();
            if (compound.contains(key)) {
                getMovementSettings(facing).readFromNBT(compound.getCompound(key));
            }
        }
    }

    public void setNightFlashEnabled(boolean enabled) {
        this.nightFlashEnabled = enabled;
        markDirty();
    }

    public boolean isNightFlashEnabled() {
        return nightFlashEnabled;
    }

    public void setNorthMainEnabled(boolean enabled) {
        this.northMain = enabled;
        markDirty();
    }

    public boolean isNorthMainEnabled() {
        return northMain;
    }

    public void setHawkBeaconEnabled(boolean enabled) {
        this.hawkBeaconEnabled = enabled;
        markDirty();
    }

    public boolean isHawkBeaconEnabled() {
        return hawkBeaconEnabled;
    }

    public void setSplitDirectionsEnabled(boolean enabled) {
        this.splitDirectionsEnabled = enabled;
        markDirty();
    }

    public boolean isSplitDirectionsEnabled() {
        return splitDirectionsEnabled;
    }

    public void setSplitNorthSouthEnabled(boolean enabled) {
        this.splitNorthSouthEnabled = enabled;
        markDirty();
    }

    public boolean isSplitNorthSouthEnabled() {
        return splitNorthSouthEnabled;
    }

    public void setSplitWestEastEnabled(boolean enabled) {
        this.splitWestEastEnabled = enabled;
        markDirty();
    }

    public boolean isSplitWestEastEnabled() {
        return splitWestEastEnabled;
    }

    public void setFyaNightOnlyEnabled(boolean enabled) {
        this.fyaNightOnlyEnabled = enabled;
        markDirty();
    }

    public boolean isFyaNightOnlyEnabled() {
        return fyaNightOnlyEnabled;
    }

    public boolean isAutoMode() {
        return isAutoMode;
    }

    public boolean isSplitEnabledForRow(RightOfWays rightOfWay) {
        if (!splitDirectionsEnabled) {
            return false;
        }
        return rightOfWay == RightOfWays.NorthSouth ? splitNorthSouthEnabled : splitWestEastEnabled;
    }

    public List<BlockPos> getNorthSouthLights() {
        return northSouthLights;
    }

    public List<BlockPos> getWestEastLights() {
        return westEastLights;
    }

    // --- persistence ---

    @Override
    protected void saveAdditional(CompoundTag compound, HolderLookup.Provider registries) {
        super.saveAdditional(compound, registries);
        for (int i = 0; i < westEastLights.size(); i++) {
            BlockPos pos = westEastLights.get(i);
            compound.putIntArray("westEast" + i, new int[] { pos.getX(), pos.getY(), pos.getZ() });
        }
        for (int i = 0; i < northSouthLights.size(); i++) {
            BlockPos pos = northSouthLights.get(i);
            compound.putIntArray("northSouth" + i, new int[] { pos.getX(), pos.getY(), pos.getZ() });
        }
        compound.putBoolean("hasNorth", hasNorth);
        compound.putBoolean("hasSouth", hasSouth);
        compound.putBoolean("hasEast", hasEast);
        compound.putBoolean("hasWest", hasWest);
        compound.putBoolean("NightFlashEnabled", nightFlashEnabled);
        compound.putBoolean("northMain", northMain);
        compound.putBoolean("hawkBeaconEnabled", hawkBeaconEnabled);
        compound.putBoolean("splitDirectionsEnabled", splitDirectionsEnabled);
        compound.putBoolean("splitNorthSouthEnabled", splitNorthSouthEnabled);
        compound.putBoolean("splitWestEastEnabled", splitWestEastEnabled);
        compound.putBoolean("FyaNightOnlyEnabled", fyaNightOnlyEnabled);
        writeMovementSettingsToNBT(compound);

        compound.putInt("TicksInCurrentStage", ticksInCurrentStage);

        writeManualSettingDictionary(compound, manualNorthSouthActive, "manualNorthSouthActive");
        writeManualSettingDictionary(compound, manualWestEastActive, "manualWestEastActive");
        writeManualSettingDictionary(compound, manualNorthSouthInactive, "manualNorthSouthInactive");
        writeManualSettingDictionary(compound, manualWestEastInactive, "manualWestEastInactive");

        for (int i = 0; i < sensors.size(); i++) {
            compound.putLong("sensor" + i, sensors.get(i).asLong());
        }

        ListTag northSouthPedButtonsList = new ListTag();
        for (BlockPos pos : northSouthPedButtons) {
            northSouthPedButtonsList.add(LongTag.valueOf(pos.asLong()));
        }
        ListTag westEastPedButtonsList = new ListTag();
        for (BlockPos pos : westEastPedButtons) {
            westEastPedButtonsList.add(LongTag.valueOf(pos.asLong()));
        }
        compound.put("northSouthPedButtons", northSouthPedButtonsList);
        compound.put("westEastPedButtons", westEastPedButtonsList);

        compound.putBoolean("powered", powered);
        getAutomator().writeNBT(compound);
    }

    private void writeManualSettingDictionary(CompoundTag compound, HashMap<EnumTrafficLightBulbTypes, Boolean> map, String prefix) {
        ArrayList<EnumTrafficLightBulbTypes> keyList = new ArrayList<>(map.keySet());
        ArrayList<Boolean> valueList = new ArrayList<>(map.values());
        for (int i = 0; i < map.size(); i++) {
            compound.putInt(prefix + "-key-" + i, keyList.get(i).getIndex());
            compound.putBoolean(prefix + "-value-" + i, valueList.get(i));
        }
    }

    private void readManualSettingDictionary(CompoundTag compound, HashMap<EnumTrafficLightBulbTypes, Boolean> map, String prefix) {
        map.clear();
        int i = 0;
        while (compound.contains(prefix + "-key-" + i)) {
            int bulbType = compound.getInt(prefix + "-key-" + i);
            boolean flash = compound.getBoolean(prefix + "-value-" + i);
            map.put(EnumTrafficLightBulbTypes.get(bulbType), flash);
            i++;
        }
    }

    @Override
    protected void loadAdditional(CompoundTag compound, HolderLookup.Provider registries) {
        super.loadAdditional(compound, registries);

        westEastLights = new ArrayList<>();
        northSouthLights = new ArrayList<>();
        int counter = 0;
        while (compound.contains("westEast" + counter)) {
            int[] a = compound.getIntArray("westEast" + counter);
            westEastLights.add(new BlockPos(a[0], a[1], a[2]));
            counter++;
        }
        counter = 0;
        while (compound.contains("northSouth" + counter)) {
            int[] a = compound.getIntArray("northSouth" + counter);
            northSouthLights.add(new BlockPos(a[0], a[1], a[2]));
            counter++;
        }

        powered = compound.getBoolean("powered");
        if (compound.contains("NightFlashEnabled")) {
            nightFlashEnabled = compound.getBoolean("NightFlashEnabled");
        }
        if (compound.contains("northMain")) {
            northMain = compound.getBoolean("northMain");
        }
        if (compound.contains("hawkBeaconEnabled")) {
            hawkBeaconEnabled = compound.getBoolean("hawkBeaconEnabled");
        }
        if (compound.contains("splitDirectionsEnabled")) {
            splitDirectionsEnabled = compound.getBoolean("splitDirectionsEnabled");
        }
        if (compound.contains("splitNorthSouthEnabled")) {
            splitNorthSouthEnabled = compound.getBoolean("splitNorthSouthEnabled");
        }
        if (compound.contains("splitWestEastEnabled")) {
            splitWestEastEnabled = compound.getBoolean("splitWestEastEnabled");
        }
        if (compound.contains("FyaNightOnlyEnabled")) {
            fyaNightOnlyEnabled = compound.getBoolean("FyaNightOnlyEnabled");
        }
        readMovementSettingsFromNBT(compound);

        hasNorth = compound.getBoolean("hasNorth");
        hasSouth = compound.getBoolean("hasSouth");
        hasEast = compound.getBoolean("hasEast");
        hasWest = compound.getBoolean("hasWest");

        readManualSettingDictionary(compound, manualNorthSouthActive, "manualNorthSouthActive");
        readManualSettingDictionary(compound, manualWestEastActive, "manualWestEastActive");
        readManualSettingDictionary(compound, manualNorthSouthInactive, "manualNorthSouthInactive");
        readManualSettingDictionary(compound, manualWestEastInactive, "manualWestEastInactive");

        sensors = new ArrayList<>();
        for (String key : compound.getAllKeys().stream().filter(k -> k.startsWith("sensor")).collect(Collectors.toSet())) {
            sensors.add(BlockPos.of(compound.getLong(key)));
        }

        getAutomator().readNBT(compound);

        northSouthPedButtons = new ArrayList<>();
        westEastPedButtons = new ArrayList<>();
        ListTag northSouthPedButtonList = compound.getList("northSouthPedButtons", Tag.TAG_LONG);
        ListTag westEastPedButtonList = compound.getList("westEastPedButtons", Tag.TAG_LONG);
        for (Tag baseLong : northSouthPedButtonList) {
            northSouthPedButtons.add(BlockPos.of(((LongTag) baseLong).getAsLong()));
        }
        for (Tag baseLong : westEastPedButtonList) {
            westEastPedButtons.add(BlockPos.of(((LongTag) baseLong).getAsLong()));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag compound = new CompoundTag();
        writeManualSettingDictionary(compound, manualNorthSouthActive, "manualNorthSouthActive");
        writeManualSettingDictionary(compound, manualWestEastActive, "manualWestEastActive");
        writeManualSettingDictionary(compound, manualNorthSouthInactive, "manualNorthSouthInactive");
        writeManualSettingDictionary(compound, manualWestEastInactive, "manualWestEastInactive");
        compound.putBoolean("NightFlashEnabled", nightFlashEnabled);
        compound.putBoolean("northMain", northMain);
        compound.putBoolean("hawkBeaconEnabled", hawkBeaconEnabled);
        compound.putBoolean("splitDirectionsEnabled", splitDirectionsEnabled);
        compound.putBoolean("splitNorthSouthEnabled", splitNorthSouthEnabled);
        compound.putBoolean("splitWestEastEnabled", splitWestEastEnabled);
        compound.putBoolean("FyaNightOnlyEnabled", fyaNightOnlyEnabled);
        compound.putBoolean("isAutoMode", !sensors.isEmpty() || !northSouthPedButtons.isEmpty() || !westEastPedButtons.isEmpty());
        compound.putBoolean("hasNorth", hasNorth);
        compound.putBoolean("hasSouth", hasSouth);
        compound.putBoolean("hasEast", hasEast);
        compound.putBoolean("hasWest", hasWest);
        writeMovementSettingsToNBT(compound);
        getAutomator().setSyncData(compound);
        return compound;
    }

    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readManualSettingDictionary(tag, manualNorthSouthActive, "manualNorthSouthActive");
        readManualSettingDictionary(tag, manualWestEastActive, "manualWestEastActive");
        readManualSettingDictionary(tag, manualNorthSouthInactive, "manualNorthSouthInactive");
        readManualSettingDictionary(tag, manualWestEastInactive, "manualWestEastInactive");

        isAutoMode = tag.getBoolean("isAutoMode");
        hasNorth = tag.getBoolean("hasNorth");
        hasSouth = tag.getBoolean("hasSouth");
        hasEast = tag.getBoolean("hasEast");
        hasWest = tag.getBoolean("hasWest");
        if (tag.contains("NightFlashEnabled")) {
            nightFlashEnabled = tag.getBoolean("NightFlashEnabled");
        }
        if (tag.contains("northMain")) {
            northMain = tag.getBoolean("northMain");
        }
        if (tag.contains("hawkBeaconEnabled")) {
            hawkBeaconEnabled = tag.getBoolean("hawkBeaconEnabled");
        }
        if (tag.contains("splitDirectionsEnabled")) {
            splitDirectionsEnabled = tag.getBoolean("splitDirectionsEnabled");
        }
        if (tag.contains("splitNorthSouthEnabled")) {
            splitNorthSouthEnabled = tag.getBoolean("splitNorthSouthEnabled");
        }
        if (tag.contains("splitWestEastEnabled")) {
            splitWestEastEnabled = tag.getBoolean("splitWestEastEnabled");
        }
        if (tag.contains("FyaNightOnlyEnabled")) {
            fyaNightOnlyEnabled = tag.getBoolean("FyaNightOnlyEnabled");
        }
        readMovementSettingsFromNBT(tag);
        getAutomator().readSyncData(tag);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        handleUpdateTag(pkt.getTag(), registries);
    }

    public void setPowered(boolean powered) {
        if (!sensors.isEmpty()) {
            return;
        }
        this.powered = powered;

        for (BlockPos westEastLight : westEastLights) {
            if (level.getBlockEntity(westEastLight) instanceof TrafficLightBlockEntity light) {
                light.powerOff();
                light.setActive(EnumTrafficLightBulbTypes.DontCross, false, false);
            }
        }
        for (BlockPos northSouthLight : northSouthLights) {
            if (level.getBlockEntity(northSouthLight) instanceof TrafficLightBlockEntity light) {
                light.powerOff();
                light.setActive(EnumTrafficLightBulbTypes.DontCross, false, false);
            }
        }

        if (powered) {
            for (EnumTrafficLightBulbTypes bulbType : manualNorthSouthActive.keySet()) {
                for (BlockPos northSouthLight : northSouthLights) {
                    if (level.getBlockEntity(northSouthLight) instanceof TrafficLightBlockEntity light) {
                        light.setActive(bulbType, true, manualNorthSouthActive.get(bulbType));
                    }
                }
            }
            for (EnumTrafficLightBulbTypes bulbType : manualWestEastActive.keySet()) {
                for (BlockPos westEastLight : westEastLights) {
                    if (level.getBlockEntity(westEastLight) instanceof TrafficLightBlockEntity light) {
                        light.setActive(bulbType, true, manualWestEastActive.get(bulbType));
                    }
                }
            }
        } else {
            for (EnumTrafficLightBulbTypes bulbType : manualNorthSouthInactive.keySet()) {
                for (BlockPos northSouthLight : northSouthLights) {
                    if (level.getBlockEntity(northSouthLight) instanceof TrafficLightBlockEntity light) {
                        light.setActive(bulbType, true, manualNorthSouthInactive.get(bulbType));
                    }
                }
            }
            for (EnumTrafficLightBulbTypes bulbType : manualWestEastInactive.keySet()) {
                for (BlockPos westEastLight : westEastLights) {
                    if (level.getBlockEntity(westEastLight) instanceof TrafficLightBlockEntity light) {
                        light.setActive(bulbType, true, manualWestEastInactive.get(bulbType));
                    }
                }
            }
        }
        markDirty();
    }

    public boolean addOrRemoveWestEastTrafficLight(BlockPos pos) {
        if (westEastLights.contains(pos)) {
            westEastLights.remove(pos);
            return false;
        }
        westEastLights.add(pos);
        markDirty();
        return true;
    }

    public boolean addOrRemoveNorthSouthTrafficLight(BlockPos pos) {
        if (northSouthLights.contains(pos)) {
            northSouthLights.remove(pos);
            return false;
        }
        northSouthLights.add(pos);
        markDirty();
        return true;
    }

    public boolean addOrRemoveSensor(BlockPos pos) {
        if (sensors.contains(pos)) {
            sensors.remove(pos);
            markDirty();
            notifyUpdate();
            return false;
        }
        sensors.add(pos);
        markDirty();
        notifyUpdate();
        return true;
    }

    public boolean addOrRemoveNorthSouthPedButton(BlockPos pos) {
        if (northSouthPedButtons.contains(pos)) {
            northSouthPedButtons.remove(pos);
            markDirty();
            notifyUpdate();
            return false;
        }
        northSouthPedButtons.add(pos);
        markDirty();
        notifyUpdate();
        return true;
    }

    public boolean addOrRemoveWestEastPedButton(BlockPos pos) {
        if (westEastPedButtons.contains(pos)) {
            westEastPedButtons.remove(pos);
            markDirty();
            notifyUpdate();
            return false;
        }
        westEastPedButtons.add(pos);
        markDirty();
        notifyUpdate();
        return true;
    }

    public void addRemoveNorthSouthActive(EnumTrafficLightBulbTypes type, boolean flash, boolean add) {
        if (add) {
            manualNorthSouthActive.put(type, flash);
        } else if (flash) {
            manualNorthSouthActive.put(type, false);
        } else {
            manualNorthSouthActive.remove(type);
        }
    }

    public void addRemoveWestEastActive(EnumTrafficLightBulbTypes type, boolean flash, boolean add) {
        if (add) {
            manualWestEastActive.put(type, flash);
        } else if (flash) {
            manualWestEastActive.put(type, false);
        } else {
            manualWestEastActive.remove(type);
        }
    }

    public void addRemoveNorthSouthInactive(EnumTrafficLightBulbTypes type, boolean flash, boolean add) {
        if (add) {
            manualNorthSouthInactive.put(type, flash);
        } else if (flash) {
            manualNorthSouthInactive.put(type, false);
        } else {
            manualNorthSouthInactive.remove(type);
        }
    }

    public void addRemoveWestEastInactive(EnumTrafficLightBulbTypes type, boolean flash, boolean add) {
        if (add) {
            manualWestEastInactive.put(type, flash);
        } else if (flash) {
            manualWestEastInactive.put(type, false);
        } else {
            manualWestEastInactive.remove(type);
        }
    }

    public void setNorth(Boolean hi) {
        hasNorth = hi;
        markDirty();
    }

    public void setSouth(Boolean hi) {
        hasSouth = hi;
        markDirty();
    }

    public void setEast(Boolean hi) {
        hasEast = hi;
        markDirty();
    }

    public void setWest(Boolean hi) {
        hasWest = hi;
        markDirty();
    }

    @Override
    public CompoundTag getClientToServerUpdateTag(HolderLookup.Provider registries) {
        CompoundTag compound = new CompoundTag();
        writeManualSettingDictionary(compound, manualNorthSouthActive, "manualNorthSouthActive");
        writeManualSettingDictionary(compound, manualWestEastActive, "manualWestEastActive");
        writeManualSettingDictionary(compound, manualNorthSouthInactive, "manualNorthSouthInactive");
        writeManualSettingDictionary(compound, manualWestEastInactive, "manualWestEastInactive");
        compound.putBoolean("hasNorth", hasNorth);
        compound.putBoolean("hasSouth", hasSouth);
        compound.putBoolean("hasEast", hasEast);
        compound.putBoolean("hasWest", hasWest);
        writeMovementSettingsToNBT(compound);
        getAutomator().setSyncData(compound);
        return compound;
    }

    @Override
    public void handleClientToServerUpdateTag(CompoundTag compound, HolderLookup.Provider registries) {
        readManualSettingDictionary(compound, manualNorthSouthActive, "manualNorthSouthActive");
        readManualSettingDictionary(compound, manualWestEastActive, "manualWestEastActive");
        readManualSettingDictionary(compound, manualNorthSouthInactive, "manualNorthSouthInactive");
        readManualSettingDictionary(compound, manualWestEastInactive, "manualWestEastInactive");

        getAutomator().readSyncData(compound);
        if (compound.contains("hasNorth")) {
            this.hasNorth = compound.getBoolean("hasNorth");
        }
        if (compound.contains("hasSouth")) {
            this.hasSouth = compound.getBoolean("hasSouth");
        }
        if (compound.contains("hasEast")) {
            this.hasEast = compound.getBoolean("hasEast");
        }
        if (compound.contains("hasWest")) {
            this.hasWest = compound.getBoolean("hasWest");
        }
        readMovementSettingsFromNBT(compound);
        markDirty();
        notifyUpdate();
    }

    public boolean hasSpecificNorthSouthManualOption(EnumTrafficLightBulbTypes bulbType, boolean flash, boolean forActive) {
        if (forActive) {
            boolean result = manualNorthSouthActive.containsKey(bulbType);
            if (flash) {
                result = result && manualNorthSouthActive.get(bulbType);
            }
            return result;
        }
        boolean result = manualNorthSouthInactive.containsKey(bulbType);
        if (flash) {
            result = result && manualNorthSouthInactive.get(bulbType);
        }
        return result;
    }

    public boolean hasSpecificWestEastManualOption(EnumTrafficLightBulbTypes bulbType, boolean flash, boolean forActive) {
        if (forActive) {
            boolean result = manualWestEastActive.containsKey(bulbType);
            if (flash) {
                result = result && manualWestEastActive.get(bulbType);
            }
            return result;
        }
        boolean result = manualWestEastInactive.containsKey(bulbType);
        if (flash) {
            result = result && manualWestEastInactive.get(bulbType);
        }
        return result;
    }

    // --- server tick (was ITickable.update) ---

    public static void serverTick(Level level, BlockPos pos, BlockState state, TrafficLightControlBoxBlockEntity be) {
        be.serverTick();
    }

    private void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }

        // Recovery flash countdown (replaces the 1.12.2 background thread).
        if (isFlashingEmergency && emergencyFlashTicksRemaining > 0) {
            emergencyFlashTicksRemaining--;
            if (emergencyFlashTicksRemaining <= 0) {
                getAutomator().reset();
                isFlashingEmergency = false;
            }
        }

        if (!sensors.isEmpty() || !northSouthPedButtons.isEmpty() || !westEastPedButtons.isEmpty()) {
            boolean wasPowered = this.powered;
            boolean isNowPowered = level.hasNeighborSignal(worldPosition);
            if (wasPowered != isNowPowered) {
                setPowered(isNowPowered);
                if (!isNowPowered) {
                    enterDarkMode();
                } else {
                    isInDarkMode = false;
                    flashRedYellowForRecovery();
                }
            }
            this.powered = isNowPowered;
            getAutomator().update();
        }
    }

    private void enterDarkMode() {
        isInDarkMode = true;
        for (BlockPos pos : northSouthLights) {
            if (level.getBlockEntity(pos) instanceof TrafficLightBlockEntity light) {
                light.powerOff();
            }
        }
        for (BlockPos pos : westEastLights) {
            if (level.getBlockEntity(pos) instanceof TrafficLightBlockEntity light) {
                light.powerOff();
            }
        }
    }

    private void flashRedYellowForRecovery() {
        isFlashingEmergency = true;

        if (!northMain) {
            for (BlockPos pos : northSouthLights) {
                if (level.getBlockEntity(pos) instanceof TrafficLightBlockEntity light) {
                    light.setActive(EnumTrafficLightBulbTypes.Red, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.Red2, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.StraightRed, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, true);
                }
            }
            for (BlockPos pos : westEastLights) {
                if (level.getBlockEntity(pos) instanceof TrafficLightBlockEntity light) {
                    light.setActive(EnumTrafficLightBulbTypes.Yellow, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.StraightYellow, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, true);
                }
            }
        } else {
            for (BlockPos pos : westEastLights) {
                if (level.getBlockEntity(pos) instanceof TrafficLightBlockEntity light) {
                    light.setActive(EnumTrafficLightBulbTypes.Red, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.Red2, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.StraightRed, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, true);
                }
            }
            for (BlockPos pos : northSouthLights) {
                if (level.getBlockEntity(pos) instanceof TrafficLightBlockEntity light) {
                    light.setActive(EnumTrafficLightBulbTypes.Yellow, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.StraightYellow, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, true);
                    light.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, true);
                }
            }
        }

        emergencyFlashTicksRemaining = 15 * 20; // 15 seconds
    }

    public void onBreak(Level level) {
        for (BlockPos pos : northSouthPedButtons) {
            if (level.getBlockEntity(pos) instanceof PedestrianButtonBlockEntity ped) {
                ped.removePairedBox(getBlockPos());
            }
        }
        for (BlockPos pos : westEastPedButtons) {
            if (level.getBlockEntity(pos) instanceof PedestrianButtonBlockEntity ped) {
                ped.removePairedBox(getBlockPos());
            }
        }
    }

    public Automator getAutomator() {
        if (automator == null) {
            automator = new Automator();
        }
        return automator;
    }

    private enum LeftTripDirection {
        NONE,
        NORTH_SOUTH_LEFT,
        EAST_WEST_LEFT
    }

    // --- automation engine ---

    public class Automator {
        private long nextUpdate;
        private boolean hasInitialized = false;

        private final String nbtPrefix = "automated_";

        private Stages lastStage = Stages.Red;
        private long stageStartTime = 0;

        private RightOfWays lastRightOfWay = RightOfWays.EastWest;
        private RightOfWays forcedNextRightOfWay = null;
        private Direction activeSplitDirection = Direction.NORTH;
        private Direction nextNorthSouthSplitDirection = Direction.NORTH;
        private Direction nextWestEastSplitDirection = Direction.EAST;
        private boolean swappedWithinCurrentRow = false;
        private RightOfWays pendingSplitSwapRow = null;
        private Direction pendingSplitSwapDirection = null;
        private final Direction[] splitOrder = new Direction[] { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST };

        private double greenMinimumNS = 0;
        private double greenMinimumEW = 0;
        private double greenMaxNS = 10;
        private double greenMaxEW = 10;
        private double yellowTimeNS = 3;
        private double yellowTimeEW = 3;
        private double redTimeNS = 2;
        private double redTimeEW = 2;
        private double arrowMinimumNS = 0;
        private double arrowMinimumEW = 0;
        public double arrowMaxNS = 5;
        public double arrowMaxEW = 5;
        private double crossTime = 5;
        private double crossWarningTime = 7;
        private double rightArrowTime = 5;
        private boolean northSouthPedQueued;
        private boolean westEastPedQueued;

        public Automator() {
            this.crossTime = Config.hawkDefaultSolidRedSeconds;
            this.crossWarningTime = Config.hawkDefaultFlashRedSeconds;
        }

        public void reset() {
            lastStage = Stages.Red;
            forcedNextRightOfWay = null;
            if (northMain) {
                lastRightOfWay = RightOfWays.EastWest;
            } else {
                lastRightOfWay = RightOfWays.NorthSouth;
            }
            activeSplitDirection = Direction.NORTH;
            nextNorthSouthSplitDirection = Direction.NORTH;
            nextWestEastSplitDirection = Direction.EAST;
            swappedWithinCurrentRow = false;
            pendingSplitSwapRow = null;
            pendingSplitSwapDirection = null;

            hasInitialized = false;
            nextUpdate = 0;
            stageStartTime = 0;
        }

        private Direction getOtherRowDirection(RightOfWays row, Direction dir) {
            if (row == RightOfWays.NorthSouth) {
                return dir == Direction.NORTH ? Direction.SOUTH : Direction.NORTH;
            }
            return dir == Direction.EAST ? Direction.WEST : Direction.EAST;
        }

        private Direction getAndAdvancePreferredSplitDirection(RightOfWays row) {
            if (row == RightOfWays.NorthSouth) {
                Direction chosen = nextNorthSouthSplitDirection;
                nextNorthSouthSplitDirection = (nextNorthSouthSplitDirection == Direction.NORTH) ? Direction.SOUTH : Direction.NORTH;
                return chosen;
            }
            Direction chosen = nextWestEastSplitDirection;
            nextWestEastSplitDirection = (nextWestEastSplitDirection == Direction.EAST) ? Direction.WEST : Direction.EAST;
            return chosen;
        }

        private Direction chooseSplitDirectionForRowFixedOrder(RightOfWays row) {
            Direction preferred = getAndAdvancePreferredSplitDirection(row);
            Direction other = getOtherRowDirection(row, preferred);

            if (!isSplitDirectionEnabled(preferred) && isSplitDirectionEnabled(other)) {
                preferred = other;
                other = getOtherRowDirection(row, preferred);
            }

            final boolean preferredDemand = isSplitDirectionEnabled(preferred) && hasAnyDemandForFacing(preferred);
            final boolean otherDemand = isSplitDirectionEnabled(other) && hasAnyDemandForFacing(other);
            if (!preferredDemand && otherDemand) {
                return other;
            }
            return preferred;
        }

        private boolean isSplitDirectionEnabled(Direction facing) {
            switch (facing) {
                case NORTH:
                    return TrafficLightControlBoxBlockEntity.this.hasNorth;
                case SOUTH:
                    return TrafficLightControlBoxBlockEntity.this.hasSouth;
                case EAST:
                    return TrafficLightControlBoxBlockEntity.this.hasEast;
                case WEST:
                    return TrafficLightControlBoxBlockEntity.this.hasWest;
                default:
                    return true;
            }
        }

        private int splitIndex(Direction facing) {
            for (int i = 0; i < splitOrder.length; i++) {
                if (splitOrder[i] == facing) {
                    return i;
                }
            }
            return 0;
        }

        private Direction getNextSplitDirection(Direction current) {
            final int start = splitIndex(current);
            for (int step = 1; step <= splitOrder.length; step++) {
                Direction candidate = splitOrder[(start + step) % splitOrder.length];
                if (!isSplitDirectionEnabled(candidate)) {
                    continue;
                }
                if (hasAnyDemandForFacing(candidate)) {
                    return candidate;
                }
            }
            for (int step = 1; step <= splitOrder.length; step++) {
                Direction candidate = splitOrder[(start + step) % splitOrder.length];
                if (isSplitDirectionEnabled(candidate)) {
                    return candidate;
                }
            }
            return current;
        }

        private Direction getRowDir1(RightOfWays row) {
            return row == RightOfWays.NorthSouth ? Direction.NORTH : Direction.EAST;
        }

        private Direction getRowDir2(RightOfWays row) {
            return row == RightOfWays.NorthSouth ? Direction.SOUTH : Direction.WEST;
        }

        private boolean isApproachEnabled(Direction facing) {
            return isSplitDirectionEnabled(facing);
        }

        private boolean hasAnyDemandForRow(RightOfWays row) {
            final Direction dir1 = getRowDir1(row);
            final Direction dir2 = getRowDir2(row);
            final boolean enabled1 = isApproachEnabled(dir1);
            final boolean enabled2 = isApproachEnabled(dir2);
            if (!enabled1 && !enabled2) {
                return false;
            }
            return (enabled1 && hasAnyDemandForFacing(dir1)) || (enabled2 && hasAnyDemandForFacing(dir2));
        }

        private Direction pickSplitDirectionForRow(RightOfWays row, Direction current) {
            final Direction dir1 = getRowDir1(row);
            final Direction dir2 = getRowDir2(row);

            Direction normalized = (current == dir1 || current == dir2) ? current : dir1;
            if (!isApproachEnabled(normalized)) {
                normalized = isApproachEnabled(dir1) ? dir1 : dir2;
            }

            final boolean demand1 = isApproachEnabled(dir1) && hasAnyDemandForFacing(dir1);
            final boolean demand2 = isApproachEnabled(dir2) && hasAnyDemandForFacing(dir2);

            if (demand1 && !demand2) {
                return dir1;
            }
            if (demand2 && !demand1) {
                return dir2;
            }
            if (demand1 && demand2) {
                return normalized == dir1 ? dir2 : dir1;
            }
            return normalized;
        }

        public double getGreenMinimumNS() {
            return greenMinimumNS;
        }

        public double getGreenMinimumEW() {
            return greenMinimumEW;
        }

        public void setGreenMinimumEW(double greenMinimum) {
            this.greenMinimumEW = greenMinimum;
        }

        public void setGreenMinimumNS(double greenMinimum) {
            this.greenMinimumNS = greenMinimum;
        }

        public double getGreenMaxNS() {
            return greenMaxNS;
        }

        public void setGreenMaxEW(double greenMinimum) {
            this.greenMaxEW = greenMinimum;
        }

        public double getGreenMaxEW() {
            return greenMaxEW;
        }

        public void setGreenMaxNS(double greenMinimum) {
            this.greenMaxNS = greenMinimum;
        }

        public double getYellowTimeNS() {
            return yellowTimeNS;
        }

        public double getYellowTimeEW() {
            return yellowTimeEW;
        }

        public void setYellowTimeNS(double yellowTime) {
            this.yellowTimeNS = yellowTime;
        }

        public void setYellowTimeEW(double yellowTime) {
            this.yellowTimeEW = yellowTime;
        }

        public double getRedTimeNS() {
            return redTimeNS;
        }

        public double getRedTimeEW() {
            return redTimeEW;
        }

        public void setRedTimeNS(double redTime) {
            this.redTimeNS = redTime;
        }

        public void setRedTimeEW(double redTime) {
            this.redTimeEW = redTime;
        }

        public double getArrowMinimumNS() {
            return arrowMinimumNS;
        }

        public double getArrowMinimumES() {
            return arrowMinimumEW;
        }

        public void setArrowMinimumNS(double arrowMinimum) {
            this.arrowMinimumNS = arrowMinimum;
        }

        public void setArrowMinimumEW(double arrowMinimum) {
            this.arrowMinimumEW = arrowMinimum;
        }

        public double getArrowMaxNS() {
            return arrowMaxNS;
        }

        public double getArrowMaxEW() {
            return arrowMaxEW;
        }

        public void setArrowMaxNS(double arrowMinimum) {
            this.arrowMaxNS = arrowMinimum;
        }

        public void setArrowMaxEW(double arrowMinimum) {
            this.arrowMaxEW = arrowMinimum;
        }

        public double getCrossTime() {
            return crossTime;
        }

        public void setCrossTime(double crossTime) {
            this.crossTime = crossTime;
        }

        public double getCrossWarningTime() {
            return crossWarningTime;
        }

        public void setCrossWarningTime(double crossWarningTime) {
            this.crossWarningTime = crossWarningTime;
        }

        public double getRightArrowTime() {
            return rightArrowTime;
        }

        public void setRightArrowTime(double rightArrowTime) {
            this.rightArrowTime = rightArrowTime;
        }

        public boolean isNorthSouthPedQueued() {
            return northSouthPedQueued;
        }

        public void setNorthSouthPedQueued(boolean northSouthPedQueued) {
            this.northSouthPedQueued = northSouthPedQueued;
        }

        public boolean isWestEastPedQueued() {
            return westEastPedQueued;
        }

        public void setWestEastPedQueued(boolean westEastPedQueued) {
            this.westEastPedQueued = westEastPedQueued;
        }

        public void update() {
            long time = level.getDayTime() % 24000;
            inNightFlash = (time >= nightFlashStart || time <= nightFlashEnd);

            if (fyaNightOnlyEnabled && lastInNightFlash && !inNightFlash) {
                final double yellowSeconds = (lastRightOfWay == RightOfWays.NorthSouth) ? getYellowTimeNS() : getYellowTimeEW();
                final int yellowTicks = (int) Math.max(10, Math.min(200, Math.round(yellowSeconds * 20.0)));
                fyaDayTransitionTicksRemaining = yellowTicks;
            }
            lastInNightFlash = inNightFlash;
            if (fyaDayTransitionTicksRemaining > 0) {
                fyaDayTransitionTicksRemaining--;
            }

            if (!inNightFlash || !nightFlashEnabled) {
                isFlashOn = false;
            }

            if (nightFlashEnabled && inNightFlash) {
                if (!isFlashOn) {
                    for (BlockPos pos : northSouthLights) {
                        if (level.getBlockEntity(pos) instanceof TrafficLightBlockEntity light) {
                            light.powerOff();
                        }
                    }
                    for (BlockPos pos : westEastLights) {
                        if (level.getBlockEntity(pos) instanceof TrafficLightBlockEntity light) {
                            light.powerOff();
                        }
                    }

                    if (!northMain) {
                        for (BlockPos pos : northSouthLights) {
                            if (level.getBlockEntity(pos) instanceof TrafficLightBlockEntity light) {
                                light.setActive(EnumTrafficLightBulbTypes.Red, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.Red2, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.StraightRed, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, true);
                            }
                        }
                        for (BlockPos pos : westEastLights) {
                            if (level.getBlockEntity(pos) instanceof TrafficLightBlockEntity light) {
                                light.setActive(EnumTrafficLightBulbTypes.Yellow, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.StraightYellow, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, true);
                            }
                        }
                    } else {
                        for (BlockPos pos : westEastLights) {
                            if (level.getBlockEntity(pos) instanceof TrafficLightBlockEntity light) {
                                light.setActive(EnumTrafficLightBulbTypes.Red, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.Red2, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.StraightRed, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, true);
                            }
                        }
                        for (BlockPos pos : northSouthLights) {
                            if (level.getBlockEntity(pos) instanceof TrafficLightBlockEntity light) {
                                light.setActive(EnumTrafficLightBulbTypes.Yellow, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.StraightYellow, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, true);
                                light.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, true);
                            }
                        }
                    }

                    isFlashOn = true;
                }
                return;
            }

            if (isInDarkMode || isFlashingEmergency) {
                return;
            }

            if (!hasInitialized) {
                initialize();
            }

            if (TrafficLightControlBoxBlockEntity.this.isHawkBeaconEnabled()) {
                lastRightOfWay = northMain ? RightOfWays.NorthSouth : RightOfWays.EastWest;
            }

            if (System.currentTimeMillis() < nextUpdate) {
                return;
            }

            if (lastStage == Stages.Red) {
                if (!TrafficLightControlBoxBlockEntity.this.isHawkBeaconEnabled()) {
                    if (forcedNextRightOfWay != null) {
                        lastRightOfWay = forcedNextRightOfWay;
                    } else if (pendingSplitSwapRow != null && pendingSplitSwapDirection != null) {
                        lastRightOfWay = pendingSplitSwapRow;
                    } else {
                        lastRightOfWay = lastRightOfWay.getNext();
                    }
                    forcedNextRightOfWay = null;
                    swappedWithinCurrentRow = false;
                    boolean appliedPendingSwap = false;

                    if (pendingSplitSwapRow == lastRightOfWay && pendingSplitSwapDirection != null) {
                        activeSplitDirection = pendingSplitSwapDirection;
                        pendingSplitSwapRow = null;
                        pendingSplitSwapDirection = null;
                        swappedWithinCurrentRow = true;
                        appliedPendingSwap = true;
                    }

                    if (!appliedPendingSwap && TrafficLightControlBoxBlockEntity.this.isSplitEnabledForRow(lastRightOfWay)) {
                        activeSplitDirection = chooseSplitDirectionForRowFixedOrder(lastRightOfWay);
                    }
                }
            }

            SensorCheckResult sensorResults = checkSensors(lastRightOfWay);
            applyApproachEnableRules(lastRightOfWay, sensorResults);
            lastStage = updateLightsByStage(getNextLogicalStage(lastStage, lastRightOfWay, sensorResults));

            markDirty();
        }

        private LeftTripDirection getTrippedLeftDirection(SensorCheckResult sensorResults, RightOfWays currentRightOfWay) {
            if (currentRightOfWay == RightOfWays.NorthSouth && sensorResults.Direction1SensorLeft) {
                return LeftTripDirection.NORTH_SOUTH_LEFT;
            }
            if (currentRightOfWay == RightOfWays.EastWest && sensorResults.Direction2SensorLeft) {
                return LeftTripDirection.EAST_WEST_LEFT;
            }
            return LeftTripDirection.NONE;
        }

        private void applyApproachEnableRules(RightOfWays row, SensorCheckResult result) {
            final Direction dir1 = getRowDir1(row);
            final Direction dir2 = getRowDir2(row);
            final boolean dir1Enabled = isApproachEnabled(dir1);
            final boolean dir2Enabled = isApproachEnabled(dir2);

            if (!dir1Enabled) {
                result.Direction1Sensor = false;
                result.Direction1SensorLeft = false;
                result.Direction1SensorRight = false;
            }
            if (!dir2Enabled) {
                result.Direction2Sensor = false;
                result.Direction2SensorLeft = false;
                result.Direction2SensorRight = false;
            }

            if (dir1Enabled && !dir2Enabled && result.Direction1SensorLeft) {
                result.Direction1Sensor = true;
                result.Direction1SensorLeft = false;
            }
            if (dir2Enabled && !dir1Enabled && result.Direction2SensorLeft) {
                result.Direction2Sensor = true;
                result.Direction2SensorLeft = false;
            }

            applyMovementSensorRules(dir1, result, true);
            applyMovementSensorRules(dir2, result, false);
        }

        private void applyMovementSensorRules(Direction facing, SensorCheckResult result, boolean isDir1) {
            ApproachMovementSettings settings = TrafficLightControlBoxBlockEntity.this.getMovementSettings(facing);
            if (!settings.straightEnabled) {
                if (isDir1) {
                    result.Direction1Sensor = false;
                } else {
                    result.Direction2Sensor = false;
                }
            }
            if (!settings.leftEnabled) {
                if (isDir1) {
                    result.Direction1SensorLeft = false;
                } else {
                    result.Direction2SensorLeft = false;
                }
            }
            if (!settings.rightEnabled) {
                if (isDir1) {
                    result.Direction1SensorRight = false;
                } else {
                    result.Direction2SensorRight = false;
                }
            }
        }

        private void initialize() {
            for (BlockPos bp : northSouthLights) {
                if (level.getBlockEntity(bp) instanceof TrafficLightBlockEntity te) {
                    te.powerOff();
                }
            }
            for (BlockPos bp : westEastLights) {
                if (level.getBlockEntity(bp) instanceof TrafficLightBlockEntity te) {
                    te.powerOff();
                }
            }
            hasInitialized = true;
        }

        private List<TrafficLightBlockEntity> resolveLights(List<BlockPos> positions) {
            return positions.stream()
                    .map(p -> level.getBlockEntity(p) instanceof TrafficLightBlockEntity te ? te : null)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        private Stages updateLightsByStage(Stages stage) {
            final boolean hawkStage = stage == Stages.HawkFlashYellow || stage == Stages.HawkSolidYellow
                    || stage == Stages.HawkSolidRed || stage == Stages.HawkFlashRed;
            if (hawkStage && TrafficLightControlBoxBlockEntity.this.isHawkBeaconEnabled()) {
                lastRightOfWay = northMain ? RightOfWays.NorthSouth : RightOfWays.EastWest;
            }

            List<BlockPos> trafficLightPosForRightOfWay;
            List<BlockPos> trafficLightPosOpposingRightOfWay;
            List<TrafficLightBlockEntity> trafficLightsForRightOfWay;
            List<TrafficLightBlockEntity> trafficLightsOpposingRightOfWay;
            Direction direction1;
            Direction direction2;

            if (lastRightOfWay == RightOfWays.NorthSouth) {
                trafficLightPosForRightOfWay = northSouthLights;
                trafficLightPosOpposingRightOfWay = westEastLights;
                direction1 = Direction.NORTH;
                direction2 = Direction.SOUTH;
            } else {
                trafficLightPosForRightOfWay = westEastLights;
                trafficLightPosOpposingRightOfWay = northSouthLights;
                direction1 = Direction.EAST;
                direction2 = Direction.WEST;
            }

            trafficLightsForRightOfWay = resolveLights(trafficLightPosForRightOfWay);
            trafficLightsOpposingRightOfWay = resolveLights(trafficLightPosOpposingRightOfWay);

            final Direction direction1cw = direction1.getClockWise();
            final Direction direction2cw = direction2.getClockWise();
            final Direction fDirection1 = direction1;
            final Direction fDirection2 = direction2;

            switch (stage) {
                case Red:
                    trafficLightsForRightOfWay.forEach(tl -> {
                        tl.powerOff();
                        tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
                    });
                    trafficLightsOpposingRightOfWay.forEach(tl -> {
                        tl.powerOff();
                        tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
                    });
                    break;
                case Direction1RightTurnArrow:
                case Direction1LeftTurnArrow:
                    trafficLightsForRightOfWay.forEach(tl -> {
                        if (TrafficLightFacingResolver.isFacing(tl, fDirection1.getOpposite())) {
                            tl.powerOff();
                            tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
                            return;
                        }
                        if (!TrafficLightFacingResolver.isFacing(tl, fDirection1)) {
                            return;
                        }
                        tl.powerOff();
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowUTurn, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowUTurn2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.Green, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.StraightGreen, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight2, true, false);
                    });
                    trafficLightsOpposingRightOfWay.forEach(tl -> {
                        if (!TrafficLightFacingResolver.isFacing(tl, direction1cw)) {
                            tl.powerOff();
                            tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
                            return;
                        }
                        tl.powerOff();
                        tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
                    });
                    break;
                case Direction2RightTurnArrow:
                case Direction2LeftTurnArrow:
                    trafficLightsForRightOfWay.forEach(tl -> {
                        if (TrafficLightFacingResolver.isFacing(tl, fDirection2.getOpposite())) {
                            tl.powerOff();
                            tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
                            final boolean allowFyaNow = !TrafficLightControlBoxBlockEntity.this.fyaNightOnlyEnabled || TrafficLightControlBoxBlockEntity.this.inNightFlash;
                            final boolean isFyaDayTransition = TrafficLightControlBoxBlockEntity.this.fyaNightOnlyEnabled
                                    && !TrafficLightControlBoxBlockEntity.this.inNightFlash
                                    && TrafficLightControlBoxBlockEntity.this.fyaDayTransitionTicksRemaining > 0;
                            if (allowFyaNow) {
                                tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft, true, true);
                                tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn, true, true);
                                tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft2, true, true);
                                tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn2, true, true);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                            } else if (isFyaDayTransition) {
                                tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                            } else {
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
                            }
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
                            return;
                        }
                        if (!TrafficLightFacingResolver.isFacing(tl, fDirection2)) {
                            return;
                        }
                        tl.powerOff();
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowUTurn, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowUTurn2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.Green, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.StraightGreen, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight2, true, false);
                    });
                    trafficLightsOpposingRightOfWay.forEach(tl -> {
                        if (!TrafficLightFacingResolver.isFacing(tl, direction2cw)) {
                            tl.powerOff();
                            tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
                            return;
                        }
                        tl.powerOff();
                        tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
                    });
                    break;
                case BothTurnArrow:
                    trafficLightsForRightOfWay.forEach(tl -> {
                        tl.powerOff();
                        tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowUTurn, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowUTurn2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
                    });
                    trafficLightsOpposingRightOfWay.forEach(tl -> {
                        tl.powerOff();
                        tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
                    });
                    break;
                case Direction1LeftTurnArrowYellow:
                    trafficLightsForRightOfWay.forEach(tl -> {
                        if (TrafficLightFacingResolver.isFacing(tl, fDirection1.getOpposite())) {
                            tl.powerOff();
                            tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
                            final boolean allowFyaNow = !TrafficLightControlBoxBlockEntity.this.fyaNightOnlyEnabled || TrafficLightControlBoxBlockEntity.this.inNightFlash;
                            final boolean isFyaDayTransition = TrafficLightControlBoxBlockEntity.this.fyaNightOnlyEnabled
                                    && !TrafficLightControlBoxBlockEntity.this.inNightFlash
                                    && TrafficLightControlBoxBlockEntity.this.fyaDayTransitionTicksRemaining > 0;
                            if (allowFyaNow) {
                                tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft, true, true);
                                tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn, true, true);
                                tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft2, true, true);
                                tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn2, true, true);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                            } else if (isFyaDayTransition) {
                                tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                            } else {
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
                            }
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
                        }
                        if (!TrafficLightFacingResolver.isFacing(tl, fDirection1)) {
                            return;
                        }
                        tl.powerOff();
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft3, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn3, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowUTurn2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.Green, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.StraightGreen, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight3, true, false);
                    });
                    trafficLightsOpposingRightOfWay.forEach(tl -> {
                        if (TrafficLightFacingResolver.isFacing(tl, direction1cw.getOpposite())) {
                            tl.powerOff();
                            tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
                        }
                        if (!TrafficLightFacingResolver.isFacing(tl, direction1cw)) {
                            return;
                        }
                        tl.powerOff();
                        tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight3, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
                    });
                    break;
                case Direction2LeftTurnArrowYellow:
                    trafficLightsForRightOfWay.forEach(tl -> {
                        if (TrafficLightFacingResolver.isFacing(tl, fDirection2.getOpposite())) {
                            tl.powerOff();
                            tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
                            final boolean allowFyaNow = !TrafficLightControlBoxBlockEntity.this.fyaNightOnlyEnabled || TrafficLightControlBoxBlockEntity.this.inNightFlash;
                            final boolean isFyaDayTransition = TrafficLightControlBoxBlockEntity.this.fyaNightOnlyEnabled
                                    && !TrafficLightControlBoxBlockEntity.this.inNightFlash
                                    && TrafficLightControlBoxBlockEntity.this.fyaDayTransitionTicksRemaining > 0;
                            if (allowFyaNow) {
                                tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft, true, true);
                                tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn, true, true);
                                tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft2, true, true);
                                tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn2, true, true);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                            } else if (isFyaDayTransition) {
                                tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                            } else {
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
                            }
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
                        }
                        if (!TrafficLightFacingResolver.isFacing(tl, fDirection2)) {
                            return;
                        }
                        tl.powerOff();
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft3, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn3, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight3, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowUTurn2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.Green, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.StraightGreen, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight, true, false);
                    });
                    trafficLightsOpposingRightOfWay.forEach(tl -> {
                        if (TrafficLightFacingResolver.isFacing(tl, direction2cw.getOpposite())) {
                            tl.powerOff();
                            tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
                        }
                        if (!TrafficLightFacingResolver.isFacing(tl, direction2cw)) {
                            return;
                        }
                        tl.powerOff();
                        tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight3, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
                    });
                    break;
                case BothTurnArrowYellow:
                    trafficLightsForRightOfWay.forEach(tl -> {
                        tl.powerOff();
                        tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn3, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft3, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowUTurn2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
                    });
                    trafficLightsOpposingRightOfWay.forEach(tl -> {
                        tl.powerOff();
                        tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight3, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
                    });
                    break;
                case Direction1RightTurnArrowYellow:
                    trafficLightsForRightOfWay.forEach(tl -> {
                        if (!TrafficLightFacingResolver.isFacing(tl, fDirection1)) {
                            return;
                        }
                        tl.powerOff();
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight3, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowUTurn2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.Yellow, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.StraightYellow, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight, true, false);
                    });
                    trafficLightsOpposingRightOfWay.forEach(tl -> {
                        if (!TrafficLightFacingResolver.isFacing(tl, direction1cw)) {
                            return;
                        }
                        tl.powerOff();
                        tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight3, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
                    });
                    break;
                case Direction2RightTurnArrowYellow:
                    trafficLightsForRightOfWay.forEach(tl -> {
                        if (!TrafficLightFacingResolver.isFacing(tl, fDirection2)) {
                            return;
                        }
                        tl.powerOff();
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight3, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowUTurn2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.Yellow, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.StraightYellow, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight, true, false);
                    });
                    trafficLightsOpposingRightOfWay.forEach(tl -> {
                        if (!TrafficLightFacingResolver.isFacing(tl, direction2cw)) {
                            return;
                        }
                        tl.powerOff();
                        tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight3, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
                    });
                    break;
                case Yellow: {
                    final boolean splitYellow = TrafficLightControlBoxBlockEntity.this.isSplitEnabledForRow(lastRightOfWay);
                    final Direction splitYellowDir = activeSplitDirection;
                    final boolean allowFyaNowYellow = !TrafficLightControlBoxBlockEntity.this.fyaNightOnlyEnabled || TrafficLightControlBoxBlockEntity.this.inNightFlash;
                    final boolean isFyaDayTransitionYellow = TrafficLightControlBoxBlockEntity.this.fyaNightOnlyEnabled
                            && !TrafficLightControlBoxBlockEntity.this.inNightFlash
                            && TrafficLightControlBoxBlockEntity.this.fyaDayTransitionTicksRemaining > 0;
                    trafficLightsForRightOfWay.forEach(tl -> {
                        tl.powerOff();
                        final boolean facesDir1 = TrafficLightFacingResolver.isFacing(tl, fDirection1);
                        final boolean facesDir2 = TrafficLightFacingResolver.isFacing(tl, fDirection2);
                        if (splitYellow) {
                            final boolean allowYellow = (facesDir1 && splitYellowDir == fDirection1) || (facesDir2 && splitYellowDir == fDirection2);
                            if (!allowYellow) {
                                tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
                                return;
                            }
                        }

                        final boolean oppositeApproachDisabled = !splitYellow
                                && ((facesDir1 && !isApproachEnabled(fDirection2)) || (facesDir2 && !isApproachEnabled(fDirection1)));
                        final boolean showYellowTurnArrows = oppositeApproachDisabled || allowFyaNowYellow || isFyaDayTransitionYellow;
                        if (showYellowTurnArrows) {
                            tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft2, false, false);
                            tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft3, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight3, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn2, false, false);
                            tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight2, false, false);
                        } else {
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
                        }

                        tl.setActive(EnumTrafficLightBulbTypes.Yellow, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.StraightYellow, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
                    });
                    trafficLightsOpposingRightOfWay.forEach(tl -> {
                        final Direction opposingRightTurnFacing;
                        if (splitYellowDir == fDirection1) {
                            opposingRightTurnFacing = direction1cw;
                        } else if (splitYellowDir == fDirection2) {
                            opposingRightTurnFacing = direction2cw;
                        } else {
                            opposingRightTurnFacing = null;
                        }
                        final boolean allowOpposingRightTurnYellow;
                        if ((allowFyaNowYellow || isFyaDayTransitionYellow) && splitYellow && opposingRightTurnFacing != null) {
                            allowOpposingRightTurnYellow = TrafficLightFacingResolver.isFacing(tl, opposingRightTurnFacing);
                        } else {
                            allowOpposingRightTurnYellow = false;
                        }

                        tl.powerOff();
                        tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
                        if (allowOpposingRightTurnYellow) {
                            tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight3, true, false);
                        } else {
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
                        }
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
                    });
                    break;
                }
                case Green:
                case GreenCross:
                case GreenDontCrossWarning: {
                    final boolean split = TrafficLightControlBoxBlockEntity.this.isSplitEnabledForRow(lastRightOfWay);
                    final Direction splitDir = activeSplitDirection;
                    final Stages fStage = stage;

                    trafficLightsForRightOfWay.forEach(tl -> {
                        final boolean facesDir1 = TrafficLightFacingResolver.isFacing(tl, fDirection1);
                        final boolean facesDir2 = TrafficLightFacingResolver.isFacing(tl, fDirection2);
                        final boolean allowGreen = !split || (facesDir1 && splitDir == fDirection1) || (facesDir2 && splitDir == fDirection2);
                        final boolean oppositeApproachDisabled = !split && ((facesDir1 && !isApproachEnabled(fDirection2)) || (facesDir2 && !isApproachEnabled(fDirection1)));
                        final boolean allowFyaNow = !TrafficLightControlBoxBlockEntity.this.fyaNightOnlyEnabled || TrafficLightControlBoxBlockEntity.this.inNightFlash;
                        final boolean isFyaDayTransition = TrafficLightControlBoxBlockEntity.this.fyaNightOnlyEnabled
                                && !TrafficLightControlBoxBlockEntity.this.inNightFlash
                                && TrafficLightControlBoxBlockEntity.this.fyaDayTransitionTicksRemaining > 0;

                        tl.powerOff();
                        if (allowGreen) {
                            if (!split) {
                                if (oppositeApproachDisabled) {
                                    tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft, true, false);
                                    tl.setActive(EnumTrafficLightBulbTypes.GreenArrowUTurn, true, false);
                                    tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight, true, false);
                                    tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft2, true, false);
                                    tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, false, false);
                                    tl.setActive(EnumTrafficLightBulbTypes.GreenArrowUTurn2, true, false);
                                    tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight2, true, false);
                                } else {
                                    if (allowFyaNow) {
                                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft, true, true);
                                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn, true, true);
                                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft2, true, true);
                                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn2, true, true);
                                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                                    } else if (isFyaDayTransition) {
                                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft, true, false);
                                        tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn, true, false);
                                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                                    } else {
                                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
                                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
                                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
                                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
                                    }
                                }
                            }
                            if (split || oppositeApproachDisabled || allowFyaNow || isFyaDayTransition) {
                                // leave off
                            } else {
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                            }
                            tl.setActive(EnumTrafficLightBulbTypes.Green, true, false);
                            if (split) {
                                tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft2, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight2, true, false);
                            }
                            tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.GreenArrowUTurn2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.StraightGreen, true, false);
                            if (split || oppositeApproachDisabled || allowFyaNow || isFyaDayTransition) {
                                // leave off
                            } else {
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
                            }
                            if (!oppositeApproachDisabled) {
                                if (allowFyaNow) {
                                    tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight2, true, true);
                                } else if (isFyaDayTransition) {
                                    tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight2, true, false);
                                }
                            }
                            tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);

                            if (fStage == Stages.GreenCross) {
                                tl.setActive(EnumTrafficLightBulbTypes.DontCross, false, false);
                                tl.setActive(EnumTrafficLightBulbTypes.Cross, true, false);
                            } else if (fStage == Stages.GreenDontCrossWarning) {
                                tl.setActive(EnumTrafficLightBulbTypes.DontCross, true, true);
                            }
                        } else {
                            final boolean allowRightTurnOnOtherApproach = split
                                    && lastRightOfWay == RightOfWays.EastWest
                                    && ((facesDir1 && splitDir == fDirection2) || (facesDir2 && splitDir == fDirection1));
                            tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
                            if (!allowRightTurnOnOtherApproach) {
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
                                tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
                            }
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
                        }
                    });
                    trafficLightsOpposingRightOfWay.forEach(tl -> {
                        final Direction opposingRightTurnFacing;
                        if (splitDir == fDirection1) {
                            opposingRightTurnFacing = direction1cw;
                        } else if (splitDir == fDirection2) {
                            opposingRightTurnFacing = direction2cw;
                        } else {
                            opposingRightTurnFacing = null;
                        }

                        final boolean allowOpposingRightTurn;
                        if (split && opposingRightTurnFacing != null) {
                            allowOpposingRightTurn = TrafficLightFacingResolver.isFacing(tl, opposingRightTurnFacing);
                        } else {
                            allowOpposingRightTurn = false;
                        }

                        tl.powerOff();
                        tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
                        if (allowOpposingRightTurn) {
                            tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight2, true, false);
                        } else {
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
                            tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
                        }
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
                    });
                    break;
                }
                case HawkFlashYellow:
                case HawkSolidYellow: {
                    final boolean flash = stage == Stages.HawkFlashYellow;
                    trafficLightsForRightOfWay.forEach(tl -> {
                        tl.powerOff();
                        tl.setActive(EnumTrafficLightBulbTypes.Yellow, true, flash);
                        tl.setActive(EnumTrafficLightBulbTypes.StraightYellow, true, flash);
                    });
                    trafficLightsOpposingRightOfWay.forEach(tl -> {
                        tl.powerOff();
                        tl.setActive(EnumTrafficLightBulbTypes.DontCross, true, false);
                    });
                    break;
                }
                case HawkSolidRed: {
                    trafficLightsForRightOfWay.forEach(tl -> {
                        tl.powerOff();
                        tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
                        tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
                    });
                    trafficLightsOpposingRightOfWay.forEach(tl -> {
                        tl.powerOff();
                        tl.setActive(EnumTrafficLightBulbTypes.DontCross, false, false);
                        tl.setActive(EnumTrafficLightBulbTypes.Cross, true, false);
                    });
                    break;
                }
                case HawkFlashRed: {
                    final long wigwagTicks = Math.max(1L, (long) Config.hawkWigwagPeriodTicks);
                    final boolean alt = ((level.getGameTime() / wigwagTicks) % 2) == 0;
                    trafficLightsForRightOfWay.forEach(tl -> {
                        tl.powerOff();
                        tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
                        tl.setActive(alt ? EnumTrafficLightBulbTypes.Red : EnumTrafficLightBulbTypes.Red2, true, false);
                    });
                    trafficLightsOpposingRightOfWay.forEach(tl -> {
                        tl.powerOff();
                        tl.setActive(EnumTrafficLightBulbTypes.DontCross, true, true);
                    });
                    break;
                }
                default:
                    break;
            }

            forceDisabledApproachesRed(trafficLightsForRightOfWay);
            forceDisabledApproachesRed(trafficLightsOpposingRightOfWay);
            applyMovementConfigToLights(trafficLightsForRightOfWay);
            applyMovementConfigToLights(trafficLightsOpposingRightOfWay);

            return stage;
        }

        private void applyMovementConfigToLights(List<TrafficLightBlockEntity> lights) {
            for (TrafficLightBlockEntity tl : lights) {
                Direction facing = TrafficLightFacingResolver.resolveApproachFacing(tl);
                if (!isSplitDirectionEnabled(facing)) {
                    continue;
                }
                ApproachMovementBulbHelper.applyOverrides(tl, TrafficLightControlBoxBlockEntity.this.getMovementSettings(facing));
            }
        }

        private boolean isStraightMovementEnabled(Direction facing) {
            return TrafficLightControlBoxBlockEntity.this.getMovementSettings(facing).straightEnabled;
        }

        private boolean isLeftMovementEnabled(Direction facing) {
            return TrafficLightControlBoxBlockEntity.this.getMovementSettings(facing).leftEnabled;
        }

        private boolean isRightMovementEnabled(Direction facing) {
            return TrafficLightControlBoxBlockEntity.this.getMovementSettings(facing).rightEnabled;
        }

        private void forceDisabledApproachesRed(List<TrafficLightBlockEntity> lights) {
            for (TrafficLightBlockEntity tl : lights) {
                Direction approach = TrafficLightFacingResolver.resolveApproachFacing(tl);
                boolean disabled = false;
                switch (approach) {
                    case NORTH:
                        disabled = !TrafficLightControlBoxBlockEntity.this.hasNorth;
                        break;
                    case SOUTH:
                        disabled = !TrafficLightControlBoxBlockEntity.this.hasSouth;
                        break;
                    case EAST:
                        disabled = !TrafficLightControlBoxBlockEntity.this.hasEast;
                        break;
                    case WEST:
                        disabled = !TrafficLightControlBoxBlockEntity.this.hasWest;
                        break;
                    default:
                        break;
                }

                if (!disabled) {
                    continue;
                }

                tl.powerOff();
                tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
                tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
                tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
                tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
                tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
                tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
                tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
                tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
                tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
                tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
                tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
            }
        }

        public void readNBT(CompoundTag nbt) {
            lastStage = Stages.getById(nbt.getInt(getNbtKey("lastStage")));
            lastRightOfWay = RightOfWays.getbyIndex(nbt.getInt(getNbtKey("lastRightOfWay")));
            if (lastStage == null) {
                lastStage = Stages.Red;
            }
            if (lastRightOfWay == null) {
                lastRightOfWay = RightOfWays.EastWest;
            }
            isInDarkMode = nbt.getBoolean("DarkMode");
            isFlashingEmergency = nbt.getBoolean("EmergencyFlash");
            readSyncData(nbt);
        }

        public void writeNBT(CompoundTag nbt) {
            nbt.putInt(getNbtKey("lastStage"), lastStage.id);
            nbt.putInt(getNbtKey("lastRightOfWay"), lastRightOfWay.index);
            nbt.putBoolean("DarkMode", isInDarkMode);
            nbt.putBoolean("EmergencyFlash", isFlashingEmergency);
            setSyncData(nbt);
        }

        public void readSyncData(CompoundTag nbt) {
            boolean legacyFallback = !nbt.contains(getNbtKey("greenMinimumNS"));

            if (legacyFallback) {
                double gMin = nbt.getDouble(getNbtKey("greenMinimum"));
                double gMax = nbt.getDouble(getNbtKey("greenMax"));
                double aMin = nbt.getDouble(getNbtKey("arrowMinimum"));
                double aMax = nbt.getDouble(getNbtKey("arrowMax"));
                double yTime = nbt.getDouble(getNbtKey("yellowTime"));
                double rTime = nbt.getDouble(getNbtKey("redTime"));

                greenMinimumNS = greenMinimumEW = gMin;
                greenMaxNS = greenMaxEW = gMax;
                arrowMinimumNS = arrowMinimumEW = aMin;
                arrowMaxNS = arrowMaxEW = aMax;
                yellowTimeNS = yellowTimeEW = yTime;
                redTimeNS = redTimeEW = rTime;
            } else {
                greenMinimumNS = nbt.contains(getNbtKey("greenMinimumNS")) ? nbt.getDouble(getNbtKey("greenMinimumNS")) : nbt.getDouble(getNbtKey("greenMinimum"));
                greenMaxNS = nbt.contains(getNbtKey("greenMaxNS")) ? nbt.getDouble(getNbtKey("greenMaxNS")) : nbt.getDouble(getNbtKey("greenMax"));
                arrowMinimumNS = nbt.contains(getNbtKey("arrowMinimumNS")) ? nbt.getDouble(getNbtKey("arrowMinimumNS")) : nbt.getDouble(getNbtKey("arrowMinimum"));
                arrowMaxNS = nbt.contains(getNbtKey("arrowMaxNS")) ? nbt.getDouble(getNbtKey("arrowMaxNS")) : nbt.getDouble(getNbtKey("arrowMax"));
                yellowTimeNS = nbt.contains(getNbtKey("yellowTimeNS")) ? nbt.getDouble(getNbtKey("yellowTimeNS")) : nbt.getDouble(getNbtKey("yellowTime"));
                redTimeNS = nbt.contains(getNbtKey("redTimeNS")) ? nbt.getDouble(getNbtKey("redTimeNS")) : nbt.getDouble(getNbtKey("redTime"));

                greenMinimumEW = nbt.contains(getNbtKey("greenMinimumEW")) ? nbt.getDouble(getNbtKey("greenMinimumEW")) : nbt.getDouble(getNbtKey("greenMinimum"));
                greenMaxEW = nbt.contains(getNbtKey("greenMaxEW")) ? nbt.getDouble(getNbtKey("greenMaxEW")) : nbt.getDouble(getNbtKey("greenMax"));
                arrowMinimumEW = nbt.contains(getNbtKey("arrowMinimumEW")) ? nbt.getDouble(getNbtKey("arrowMinimumEW")) : nbt.getDouble(getNbtKey("arrowMinimum"));
                arrowMaxEW = nbt.contains(getNbtKey("arrowMaxEW")) ? nbt.getDouble(getNbtKey("arrowMaxEW")) : nbt.getDouble(getNbtKey("arrowMax"));
                yellowTimeEW = nbt.contains(getNbtKey("yellowTimeEW")) ? nbt.getDouble(getNbtKey("yellowTimeEW")) : nbt.getDouble(getNbtKey("yellowTime"));
                redTimeEW = nbt.contains(getNbtKey("redTimeEW")) ? nbt.getDouble(getNbtKey("redTimeEW")) : nbt.getDouble(getNbtKey("redTime"));
            }

            crossTime = nbt.getDouble(getNbtKey("crossTime"));
            crossWarningTime = nbt.getDouble(getNbtKey("crossWarningTime"));
            rightArrowTime = nbt.getDouble(getNbtKey("rightArrowTime"));
        }

        public void setSyncData(CompoundTag nbt) {
            nbt.putDouble(getNbtKey("greenMinimumNS"), greenMinimumNS);
            nbt.putDouble(getNbtKey("greenMaxNS"), greenMaxNS);
            nbt.putDouble(getNbtKey("yellowTimeNS"), yellowTimeNS);
            nbt.putDouble(getNbtKey("redTimeNS"), redTimeNS);
            nbt.putDouble(getNbtKey("arrowMinimumNS"), arrowMinimumNS);
            nbt.putDouble(getNbtKey("arrowMaxNS"), arrowMaxNS);

            nbt.putDouble(getNbtKey("greenMinimumEW"), greenMinimumEW);
            nbt.putDouble(getNbtKey("greenMaxEW"), greenMaxEW);
            nbt.putDouble(getNbtKey("yellowTimeEW"), yellowTimeEW);
            nbt.putDouble(getNbtKey("redTimeEW"), redTimeEW);
            nbt.putDouble(getNbtKey("arrowMinimumEW"), arrowMinimumEW);
            nbt.putDouble(getNbtKey("arrowMaxEW"), arrowMaxEW);

            nbt.putDouble(getNbtKey("crossTime"), crossTime);
            nbt.putDouble(getNbtKey("crossWarningTime"), crossWarningTime);
            nbt.putDouble(getNbtKey("rightArrowMinimum"), rightArrowTime);
        }

        private String getNbtKey(String key) {
            return nbtPrefix + key;
        }

        private class SensorCheckResult {
            public boolean Direction1Sensor;
            public boolean Direction2Sensor;
            public boolean Direction1SensorLeft;
            public boolean Direction2SensorLeft;
            public boolean Direction1SensorRight;
            public boolean Direction2SensorRight;
        }

        private Direction getSensorFacing(BlockState senseState) {
            if (!(senseState.getBlock() instanceof TrafficSensorBlock)) {
                return null;
            }
            int rotation = senseState.getValue(RTCProperties.ROTATION);
            for (Direction d : new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST }) {
                if (CustomAngleCalculator.isRotationFacing(rotation, d)) {
                    return d;
                }
            }
            return null;
        }

        private boolean isSensorTrippedAt(BlockPos sensePos) {
            return level
                    .getEntities((Entity) null, new AABB(sensePos).expandTowards(-1, Config.sensorScanHeight, 1))
                    .stream()
                    .anyMatch(e -> (e instanceof ServerPlayer) || Config.sensorClasses.stream().anyMatch(eName -> {
                        Class<?> nextClass = e.getClass();
                        while (nextClass != null) {
                            if (eName.equals(nextClass.getName())) {
                                return true;
                            }
                            nextClass = nextClass.getSuperclass();
                        }
                        return false;
                    }));
        }

        private boolean hasAnyDemandForFacing(Direction facing) {
            if (!isSplitDirectionEnabled(facing)) {
                return false;
            }

            for (BlockPos sensePos : sensors) {
                BlockState senseState = level.getBlockState(sensePos);
                if (!(senseState.getBlock() instanceof TrafficSensorBlock)) {
                    continue;
                }

                Direction currentFacing = getSensorFacing(senseState);
                if (currentFacing == null || currentFacing != facing) {
                    continue;
                }

                if (isSensorTrippedAt(sensePos)) {
                    return true;
                }
            }

            return false;
        }

        private boolean hasAnySplitDemandExceptActive(Direction active) {
            for (Direction candidate : splitOrder) {
                if (candidate == active) {
                    continue;
                }
                if (hasAnyDemandForFacing(candidate)) {
                    return true;
                }
            }
            return false;
        }

        private SensorCheckResult checkSensors(RightOfWays rightOfWay) {
            Direction direction1 = rightOfWay == RightOfWays.NorthSouth ? Direction.NORTH : Direction.EAST;
            Direction direction2 = rightOfWay == RightOfWays.NorthSouth ? Direction.SOUTH : Direction.WEST;

            ArrayList<BlockPos> invalidSensors = new ArrayList<>();
            SensorCheckResult result = new SensorCheckResult();

            boolean pedTripped = direction1 == Direction.NORTH ? isNorthSouthPedQueued() : isWestEastPedQueued();
            result.Direction1Sensor = pedTripped;
            result.Direction2Sensor = pedTripped;

            for (BlockPos sensePos : sensors) {
                BlockState senseState = level.getBlockState(sensePos);

                if (!(senseState.getBlock() instanceof TrafficSensorBlock sensorBlock)) {
                    invalidSensors.add(sensePos);
                    continue;
                }

                Direction currentFacing = getSensorFacing(senseState);
                if (currentFacing == null) {
                    continue;
                }
                boolean isStraight = sensorBlock.getKind() == TrafficSensorBlock.SensorKind.STRAIGHT;
                boolean isLeft = sensorBlock.getKind() == TrafficSensorBlock.SensorKind.LEFT;
                boolean isRight = sensorBlock.getKind() == TrafficSensorBlock.SensorKind.RIGHT;

                if (!currentFacing.equals(direction1) && !currentFacing.equals(direction2)) {
                    continue;
                }

                if ((isStraight && currentFacing.equals(direction1) && result.Direction1Sensor)
                        || (isStraight && currentFacing.equals(direction2) && result.Direction2Sensor)
                        || (isLeft && currentFacing.equals(direction1) && result.Direction1SensorLeft)
                        || (isLeft && currentFacing.equals(direction2) && result.Direction2SensorLeft)
                        || (isRight && currentFacing.equals(direction1) && result.Direction1SensorRight)
                        || (isRight && currentFacing.equals(direction2) && result.Direction2SensorRight)) {
                    continue;
                }

                boolean isTripped = isSensorTrippedAt(sensePos);
                if (isTripped) {
                    setSensorCheckResults(isStraight, isLeft, isRight, currentFacing.equals(direction1), result);
                }
            }

            for (BlockPos invalidSensor : invalidSensors) {
                sensors.remove(invalidSensor);
            }

            return result;
        }

        private void setSensorCheckResults(boolean isStraight, boolean isLeft, boolean isRight, boolean isDirection1, SensorCheckResult results) {
            if (isDirection1) {
                if (isStraight) {
                    results.Direction1Sensor = true;
                } else if (isLeft) {
                    results.Direction1SensorLeft = true;
                } else if (isRight) {
                    results.Direction1SensorRight = true;
                }
            } else {
                if (isStraight) {
                    results.Direction2Sensor = true;
                } else if (isLeft) {
                    results.Direction2SensorLeft = true;
                } else if (isRight) {
                    results.Direction2SensorRight = true;
                }
            }
        }

        private Stages getNextLogicalStage(Stages currentStage, RightOfWays currentRightOfWay, SensorCheckResult sensorResult) {
            long ticksInStage = level.getGameTime() - this.stageStartTime;
            final boolean splitForRow = TrafficLightControlBoxBlockEntity.this.isSplitEnabledForRow(currentRightOfWay);
            final Direction rowDir1 = currentRightOfWay == RightOfWays.NorthSouth ? Direction.NORTH : Direction.EAST;
            final Direction rowDir2 = currentRightOfWay == RightOfWays.NorthSouth ? Direction.SOUTH : Direction.WEST;
            final boolean bothApproachesEnabled = isApproachEnabled(rowDir1) && isApproachEnabled(rowDir2);
            if (splitForRow && activeSplitDirection != rowDir1 && activeSplitDirection != rowDir2) {
                activeSplitDirection = rowDir1;
            }
            final boolean splitActiveIsDir1 = activeSplitDirection == rowDir1;

            double arrowMinNS = getArrowMinimumNS();
            double arrowMaxNS = getArrowMaxNS();
            double yellowNS = getYellowTimeNS();
            double greenMinNS = getGreenMinimumNS();
            double greenMaxNS = getGreenMaxNS();
            double redNS = getRedTimeNS();

            double arrowMinEW = getArrowMinimumES();
            double arrowMaxEW = getArrowMaxEW();
            double yellowEW = getYellowTimeEW();
            double greenMinEW = getGreenMinimumEW();
            double greenMaxEW = getGreenMaxEW();
            double redEW = getRedTimeEW();

            double yellowTime = currentRightOfWay == RightOfWays.NorthSouth ? yellowNS : yellowEW;
            double redTime = currentRightOfWay == RightOfWays.NorthSouth ? redNS : redEW;
            double greenMinimum = currentRightOfWay == RightOfWays.NorthSouth ? greenMinNS : greenMinEW;
            double greenMax = currentRightOfWay == RightOfWays.NorthSouth ? greenMaxNS : greenMaxEW;
            double arrowMinimum = currentRightOfWay == RightOfWays.NorthSouth ? arrowMinNS : arrowMinEW;
            double arrowMax = currentRightOfWay == RightOfWays.NorthSouth ? arrowMaxNS : arrowMaxEW;

            boolean timeExceeded = (arrowMinimum > 0) && ticksInStage >= (arrowMinimum * 20);

            switch (currentStage) {
                case HawkFlashYellow:
                    this.stageStartTime = level.getGameTime();
                    setNextUpdate(Config.hawkSolidYellowSeconds);
                    return Stages.HawkSolidYellow;

                case HawkSolidYellow:
                    this.stageStartTime = level.getGameTime();
                    setNextUpdate(getCrossTime());
                    return Stages.HawkSolidRed;

                case HawkSolidRed:
                    this.stageStartTime = level.getGameTime();
                    setNextUpdate(1);
                    return Stages.HawkFlashRed;

                case HawkFlashRed:
                    if (ticksInStage >= (getCrossWarningTime() * 20)) {
                        this.stageStartTime = level.getGameTime();
                        setNextUpdate(greenMinimum);
                        return Stages.Green;
                    }
                    setNextUpdate(1);
                    return Stages.HawkFlashRed;

                case Red:
                    if (splitForRow) {
                        return pedCheckedGreen(currentRightOfWay);
                    }
                    if (sensorResult.Direction1SensorRight && isRightMovementEnabled(rowDir1)) {
                        this.stageStartTime = level.getGameTime();
                        setNextUpdate(getRightArrowTime());
                        return Stages.Direction1RightTurnArrow;
                    } else if (sensorResult.Direction2SensorRight && isRightMovementEnabled(rowDir2)) {
                        this.stageStartTime = level.getGameTime();
                        setNextUpdate(getRightArrowTime());
                        return Stages.Direction2RightTurnArrow;
                    } else if (((sensorResult.Direction1SensorLeft && isLeftMovementEnabled(rowDir1))
                            && (sensorResult.Direction2SensorLeft && isLeftMovementEnabled(rowDir2)))
                            || (bothApproachesEnabled && arrowMinimum != 0
                                    && (isLeftMovementEnabled(rowDir1) || isLeftMovementEnabled(rowDir2)))) {
                        this.stageStartTime = level.getGameTime();
                        setNextUpdate(arrowMinimum);
                        return Stages.BothTurnArrow;
                    } else if (sensorResult.Direction1SensorLeft && isLeftMovementEnabled(rowDir1)) {
                        this.stageStartTime = level.getGameTime();
                        setNextUpdate(arrowMinNS);
                        return Stages.Direction1LeftTurnArrow;
                    } else if (sensorResult.Direction2SensorLeft && isLeftMovementEnabled(rowDir2)) {
                        this.stageStartTime = level.getGameTime();
                        setNextUpdate(arrowMinEW);
                        return Stages.Direction2LeftTurnArrow;
                    }
                    return pedCheckedGreen(currentRightOfWay);

                case Direction1RightTurnArrow:
                    if (arrowMinimum == 0 && ticksInStage >= (arrowMax * 20)) {
                        this.stageStartTime = level.getGameTime();
                        setNextUpdate(yellowTime);
                        return Stages.Direction1LeftTurnArrowYellow;
                    } else if (timeExceeded && arrowMinimum != 0 && ticksInStage >= (arrowMinimum * 20)) {
                        this.stageStartTime = level.getGameTime();
                        setNextUpdate(yellowTime);
                        return Stages.Direction1LeftTurnArrowYellow;
                    }
                    if (sensorResult.Direction2SensorRight || sensorResult.Direction2SensorLeft) {
                        return Stages.Direction1RightTurnArrowYellow;
                    } else {
                        return Stages.Direction1LeftTurnArrowYellow;
                    }

                case Direction1RightTurnArrowYellow:
                    this.stageStartTime = level.getGameTime();
                    setNextUpdate(sensorResult.Direction2SensorRight ? getRightArrowTime() : arrowMinEW);
                    return Stages.Direction2LeftTurnArrow;

                case Direction2RightTurnArrow:
                    if (arrowMinimum == 0 && ticksInStage >= (arrowMax * 20)) {
                        this.stageStartTime = level.getGameTime();
                        setNextUpdate(yellowTime);
                        return Stages.BothTurnArrowYellow;
                    } else if (timeExceeded && arrowMinimum != 0 && ticksInStage >= (arrowMinimum * 20)) {
                        this.stageStartTime = level.getGameTime();
                        setNextUpdate(yellowTime);
                        return Stages.BothTurnArrowYellow;
                    }
                    return sensorResult.Direction1SensorLeft || sensorResult.Direction1SensorRight
                            ? Stages.Direction2RightTurnArrowYellow
                            : Stages.Direction2LeftTurnArrowYellow;

                case Direction2RightTurnArrowYellow:
                    this.stageStartTime = level.getGameTime();
                    setNextUpdate(sensorResult.Direction2SensorRight ? getRightArrowTime() : arrowMinNS);
                    return Stages.Direction1LeftTurnArrow;

                case BothTurnArrow:
                    if (arrowMinimum == 0 && ticksInStage >= (arrowMax * 20)) {
                        this.stageStartTime = level.getGameTime();
                        setNextUpdate(yellowTime);
                        return Stages.BothTurnArrowYellow;
                    } else if (timeExceeded && arrowMinimum != 0 && ticksInStage >= (arrowMinimum * 20)) {
                        this.stageStartTime = level.getGameTime();
                        setNextUpdate(yellowTime);
                        return Stages.BothTurnArrowYellow;
                    }
                    return Stages.BothTurnArrow;

                case BothTurnArrowYellow:
                    this.stageStartTime = level.getGameTime();
                    return pedCheckedGreen(currentRightOfWay);

                case Direction1LeftTurnArrow:
                    if (arrowMinimum == 0 && ticksInStage >= (arrowMax * 20)) {
                        this.stageStartTime = level.getGameTime();
                        setNextUpdate(yellowTime);
                        return Stages.Direction1LeftTurnArrowYellow;
                    } else if (timeExceeded && arrowMinimum != 0 && ticksInStage >= (arrowMinimum * 20)) {
                        this.stageStartTime = level.getGameTime();
                        setNextUpdate(yellowTime);
                        return Stages.Direction1LeftTurnArrowYellow;
                    }
                    return Stages.Direction1LeftTurnArrow;

                case Direction1LeftTurnArrowYellow:
                    this.stageStartTime = level.getGameTime();
                    return pedCheckedGreen(currentRightOfWay);

                case Direction2LeftTurnArrow:
                    if (arrowMinimum == 0 && ticksInStage >= (arrowMax * 20)) {
                        this.stageStartTime = level.getGameTime();
                        setNextUpdate(yellowTime);
                        return Stages.Direction2LeftTurnArrowYellow;
                    } else if (timeExceeded && arrowMinimum != 0 && ticksInStage >= (arrowMinimum * 20)) {
                        this.stageStartTime = level.getGameTime();
                        setNextUpdate(yellowTime);
                        return Stages.BothTurnArrowYellow;
                    }
                    return Stages.Direction2LeftTurnArrow;

                case Direction2LeftTurnArrowYellow:
                    this.stageStartTime = level.getGameTime();
                    return pedCheckedGreen(currentRightOfWay);

                case Green:
                    if (TrafficLightControlBoxBlockEntity.this.isHawkBeaconEnabled()) {
                        final RightOfWays roadRightOfWay = northMain ? RightOfWays.NorthSouth : RightOfWays.EastWest;
                        final boolean hawkPedQueued = (roadRightOfWay == RightOfWays.NorthSouth) ? isWestEastPedQueued() : isNorthSouthPedQueued();
                        final boolean hawkMinGreenMet = (greenMinimum > 0)
                                ? (ticksInStage >= (greenMinimum * 20))
                                : (ticksInStage >= (greenMax * 20));
                        final double hawkFlashYellowTime = Config.hawkFlashYellowSeconds;
                        if (hawkPedQueued && hawkMinGreenMet) {
                            if (roadRightOfWay == RightOfWays.NorthSouth) {
                                setWestEastPedQueued(false);
                            } else {
                                setNorthSouthPedQueued(false);
                            }
                            this.stageStartTime = level.getGameTime();
                            setNextUpdate(hawkFlashYellowTime);
                            return Stages.HawkFlashYellow;
                        }
                        return Stages.Green;
                    }

                    if (splitForRow) {
                        final boolean activeStraight = splitActiveIsDir1 ? sensorResult.Direction1Sensor : sensorResult.Direction2Sensor;
                        final boolean activeLeft = splitActiveIsDir1 ? sensorResult.Direction1SensorLeft : sensorResult.Direction2SensorLeft;
                        final boolean activeRight = splitActiveIsDir1 ? sensorResult.Direction1SensorRight : sensorResult.Direction2SensorRight;
                        final boolean activeDemand = activeStraight || activeLeft || activeRight;
                        final Direction otherDir = splitActiveIsDir1 ? rowDir2 : rowDir1;
                        final boolean otherStraight = splitActiveIsDir1 ? sensorResult.Direction2Sensor : sensorResult.Direction1Sensor;
                        final boolean otherLeft = splitActiveIsDir1 ? sensorResult.Direction2SensorLeft : sensorResult.Direction1SensorLeft;
                        final boolean otherRight = splitActiveIsDir1 ? sensorResult.Direction2SensorRight : sensorResult.Direction1SensorRight;
                        final boolean otherDemand = otherStraight || otherLeft || otherRight;
                        final boolean activeVehicleDemand = hasAnyDemandForFacing(activeSplitDirection);
                        final boolean otherVehicleDemand = hasAnyDemandForFacing(otherDir);
                        final RightOfWays nextRow = currentRightOfWay.getNext();

                        final boolean timerMode = greenMinimum == 0;
                        final boolean minMet = (greenMinimum > 0) && ticksInStage >= (greenMinimum * 20);
                        final boolean maxMet = ticksInStage >= (greenMax * 20);
                        final boolean canChange = timerMode ? maxMet : minMet;

                        if (!swappedWithinCurrentRow && canChange && isSplitDirectionEnabled(otherDir)) {
                            final boolean shouldSwap = timerMode || otherVehicleDemand;
                            if (shouldSwap) {
                                swappedWithinCurrentRow = true;
                                pendingSplitSwapRow = currentRightOfWay;
                                pendingSplitSwapDirection = otherDir;
                                if (!timerMode) {
                                    ModRealisticTrafficControl.LOGGER.info(
                                            "[RTC] Split swap scheduled row={} from {} to {} min={} max={} ticks={} activeVeh={} otherVeh={} D1={} D2={} D1L={} D2L={} D1R={} D2R={} pedNS={} pedEW={}",
                                            currentRightOfWay,
                                            activeSplitDirection,
                                            otherDir,
                                            greenMinimum,
                                            greenMax,
                                            ticksInStage,
                                            activeVehicleDemand,
                                            otherVehicleDemand,
                                            sensorResult.Direction1Sensor,
                                            sensorResult.Direction2Sensor,
                                            sensorResult.Direction1SensorLeft,
                                            sensorResult.Direction2SensorLeft,
                                            sensorResult.Direction1SensorRight,
                                            sensorResult.Direction2SensorRight,
                                            isNorthSouthPedQueued(),
                                            isWestEastPedQueued());
                                }
                                this.stageStartTime = level.getGameTime();
                                forcedNextRightOfWay = currentRightOfWay;
                                setNextUpdate(yellowTime);
                                return Stages.Yellow;
                            }
                        }

                        if (swappedWithinCurrentRow) {
                            if (timerMode) {
                                if (maxMet) {
                                    this.stageStartTime = level.getGameTime();
                                    forcedNextRightOfWay = nextRow;
                                    setNextUpdate(yellowTime);
                                    return Stages.Yellow;
                                }
                                return Stages.Green;
                            }
                            if ((!activeVehicleDemand && minMet) || maxMet) {
                                this.stageStartTime = level.getGameTime();
                                forcedNextRightOfWay = nextRow;
                                setNextUpdate(yellowTime);
                                return Stages.Yellow;
                            }
                            return Stages.Green;
                        }

                        if (timerMode) {
                            if (maxMet) {
                                this.stageStartTime = level.getGameTime();
                                forcedNextRightOfWay = nextRow;
                                setNextUpdate(yellowTime);
                                return Stages.Yellow;
                            }
                            return Stages.Green;
                        }

                        if ((!activeVehicleDemand && minMet) || maxMet) {
                            this.stageStartTime = level.getGameTime();
                            forcedNextRightOfWay = nextRow;
                            setNextUpdate(yellowTime);
                            return Stages.Yellow;
                        }

                        return Stages.Green;
                    }

                    SensorCheckResult crossSensorCheck = checkSensors(currentRightOfWay.getNext());

                    timeExceeded = (greenMinimum > 0) && ticksInStage >= (greenMinimum * 20);
                    boolean maxTimeExceeded = ticksInStage >= (greenMax * 20);

                    if (!sensorResult.Direction1Sensor && !sensorResult.Direction2Sensor && greenMinimum > 0 && timeExceeded) {
                        this.stageStartTime = level.getGameTime();
                        setNextUpdate(yellowTime);
                        return Stages.Yellow;
                    }

                    if (greenMinimum == 0 && maxTimeExceeded
                            && (sensorResult.Direction1SensorLeft || sensorResult.Direction2SensorLeft)) {
                        this.stageStartTime = level.getGameTime();
                        setNextUpdate(yellowTime);
                        return Stages.Yellow;
                    }

                    if (greenMinimum == 0 && maxTimeExceeded
                            && !sensorResult.Direction1Sensor && !sensorResult.Direction2Sensor
                            && !sensorResult.Direction1SensorLeft && !sensorResult.Direction2SensorLeft
                            && !sensorResult.Direction1SensorRight && !sensorResult.Direction2SensorRight
                            && (crossSensorCheck.Direction1Sensor || crossSensorCheck.Direction2Sensor
                                    || crossSensorCheck.Direction1SensorLeft || crossSensorCheck.Direction2SensorLeft
                                    || crossSensorCheck.Direction1SensorRight || crossSensorCheck.Direction2SensorRight)) {
                        this.stageStartTime = level.getGameTime();
                        setNextUpdate(yellowTime);
                        return Stages.Yellow;
                    }

                    return Stages.Green;

                case Yellow:
                    this.stageStartTime = level.getGameTime();
                    setNextUpdate(redTime);
                    return Stages.Red;

                case GreenCross:
                    this.stageStartTime = level.getGameTime();
                    setNextUpdate(getCrossWarningTime());
                    return Stages.GreenDontCrossWarning;

                case GreenDontCrossWarning:
                    return Stages.Green;
            }

            return null;
        }

        private Stages pedCheckedGreen(RightOfWays rightOfWay) {
            double crossTime = getCrossTime();

            double greenMinimum = (rightOfWay == RightOfWays.NorthSouth)
                    ? getGreenMinimumNS()
                    : getGreenMinimumEW();

            if (TrafficLightControlBoxBlockEntity.this.isHawkBeaconEnabled()) {
                setNextUpdate(greenMinimum);
                return Stages.Green;
            }

            if ((rightOfWay == RightOfWays.NorthSouth && isNorthSouthPedQueued())
                    || (rightOfWay == RightOfWays.EastWest && isWestEastPedQueued())) {

                if (rightOfWay == RightOfWays.NorthSouth) {
                    setNorthSouthPedQueued(false);
                } else {
                    setWestEastPedQueued(false);
                }

                setNextUpdate(crossTime);
                return Stages.GreenCross;
            }

            setNextUpdate(greenMinimum);
            return Stages.Green;
        }

        private void setNextUpdate(double secondsIntoFuture) {
            nextUpdate = System.currentTimeMillis() + (long) (secondsIntoFuture * 1000);
        }
    }

    private enum RightOfWays {
        NorthSouth(0),
        EastWest(1);

        private final int index;

        RightOfWays(int index) {
            this.index = index;
        }

        public static RightOfWays getbyIndex(int index) {
            for (RightOfWays rightOfWay : RightOfWays.values()) {
                if (rightOfWay.index == index) {
                    return rightOfWay;
                }
            }
            return null;
        }

        public RightOfWays getNext() {
            RightOfWays newRow = getbyIndex(index + 1);
            if (newRow == null) {
                newRow = getbyIndex(0);
            }
            return newRow;
        }
    }

    private enum Stages {
        Red(0),
        Direction1LeftTurnArrow(1),
        Direction2LeftTurnArrow(2),
        BothTurnArrow(3),
        Direction1LeftTurnArrowYellow(4),
        Direction2LeftTurnArrowYellow(5),
        BothTurnArrowYellow(6),
        GreenCross(7),
        GreenDontCrossWarning(8),
        Green(9),
        Yellow(10),
        Direction1RightTurnArrow(11),
        Direction2RightTurnArrow(12),
        Direction1RightTurnArrowYellow(13),
        Direction2RightTurnArrowYellow(14),
        HawkFlashYellow(15),
        HawkSolidYellow(16),
        HawkSolidRed(17),
        HawkFlashRed(18);

        private final int id;

        Stages(int id) {
            this.id = id;
        }

        public static Stages getById(int id) {
            for (Stages stage : Stages.values()) {
                if (stage.id == id) {
                    return stage;
                }
            }
            return null;
        }
    }
}
