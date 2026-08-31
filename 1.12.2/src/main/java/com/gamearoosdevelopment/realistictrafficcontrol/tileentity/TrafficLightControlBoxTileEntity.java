package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.gamearoosdevelopment.realistictrafficcontrol.Config;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockBaseTrafficLight;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficSensorLeft;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficSensorRight;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficSensorStraight;
import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.util.ApproachMovementBulbHelper;
import com.gamearoosdevelopment.realistictrafficcontrol.util.ApproachMovementPhaseHelper;
import com.gamearoosdevelopment.realistictrafficcontrol.util.ApproachMovementSettings;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;
import com.gamearoosdevelopment.realistictrafficcontrol.util.FyaMode;
import com.gamearoosdevelopment.realistictrafficcontrol.util.LeftTurnBulbHelper;
import com.gamearoosdevelopment.realistictrafficcontrol.util.TrafficLightFacingResolver;
import com.gamearoosdevelopment.realistictrafficcontrol.util.EnumTrafficLightBulbTypes;
import com.google.common.collect.ImmutableList;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.common.capabilities.Capability;




public class TrafficLightControlBoxTileEntity extends SyncableTileEntity implements ITickable {

	private ArrayList<BlockPos> westEastLights = new ArrayList<BlockPos>();
	private ArrayList<BlockPos> northSouthLights = new ArrayList<BlockPos>();
	private HashMap<EnumTrafficLightBulbTypes, Boolean> manualNorthSouthActive = new HashMap<EnumTrafficLightBulbTypes, Boolean>();
	private HashMap<EnumTrafficLightBulbTypes, Boolean> manualWestEastActive = new HashMap<EnumTrafficLightBulbTypes, Boolean>();
	private HashMap<EnumTrafficLightBulbTypes, Boolean> manualNorthSouthInactive = new HashMap<EnumTrafficLightBulbTypes, Boolean>();
	private HashMap<EnumTrafficLightBulbTypes, Boolean> manualWestEastInactive = new HashMap<EnumTrafficLightBulbTypes, Boolean>();
	private ArrayList<BlockPos> sensors = new ArrayList<>();
	private ArrayList<BlockPos> northSouthPedButtons = new ArrayList<>();
	private ArrayList<BlockPos> westEastPedButtons = new ArrayList<>();
	private boolean isAutoMode = false; // Client only property
	private boolean powered = false;
	private Automator automator = null;
	private static final AxisAlignedBB FULL_BLOCK_AABB = new AxisAlignedBB(0, 0, 0, 1, 1, 1);
	public boolean hasNorth = true;
	public boolean hasSouth = true;
	public boolean hasEast  = true;
	public boolean hasWest  = true;
	private int ticksInCurrentStage = 0;
	private boolean nightFlashEnabled = false;
	private long nightFlashStart = 13000; // 7 PM
	private long nightFlashEnd = 0;   // 5 AM
	private boolean inNightFlash = false;
	private boolean powerOnFlashEnabled = false;
	private boolean fyaNightOnlyEnabled = false;
	private boolean lastInNightFlash = false;
	private int fyaDayTransitionTicksRemaining = 0;
	private boolean flashState = false; // toggles on/off
	private boolean previousFlashState = false;
	private boolean wasFlashOn = false; // toggle tracker
	private boolean northMain = true;
	private boolean hawkBeaconEnabled = false;
	private boolean splitDirectionsEnabled = false;
	private boolean splitNorthSouthEnabled = true;
	private boolean splitWestEastEnabled = true;
	private final EnumMap<EnumFacing, ApproachMovementSettings> approachMovementSettings = new EnumMap<>(EnumFacing.class);

	public TrafficLightControlBoxTileEntity() {
		for (EnumFacing facing : new EnumFacing[] { EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST }) {
			approachMovementSettings.put(facing, new ApproachMovementSettings());
		}
	}

	public ApproachMovementSettings getMovementSettings(EnumFacing facing) {
		ApproachMovementSettings settings = approachMovementSettings.get(facing);
		if (settings == null) {
			settings = new ApproachMovementSettings();
			approachMovementSettings.put(facing, settings);
		}
		return settings;
	}

	public void setMovementSettings(EnumFacing facing, ApproachMovementSettings settings) {
		approachMovementSettings.put(facing, settings == null ? new ApproachMovementSettings() : settings.copy());
		this.fyaNightOnlyEnabled = isFyaNightOnlyEnabled();
		markDirty();
	}

	private void writeMovementSettingsToNBT(NBTTagCompound compound) {
		for (EnumFacing facing : new EnumFacing[] { EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST }) {
			compound.setTag("movement" + facing.getName2(), getMovementSettings(facing).writeToNBT());
		}
	}

	private void readMovementSettingsFromNBT(NBTTagCompound compound) {
		for (EnumFacing facing : new EnumFacing[] { EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST }) {
			String key = "movement" + facing.getName2();
			if (compound.hasKey(key)) {
				getMovementSettings(facing).readFromNBT(compound.getCompoundTag(key));
			}
		}
	}
	public void setNightFlashEnabled(boolean enabled) {
	    this.nightFlashEnabled = enabled;
	    markDirty(); // Ensure the tile is saved
	}
	public boolean isNightFlashEnabled() {
	    return nightFlashEnabled;
	}

	public void setPowerOnFlashEnabled(boolean enabled) {
		this.powerOnFlashEnabled = enabled;
		markDirty();
		if (world != null && !world.isRemote) {
			if (enabled && powered && !isFlashingEmergency) {
				flashRedYellowForRecovery();
			} else if (!enabled && isFlashingEmergency) {
				recoveryGeneration++;
				isFlashingEmergency = false;
				getAutomator().reset();
			}
		}
	}

	public boolean isPowerOnFlashEnabled() {
		return powerOnFlashEnabled;
	}
	
	public void setNorthMainEnabled(boolean enabled) {
	    this.northMain = enabled;
	    markDirty(); // Ensure the tile is saved
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
		FyaMode mode = enabled ? FyaMode.NIGHT_ONLY : FyaMode.ALWAYS;
		for (EnumFacing facing : new EnumFacing[] { EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST }) {
			ApproachMovementSettings settings = getMovementSettings(facing);
			settings.leftFya = mode;
			settings.rightFya = mode;
		}
		this.fyaNightOnlyEnabled = enabled;
		markDirty();
	}

	public boolean isFyaNightOnlyEnabled() {
		for (EnumFacing facing : new EnumFacing[] { EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST }) {
			ApproachMovementSettings settings = getMovementSettings(facing);
			if (settings.leftFya != FyaMode.NIGHT_ONLY || settings.rightFya != FyaMode.NIGHT_ONLY) {
				return false;
			}
		}
		return true;
	}

	private void migrateLegacyFyaSettings(NBTTagCompound compound) {
		if (compound == null || !compound.hasKey("FyaNightOnlyEnabled")) {
			return;
		}
		FyaMode legacyMode = compound.getBoolean("FyaNightOnlyEnabled") ? FyaMode.NIGHT_ONLY : FyaMode.ALWAYS;
		for (EnumFacing facing : new EnumFacing[] { EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST }) {
			String key = "movement" + facing.getName2();
			boolean hasPerApproachFya = compound.hasKey(key) && compound.getCompoundTag(key).hasKey("leftFya");
			if (!hasPerApproachFya) {
				ApproachMovementSettings settings = getMovementSettings(facing);
				settings.leftFya = legacyMode;
				settings.rightFya = legacyMode;
			}
		}
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

	public List<BlockPos> getAllTrafficLightPositions() {
		java.util.LinkedHashSet<BlockPos> positions = new java.util.LinkedHashSet<>();
		positions.addAll(northSouthLights);
		positions.addAll(westEastLights);
		return new ArrayList<>(positions);
	}

	@Nullable
	public BaseTrafficLightTileEntity getTrafficLightAt(BlockPos pos) {
		if (world == null) {
			return null;
		}
		TileEntity te = world.getTileEntity(pos);
		return te instanceof BaseTrafficLightTileEntity ? (BaseTrafficLightTileEntity) te : null;
	}

	public List<BaseTrafficLightTileEntity> getTrafficLightsForApproaches(EnumFacing... approaches) {
		java.util.Set<EnumFacing> allowed = java.util.EnumSet.noneOf(EnumFacing.class);
		java.util.Collections.addAll(allowed, approaches);
		return getAllTrafficLightPositions()
				.stream()
				.map(this::getTrafficLightAt)
				.filter(Objects::nonNull)
				.filter(tl -> allowed.contains(TrafficLightFacingResolver.resolveApproachFacing(tl)))
				.collect(Collectors.toList());
	}

	public static boolean isNorthSouthApproach(EnumFacing approach) {
		return approach == EnumFacing.NORTH || approach == EnumFacing.SOUTH;
	}

	public static EnumFacing resolveTrafficLightApproach(BaseTrafficLightTileEntity light, int rotation) {
		return TrafficLightFacingResolver.resolveApproachFacing(light);
	}

	public void reconcileTrafficLightPairing(BlockPos pos) {
		if (world == null || (!northSouthLights.contains(pos) && !westEastLights.contains(pos))) {
			return;
		}
		BaseTrafficLightTileEntity light = getTrafficLightAt(pos);
		if (light == null) {
			return;
		}
		EnumFacing approach = TrafficLightFacingResolver.resolveApproachFacing(light);
		northSouthLights.remove(pos);
		westEastLights.remove(pos);
		if (isNorthSouthApproach(approach)) {
			if (!northSouthLights.contains(pos)) {
				northSouthLights.add(pos);
			}
		} else if (!westEastLights.contains(pos)) {
			westEastLights.add(pos);
		}
		markDirty();
	}

	public static void notifyApproachFacingChanged(World world, BlockPos lightPos) {
		if (world == null) {
			return;
		}
		for (TileEntity te : world.loadedTileEntityList) {
			if (te instanceof TrafficLightControlBoxTileEntity) {
				((TrafficLightControlBoxTileEntity) te).reconcileTrafficLightPairing(lightPos);
			}
		}
	}
	
	
	

	
	
	


	// ComputerCraft integration is registered via TrafficLightPeripheralProvider when CC is installed.
	// Keeping this tile entity free of CC API references ensures RTC loads when CC is not present.

	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		for(int i = 0; i < westEastLights.size(); i++)
		{
			BlockPos pos = westEastLights.get(i);
			int[] blockPosArray = new int[] { pos.getX(), pos.getY(), pos.getZ() };
			compound.setIntArray("westEast" + i, blockPosArray);
		}
		
		for(int i = 0; i < northSouthLights.size(); i++)
		{
			BlockPos pos = northSouthLights.get(i);
			int[] blockPosArray = new int[] { pos.getX(), pos.getY(), pos.getZ() };
			compound.setIntArray("northSouth" + i, blockPosArray);
		}
		compound.setBoolean("hasNorth", hasNorth);
		compound.setBoolean("hasSouth", hasSouth);
		compound.setBoolean("hasEast", hasEast);
		compound.setBoolean("hasWest", hasWest);
		compound.setBoolean("NightFlashEnabled", nightFlashEnabled);
		compound.setBoolean("PowerOnFlashEnabled", powerOnFlashEnabled);
		compound.setBoolean("northMain", northMain);
		compound.setBoolean("hawkBeaconEnabled", hawkBeaconEnabled);
		compound.setBoolean("splitDirectionsEnabled", splitDirectionsEnabled);
		compound.setBoolean("splitNorthSouthEnabled", splitNorthSouthEnabled);
		compound.setBoolean("splitWestEastEnabled", splitWestEastEnabled);
		compound.setBoolean("FyaNightOnlyEnabled", isFyaNightOnlyEnabled());
		writeMovementSettingsToNBT(compound);

		
		    compound.setInteger("TicksInCurrentStage", ticksInCurrentStage);
		
		writeManualSettingDictionary(compound, manualNorthSouthActive, "manualNorthSouthActive");
		writeManualSettingDictionary(compound, manualWestEastActive, "manualWestEastActive");
		writeManualSettingDictionary(compound, manualNorthSouthInactive, "manualNorthSouthInactive");
		writeManualSettingDictionary(compound, manualWestEastInactive, "manualWestEastInactive");
		
		for(int i = 0; i < sensors.size(); i++)
		{
			BlockPos sensorPos = sensors.get(i);
			compound.setLong("sensor" + i, sensorPos.toLong());
		}
		
		NBTTagList northSouthPedButtonsList = new NBTTagList();
		for(BlockPos pos : northSouthPedButtons)
		{
			northSouthPedButtonsList.appendTag(new NBTTagLong(pos.toLong()));
		}
		
		NBTTagList westEastPedButtonsList = new NBTTagList();
		for(BlockPos pos : westEastPedButtons)
		{
			westEastPedButtonsList.appendTag(new NBTTagLong(pos.toLong()));
		}
		
		compound.setTag("northSouthPedButtons", northSouthPedButtonsList);
		compound.setTag("westEastPedButtons", westEastPedButtonsList);
		
		getAutomator().writeNBT(compound);
		
		
		
		return super.writeToNBT(compound);
	}
	
	private void writeManualSettingDictionary(NBTTagCompound compound, HashMap<EnumTrafficLightBulbTypes, Boolean> map, String prefix)
	{
		ArrayList<EnumTrafficLightBulbTypes> keyList = new ArrayList<EnumTrafficLightBulbTypes>(map.keySet());
		ArrayList<Boolean> valueList = new ArrayList<Boolean>(map.values());
		
		for(int i = 0; i < map.size(); i++)
		{
			String keyKey = prefix + "-key-" + i;
			String valueKey = prefix + "-value-" + i;
			
			compound.setInteger(keyKey, keyList.get(i).getIndex());
			compound.setBoolean(valueKey, valueList.get(i));
		}
	}
	
	private void readManualSettingDictionary(NBTTagCompound compound, HashMap<EnumTrafficLightBulbTypes, Boolean> map, String prefix)
	{
		map.clear();
		int i = 0;
		while(compound.hasKey(prefix + "-key-" + i))
		{
			int bulbType = compound.getInteger(prefix + "-key-" + i);
			boolean flash = compound.getBoolean(prefix + "-value-" + i);
			
			map.put(EnumTrafficLightBulbTypes.get(bulbType), flash);
			
			i++;
		}
	}
	
	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);

		westEastLights.clear();
		northSouthLights.clear();
		sensors.clear();
		northSouthPedButtons.clear();
		westEastPedButtons.clear();
		
		int counter = 0;
		while(compound.hasKey("westEast" + counter))
		{
			int[] blockPosArray = compound.getIntArray("westEast" + counter);
			
			BlockPos newBlockPos = new BlockPos(blockPosArray[0], blockPosArray[1], blockPosArray[2]);
			westEastLights.add(newBlockPos);
			counter++;
		}
		
		counter = 0;
		while(compound.hasKey("northSouth" + counter))
		{
			int[] blockPosArray = compound.getIntArray("northSouth" + counter);
			
			BlockPos newBlockPos = new BlockPos(blockPosArray[0], blockPosArray[1], blockPosArray[2]);
			northSouthLights.add(newBlockPos);
			counter++;
		}
		
		powered = compound.getBoolean("powered");
		if (compound.hasKey("NightFlashEnabled")) {
		    nightFlashEnabled = compound.getBoolean("NightFlashEnabled");
		}
		if (compound.hasKey("PowerOnFlashEnabled")) {
			powerOnFlashEnabled = compound.getBoolean("PowerOnFlashEnabled");
		}
		
		if(compound.hasKey("northMain")) {
			northMain = compound.getBoolean("northMain");
		}
		if (compound.hasKey("hawkBeaconEnabled")) {
			hawkBeaconEnabled = compound.getBoolean("hawkBeaconEnabled");
		}
		if (compound.hasKey("splitDirectionsEnabled")) {
			splitDirectionsEnabled = compound.getBoolean("splitDirectionsEnabled");
		}
		if (compound.hasKey("splitNorthSouthEnabled")) {
			splitNorthSouthEnabled = compound.getBoolean("splitNorthSouthEnabled");
		}
		if (compound.hasKey("splitWestEastEnabled")) {
			splitWestEastEnabled = compound.getBoolean("splitWestEastEnabled");
		}
		if (compound.hasKey("FyaNightOnlyEnabled")) {
			fyaNightOnlyEnabled = compound.getBoolean("FyaNightOnlyEnabled");
		}
		readMovementSettingsFromNBT(compound);
		migrateLegacyFyaSettings(compound);
		fyaNightOnlyEnabled = isFyaNightOnlyEnabled();

		
	 
		 hasNorth = compound.getBoolean("hasNorth");
		    hasSouth = compound.getBoolean("hasSouth");
		    hasEast = compound.getBoolean("hasEast");
		    hasWest = compound.getBoolean("hasWest");
	  
	  
		readManualSettingDictionary(compound, manualNorthSouthActive, "manualNorthSouthActive");
		readManualSettingDictionary(compound, manualWestEastActive, "manualWestEastActive");
		readManualSettingDictionary(compound, manualNorthSouthInactive, "manualNorthSouthInactive");
		readManualSettingDictionary(compound, manualWestEastInactive, "manualWestEastInactive");
		
		for(String key : compound
				.getKeySet()
				.stream()
				.filter((key) -> key.startsWith("sensor"))
				.collect(Collectors.toSet()))
		{
			BlockPos sensorPos = BlockPos.fromLong(compound.getLong(key));
			sensors.add(sensorPos);
		}
		
		getAutomator().readNBT(compound);
		
		NBTTagList northSouthPedButtonList = compound.getTagList("northSouthPedButtons", NBT.TAG_LONG);
		NBTTagList westEastPedButtonList = compound.getTagList("westEastPedButtons", NBT.TAG_LONG);
		
		northSouthPedButtons = new ArrayList<>();
		westEastPedButtons = new ArrayList<>();
		
		for(NBTBase baseLong : northSouthPedButtonList)
		{
			NBTTagLong posLong = (NBTTagLong)baseLong;
			northSouthPedButtons.add(BlockPos.fromLong(posLong.getLong()));
		}
		
		for(NBTBase baseLong : westEastPedButtonList)
		{
			NBTTagLong posLong = (NBTTagLong)baseLong;
			westEastPedButtons.add(BlockPos.fromLong(posLong.getLong()));
		}
	}
	
	@Override
	public NBTTagCompound getUpdateTag() {
		NBTTagCompound compound = super.getUpdateTag();
		
		writeManualSettingDictionary(compound, manualNorthSouthActive, "manualNorthSouthActive");
		writeManualSettingDictionary(compound, manualWestEastActive, "manualWestEastActive");
		writeManualSettingDictionary(compound, manualNorthSouthInactive, "manualNorthSouthInactive");
		writeManualSettingDictionary(compound, manualWestEastInactive, "manualWestEastInactive");
		compound.setBoolean("NightFlashEnabled", nightFlashEnabled);
		compound.setBoolean("PowerOnFlashEnabled", powerOnFlashEnabled);
		compound.setBoolean("northMain", northMain);
		compound.setBoolean("hawkBeaconEnabled", hawkBeaconEnabled);
		compound.setBoolean("splitDirectionsEnabled", splitDirectionsEnabled);
		compound.setBoolean("splitNorthSouthEnabled", splitNorthSouthEnabled);
		compound.setBoolean("splitWestEastEnabled", splitWestEastEnabled);
		compound.setBoolean("FyaNightOnlyEnabled", isFyaNightOnlyEnabled());
		compound.setBoolean("isAutoMode", !sensors.isEmpty() || !northSouthPedButtons.isEmpty() || !westEastPedButtons.isEmpty());
		compound.setBoolean("hasNorth", hasNorth);
		compound.setBoolean("hasSouth", hasSouth);
		compound.setBoolean("hasEast", hasEast);
		compound.setBoolean("hasWest", hasWest);
		writeMovementSettingsToNBT(compound);

		getAutomator().setSyncData(compound);
		
		return compound;
	}
	
	@Override
	public void handleUpdateTag(NBTTagCompound tag) {
		super.handleUpdateTag(tag);
		
		readManualSettingDictionary(tag, manualNorthSouthActive, "manualNorthSouthActive");
		readManualSettingDictionary(tag, manualWestEastActive, "manualWestEastActive");
		readManualSettingDictionary(tag, manualNorthSouthInactive, "manualNorthSouthInactive");
		readManualSettingDictionary(tag, manualWestEastInactive, "manualWestEastInactive");
		
		isAutoMode = tag.getBoolean("isAutoMode");
		hasNorth = tag.getBoolean("hasNorth");
		hasSouth = tag.getBoolean("hasSouth");
		hasEast = tag.getBoolean("hasEast");
		hasWest = tag.getBoolean("hasWest");
		if (tag.hasKey("NightFlashEnabled")) {
		    nightFlashEnabled = tag.getBoolean("NightFlashEnabled");
		}
		if (tag.hasKey("PowerOnFlashEnabled")) {
			powerOnFlashEnabled = tag.getBoolean("PowerOnFlashEnabled");
		}
		if(tag.hasKey("northMain")) {
			northMain = tag.getBoolean("northMain");
		}
		if (tag.hasKey("hawkBeaconEnabled")) {
			hawkBeaconEnabled = tag.getBoolean("hawkBeaconEnabled");
		}
		if (tag.hasKey("splitDirectionsEnabled")) {
			splitDirectionsEnabled = tag.getBoolean("splitDirectionsEnabled");
		}
		if (tag.hasKey("splitNorthSouthEnabled")) {
			splitNorthSouthEnabled = tag.getBoolean("splitNorthSouthEnabled");
		}
		if (tag.hasKey("splitWestEastEnabled")) {
			splitWestEastEnabled = tag.getBoolean("splitWestEastEnabled");
		}
		if (tag.hasKey("FyaNightOnlyEnabled")) {
			fyaNightOnlyEnabled = tag.getBoolean("FyaNightOnlyEnabled");
		}
		readMovementSettingsFromNBT(tag);
		fyaNightOnlyEnabled = isFyaNightOnlyEnabled();
		getAutomator().readSyncData(tag);
	}
	
	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(getPos(), 0, getUpdateTag());
	}
	
	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		super.onDataPacket(net, pkt);
		
		handleUpdateTag(pkt.getNbtCompound());
	}
	
	public void setPowered(boolean powered)
	{
		if (!sensors.isEmpty())
		{
			return;
		}
		
		this.powered = powered;
		
		// Power off all lamps
		for(BlockPos westEastLight : westEastLights)
		{
			TileEntity te = world.getTileEntity(westEastLight);
			if (te instanceof BaseTrafficLightTileEntity)
			{
				BaseTrafficLightTileEntity light = (BaseTrafficLightTileEntity)te;
				light.powerOff();
				light.setActive(EnumTrafficLightBulbTypes.DontCross, false, false);
			}
		}
		
		for(BlockPos northSouthLight : northSouthLights)
		{
			TileEntity te = world.getTileEntity(northSouthLight);
			if (te instanceof BaseTrafficLightTileEntity)
			{
				BaseTrafficLightTileEntity light = (BaseTrafficLightTileEntity)te;
				light.powerOff();
				light.setActive(EnumTrafficLightBulbTypes.DontCross, false, false);
			}
		}
		
		if (powered)
		{
			for(EnumTrafficLightBulbTypes bulbType : manualNorthSouthActive.keySet())
			{
				for(BlockPos northSouthLight : northSouthLights)
				{
					TileEntity te = world.getTileEntity(northSouthLight);
					if (te instanceof BaseTrafficLightTileEntity)
					{
						BaseTrafficLightTileEntity light = (BaseTrafficLightTileEntity)te;
						light.setActive(bulbType, true, manualNorthSouthActive.get(bulbType));
					}
				}
			}
			
			for(EnumTrafficLightBulbTypes bulbType : manualWestEastActive.keySet())
			{
				for(BlockPos westEastLight : westEastLights)
				{
					TileEntity te = world.getTileEntity(westEastLight);
					if (te instanceof BaseTrafficLightTileEntity)
					{
						BaseTrafficLightTileEntity light = (BaseTrafficLightTileEntity)te;
						light.setActive(bulbType, true, manualWestEastActive.get(bulbType));
					}
				}
			}
		}
		else
		{
			for(EnumTrafficLightBulbTypes bulbType : manualNorthSouthInactive.keySet())
			{
				for(BlockPos northSouthLight : northSouthLights)
				{
					TileEntity te = world.getTileEntity(northSouthLight);
					if (te instanceof BaseTrafficLightTileEntity)
					{
						BaseTrafficLightTileEntity light = (BaseTrafficLightTileEntity)te;
						light.setActive(bulbType, true, manualNorthSouthInactive.get(bulbType));
					}
				}
			}
			
			for(EnumTrafficLightBulbTypes bulbType : manualWestEastInactive.keySet())
			{
				for(BlockPos westEastLight : westEastLights)
				{
					TileEntity te = world.getTileEntity(westEastLight);
					if (te instanceof BaseTrafficLightTileEntity)
					{
						BaseTrafficLightTileEntity light = (BaseTrafficLightTileEntity)te;
						light.setActive(bulbType, true, manualWestEastInactive.get(bulbType));
					}
				}
			}
		}
		
		markDirty();
	}
	
	public boolean addOrRemoveWestEastTrafficLight(BlockPos pos)
	{
		if (westEastLights.contains(pos))
		{
			westEastLights.remove(pos);
			return false;
		}
		
		westEastLights.add(pos);
		markDirty();
		return true;		
	}
	
	public boolean addOrRemoveNorthSouthTrafficLight(BlockPos pos)
	{
		if (northSouthLights.contains(pos))
		{
			northSouthLights.remove(pos);
			return false;
		}
		
		northSouthLights.add(pos);
		markDirty();
		return true;		
	}

	public boolean addOrRemoveSensor(BlockPos pos)
	{
		if (sensors.contains(pos))
		{
			sensors.remove(pos);
			markDirty();
			world.notifyBlockUpdate(getPos(), world.getBlockState(getPos()), world.getBlockState(getPos()), 3);
			return false;
		}
		else
		{
			sensors.add(pos);
			markDirty();
			world.notifyBlockUpdate(getPos(), world.getBlockState(getPos()), world.getBlockState(getPos()), 3);
			return true;
		}
	}
	
	public boolean addOrRemoveNorthSouthPedButton(BlockPos pos)
	{
		if (northSouthPedButtons.contains(pos))
		{
			northSouthPedButtons.remove(pos);
			markDirty();
			world.notifyBlockUpdate(getPos(), world.getBlockState(getPos()), world.getBlockState(getPos()), 3);
			return false;
		}
		else
		{
			northSouthPedButtons.add(pos);
			markDirty();
			world.notifyBlockUpdate(getPos(), world.getBlockState(getPos()), world.getBlockState(getPos()), 3);
			return true;
		}
	}
	
	public boolean addOrRemoveWestEastPedButton(BlockPos pos)
	{
		if (westEastPedButtons.contains(pos))
		{
			westEastPedButtons.remove(pos);
			markDirty();
			world.notifyBlockUpdate(getPos(), world.getBlockState(getPos()), world.getBlockState(getPos()), 3);
			return false;
		}
		else
		{
			westEastPedButtons.add(pos);
			markDirty();
			world.notifyBlockUpdate(getPos(), world.getBlockState(getPos()), world.getBlockState(getPos()), 3);
			return true;
		}
	}
	
	public void addRemoveNorthSouthActive(EnumTrafficLightBulbTypes type, boolean flash, boolean add)
	{
		if (add)
		{
			manualNorthSouthActive.put(type, flash);
		}
		else if (!add && flash)
		{
			manualNorthSouthActive.put(type, false);
		}
		else
		{
			manualNorthSouthActive.remove(type);
		}
	}
	public void addRemoveWestEastActive(EnumTrafficLightBulbTypes type, boolean flash, boolean add)
	{
		if (add)
		{
			manualWestEastActive.put(type, flash);
		}
		else if (!add && flash)
		{
			manualWestEastActive.put(type, false);
		}
		else
		{
			manualWestEastActive.remove(type);
		}
	}
	public void addRemoveNorthSouthInactive(EnumTrafficLightBulbTypes type, boolean flash, boolean add)
	{
		if (add)
		{
			manualNorthSouthInactive.put(type, flash);
		}
		else if (!add && flash)
		{
			manualNorthSouthInactive.put(type, false);
		}
		else
		{
			manualNorthSouthInactive.remove(type);
		}
	}
	public void addRemoveWestEastInactive(EnumTrafficLightBulbTypes type, boolean flash, boolean add)
	{
		if (add)
		{
			manualWestEastInactive.put(type, flash);
		}
		else if (!add && flash)
		{
			manualWestEastInactive.put(type, false);
		}
		else
		{
			manualWestEastInactive.remove(type);
		}
	}
	
	public void setNorth(Boolean hi) {
		hasNorth = hi;
	}
	public void setSouth(Boolean hi) {
		hasSouth = hi;
	}
	public void setEast(Boolean hi) {
		hasEast = hi;
	}
	public void setWest(Boolean hi) {
		hasWest = hi;
	}

	@Override
	public NBTTagCompound getClientToServerUpdateTag() {
		NBTTagCompound compound = new NBTTagCompound();
		writeManualSettingDictionary(compound, manualNorthSouthActive, "manualNorthSouthActive");
		writeManualSettingDictionary(compound, manualWestEastActive, "manualWestEastActive");
		writeManualSettingDictionary(compound, manualNorthSouthInactive, "manualNorthSouthInactive");
		writeManualSettingDictionary(compound, manualWestEastInactive, "manualWestEastInactive");
		compound.setBoolean("hasNorth", hasNorth);
		compound.setBoolean("hasSouth", hasSouth);
		compound.setBoolean("hasEast", hasEast);
		compound.setBoolean("hasWest", hasWest);
		writeMovementSettingsToNBT(compound);
		getAutomator().setSyncData(compound);
		
		return compound;
	}

	@Override
	public void handleClientToServerUpdateTag(NBTTagCompound compound) {
		readManualSettingDictionary(compound, manualNorthSouthActive, "manualNorthSouthActive");
		readManualSettingDictionary(compound, manualWestEastActive, "manualWestEastActive");
		readManualSettingDictionary(compound, manualNorthSouthInactive, "manualNorthSouthInactive");
		readManualSettingDictionary(compound, manualWestEastInactive, "manualWestEastInactive");
		
		getAutomator().readSyncData(compound);
		if (compound.hasKey("hasNorth")) this.hasNorth = compound.getBoolean("hasNorth");
		if (compound.hasKey("hasSouth")) this.hasSouth = compound.getBoolean("hasSouth");
		if (compound.hasKey("hasEast")) this.hasEast = compound.getBoolean("hasEast");
		if (compound.hasKey("hasWest")) this.hasWest = compound.getBoolean("hasWest");
		readMovementSettingsFromNBT(compound);
		markDirty();
		world.notifyBlockUpdate(getPos(), world.getBlockState(getPos()), world.getBlockState(getPos()), 3);
	}

	public boolean hasSpecificNorthSouthManualOption(EnumTrafficLightBulbTypes bulbType, boolean flash, boolean forActive)
	{
		if (forActive)
		{
			boolean result = manualNorthSouthActive.containsKey(bulbType);
			if (flash)
			{
				result = result && manualNorthSouthActive.get(bulbType);
			}
			
			return result;
		}
		else
		{
			boolean result = manualNorthSouthInactive.containsKey(bulbType);
			if (flash)
			{
				result = result && manualNorthSouthInactive.get(bulbType);
			}
			
			return result;
		}
	}

	public boolean hasSpecificWestEastManualOption(EnumTrafficLightBulbTypes bulbType, boolean flash, boolean forActive)
	{
		if (forActive)
		{
			boolean result = manualWestEastActive.containsKey(bulbType);
			if (flash)
			{
				result = result && manualWestEastActive.get(bulbType);
			}
			
			return result;
		}
		else
		{
			boolean result = manualWestEastInactive.containsKey(bulbType);
			if (flash)
			{
				result = result && manualWestEastInactive.get(bulbType);
			}
			
			return result;
		}
	}

	public boolean isAutoMode()
	{
		return isAutoMode;
	}
	private boolean isInDarkMode = true;
	@Override
	public void update() {
		if (world.isRemote)
		{
			return;
		}
		
		if (!sensors.isEmpty() || !northSouthPedButtons.isEmpty() || !westEastPedButtons.isEmpty())
		{
			boolean wasPowered = this.powered;
			boolean isNowPowered = world.isBlockPowered(pos);
			if (wasPowered != isNowPowered) {
			    setPowered(isNowPowered);
			    if (!isNowPowered) {
			        enterDarkMode(); // shut off all signals
			    } else {
			    	isInDarkMode = false;
			        flashRedYellowForRecovery(); // flash mode before normal automation
			    }
			}
			this.powered = isNowPowered;
			if (isNowPowered && powerOnFlashEnabled && !isFlashingEmergency) {
				isInDarkMode = false;
				flashRedYellowForRecovery();
			}
			getAutomator().update();
		}
	}
	
	private void enterDarkMode() {
		isInDarkMode = true;
		recoveryGeneration++;
		isFlashingEmergency = false;
	    for (BlockPos pos : northSouthLights) {
	        TileEntity te = world.getTileEntity(pos);
	        if (te instanceof BaseTrafficLightTileEntity) {
	            ((BaseTrafficLightTileEntity) te).powerOff();
	        }
	    }
	    for (BlockPos pos : westEastLights) {
	        TileEntity te = world.getTileEntity(pos);
	        if (te instanceof BaseTrafficLightTileEntity) {
	            ((BaseTrafficLightTileEntity) te).powerOff();
	        }
	    }
	}
	
	private boolean isFlashingEmergency = false;
	private volatile int recoveryGeneration = 0;
	public boolean isFlashOn;

	
	private void flashRedYellowForRecovery() {
	    isFlashingEmergency = true;
	    final int generation = ++recoveryGeneration;
	    final boolean holdFlash = powerOnFlashEnabled;
	    clearLightsBeforeRecoveryFlash();

	    new Thread(() -> {
	        clearLightsBeforeRecoveryFlash();
	        // Set flashing state once
	    	if(!northMain) {
	    		  for (BlockPos pos : northSouthLights) {
	  	            TileEntity te = world.getTileEntity(pos);
	  	            if (te instanceof BaseTrafficLightTileEntity) {
	  	                BaseTrafficLightTileEntity light = (BaseTrafficLightTileEntity) te;
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
	  	            TileEntity te = world.getTileEntity(pos);
	  	            if (te instanceof BaseTrafficLightTileEntity) {
	  	                BaseTrafficLightTileEntity light = (BaseTrafficLightTileEntity) te;
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
	            TileEntity te = world.getTileEntity(pos);
	            if (te instanceof BaseTrafficLightTileEntity) {
	                BaseTrafficLightTileEntity light = (BaseTrafficLightTileEntity) te;
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
	            TileEntity te = world.getTileEntity(pos);
	            if (te instanceof BaseTrafficLightTileEntity) {
	                BaseTrafficLightTileEntity light = (BaseTrafficLightTileEntity) te;
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

	        try {
	            if (holdFlash) {
	                while (generation == recoveryGeneration && powerOnFlashEnabled && powered) {
	                    Thread.sleep(250);
	                }
	            } else {
	                Thread.sleep(15000); // wait 15 seconds
	            }
	        } catch (InterruptedException ignored) {}

	        ((WorldServer) world).addScheduledTask(() -> {
	            if (generation != recoveryGeneration) {
	                return;
	            }
	            isFlashingEmergency = false;
	            if (!powerOnFlashEnabled && powered) {
	                getAutomator().reset();
	            }
	        });
	    }).start();
	}

	private void clearLightsBeforeRecoveryFlash() {
		for (BlockPos pos : northSouthLights) {
			TileEntity te = world.getTileEntity(pos);
			if (te instanceof BaseTrafficLightTileEntity) {
				((BaseTrafficLightTileEntity) te).powerOff();
			}
		}
		for (BlockPos pos : westEastLights) {
			TileEntity te = world.getTileEntity(pos);
			if (te instanceof BaseTrafficLightTileEntity) {
				((BaseTrafficLightTileEntity) te).powerOff();
			}
		}
	}


	
	public void onBreak(World world)
	{
		for(BlockPos pos : northSouthPedButtons)
		{
			TileEntity prelimPed = world.getTileEntity(pos);
			if (prelimPed == null || !(prelimPed instanceof PedestrianButtonTileEntity))
			{
				continue;
			}
			
			((PedestrianButtonTileEntity)prelimPed).removePairedBox(getPos());
		}
		
		for(BlockPos pos : westEastPedButtons)
		{
			TileEntity prelimPed = world.getTileEntity(pos);
			if (prelimPed == null || !(prelimPed instanceof PedestrianButtonTileEntity))
			{
				continue;
			}
			
			((PedestrianButtonTileEntity)prelimPed).removePairedBox(getPos());
		}
	}
	
	public Automator getAutomator()
	{
		if (automator == null)
		{
			automator = new Automator();
		}
		
		return automator;
	}
	
	private enum LeftTripDirection {
	    NONE,
	    NORTH_SOUTH_LEFT,
	    EAST_WEST_LEFT
	}
	
	
	
	public class Automator
	{
		private long nextUpdate;
		private boolean hasInitialized = false;
		
		private final ImmutableList<Class<?>> sensorClasses = ImmutableList
				.<Class<?>>builder()
				.add(BlockTrafficSensorLeft.class)
				.add(BlockTrafficSensorStraight.class)
				.add(BlockTrafficSensorRight.class)
				.build();
		
		private final String nbtPrefix = "automated_";
		
		private Stages lastStage = Stages.Red;
		private long stageStartTime = 0;

		
		private RightOfWays lastRightOfWay = RightOfWays.EastWest;
		private RightOfWays forcedNextRightOfWay = null;
		private EnumFacing activeSplitDirection = EnumFacing.NORTH;
		private EnumFacing nextNorthSouthSplitDirection = EnumFacing.NORTH;
		private EnumFacing nextWestEastSplitDirection = EnumFacing.EAST;
		private boolean swappedWithinCurrentRow = false;
		private RightOfWays pendingSplitSwapRow = null;
		private EnumFacing pendingSplitSwapDirection = null;
		private final EnumFacing[] splitOrder = new EnumFacing[] { EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST };
		private boolean dir1RightAfterLeft = false;
		private boolean dir2RightAfterLeft = false;
		
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
				public Automator() {
					// Pull HAWK defaults from config for newly created controllers.
					// Controllers that already exist in-world will overwrite these via NBT load.
					this.crossTime = Config.hawkDefaultSolidRedSeconds;
					this.crossWarningTime = Config.hawkDefaultFlashRedSeconds;
				}
		private double rightArrowTime = 5;
		private boolean northSouthPedQueued;
		private boolean westEastPedQueued;
		
		
		
		public void reset() {
		    lastStage = Stages.Red;
		    forcedNextRightOfWay = null;
		    if(northMain) {
		    	lastRightOfWay = RightOfWays.EastWest;
		    } else {
		    	lastRightOfWay = RightOfWays.NorthSouth;
		    }
		    activeSplitDirection = EnumFacing.NORTH;
		    nextNorthSouthSplitDirection = EnumFacing.NORTH;
		    nextWestEastSplitDirection = EnumFacing.EAST;
		    swappedWithinCurrentRow = false;
		    pendingSplitSwapRow = null;
		    pendingSplitSwapDirection = null;
		    
		    hasInitialized = false;
		    nextUpdate = 0;
		    stageStartTime = 0;
		}

		private EnumFacing getOtherRowDirection(RightOfWays row, EnumFacing dir) {
			if (row == RightOfWays.NorthSouth) {
				return dir == EnumFacing.NORTH ? EnumFacing.SOUTH : EnumFacing.NORTH;
			}
			return dir == EnumFacing.EAST ? EnumFacing.WEST : EnumFacing.EAST;
		}

		private EnumFacing getAndAdvancePreferredSplitDirection(RightOfWays row) {
			if (row == RightOfWays.NorthSouth) {
				EnumFacing chosen = nextNorthSouthSplitDirection;
				nextNorthSouthSplitDirection = (nextNorthSouthSplitDirection == EnumFacing.NORTH) ? EnumFacing.SOUTH : EnumFacing.NORTH;
				return chosen;
			}
			EnumFacing chosen = nextWestEastSplitDirection;
			nextWestEastSplitDirection = (nextWestEastSplitDirection == EnumFacing.EAST) ? EnumFacing.WEST : EnumFacing.EAST;
			return chosen;
		}

		private EnumFacing chooseSplitDirectionForRowFixedOrder(RightOfWays row) {
			EnumFacing preferred = getAndAdvancePreferredSplitDirection(row);
			EnumFacing other = getOtherRowDirection(row, preferred);

			// Respect enabled approaches with at least one movement configured.
			if (!isApproachServicable(preferred)) {
				if (isApproachServicable(other)) {
					return other;
				}
				return isApproachEnabled(preferred) ? preferred : other;
			}

			// Prefer demand on the preferred approach; otherwise use the other if it has demand.
			final boolean preferredDemand = isApproachServicable(preferred) && hasAnyDemandForFacing(preferred);
			final boolean otherDemand = isApproachServicable(other) && hasAnyDemandForFacing(other);
			if (!preferredDemand && otherDemand) {
				return other;
			}
			return preferred;
		}

		private boolean isSplitDirectionEnabled(EnumFacing facing) {
			switch (facing) {
				case NORTH:
					return TrafficLightControlBoxTileEntity.this.hasNorth;
				case SOUTH:
					return TrafficLightControlBoxTileEntity.this.hasSouth;
				case EAST:
					return TrafficLightControlBoxTileEntity.this.hasEast;
				case WEST:
					return TrafficLightControlBoxTileEntity.this.hasWest;
				default:
					return true;
			}
		}

		private int splitIndex(EnumFacing facing) {
			for (int i = 0; i < splitOrder.length; i++) {
				if (splitOrder[i] == facing) {
					return i;
				}
			}
			return 0;
		}

		private EnumFacing getNextSplitDirection(EnumFacing current) {
			final int start = splitIndex(current);
			// First pass: pick next servicable direction with demand.
			for (int step = 1; step <= splitOrder.length; step++) {
				EnumFacing candidate = splitOrder[(start + step) % splitOrder.length];
				if (!isApproachServicable(candidate)) {
					continue;
				}
				if (hasAnyDemandForFacing(candidate)) {
					return candidate;
				}
			}
			// Second pass: if no demand anywhere, just rotate to next servicable.
			for (int step = 1; step <= splitOrder.length; step++) {
				EnumFacing candidate = splitOrder[(start + step) % splitOrder.length];
				if (isApproachServicable(candidate)) {
					return candidate;
				}
			}
			return current;
		}

		private EnumFacing getRowDir1(RightOfWays row) {
			return row == RightOfWays.NorthSouth ? EnumFacing.NORTH : EnumFacing.EAST;
		}

		private EnumFacing getRowDir2(RightOfWays row) {
			return row == RightOfWays.NorthSouth ? EnumFacing.SOUTH : EnumFacing.WEST;
		}

		private boolean isApproachEnabled(EnumFacing facing) {
			return isSplitDirectionEnabled(facing);
		}

		private boolean isApproachServicable(EnumFacing facing) {
			if (!isApproachEnabled(facing) || !hasAnyMovementEnabled(facing)) {
				return false;
			}
			// Right-only approaches are served during the coupled perpendicular left phase when available.
			return !isRightOnlyCoupledToPerpendicularLeft(facing);
		}

		private boolean isRightOnlyCoupledToPerpendicularLeft(EnumFacing facing) {
			ApproachMovementSettings settings = TrafficLightControlBoxTileEntity.this.getMovementSettings(facing);
			if (!ApproachMovementPhaseHelper.isRightOnly(settings)) {
				return false;
			}
			EnumFacing coupledLeft = TrafficLightFacingResolver.getCoupledLeftApproachForRightOnly(facing);
			return isApproachEnabled(coupledLeft) && isLeftMovementEnabled(coupledLeft);
		}

		private boolean hasAnyDemandForRow(RightOfWays row) {
			final EnumFacing dir1 = getRowDir1(row);
			final EnumFacing dir2 = getRowDir2(row);
			final boolean enabled1 = isApproachEnabled(dir1);
			final boolean enabled2 = isApproachEnabled(dir2);
			if (!enabled1 && !enabled2) {
				return false;
			}
			return (enabled1 && hasAnyDemandForFacing(dir1)) || (enabled2 && hasAnyDemandForFacing(dir2));
		}

		private EnumFacing pickSplitDirectionForRow(RightOfWays row, EnumFacing current) {
			final EnumFacing dir1 = getRowDir1(row);
			final EnumFacing dir2 = getRowDir2(row);

			EnumFacing normalized = (current == dir1 || current == dir2) ? current : dir1;
			if (!isApproachServicable(normalized)) {
				normalized = isApproachServicable(dir1) ? dir1 : dir2;
			}

			final boolean demand1 = isApproachServicable(dir1) && hasAnyDemandForFacing(dir1);
			final boolean demand2 = isApproachServicable(dir2) && hasAnyDemandForFacing(dir2);

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
		    long time = world.getWorldTime() % 24000;
		    inNightFlash = (time >= nightFlashStart || time <= nightFlashEnd);

		    // If FYA is configured to be night-only, don't hard cut to red when day starts.
		    // Instead, run a short solid-yellow transition before holding red.
		    if (anyFyaNightOnlyConfigured() && lastInNightFlash && !inNightFlash) {
		    	final double yellowSeconds = (lastRightOfWay == RightOfWays.NorthSouth) ? getYellowTimeNS() : getYellowTimeEW();
		    	final int yellowTicks = (int) Math.max(10, Math.min(200, Math.round(yellowSeconds * 20.0)));
		    	fyaDayTransitionTicksRemaining = yellowTicks;
		    }
		    lastInNightFlash = inNightFlash;
		    if (fyaDayTransitionTicksRemaining > 0) {
		    	fyaDayTransitionTicksRemaining--;
		    }

		   
		   
		        
		        if(!inNightFlash || !nightFlashEnabled) {
		        	isFlashOn = false;
		        }

		        if (nightFlashEnabled && inNightFlash) {
		            // Turn all lights off before updating
		          

		            if (!isFlashOn) {
		                // West/East = Red
		            	  for (BlockPos pos : northSouthLights) {
				                TileEntity te = world.getTileEntity(pos);
				                if (te instanceof BaseTrafficLightTileEntity) {
				                    ((BaseTrafficLightTileEntity) te).powerOff();
				                }
				            }

				            for (BlockPos pos : westEastLights) {
				                TileEntity te = world.getTileEntity(pos);
				                if (te instanceof BaseTrafficLightTileEntity) {
				                    ((BaseTrafficLightTileEntity) te).powerOff();
				                }
				            }
				            
				            
				            if(!northMain) {
				            	 for (BlockPos pos : northSouthLights) {
					                    TileEntity te = world.getTileEntity(pos);
					                    if (te instanceof BaseTrafficLightTileEntity) {
					                        BaseTrafficLightTileEntity light = (BaseTrafficLightTileEntity) te;
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
					                

					                // North/South = Yellow
					                for (BlockPos pos : westEastLights ) {
					                    TileEntity te = world.getTileEntity(pos);
					                    if (te instanceof BaseTrafficLightTileEntity) {
					                        BaseTrafficLightTileEntity light = (BaseTrafficLightTileEntity) te;
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
		                    TileEntity te = world.getTileEntity(pos);
		                    if (te instanceof BaseTrafficLightTileEntity) {
		                        BaseTrafficLightTileEntity light = (BaseTrafficLightTileEntity) te;
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
		                

		                // North/South = Yellow
		                for (BlockPos pos : northSouthLights) {
		                    TileEntity te = world.getTileEntity(pos);
		                    if (te instanceof BaseTrafficLightTileEntity) {
		                        BaseTrafficLightTileEntity light = (BaseTrafficLightTileEntity) te;
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

		    // Skip everything else if in alternate states
		    if (isInDarkMode || isFlashingEmergency) return;

		    if (!hasInitialized) {
		        initialize();
		    }

		    if (TrafficLightControlBoxTileEntity.this.isHawkBeaconEnabled()) {
		    	lastRightOfWay = northMain ? RightOfWays.NorthSouth : RightOfWays.EastWest;
		    }

		    if (MinecraftServer.getCurrentTimeMillis() < nextUpdate) {
		        return;
		    }

		    if (lastStage == Stages.Red) {
		    	if (!TrafficLightControlBoxTileEntity.this.isHawkBeaconEnabled()) {
		    		// Always alternate axis order: N/S then E/W then repeat.
		    		// BUT: if we have a pending split swap (E<->W or N<->S), we must stay on the same axis
		    		// so the other approach can be served next (after Yellow+Red), otherwise it gets delayed
		    		// until after the other axis runs.
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

		    		// If we scheduled an approach swap on this same axis (E->W / N->S), apply it now
		    		// (after Yellow and Red have run for the previous approach).
		    		if (pendingSplitSwapRow == lastRightOfWay && pendingSplitSwapDirection != null) {
		    			activeSplitDirection = pendingSplitSwapDirection;
		    			pendingSplitSwapRow = null;
		    			pendingSplitSwapDirection = null;
		    			// We have effectively already served the first approach this cycle.
		    			swappedWithinCurrentRow = true;
		    				appliedPendingSwap = true;
		    		}

		            			if (!appliedPendingSwap && TrafficLightControlBoxTileEntity.this.isSplitEnabledForRow(lastRightOfWay)) {
		    			// Deterministic approach order per axis: N,S then E,W.
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
		    if (currentRightOfWay == RightOfWays.NorthSouth && sensorResults.Direction1SensorLeft)
		        return LeftTripDirection.NORTH_SOUTH_LEFT;
		    if (currentRightOfWay == RightOfWays.EastWest && sensorResults.Direction2SensorLeft)
		        return LeftTripDirection.EAST_WEST_LEFT;
		    return LeftTripDirection.NONE;
		}

		private void applyApproachEnableRules(RightOfWays row, SensorCheckResult result) {
			final EnumFacing dir1 = getRowDir1(row);
			final EnumFacing dir2 = getRowDir2(row);
			final boolean dir1Enabled = isApproachEnabled(dir1);
			final boolean dir2Enabled = isApproachEnabled(dir2);

			// Ignore any sensor demand coming from disabled approaches.
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

			// T-intersection behavior: if one side of an axis is missing, don't run a separate protected left phase
			// for the remaining approach. Treat left-only trips as straight demand instead.
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
			applyRightOnlySensorClear(dir1, dir2, result);
			applyCoupledRightOnlyLeftDemand(row, result);
		}

		private void applyRightOnlySensorClear(EnumFacing dir1, EnumFacing dir2, SensorCheckResult result) {
			ApproachMovementSettings settings1 = TrafficLightControlBoxTileEntity.this.getMovementSettings(dir1);
			ApproachMovementSettings settings2 = TrafficLightControlBoxTileEntity.this.getMovementSettings(dir2);

			if (ApproachMovementPhaseHelper.isRightOnly(settings1)) {
				result.Direction1SensorRight = false;
			}
			if (ApproachMovementPhaseHelper.isRightOnly(settings2)) {
				result.Direction2SensorRight = false;
			}
		}

		/** Right-only demand is served during the coupled perpendicular left (E right -> N left). */
		private void applyCoupledRightOnlyLeftDemand(RightOfWays row, SensorCheckResult result) {
			final EnumFacing dir1 = getRowDir1(row);
			final EnumFacing dir2 = getRowDir2(row);

			for (EnumFacing facing : new EnumFacing[] { EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST }) {
				if (!isApproachEnabled(facing)) {
					continue;
				}
				ApproachMovementSettings settings = TrafficLightControlBoxTileEntity.this.getMovementSettings(facing);
				if (!ApproachMovementPhaseHelper.isRightOnly(settings)) {
					continue;
				}
				if (!hasRightSensorDemandForFacing(facing)) {
					continue;
				}

				EnumFacing coupledLeft = TrafficLightFacingResolver.getCoupledLeftApproachForRightOnly(facing);
				if (!isApproachEnabled(coupledLeft) || !isLeftMovementEnabled(coupledLeft)) {
					continue;
				}
				if (coupledLeft == dir1) {
					result.Direction1SensorLeft = true;
				} else if (coupledLeft == dir2) {
					result.Direction2SensorLeft = true;
				}
			}
		}

		private boolean hasRightSensorDemandForFacing(EnumFacing facing) {
			if (!isApproachEnabled(facing)) {
				return false;
			}
			for (BlockPos sensePos : sensors) {
				IBlockState senseState = world.getBlockState(sensePos);
				if (!(senseState.getBlock() instanceof BlockTrafficSensorRight)) {
					continue;
				}
				EnumFacing sensorFacing = getSensorFacing(senseState);
				if (sensorFacing != facing) {
					continue;
				}
				if (isSensorTrippedAt(sensePos)) {
					return true;
				}
			}
			return false;
		}

		private Stages getLeftTurnStageForApproach(EnumFacing leftApproach, RightOfWays row) {
			final EnumFacing dir1 = getRowDir1(row);
			final EnumFacing dir2 = getRowDir2(row);
			if (leftApproach == dir1) {
				return Stages.Direction1LeftTurnArrow;
			}
			if (leftApproach == dir2) {
				return Stages.Direction2LeftTurnArrow;
			}
			return null;
		}

		private Stages tryCoupledLeftForRightOnly(EnumFacing rightOnlyApproach, RightOfWays row, double arrowMinNS, double arrowMinEW) {
			EnumFacing coupledLeft = TrafficLightFacingResolver.getCoupledLeftApproachForRightOnly(rightOnlyApproach);
			if (!isLeftMovementEnabled(coupledLeft)) {
				return null;
			}
			Stages leftStage = getLeftTurnStageForApproach(coupledLeft, row);
			if (leftStage == null) {
				return null;
			}
			setNextUpdate(coupledLeft == getRowDir1(row) ? arrowMinNS : arrowMinEW);
			return leftStage;
		}

		private void applyMovementSensorRules(EnumFacing facing, SensorCheckResult result, boolean isDir1) {
			if (!hasAnyMovementEnabled(facing)) {
				if (isDir1) {
					result.Direction1Sensor = false;
					result.Direction1SensorLeft = false;
					result.Direction1SensorRight = false;
				} else {
					result.Direction2Sensor = false;
					result.Direction2SensorLeft = false;
					result.Direction2SensorRight = false;
				}
				return;
			}
			ApproachMovementSettings settings = TrafficLightControlBoxTileEntity.this.getMovementSettings(facing);
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

		
		private void initialize()
		{
			
			for(BaseTrafficLightTileEntity te : northSouthLights
					.stream()
					.map(bp ->
					{
						TileEntity teAtPos = world.getTileEntity(bp);
						if (teAtPos instanceof BaseTrafficLightTileEntity)
						{
							return (BaseTrafficLightTileEntity)teAtPos;
						}
						
						return null;
					})
					.filter(Objects::nonNull)
					.collect(Collectors.toList()))
			{
				te.powerOff();
				
			};
			
			for(BaseTrafficLightTileEntity te : westEastLights
					.stream()
					.map(bp ->
					{
						TileEntity teAtPos = world.getTileEntity(bp);
						if (teAtPos instanceof BaseTrafficLightTileEntity)
						{
							return (BaseTrafficLightTileEntity)teAtPos;
						}
						
						return null;
					})
					.filter(Objects::nonNull)
					.collect(Collectors.toList()))
			{
				te.powerOff();
				
			};
			
			hasInitialized = true;
		}
		
		private Stages updateLightsByStage(Stages stage)
		{
			final boolean hawkStage = stage == Stages.HawkFlashYellow || stage == Stages.HawkSolidYellow || stage == Stages.HawkSolidRed || stage == Stages.HawkFlashRed;
			if (hawkStage && TrafficLightControlBoxTileEntity.this.isHawkBeaconEnabled())
			{
				lastRightOfWay = northMain ? RightOfWays.NorthSouth : RightOfWays.EastWest;
			}
			
			

			//System.out.print("AUTO:" + Automator.this.stageStartTime);
			
			List<BaseTrafficLightTileEntity> trafficLightsForRightOfWay;
			List<BaseTrafficLightTileEntity> trafficLightsOpposingRightOfWay;
			EnumFacing direction1;
			EnumFacing direction2;
			
			if (lastRightOfWay == RightOfWays.NorthSouth )
			{
				trafficLightsForRightOfWay = TrafficLightControlBoxTileEntity.this.getTrafficLightsForApproaches(EnumFacing.NORTH, EnumFacing.SOUTH);
				trafficLightsOpposingRightOfWay = TrafficLightControlBoxTileEntity.this.getTrafficLightsForApproaches(EnumFacing.EAST, EnumFacing.WEST);
				
				direction1 = EnumFacing.NORTH;
				direction2 = EnumFacing.SOUTH;
			}
			 else {
				trafficLightsForRightOfWay = TrafficLightControlBoxTileEntity.this.getTrafficLightsForApproaches(EnumFacing.EAST, EnumFacing.WEST);
				trafficLightsOpposingRightOfWay = TrafficLightControlBoxTileEntity.this.getTrafficLightsForApproaches(EnumFacing.NORTH, EnumFacing.SOUTH);
				
				direction1 = EnumFacing.EAST;
				direction2 = EnumFacing.WEST;
			}
			
			EnumFacing direction1cw = direction1.rotateY();
			EnumFacing direction2cw = direction2.rotateY();
			
		
			
			switch(stage)
			{
				case Red:
					trafficLightsForRightOfWay
					.stream()
					.forEach(tl ->
					{
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
					
					trafficLightsOpposingRightOfWay
					.stream()
					.forEach(tl ->
					{
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
					trafficLightsForRightOfWay
						.stream()
						.forEach(tl -> {
							final EnumFacing approach = TrafficLightFacingResolver.resolveApproachFacing(tl);
							final boolean servingCoupledRightOnly = isRightOnlyCoupledToPerpendicularLeft(approach);
							if (approach != direction1) {
								ApproachMovementBulbHelper.forceAllRed(tl);
								return;
							}
							if ((stage == Stages.Direction1LeftTurnArrow && !isLeftMovementEnabled(direction1))
									|| (stage == Stages.Direction1RightTurnArrow && !isRightMovementEnabled(direction1))
									|| servingCoupledRightOnly) {
								ApproachMovementBulbHelper.forceAllRed(tl);
								return;
							}

							tl.powerOff();
							tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.GreenArrowUTurn, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft2, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.GreenArrowUTurn2, true, false);
							if (isStraightMovementEnabled(direction1)) {
								tl.setActive(EnumTrafficLightBulbTypes.Green, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.StraightGreen, true, false);
							}
							tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight2, true, false);
						});

					trafficLightsOpposingRightOfWay
					.stream()
					.forEach(tl -> {
						final EnumFacing approach = TrafficLightFacingResolver.resolveApproachFacing(tl);
						if (approach != direction1cw) {
							ApproachMovementBulbHelper.forceAllRed(tl);
							return;
						}

						tl.powerOff();
						tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
						if (stage != Stages.Direction1LeftTurnArrow
								|| allowOpposingRightWithLeft(direction1, approach)) {
							tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight2, true, false);
						} else {
							setOpposingRightRed(tl);
						}
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
					});
					break;
				case Direction2RightTurnArrow:
				case Direction2LeftTurnArrow:
					trafficLightsForRightOfWay
					.stream()
					.forEach(tl -> {
						final EnumFacing approach = TrafficLightFacingResolver.resolveApproachFacing(tl);
						final boolean servingCoupledRightOnly = isRightOnlyCoupledToPerpendicularLeft(approach);
						if (approach == direction1) {
							final ApproachMovementSettings crossSettings = TrafficLightControlBoxTileEntity.this.getMovementSettings(direction1);
							tl.powerOff();
							if (crossSettings.straightEnabled) {
								tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
							}
							applyLeftFyaYellow(tl, approach, true);
							if (!allowFyaFlashFor(approach, false) && !isFyaDayTransitionFor(approach, false)) {
								tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
							}
							tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
							return;
						}
						if (approach != direction2) {
							ApproachMovementBulbHelper.forceAllRed(tl);
							return;
						}
						if ((stage == Stages.Direction2LeftTurnArrow && !isLeftMovementEnabled(direction2))
								|| (stage == Stages.Direction2RightTurnArrow && !isRightMovementEnabled(direction2))
								|| servingCoupledRightOnly) {
							ApproachMovementBulbHelper.forceAllRed(tl);
							return;
						}

						tl.powerOff();
						tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.GreenArrowUTurn, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft2, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.GreenArrowUTurn2, true, false);
						if (isStraightMovementEnabled(direction2)) {
							tl.setActive(EnumTrafficLightBulbTypes.Green, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.StraightGreen, true, false);
						}
						tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight2, true, false);
					});

					trafficLightsOpposingRightOfWay
					.stream()
					.forEach(tl -> {
						final EnumFacing approach = TrafficLightFacingResolver.resolveApproachFacing(tl);
						if (approach != direction2cw) {
							ApproachMovementBulbHelper.forceAllRed(tl);
							return;
						}

						tl.powerOff();
						tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
						if (stage != Stages.Direction2LeftTurnArrow
								|| allowOpposingRightWithLeft(direction2, approach)) {
							tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight2, true, false);
						} else {
							setOpposingRightRed(tl);
						}
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
					});
					break;
				case BothTurnArrow:
					trafficLightsForRightOfWay
					.stream()
					.forEach(tl ->
					{
						final EnumFacing approach = TrafficLightFacingResolver.resolveApproachFacing(tl);
						final boolean serveLeft = (approach == direction1 && isLeftMovementEnabled(direction1))
								|| (approach == direction2 && isLeftMovementEnabled(direction2));
						if (!serveLeft) {
							ApproachMovementBulbHelper.forceAllRed(tl);
							return;
						}

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
					
					trafficLightsOpposingRightOfWay
					.stream()
					.forEach(tl -> {
						final EnumFacing approach = TrafficLightFacingResolver.resolveApproachFacing(tl);
						final boolean allowRight = (approach != direction1cw && approach != direction2cw)
								|| (approach == direction1cw
								&& allowOpposingRightWithLeft(direction1, approach))
								|| (approach == direction2cw
								&& allowOpposingRightWithLeft(direction2, approach));
						tl.powerOff();
						tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
						if (allowRight) {
							tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight2, true, false);
						} else {
							setOpposingRightRed(tl);
						}
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
					});
					break;
				case Direction1LeftTurnArrowYellow:
					trafficLightsForRightOfWay
						.stream()
						.forEach(tl -> {
							IBlockState tlBs = world.getBlockState(tl.getPos());
							if (TrafficLightFacingResolver.isFacing(tl, direction1.getOpposite()))
							{
								
								tl.powerOff();
								tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
								final EnumFacing approach = TrafficLightFacingResolver.resolveApproachFacing(tl);
								applyLeftFyaYellow(tl, approach, true);
								if (!allowFyaFlashFor(approach, false) && !isFyaDayTransitionFor(approach, false)) {
									tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
									tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
								}
								tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
							}
							
							
							
							
							if (!TrafficLightFacingResolver.isFacing(tl, direction1))
							{
								ApproachMovementBulbHelper.forceAllRed(tl);
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
					
					trafficLightsOpposingRightOfWay
					.stream()
					.forEach(tl -> {
						IBlockState tlBs = world.getBlockState(tl.getPos());
						
						if (TrafficLightFacingResolver.isFacing(tl, direction1cw.getOpposite()))
						{
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
						
						if (!TrafficLightFacingResolver.isFacing(tl, direction1cw))
						{
							ApproachMovementBulbHelper.forceAllRed(tl);
							return;
						}
						
						tl.powerOff();
						tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
						if (allowOpposingRightWithLeft(direction1, direction1cw)) {
							tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight3, true, false);
						} else {
							setOpposingRightRed(tl);
						}
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
					});
					break;
				case Direction2LeftTurnArrowYellow:
					trafficLightsForRightOfWay
					.stream()
					.forEach(tl -> {
						IBlockState tlBs = world.getBlockState(tl.getPos());
						if (TrafficLightFacingResolver.isFacing(tl, direction2.getOpposite()))
						{
							
							tl.powerOff();
							tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
							final EnumFacing approach = TrafficLightFacingResolver.resolveApproachFacing(tl);
							applyLeftFyaYellow(tl, approach, true);
							if (!allowFyaFlashFor(approach, false) && !isFyaDayTransitionFor(approach, false)) {
								tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
							}
							tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
						}
						if (!TrafficLightFacingResolver.isFacing(tl, direction2))
						{
							ApproachMovementBulbHelper.forceAllRed(tl);
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
					
					trafficLightsOpposingRightOfWay
					.stream()
					.forEach(tl -> {
						IBlockState tlBs = world.getBlockState(tl.getPos());
						if (TrafficLightFacingResolver.isFacing(tl, direction2cw.getOpposite()))
						{
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
						
						
						if (!TrafficLightFacingResolver.isFacing(tl, direction2cw))
						{
							ApproachMovementBulbHelper.forceAllRed(tl);
							return;
						}
						
						tl.powerOff();
						tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
						if (allowOpposingRightWithLeft(direction2, direction2cw)) {
							tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight3, true, false);
						} else {
							setOpposingRightRed(tl);
						}
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
					});
					break;
				case BothTurnArrowYellow:
					trafficLightsForRightOfWay
					.stream()
					.forEach(tl ->
					{
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
					
					trafficLightsOpposingRightOfWay
					.stream()
					.forEach(tl -> {
						final EnumFacing approach = TrafficLightFacingResolver.resolveApproachFacing(tl);
						final boolean allowRight = (approach != direction1cw && approach != direction2cw)
								|| (approach == direction1cw
								&& allowOpposingRightWithLeft(direction1, approach))
								|| (approach == direction2cw
								&& allowOpposingRightWithLeft(direction2, approach));
						tl.powerOff();
						tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
						if (allowRight) {
							tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight3, true, false);
						} else {
							setOpposingRightRed(tl);
						}
						
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
					});
					break;
				case Direction1RightTurnArrowYellow:
					trafficLightsForRightOfWay
					.stream()
					.forEach(tl -> {
						IBlockState tlBs = world.getBlockState(tl.getPos());
						if (!TrafficLightFacingResolver.isFacing(tl, direction1))
						{
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
				
					trafficLightsOpposingRightOfWay
					.stream()
					.forEach(tl -> {
						IBlockState tlBs = world.getBlockState(tl.getPos());
						if (!TrafficLightFacingResolver.isFacing(tl, direction1cw))
						{
							ApproachMovementBulbHelper.forceAllRed(tl);
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
					trafficLightsForRightOfWay
					.stream()
					.forEach(tl -> {
						IBlockState tlBs = world.getBlockState(tl.getPos());
						if (!TrafficLightFacingResolver.isFacing(tl, direction2))
						{
							ApproachMovementBulbHelper.forceAllRed(tl);
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
				
					trafficLightsOpposingRightOfWay
					.stream()
					.forEach(tl -> {
						IBlockState tlBs = world.getBlockState(tl.getPos());
						if (!TrafficLightFacingResolver.isFacing(tl, direction2cw))
						{
							ApproachMovementBulbHelper.forceAllRed(tl);
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
				case Yellow:
					final boolean splitYellow = TrafficLightControlBoxTileEntity.this.isSplitEnabledForRow(lastRightOfWay);
					final EnumFacing splitYellowDir = activeSplitDirection;
					trafficLightsForRightOfWay
					.stream()
					.forEach(tl ->
					{
						tl.powerOff();
						final EnumFacing approach = TrafficLightFacingResolver.resolveApproachFacing(tl);
						if (splitYellow) {
							final boolean allowYellow = approach == splitYellowDir;
							if (!allowYellow) {
								ApproachMovementBulbHelper.forceAllRed(tl);
								return;
							}
						}

						final ApproachMovementSettings yellowSettings = TrafficLightControlBoxTileEntity.this.getMovementSettings(approach);
						final boolean oppositeApproachDisabled = !splitYellow
								&& !isApproachEnabled(TrafficLightFacingResolver.getOppositeApproach(approach));
						// Shared turns only for THIS approach; also T-intersection / active split approach.
						final boolean shareTurnsWithStraight = isSharedTurnsFor(approach)
								|| oppositeApproachDisabled
								|| (splitYellow && approach == splitYellowDir);
						final boolean leftFyaActive = !shareTurnsWithStraight
								&& (allowFyaFlashFor(approach, true) || isFyaDayTransitionFor(approach, true));
						final boolean rightFyaActive = !shareTurnsWithStraight
								&& (allowFyaFlashFor(approach, false) || isFyaDayTransitionFor(approach, false));
						if (shareTurnsWithStraight) {
							ApproachMovementBulbHelper.applySharedTurnYellows(tl);
						} else if (leftFyaActive) {
							if (yellowSettings.leftEnabled) {
								LeftTurnBulbHelper.setYellow(tl, false);
								tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft3, true, false);
							}
						} else {
							tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
						}
						if (!shareTurnsWithStraight) {
							if (rightFyaActive) {
								if (yellowSettings.rightEnabled) {
									tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight3, true, false);
									tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight, true, false);
									tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight2, false, false);
								}
							} else {
								tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
							}
						}

						if (yellowSettings.straightEnabled) {
							tl.setActive(EnumTrafficLightBulbTypes.Yellow, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.StraightYellow, true, false);
						}
						if (!shareTurnsWithStraight) {
							tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
						}
					});
					
					trafficLightsOpposingRightOfWay
					.stream()
					.forEach(tl -> {
						final EnumFacing approach = TrafficLightFacingResolver.resolveApproachFacing(tl);
						final EnumFacing opposingRightTurnFacing;
						if (splitYellowDir == direction1) {
							opposingRightTurnFacing = direction1cw;
						} else if (splitYellowDir == direction2) {
							opposingRightTurnFacing = direction2cw;
						} else {
							opposingRightTurnFacing = null;
						}
						final EnumFacing sharedLeftApproach;
						if (approach == direction1cw) {
							sharedLeftApproach = direction1;
						} else if (approach == direction2cw) {
							sharedLeftApproach = direction2;
						} else {
							sharedLeftApproach = null;
						}
						final boolean allowOpposingRightTurnYellow;
						if (splitYellow && opposingRightTurnFacing != null) {
							allowOpposingRightTurnYellow = approach == opposingRightTurnFacing
									&& allowOpposingRightWithLeft(splitYellowDir, approach)
									&& (allowFyaFlashFor(opposingRightTurnFacing, false) || isFyaDayTransitionFor(opposingRightTurnFacing, false));
						} else if (!splitYellow && sharedLeftApproach != null && isSharedTurnsFor(sharedLeftApproach)) {
							allowOpposingRightTurnYellow = allowOpposingRightWithLeft(sharedLeftApproach, approach);
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
				case Green:
				case GreenCross:
				case GreenDontCrossWarning:
				{
					final boolean split = TrafficLightControlBoxTileEntity.this.isSplitEnabledForRow(lastRightOfWay);
					final EnumFacing splitDir = activeSplitDirection;

					trafficLightsForRightOfWay
						.stream()
						.forEach(tl ->
						{
							final EnumFacing approach = TrafficLightFacingResolver.resolveApproachFacing(tl);
							final boolean allowGreen = (!split || approach == splitDir)
									&& !isRightOnlyCoupledToPerpendicularLeft(approach);
							final boolean oppositeApproachDisabled = !split
									&& !isApproachEnabled(TrafficLightFacingResolver.getOppositeApproach(approach));
							final ApproachMovementSettings movementSettings = TrafficLightControlBoxTileEntity.this.getMovementSettings(approach);
							// Shared turns only for THIS approach; also T-intersection / active split approach.
							final boolean shareTurnsWithStraight = isSharedTurnsFor(approach)
									|| oppositeApproachDisabled
									|| (split && approach == splitDir);
							final boolean leftFyaActive = !shareTurnsWithStraight
									&& (allowFyaFlashFor(approach, true) || isFyaDayTransitionFor(approach, true));
							final boolean rightFyaActive = !shareTurnsWithStraight
									&& (allowFyaFlashFor(approach, false) || isFyaDayTransitionFor(approach, false));

							tl.powerOff();
							if (allowGreen)
							{
								if (shareTurnsWithStraight) {
									// Left + U-turn + right arrows with straight (T / ramp / shared / split).
									ApproachMovementBulbHelper.applySharedTurnGreens(tl);
								} else if (!split) {
									applyLeftFyaPermissive(tl, approach);
									if (!leftFyaActive && !rightFyaActive) {
										tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
										tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
									}
								}
								// Only show red turn arrows when permissive/protected indications are not being shown.
								if (shareTurnsWithStraight || leftFyaActive) {
									// leave off
								} else {
									tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
									tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
								}
								if (movementSettings.straightEnabled) {
									tl.setActive(EnumTrafficLightBulbTypes.Green, true, false);
									tl.setActive(EnumTrafficLightBulbTypes.StraightGreen, true, false);
									tl.setActive(EnumTrafficLightBulbTypes.GreenDownArrow, true, false);
								}
								// Re-assert shared turn arrows after straight so nothing else can leave them dark.
								if (shareTurnsWithStraight) {
									ApproachMovementBulbHelper.applySharedTurnGreens(tl);
								} else if (!split) {
									if (movementSettings.leftEnabled) {
										tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft2, true, false);
										tl.setActive(EnumTrafficLightBulbTypes.GreenArrowUTurn2, true, false);
									}
									if (movementSettings.rightEnabled) {
										tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight2, true, false);
									}
								}
								if (shareTurnsWithStraight || rightFyaActive) {
									// leave off while shared/permissive indications may be shown
								} else {
									tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
								}
								if (!shareTurnsWithStraight) {
									applyRightFyaPermissive(tl, approach);
									tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
								}
							
							if (stage == Stages.GreenCross)
							{
								tl.setActive(EnumTrafficLightBulbTypes.DontCross, false, false);
								tl.setActive(EnumTrafficLightBulbTypes.Cross, true, false);
							}
							else if (stage == Stages.GreenDontCrossWarning)
							{
								tl.setActive(EnumTrafficLightBulbTypes.DontCross, true, true);
							}
							}
							else
							{
								// In split mode, keep the non-active approach red.
								final boolean allowRightTurnOnOtherApproach = split
										&& lastRightOfWay == RightOfWays.EastWest
										&& approach == TrafficLightFacingResolver.getClockwiseApproach(splitDir);
								if (movementSettings.straightEnabled) {
									tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
									tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
									tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
								}
								if (allowRightTurnOnOtherApproach) {
									
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
							}
						});
					
					trafficLightsOpposingRightOfWay
					.stream()
					.forEach(tl -> {
						// In split mode, optionally allow a perpendicular right-turn arrow
						// (example: NS split serving southbound -> East right turn can be green).
						final EnumFacing opposingRightTurnFacing;
						if (splitDir == direction1) {
							opposingRightTurnFacing = direction1cw;
						} else if (splitDir == direction2) {
							opposingRightTurnFacing = direction2cw;
						} else {
							opposingRightTurnFacing = null;
						}

						final EnumFacing opposingApproach = TrafficLightFacingResolver.resolveApproachFacing(tl);
						final EnumFacing sharedLeftApproach;
						if (opposingApproach == direction1cw) {
							sharedLeftApproach = direction1;
						} else if (opposingApproach == direction2cw) {
							sharedLeftApproach = direction2;
						} else {
							sharedLeftApproach = null;
						}
						final boolean allowOpposingRightTurn;
						if (split && opposingRightTurnFacing != null) {
							allowOpposingRightTurn = opposingApproach == opposingRightTurnFacing
									&& allowOpposingRightWithLeft(splitDir, opposingApproach);
						} else if (!split && sharedLeftApproach != null && isSharedTurnsFor(sharedLeftApproach)) {
							allowOpposingRightTurn = allowOpposingRightWithLeft(sharedLeftApproach, opposingApproach);
						} else {
							allowOpposingRightTurn = false;
						}

						tl.powerOff();
						final ApproachMovementSettings opposingSettings = TrafficLightControlBoxTileEntity.this
								.getMovementSettings(opposingApproach);
						if (opposingSettings.straightEnabled) {
							tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
						}
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
				case HawkSolidYellow:
				{
					final boolean flash = stage == Stages.HawkFlashYellow;
					trafficLightsForRightOfWay
						.stream()
						.forEach(tl -> {
							tl.powerOff();
							tl.setActive(EnumTrafficLightBulbTypes.Yellow, true, flash);
							tl.setActive(EnumTrafficLightBulbTypes.StraightYellow, true, flash);
						});

					trafficLightsOpposingRightOfWay
						.stream()
						.forEach(tl -> {
							tl.powerOff();
							tl.setActive(EnumTrafficLightBulbTypes.DontCross, true, false);
							
						});
					break;
				}
				case HawkSolidRed:
				{
					trafficLightsForRightOfWay
						.stream()
						.forEach(tl -> {
							tl.powerOff();
							tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
						});

					trafficLightsOpposingRightOfWay
						.stream()
						.forEach(tl -> {
							tl.powerOff();
							tl.setActive(EnumTrafficLightBulbTypes.DontCross, false, false);
							tl.setActive(EnumTrafficLightBulbTypes.Cross, true, false);
						});
					break;
				}
				case HawkFlashRed:
				{
					final long wigwagTicks = Math.max(1L, (long) Config.hawkWigwagPeriodTicks);
					final boolean alt = ((world.getTotalWorldTime() / wigwagTicks) % 2) == 0;
					trafficLightsForRightOfWay
						.stream()
						.forEach(tl -> {
							tl.powerOff();
							tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
							tl.setActive(alt ? EnumTrafficLightBulbTypes.Red : EnumTrafficLightBulbTypes.Red2, true, false);
						});

					trafficLightsOpposingRightOfWay
						.stream()
						.forEach(tl -> {
							tl.powerOff();
						// Keep DON'T WALK on and flashing during the road's flashing-red interval.
							tl.setActive(EnumTrafficLightBulbTypes.DontCross, true, true);
							
						});
					break;
				}
					
			}
			
			// If an approach is disabled (ex: hasNorth=false), it must stay red regardless of stage.
			forceDisabledApproachesRed(trafficLightsForRightOfWay);
			forceDisabledApproachesRed(trafficLightsOpposingRightOfWay);
			applyMovementConfigToLights(trafficLightsForRightOfWay);
			applyMovementConfigToLights(trafficLightsOpposingRightOfWay);

			return stage;
		}

		private void applyMovementConfigToLights(List<BaseTrafficLightTileEntity> lights) {
			for (BaseTrafficLightTileEntity tl : lights) {
				EnumFacing facing = TrafficLightFacingResolver.resolveApproachFacing(tl);
				if (!isSplitDirectionEnabled(facing) || !hasAnyMovementEnabled(facing)) {
					continue;
				}
				ApproachMovementSettings settings = TrafficLightControlBoxTileEntity.this.getMovementSettings(facing);
				ApproachMovementBulbHelper.applyOverrides(tl, settings);
			}
		}

		private boolean hasAnyMovementEnabled(EnumFacing facing) {
			return ApproachMovementPhaseHelper.hasAnyMovementEnabled(
					TrafficLightControlBoxTileEntity.this.getMovementSettings(facing));
		}

		private boolean shouldServeRightAfterLeft(EnumFacing facing, RightOfWays row) {
			double arrowMinimum = row == RightOfWays.NorthSouth ? arrowMinimumNS : arrowMinimumEW;
			return ApproachMovementPhaseHelper.shouldServeRightAfterLeft(
					TrafficLightControlBoxTileEntity.this.getMovementSettings(facing), arrowMinimum);
		}

		private FyaMode getFyaMode(EnumFacing approach, boolean left) {
			if (approach == null) {
				return FyaMode.OFF;
			}
			ApproachMovementSettings settings = TrafficLightControlBoxTileEntity.this.getMovementSettings(approach);
			return left ? settings.leftFya : settings.rightFya;
		}

		private boolean allowFyaFlashFor(EnumFacing approach, boolean left) {
			FyaMode mode = getFyaMode(approach, left);
			if (mode == FyaMode.OFF) {
				return false;
			}
			if (mode == FyaMode.ALWAYS) {
				return true;
			}
			return inNightFlash;
		}

		private boolean isFyaDayTransitionFor(EnumFacing approach, boolean left) {
			FyaMode mode = getFyaMode(approach, left);
			return mode == FyaMode.NIGHT_ONLY && !inNightFlash && fyaDayTransitionTicksRemaining > 0;
		}

		private boolean anyFyaNightOnlyConfigured() {
			for (EnumFacing facing : new EnumFacing[] { EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST }) {
				ApproachMovementSettings settings = TrafficLightControlBoxTileEntity.this.getMovementSettings(facing);
				if (settings.leftFya == FyaMode.NIGHT_ONLY || settings.rightFya == FyaMode.NIGHT_ONLY) {
					return true;
				}
			}
			return false;
		}

		private void applyLeftFyaPermissive(BaseTrafficLightTileEntity tl, EnumFacing approach) {
			if (allowFyaFlashFor(approach, true)) {
				tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft, true, true);
				tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn, true, true);
				tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft2, true, true);
				tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn2, true, true);
				tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
				tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
			} else if (isFyaDayTransitionFor(approach, true)) {
				tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft, true, false);
				tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn, true, false);
				tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
				tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
			} else {
				tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
				tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
				tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
				tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
			}
		}

		private void applyRightFyaPermissive(BaseTrafficLightTileEntity tl, EnumFacing approach) {
			if (allowFyaFlashFor(approach, false)) {
				tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight, true, true);
				tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight2, true, true);
			} else if (isFyaDayTransitionFor(approach, false)) {
				tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight, true, false);
				tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight2, true, false);
			} else {
				tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
				tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
			}
		}

		private void applyLeftFyaYellow(BaseTrafficLightTileEntity tl, EnumFacing approach, boolean flash) {
			if (allowFyaFlashFor(approach, true)) {
				tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft, true, flash);
				tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn, true, flash);
				tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft2, true, flash);
				tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn2, true, flash);
			} else if (isFyaDayTransitionFor(approach, true)) {
				tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft, true, false);
				tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn, true, false);
				tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft2, true, false);
				tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn2, true, false);
			} else {
				tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft, true, false);
				tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
				tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
				tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
			}
		}

		private boolean isSharedTurnsFor(EnumFacing approach) {
			if (approach == null) {
				return false;
			}
			return TrafficLightControlBoxTileEntity.this.getMovementSettings(approach).sharedTurns;
		}

		private boolean allowOpposingRightWithLeft(EnumFacing leftApproach, EnumFacing opposingRightApproach) {
			if (leftApproach == null || opposingRightApproach == null) {
				return false;
			}
			ApproachMovementSettings opposingSettings = TrafficLightControlBoxTileEntity.this
					.getMovementSettings(opposingRightApproach);
			return opposingSettings.rightEnabled && !opposingSettings.noOpposingRightWithLeft;
		}

		private void setOpposingRightRed(BaseTrafficLightTileEntity tl) {
			tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
			tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
			tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
		}

		private boolean isStraightMovementEnabled(EnumFacing facing) {
			return TrafficLightControlBoxTileEntity.this.getMovementSettings(facing).straightEnabled;
		}

		private boolean isLeftMovementEnabled(EnumFacing facing) {
			ApproachMovementSettings s = TrafficLightControlBoxTileEntity.this.getMovementSettings(facing);
			// Shared turns is per-approach: only this facing skips its own protected left phase.
			return s.leftEnabled && !s.sharedTurns;
		}

		private boolean isRightMovementEnabled(EnumFacing facing) {
			ApproachMovementSettings s = TrafficLightControlBoxTileEntity.this.getMovementSettings(facing);
			return s.rightEnabled && !s.sharedTurns;
		}

		private void forceDisabledApproachesRed(List<BaseTrafficLightTileEntity> lights) {
			for (BaseTrafficLightTileEntity tl : lights) {
				EnumFacing approach = TrafficLightFacingResolver.resolveApproachFacing(tl);
				boolean disabled = false;
				switch (approach) {
					case NORTH:
						disabled = !TrafficLightControlBoxTileEntity.this.hasNorth;
						break;
					case SOUTH:
						disabled = !TrafficLightControlBoxTileEntity.this.hasSouth;
						break;
					case EAST:
						disabled = !TrafficLightControlBoxTileEntity.this.hasEast;
						break;
					case WEST:
						disabled = !TrafficLightControlBoxTileEntity.this.hasWest;
						break;
					default:
						break;
				}

				if (!disabled) {
					disabled = !hasAnyMovementEnabled(approach);
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
		
		public void readNBT(NBTTagCompound nbt)
		{
			lastStage = Stages.getById(nbt.getInteger(getNbtKey("lastStage")));
			lastRightOfWay = RightOfWays.getbyIndex(nbt.getInteger(getNbtKey("lastRightOfWay")));
			isInDarkMode = nbt.getBoolean("DarkMode");
		    isFlashingEmergency = nbt.getBoolean("EmergencyFlash");
			
			readSyncData(nbt); // This may need to be changed if we send/receive data not needed to be saved
		}
		
		public void writeNBT(NBTTagCompound nbt)
		{
			nbt.setInteger(getNbtKey("lastStage"), lastStage.id);
			nbt.setInteger(getNbtKey("lastRightOfWay"), lastRightOfWay.index);
			 nbt.setBoolean("DarkMode", isInDarkMode);
			nbt.setBoolean("EmergencyFlash", isFlashingEmergency);
			nbt.setBoolean("powered", powered);
			
			setSyncData(nbt); // This may need to be changed if we send/receive data not needed to be saved
		}
		
		public void readSyncData(NBTTagCompound nbt) {
			boolean legacyFallback = !nbt.hasKey(getNbtKey("greenMinimumNS"));

			if (legacyFallback) {
				// Use legacy shared values for both NS and EW
				double gMin = nbt.getDouble(getNbtKey("greenMinimum"));
				double gMax = nbt.getDouble(getNbtKey("greenMax"));
				double aMin = nbt.getDouble(getNbtKey("arrowMinimum"));
				double aMax = nbt.getDouble(getNbtKey("arrowMax"));
				double yTime = nbt.getDouble(getNbtKey("yellowTime"));
				double rTime = nbt.getDouble(getNbtKey("redTime"));

				greenMinimumNS = greenMinimumEW = gMin;
				greenMaxNS     = greenMaxEW     = gMax;
				arrowMinimumNS = arrowMinimumEW = aMin;
				arrowMaxNS     = arrowMaxEW     = aMax;
				yellowTimeNS   = yellowTimeEW   = yTime;
				redTimeNS      = redTimeEW      = rTime;
			} else {
				// North/South timings with fallback to shared (partial legacy)
				greenMinimumNS = nbt.hasKey(getNbtKey("greenMinimumNS")) ? nbt.getDouble(getNbtKey("greenMinimumNS")) : nbt.getDouble(getNbtKey("greenMinimum"));
				greenMaxNS     = nbt.hasKey(getNbtKey("greenMaxNS"))     ? nbt.getDouble(getNbtKey("greenMaxNS"))     : nbt.getDouble(getNbtKey("greenMax"));
				arrowMinimumNS = nbt.hasKey(getNbtKey("arrowMinimumNS")) ? nbt.getDouble(getNbtKey("arrowMinimumNS")) : nbt.getDouble(getNbtKey("arrowMinimum"));
				arrowMaxNS     = nbt.hasKey(getNbtKey("arrowMaxNS"))     ? nbt.getDouble(getNbtKey("arrowMaxNS"))     : nbt.getDouble(getNbtKey("arrowMax"));
				yellowTimeNS   = nbt.hasKey(getNbtKey("yellowTimeNS"))   ? nbt.getDouble(getNbtKey("yellowTimeNS"))   : nbt.getDouble(getNbtKey("yellowTime"));
				redTimeNS      = nbt.hasKey(getNbtKey("redTimeNS"))      ? nbt.getDouble(getNbtKey("redTimeNS"))      : nbt.getDouble(getNbtKey("redTime"));

				// East/West timings with fallback to shared (partial legacy)
				greenMinimumEW = nbt.hasKey(getNbtKey("greenMinimumEW")) ? nbt.getDouble(getNbtKey("greenMinimumEW")) : nbt.getDouble(getNbtKey("greenMinimum"));
				greenMaxEW     = nbt.hasKey(getNbtKey("greenMaxEW"))     ? nbt.getDouble(getNbtKey("greenMaxEW"))     : nbt.getDouble(getNbtKey("greenMax"));
				arrowMinimumEW = nbt.hasKey(getNbtKey("arrowMinimumEW")) ? nbt.getDouble(getNbtKey("arrowMinimumEW")) : nbt.getDouble(getNbtKey("arrowMinimum"));
				arrowMaxEW     = nbt.hasKey(getNbtKey("arrowMaxEW"))     ? nbt.getDouble(getNbtKey("arrowMaxEW"))     : nbt.getDouble(getNbtKey("arrowMax"));
				yellowTimeEW   = nbt.hasKey(getNbtKey("yellowTimeEW"))   ? nbt.getDouble(getNbtKey("yellowTimeEW"))   : nbt.getDouble(getNbtKey("yellowTime"));
				redTimeEW      = nbt.hasKey(getNbtKey("redTimeEW"))      ? nbt.getDouble(getNbtKey("redTimeEW"))      : nbt.getDouble(getNbtKey("redTime"));
			}

			// Shared timings (no fallback needed)
			crossTime        = nbt.getDouble(getNbtKey("crossTime"));
			crossWarningTime = nbt.getDouble(getNbtKey("crossWarningTime"));
			rightArrowTime   = nbt.getDouble(getNbtKey("rightArrowTime"));
		}


		
		public void setSyncData(NBTTagCompound nbt)
		{
			// North/South timings
			nbt.setDouble(getNbtKey("greenMinimumNS"), greenMinimumNS);
			nbt.setDouble(getNbtKey("greenMaxNS"), greenMaxNS);
			nbt.setDouble(getNbtKey("yellowTimeNS"), yellowTimeNS);
			nbt.setDouble(getNbtKey("redTimeNS"), redTimeNS);
			nbt.setDouble(getNbtKey("arrowMinimumNS"), arrowMinimumNS);
			nbt.setDouble(getNbtKey("arrowMaxNS"), arrowMaxNS);

			// East/West timings
			nbt.setDouble(getNbtKey("greenMinimumEW"), greenMinimumEW);
			nbt.setDouble(getNbtKey("greenMaxEW"), greenMaxEW);
			nbt.setDouble(getNbtKey("yellowTimeEW"), yellowTimeEW);
			nbt.setDouble(getNbtKey("redTimeEW"), redTimeEW);
			nbt.setDouble(getNbtKey("arrowMinimumEW"), arrowMinimumEW);
			nbt.setDouble(getNbtKey("arrowMaxEW"), arrowMaxEW);

			// Shared timings
			nbt.setDouble(getNbtKey("crossTime"), crossTime);
			nbt.setDouble(getNbtKey("crossWarningTime"), crossWarningTime);
			nbt.setDouble(getNbtKey("rightArrowMinimum"), rightArrowTime);
			
		}
		
		private String getNbtKey(String key)
		{
			return nbtPrefix + key;
		}
		
		private class SensorCheckResult
		{
			public boolean Direction1Sensor;
			public boolean Direction2Sensor;
			public boolean Direction1SensorLeft;
			public boolean Direction2SensorLeft;
			public boolean Direction1SensorRight;
			public boolean Direction2SensorRight;
		

		}

		private EnumFacing getSensorFacing(IBlockState senseState)
		{
			if (senseState.getBlock() instanceof BlockTrafficSensorLeft)
			{
				return senseState.getValue(BlockTrafficSensorLeft.FACING);
			}
			else if (senseState.getBlock() instanceof BlockTrafficSensorStraight)
			{
				return senseState.getValue(BlockTrafficSensorStraight.FACING);
			}
			else if (senseState.getBlock() instanceof BlockTrafficSensorRight)
			{
				return senseState.getValue(BlockTrafficSensorRight.FACING);
			}
			return null;
		}

		private boolean isSensorTrippedAt(BlockPos sensePos)
		{
			return world
					.getEntitiesWithinAABBExcludingEntity(null, new AxisAlignedBB(sensePos).expand(-1, Config.sensorScanHeight, 1))
					.stream()
					.anyMatch(e -> (e instanceof EntityPlayerMP) || Arrays.stream(Config.sensorClasses).anyMatch(eName -> {
						Class<?> nextClass = e.getClass();
						while (nextClass != null)
						{
							if (eName.equals(nextClass.getName())) return true;
							nextClass = nextClass.getSuperclass();
						}
						return false;
					}));
		}

		private boolean hasAnyDemandForFacing(EnumFacing facing)
		{
			if (!isSplitDirectionEnabled(facing)) {
				return false;
			}
			if (!hasAnyMovementEnabled(facing)) {
				return false;
			}

			for (BlockPos sensePos : sensors)
			{
				IBlockState senseState = world.getBlockState(sensePos);
				if (!sensorClasses.contains(senseState.getBlock().getClass()))
				{
					continue;
				}

				EnumFacing currentFacing = getSensorFacing(senseState);
				if (currentFacing == null || currentFacing != facing)
				{
					continue;
				}

				if (isSensorTrippedAt(sensePos))
				{
					return true;
				}
			}

			return false;
		}

		private boolean hasAnySplitDemandExceptActive(EnumFacing active)
		{
			for (EnumFacing candidate : splitOrder)
			{
				if (candidate == active)
				{
					continue;
				}
				if (hasAnyDemandForFacing(candidate))
				{
					return true;
				}
			}
			return false;
		}
		
		
		
		private SensorCheckResult checkSensors(RightOfWays rightOfWay)
		{
		    EnumFacing direction1 = rightOfWay == RightOfWays.NorthSouth ? EnumFacing.NORTH : EnumFacing.EAST;
		    EnumFacing direction2 = rightOfWay == RightOfWays.NorthSouth ? EnumFacing.SOUTH : EnumFacing.WEST;
		    
		    ArrayList<BlockPos> invalidSensors = new ArrayList<>();
		    SensorCheckResult result = new SensorCheckResult();
		    
		    boolean pedTripped = direction1 == EnumFacing.NORTH ? isNorthSouthPedQueued() : isWestEastPedQueued();
		    result.Direction1Sensor = pedTripped;
		    result.Direction2Sensor = pedTripped;

		  
		    
		    for (BlockPos sensePos : sensors)
		    {
		        IBlockState senseState = world.getBlockState(sensePos);

		        if (!sensorClasses.contains(senseState.getBlock().getClass()))
		        {
		            invalidSensors.add(sensePos);
		            continue;
		        }

		        EnumFacing currentFacing = null;
		        boolean isStraight = false;
		        boolean isLeft = false;
		        boolean isRight = false;

		        if (senseState.getBlock() instanceof BlockTrafficSensorLeft)
		        {
		            currentFacing = senseState.getValue(BlockTrafficSensorLeft.FACING);
		            isLeft = true;
		        }
		        else if (senseState.getBlock() instanceof BlockTrafficSensorStraight)
		        {
		            currentFacing = senseState.getValue(BlockTrafficSensorStraight.FACING);
		            isStraight = true;
		        }
		        else if (senseState.getBlock() instanceof BlockTrafficSensorRight)
		        {
		            currentFacing = senseState.getValue(BlockTrafficSensorRight.FACING);
		            isRight = true;
		        }

		        if (!currentFacing.equals(direction1) && !currentFacing.equals(direction2))
		        {
		            continue;
		        }

		        if ((isStraight && currentFacing.equals(direction1) && result.Direction1Sensor) ||
		            (isStraight && currentFacing.equals(direction2) && result.Direction2Sensor) ||
		            (isLeft && currentFacing.equals(direction1) && result.Direction1SensorLeft) ||
		            (isLeft && currentFacing.equals(direction2) && result.Direction2SensorLeft) ||
		            
		            (isRight && currentFacing.equals(direction1) && result.Direction1SensorRight) ||
		            (isRight && currentFacing.equals(direction2) && result.Direction2SensorRight))
		        {
		            continue;
		        }

		        boolean isTripped = isSensorTrippedAt(sensePos);

		        if (isTripped)
		        {
		            setSensorCheckResults(isStraight, isLeft, isRight, currentFacing.equals(direction1), result);

		          
		        }
		    }

		    // ✅ NEW — If only left is tripped and nothing else, force cycle
		   

		    for (BlockPos invalidSensor : invalidSensors)
		    {
		        sensors.remove(invalidSensor);
		    }
		    
		

		 // ✅ Place fake-straight logic **before** return!
		

		 return result;
		    
		    
		}

		
		private void setSensorCheckResults(boolean isStraight, boolean isLeft, boolean isRight, boolean isDirection1, SensorCheckResult results) {
		    if (isDirection1) {
		        if (isStraight) results.Direction1Sensor = true;
		        else if (isLeft) results.Direction1SensorLeft = true;
		        else if (isRight) results.Direction1SensorRight = true;
		    } else {
		        if (isStraight) results.Direction2Sensor = true;
		        else if (isLeft) results.Direction2SensorLeft = true;
		        else if (isRight) results.Direction2SensorRight = true;
		    }
		}

		
		private Stages getNextLogicalStage(Stages currentStage, RightOfWays currentRightOfWay, Automator.SensorCheckResult sensorResult) {
		    long ticksInStage = world.getTotalWorldTime() - this.stageStartTime;
		    final boolean splitForRow = TrafficLightControlBoxTileEntity.this.isSplitEnabledForRow(currentRightOfWay);
		    final EnumFacing rowDir1 = currentRightOfWay == RightOfWays.NorthSouth ? EnumFacing.NORTH : EnumFacing.EAST;
		    final EnumFacing rowDir2 = currentRightOfWay == RightOfWays.NorthSouth ? EnumFacing.SOUTH : EnumFacing.WEST;
		    final boolean bothApproachesEnabled = isApproachEnabled(rowDir1) && isApproachEnabled(rowDir2);
		    if (splitForRow && activeSplitDirection != rowDir1 && activeSplitDirection != rowDir2) {
		    	activeSplitDirection = rowDir1;
		    }
		    final boolean splitActiveIsDir1 = activeSplitDirection == rowDir1;

		    // Load direction-based timing values
		    double arrowMinNS = getAutomator().getArrowMinimumNS();
		    double arrowMaxNS = getAutomator().getArrowMaxNS();
		    double yellowNS = getAutomator().getYellowTimeNS();
		    double greenMinNS = getAutomator().getGreenMinimumNS();
		    double greenMaxNS = getAutomator().getGreenMaxNS();
		    double redNS = getAutomator().getRedTimeNS();

		    double arrowMinEW = getAutomator().getArrowMinimumES();
		    double arrowMaxEW = getAutomator().getArrowMaxEW();
		    double yellowEW = getAutomator().getYellowTimeEW();
		    double greenMinEW = getAutomator().getGreenMinimumEW();
		    double greenMaxEW = getAutomator().getGreenMaxEW();
		    double redEW = getAutomator().getRedTimeEW();

		    double yellowTime = currentRightOfWay == RightOfWays.NorthSouth ? yellowNS : yellowEW;
		    double redTime = currentRightOfWay == RightOfWays.NorthSouth ? redNS : redEW;
		    double greenMinimum = currentRightOfWay == RightOfWays.NorthSouth ? greenMinNS : greenMinEW;
		    double greenMax = currentRightOfWay == RightOfWays.NorthSouth ? greenMaxNS : greenMaxEW;
		    double arrowMinimum = currentRightOfWay == RightOfWays.NorthSouth ? arrowMinNS : arrowMinEW;
		    double arrowMax = currentRightOfWay == RightOfWays.NorthSouth ? arrowMaxNS : arrowMaxEW;

		    boolean sensorLeftTripped = sensorResult.Direction1SensorLeft || sensorResult.Direction2SensorLeft;
		    boolean sensorsStripped = sensorResult.Direction1Sensor || sensorResult.Direction2Sensor;
		    boolean timeExceeded = (arrowMinimum > 0) && ticksInStage >= (arrowMinimum * 20);

		    switch (currentStage) {
		    	case HawkFlashYellow:
		    		ticksInStage = 0;
		    		this.stageStartTime = world.getTotalWorldTime();
					setNextUpdate(Config.hawkSolidYellowSeconds);
		    		return Stages.HawkSolidYellow;

		    	case HawkSolidYellow:
		    		ticksInStage = 0;
		    		this.stageStartTime = world.getTotalWorldTime();
		    		setNextUpdate(getAutomator().getCrossTime());
		    		return Stages.HawkSolidRed;

		    	case HawkSolidRed:
		    		ticksInStage = 0;
		    		this.stageStartTime = world.getTotalWorldTime();
		    		setNextUpdate(1);
		    		return Stages.HawkFlashRed;

		    	case HawkFlashRed:
		    		if (ticksInStage >= (getAutomator().getCrossWarningTime() * 20))
		    		{
		    			ticksInStage = 0;
		    			this.stageStartTime = world.getTotalWorldTime();
		    			setNextUpdate(greenMinimum);
		    			return Stages.Green;
		    		}
		    		setNextUpdate(1);
		    		return Stages.HawkFlashRed;

		        case Red:
		        	dir1RightAfterLeft = false;
		        	dir2RightAfterLeft = false;
		        	if (!isApproachServicable(rowDir1) && !isApproachServicable(rowDir2)) {
		        		ticksInStage = 0;
		        		this.stageStartTime = world.getTotalWorldTime();
		        		setNextUpdate(redTime);
		        		forcedNextRightOfWay = currentRightOfWay.getNext();
		        		return Stages.Red;
		        	}
		        	if (splitForRow) {
		        		if (!isApproachServicable(activeSplitDirection)) {
		        			EnumFacing otherDir = activeSplitDirection == rowDir1 ? rowDir2 : rowDir1;
		        			if (isApproachServicable(otherDir)) {
		        				activeSplitDirection = otherDir;
		        			} else {
		        				ticksInStage = 0;
		        				this.stageStartTime = world.getTotalWorldTime();
		        				setNextUpdate(redTime);
		        				forcedNextRightOfWay = currentRightOfWay.getNext();
		        				return Stages.Red;
		        			}
		        		}
		        		// In split mode, skip protected arrow sequencing and just serve the active approach.
		        		return pedCheckedGreen(currentRightOfWay);
		        	}
		            if (((sensorResult.Direction1SensorLeft && isLeftMovementEnabled(rowDir1))
		            		&& (sensorResult.Direction2SensorLeft && isLeftMovementEnabled(rowDir2)))
		            		|| (bothApproachesEnabled && arrowMinimum != 0
		            				&& isLeftMovementEnabled(rowDir1) && isLeftMovementEnabled(rowDir2))) {
		                ticksInStage = 0;
		                this.stageStartTime = world.getTotalWorldTime();
		                setNextUpdate(arrowMinimum);
		                return Stages.BothTurnArrow;
		            } else if (sensorResult.Direction1SensorLeft && isLeftMovementEnabled(rowDir1)) {
		                ticksInStage = 0;
		                this.stageStartTime = world.getTotalWorldTime();
		                setNextUpdate(arrowMinNS);
		                return Stages.Direction1LeftTurnArrow;
		            } else if (sensorResult.Direction2SensorLeft && isLeftMovementEnabled(rowDir2)) {
		                ticksInStage = 0;
		                this.stageStartTime = world.getTotalWorldTime();
		                setNextUpdate(arrowMinEW);
		                return Stages.Direction2LeftTurnArrow;
		            } else if (arrowMinimum != 0 && isLeftMovementEnabled(rowDir1)) {
		                ticksInStage = 0;
		                this.stageStartTime = world.getTotalWorldTime();
		                setNextUpdate(arrowMinNS);
		                return Stages.Direction1LeftTurnArrow;
		            } else if (arrowMinimum != 0 && isLeftMovementEnabled(rowDir2)) {
		                ticksInStage = 0;
		                this.stageStartTime = world.getTotalWorldTime();
		                setNextUpdate(arrowMinEW);
		                return Stages.Direction2LeftTurnArrow;
		            } else if (sensorResult.Direction1SensorRight && isRightMovementEnabled(rowDir1)
		            		&& !ApproachMovementPhaseHelper.isRightOnly(TrafficLightControlBoxTileEntity.this.getMovementSettings(rowDir1))) {
		                this.stageStartTime = world.getTotalWorldTime();
		                ticksInStage = 0;
		                setNextUpdate(getAutomator().getRightArrowTime());
		                return Stages.Direction1RightTurnArrow;
		            } else if (sensorResult.Direction2SensorRight && isRightMovementEnabled(rowDir2)
		            		&& !ApproachMovementPhaseHelper.isRightOnly(TrafficLightControlBoxTileEntity.this.getMovementSettings(rowDir2))) {
		                this.stageStartTime = world.getTotalWorldTime();
		                ticksInStage = 0;
		                setNextUpdate(getAutomator().getRightArrowTime());
		                return Stages.Direction2RightTurnArrow;
		            }
		            return pedCheckedGreen(currentRightOfWay);

		        case Direction1RightTurnArrow:
		        	if (ApproachMovementPhaseHelper.isRightOnly(TrafficLightControlBoxTileEntity.this.getMovementSettings(rowDir1))) {
		        		Stages coupled = tryCoupledLeftForRightOnly(rowDir1, currentRightOfWay, arrowMinNS, arrowMinEW);
		        		if (coupled != null) {
		        			ticksInStage = 0;
		        			this.stageStartTime = world.getTotalWorldTime();
		        			return coupled;
		        		}
		        		return pedCheckedGreen(currentRightOfWay);
		        	}
		        	if (arrowMinimum == 0 && ticksInStage >= (arrowMax * 20)) {
		        	    ticksInStage = 0;
		        	    this.stageStartTime = world.getTotalWorldTime();
		        	    setNextUpdate(yellowTime);
		        	    return Stages.Direction1LeftTurnArrowYellow;
		        	}
		        	// Normal end: minimum time met and timeExceeded is true
		        	else if (timeExceeded && arrowMinimum != 0 && ticksInStage >= (arrowMinimum * 20)) {
		        	    ticksInStage = 0;
		        	    this.stageStartTime = world.getTotalWorldTime();
		        	    setNextUpdate(yellowTime);
		        	    return Stages.Direction1LeftTurnArrowYellow;
		        	}
		            if (sensorResult.Direction2SensorRight || sensorResult.Direction2SensorLeft) {
		                return Stages.Direction1RightTurnArrowYellow;
		            } else {
		                return Stages.Direction1LeftTurnArrowYellow;
		            }

		        case Direction1RightTurnArrowYellow:
		            ticksInStage = 0;
		            this.stageStartTime = world.getTotalWorldTime();
		            if (ApproachMovementPhaseHelper.isRightOnly(TrafficLightControlBoxTileEntity.this.getMovementSettings(rowDir2))) {
		            	Stages coupled = tryCoupledLeftForRightOnly(rowDir2, currentRightOfWay, arrowMinNS, arrowMinEW);
		            	if (coupled != null) {
		            		return coupled;
		            	}
		            	return pedCheckedGreen(currentRightOfWay);
		            }
		            setNextUpdate(sensorResult.Direction2SensorRight
		            		&& !ApproachMovementPhaseHelper.isRightOnly(TrafficLightControlBoxTileEntity.this.getMovementSettings(rowDir2))
		            		? getAutomator().getRightArrowTime() : arrowMinEW);
		            return Stages.Direction2LeftTurnArrow;

		        case Direction2RightTurnArrow:
		        	if (ApproachMovementPhaseHelper.isRightOnly(TrafficLightControlBoxTileEntity.this.getMovementSettings(rowDir2))) {
		        		Stages coupled = tryCoupledLeftForRightOnly(rowDir2, currentRightOfWay, arrowMinNS, arrowMinEW);
		        		if (coupled != null) {
		        			ticksInStage = 0;
		        			this.stageStartTime = world.getTotalWorldTime();
		        			return coupled;
		        		}
		        		return pedCheckedGreen(currentRightOfWay);
		        	}
		        	if (arrowMinimum == 0 && ticksInStage >= (arrowMax * 20)) {
		        	    ticksInStage = 0;
		        	    this.stageStartTime = world.getTotalWorldTime();
		        	    setNextUpdate(yellowTime);
		        	    return Stages.BothTurnArrowYellow;
		        	}
		        	// Normal end: minimum time met and timeExceeded is true
		        	else if (timeExceeded && arrowMinimum != 0 && ticksInStage >= (arrowMinimum * 20)) {
		        	    ticksInStage = 0;
		        	    this.stageStartTime = world.getTotalWorldTime();
		        	    setNextUpdate(yellowTime);
		        	    return Stages.BothTurnArrowYellow;
		        	}
		            return sensorResult.Direction1SensorLeft || sensorResult.Direction1SensorRight
		                ? Stages.Direction2RightTurnArrowYellow
		                : Stages.Direction2LeftTurnArrowYellow;

		        case Direction2RightTurnArrowYellow:
		            ticksInStage = 0;
		            this.stageStartTime = world.getTotalWorldTime();
		            if (ApproachMovementPhaseHelper.isRightOnly(TrafficLightControlBoxTileEntity.this.getMovementSettings(rowDir1))) {
		            	Stages coupled = tryCoupledLeftForRightOnly(rowDir1, currentRightOfWay, arrowMinNS, arrowMinEW);
		            	if (coupled != null) {
		            		return coupled;
		            	}
		            	return pedCheckedGreen(currentRightOfWay);
		            }
		            setNextUpdate(sensorResult.Direction1SensorRight
		            		&& !ApproachMovementPhaseHelper.isRightOnly(TrafficLightControlBoxTileEntity.this.getMovementSettings(rowDir1))
		            		? getAutomator().getRightArrowTime() : arrowMinNS);
		            return Stages.Direction1LeftTurnArrow;

		        case BothTurnArrow:
		        	if (arrowMinimum == 0 && ticksInStage >= (arrowMax * 20)) {
		        	    ticksInStage = 0;
		        	    this.stageStartTime = world.getTotalWorldTime();
		        	    setNextUpdate(yellowTime);
		        	    return Stages.BothTurnArrowYellow;
		        	}
		        	// Normal end: minimum time met and timeExceeded is true
		        	else if (timeExceeded && arrowMinimum != 0 && ticksInStage >= (arrowMinimum * 20)) {
		        	    ticksInStage = 0;
		        	    this.stageStartTime = world.getTotalWorldTime();
		        	    setNextUpdate(yellowTime);
		        	    return Stages.BothTurnArrowYellow;
		        	}
		            return Stages.BothTurnArrow;


		        case BothTurnArrowYellow:
		            ticksInStage = 0;
		            this.stageStartTime = world.getTotalWorldTime();
		            return pedCheckedGreen(currentRightOfWay);

		        case Direction1LeftTurnArrow:
		        	if (arrowMinimum == 0 && ticksInStage >= (arrowMax * 20)) {
		        	    ticksInStage = 0;
		        	    this.stageStartTime = world.getTotalWorldTime();
		        	    setNextUpdate(yellowTime);
		        	    dir1RightAfterLeft = shouldServeRightAfterLeft(rowDir1, currentRightOfWay);
		        	    return Stages.Direction1LeftTurnArrowYellow;
		        	}
		        	// Normal end: minimum time met and timeExceeded is true
		        	else if (timeExceeded && arrowMinimum != 0 && ticksInStage >= (arrowMinimum * 20)) {
		        	    ticksInStage = 0;
		        	    this.stageStartTime = world.getTotalWorldTime();
		        	    setNextUpdate(yellowTime);
		        	    dir1RightAfterLeft = shouldServeRightAfterLeft(rowDir1, currentRightOfWay);
		        	    return Stages.Direction1LeftTurnArrowYellow;
		        	}
		            return Stages.Direction1LeftTurnArrow;


		        case Direction1LeftTurnArrowYellow:
		            ticksInStage = 0;
		            this.stageStartTime = world.getTotalWorldTime();
		            if (dir1RightAfterLeft
		            		&& !ApproachMovementPhaseHelper.isRightOnly(TrafficLightControlBoxTileEntity.this.getMovementSettings(rowDir1))) {
		            	dir1RightAfterLeft = false;
		            	setNextUpdate(getAutomator().getRightArrowTime());
		            	return Stages.Direction1RightTurnArrow;
		            }
		            return pedCheckedGreen(currentRightOfWay);

		        case Direction2LeftTurnArrow:
		        	if (arrowMinimum == 0 && ticksInStage >= (arrowMax * 20)) {
		        	    ticksInStage = 0;
		        	    this.stageStartTime = world.getTotalWorldTime();
		        	    setNextUpdate(yellowTime);
		        	    dir2RightAfterLeft = shouldServeRightAfterLeft(rowDir2, currentRightOfWay);
		        	    return Stages.Direction2LeftTurnArrowYellow;
		        	}
		        	// Normal end: minimum time met and timeExceeded is true
		        	else if (timeExceeded && arrowMinimum != 0 && ticksInStage >= (arrowMinimum * 20)) {
		        	    ticksInStage = 0;
		        	    this.stageStartTime = world.getTotalWorldTime();
		        	    setNextUpdate(yellowTime);
		        	    return Stages.BothTurnArrowYellow;
		        	}
		            return Stages.Direction2LeftTurnArrow;


		        case Direction2LeftTurnArrowYellow:
		            ticksInStage = 0;
		            this.stageStartTime = world.getTotalWorldTime();
		            if (dir2RightAfterLeft
		            		&& !ApproachMovementPhaseHelper.isRightOnly(TrafficLightControlBoxTileEntity.this.getMovementSettings(rowDir2))) {
		            	dir2RightAfterLeft = false;
		            	setNextUpdate(getAutomator().getRightArrowTime());
		            	return Stages.Direction2RightTurnArrow;
		            }
		            return pedCheckedGreen(currentRightOfWay);

		        case Green:
		        	if (TrafficLightControlBoxTileEntity.this.isHawkBeaconEnabled()) {
		        		final RightOfWays roadRightOfWay = northMain ? RightOfWays.NorthSouth : RightOfWays.EastWest;
		        		final boolean hawkPedQueued = (roadRightOfWay == RightOfWays.NorthSouth) ? isWestEastPedQueued() : isNorthSouthPedQueued();
		        		// If greenMinimum is 0, this behaves like a fixed-timer cycle: only change at greenMax.
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

		        			ticksInStage = 0;
		        			this.stageStartTime = world.getTotalWorldTime();
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
		            	final EnumFacing otherDir = splitActiveIsDir1 ? rowDir2 : rowDir1;
		            	final boolean otherStraight = splitActiveIsDir1 ? sensorResult.Direction2Sensor : sensorResult.Direction1Sensor;
		            	final boolean otherLeft = splitActiveIsDir1 ? sensorResult.Direction2SensorLeft : sensorResult.Direction1SensorLeft;
		            	final boolean otherRight = splitActiveIsDir1 ? sensorResult.Direction2SensorRight : sensorResult.Direction1SensorRight;
		            	final boolean otherDemand = otherStraight || otherLeft || otherRight;
		            	// Vehicle-only demand (excludes pedestrian queue, which checkSensors maps into Direction1/2 straight).
		            	final boolean activeVehicleDemand = hasAnyDemandForFacing(activeSplitDirection);
		            	final boolean otherVehicleDemand = hasAnyDemandForFacing(otherDir);
		            	final RightOfWays nextRow = currentRightOfWay.getNext();

		            	final boolean timerMode = greenMinimum > 0;
		            	final boolean minMet = timerMode && ticksInStage >= (greenMinimum * 20);
		            	final boolean maxMet = ticksInStage >= (greenMax * 20);
		            	// Timer mode (min > 0): min gates changes. Sensor mode (min == 0): max gates changes.
		            	final boolean canChange = timerMode ? minMet : maxMet;

		            	// Schedule an in-row swap (E<->W or N<->S) with proper Yellow->Red first.
		            	// - Sensor mode: only swap at max if the other approach has demand.
		            	// - Timer mode: swap after min regardless of demand.
		            	if (!swappedWithinCurrentRow && canChange && isApproachServicable(otherDir)) {
		            		final boolean shouldSwap = timerMode || otherVehicleDemand;
		            		if (shouldSwap) {
		            			swappedWithinCurrentRow = true;
		            			pendingSplitSwapRow = currentRightOfWay;
		            			pendingSplitSwapDirection = otherDir;
		            			if (!timerMode) {
		            				ModRealisticTrafficControl.logger.info(
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
		            			this.stageStartTime = world.getTotalWorldTime();
		            			forcedNextRightOfWay = currentRightOfWay;
		            			setNextUpdate(yellowTime);
		            			return Stages.Yellow;
		            		}
		            	}

		            	// If we've already swapped within this row, we are currently serving the SECOND approach.
		            	// Only yield back to the other axis when this approach is finished per min/max rules.
		            	if (swappedWithinCurrentRow) {
		            		if (timerMode) {
		            			if ((minMet && !activeVehicleDemand) || maxMet) {
		            				this.stageStartTime = world.getTotalWorldTime();
		            				forcedNextRightOfWay = nextRow;
		            				setNextUpdate(yellowTime);
		            				return Stages.Yellow;
		            			}
		            			return Stages.Green;
		            		}
		            		// Sensor mode
			            		if ((!activeVehicleDemand && maxMet) || maxMet) {
		            			this.stageStartTime = world.getTotalWorldTime();
		            			forcedNextRightOfWay = nextRow;
		            			setNextUpdate(yellowTime);
		            			return Stages.Yellow;
		            		}
		            		return Stages.Green;
		            	}

		            	// First approach in this row
		            	if (timerMode) {
		            		// Swap is handled above at min; if the other approach is disabled, just yield at max.
		            		if (maxMet) {
		            			this.stageStartTime = world.getTotalWorldTime();
		            			forcedNextRightOfWay = nextRow;
		            			setNextUpdate(yellowTime);
		            			return Stages.Yellow;
		            		}
		            		return Stages.Green;
		            	}

		            	// Sensor mode: yield when no demand at max, or at max.
		            	if ((!activeVehicleDemand && maxMet) || maxMet) {
		            		this.stageStartTime = world.getTotalWorldTime();
		            		forcedNextRightOfWay = nextRow;
		            		setNextUpdate(yellowTime);
		            		return Stages.Yellow;
		            	}

		            	return Stages.Green;
		            }

		            Automator.SensorCheckResult crossSensorCheck = checkSensors(currentRightOfWay.getNext());

		            timeExceeded = (greenMinimum > 0) && ticksInStage >= (greenMinimum * 20);
		            boolean maxTimeExceeded = ticksInStage >= (greenMax * 20);

		         // (1) If on your own sensor, switch only after max time
		           

		            // (2) If not on your own sensor and greenMinimum > 0 and time exceeded
		            if (!sensorResult.Direction1Sensor && !sensorResult.Direction2Sensor && greenMinimum > 0 && timeExceeded) {
		               
		                this.stageStartTime = world.getTotalWorldTime();
		                ticksInStage = 0;
		                setNextUpdate(yellowTime);
		                return Stages.Yellow;
		            }
		            
		            if (greenMinimum == 0 && maxTimeExceeded &&
		            	    (sensorResult.Direction1SensorLeft || sensorResult.Direction2SensorLeft)) {
		            	   
		            	    this.stageStartTime = world.getTotalWorldTime();
		            	    ticksInStage = 0;
		            	    setNextUpdate(yellowTime);
		            	    return Stages.Yellow;
		            	}

		         // (3) If greenMinimum == 0, only switch after max time AND no active sensors in your direction
		            if (greenMinimum == 0 && maxTimeExceeded &&
		                !sensorResult.Direction1Sensor && !sensorResult.Direction2Sensor &&
		                !sensorResult.Direction1SensorLeft && !sensorResult.Direction2SensorLeft &&
		                !sensorResult.Direction1SensorRight && !sensorResult.Direction2SensorRight &&
		                (crossSensorCheck.Direction1Sensor || crossSensorCheck.Direction2Sensor ||
		                 crossSensorCheck.Direction1SensorLeft || crossSensorCheck.Direction2SensorLeft ||
		                 crossSensorCheck.Direction1SensorRight || crossSensorCheck.Direction2SensorRight)) {

		                this.stageStartTime = world.getTotalWorldTime();
		                ticksInStage = 0;
		                setNextUpdate(yellowTime);
		                return Stages.Yellow;
		            }

		            return Stages.Green;

		        case Yellow:
		            ticksInStage = 0;
		            this.stageStartTime = world.getTotalWorldTime();
		            setNextUpdate(redTime);
		            return Stages.Red;

		        case GreenCross:
		            ticksInStage = 0;
		            this.stageStartTime = world.getTotalWorldTime();
		            setNextUpdate(getAutomator().getCrossWarningTime());
		            return Stages.GreenDontCrossWarning;

		        case GreenDontCrossWarning:
		            // Let this drop into Green case logic if needed
		            return Stages.Green;
		    }

		    return null;
		}

		
		private Stages pedCheckedGreen(RightOfWays rightOfWay) {
		    double crossTime = (rightOfWay == RightOfWays.NorthSouth)
		        ? getAutomator().getCrossTime()
		        : getAutomator().getCrossTime();

		    double greenMinimum = (rightOfWay == RightOfWays.NorthSouth)
		        ? getAutomator().getGreenMinimumNS()
		        : getAutomator().getGreenMinimumEW();

		    if (TrafficLightControlBoxTileEntity.this.isHawkBeaconEnabled()) {
		    	setNextUpdate(greenMinimum);
		    	return Stages.Green;
		    }

		    if ((rightOfWay == RightOfWays.NorthSouth && isNorthSouthPedQueued()) ||
		        (rightOfWay == RightOfWays.EastWest && isWestEastPedQueued())) {

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

	
		private void setNextUpdate(double secondsIntoFuture)
		{
		
			nextUpdate = MinecraftServer.getCurrentTimeMillis() + (long)(secondsIntoFuture * 1000);
		}
	}
	
	private enum RightOfWays
	{
		NorthSouth(0),
		EastWest(1);
		
		private int index;
		private RightOfWays(int index)
		{
			this.index = index;			
		}
		
		public static RightOfWays getbyIndex(int index)
		{
			for(RightOfWays rightOfWay : RightOfWays.values())
			{
				if (rightOfWay.index == index)
				{
					return rightOfWay;
				}
			}
			
			return null;
		}
		
		public RightOfWays getNext()
		{
			RightOfWays newRow = getbyIndex(index + 1);
			if (newRow == null)
			{
				newRow = getbyIndex(0);
			}
			
			return newRow;
		}
	}
	
	private enum Stages
	{
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
		
		private int id;
		private Stages(int id)
		{
			this.id = id;
		}
		
		public static Stages getById(int id)
		{
			for(Stages stage : Stages.values())
			{
				if (stage.id == id)
				{
					return stage;
				}
			}
			
			return null;
		}
	}

	
}
