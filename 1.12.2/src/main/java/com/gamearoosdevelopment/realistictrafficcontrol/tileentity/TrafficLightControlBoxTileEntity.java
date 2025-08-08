package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import java.util.ArrayList;


import dan200.computercraft.api.peripheral.IPeripheral;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.gamearoosdevelopment.realistictrafficcontrol.Config;
import com.gamearoosdevelopment.realistictrafficcontrol.CC.TrafficLightCardPeripheral;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockBaseTrafficLight;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficSensorLeft;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficSensorRight;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficSensorStraight;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;
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
import net.minecraftforge.common.capabilities.CapabilityInject;




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
	private boolean flashState = false; // toggles on/off
	private boolean previousFlashState = false;
	private boolean wasFlashOn = false; // toggle tracker
	

	
	public void setNightFlashEnabled(boolean enabled) {
	    this.nightFlashEnabled = enabled;
	    markDirty(); // Ensure the tile is saved
	}
	public boolean isNightFlashEnabled() {
	    return nightFlashEnabled;
	}


	
	public List<BlockPos> getNorthSouthLights() {
	    return northSouthLights;
	}

	public List<BlockPos> getWestEastLights() {
	    return westEastLights;
	}
	
	
	@CapabilityInject(IPeripheral.class)
	public static Capability<IPeripheral> CAPABILITY_PERIPHERAL = null;

	private TrafficLightCardPeripheral peripheral;
	
	
	

	
	
	


	@Override
	public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
	    if (capability == CAPABILITY_PERIPHERAL) {
	        return true;
	    }
	    return super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
	    if (capability == CAPABILITY_PERIPHERAL) {
	        if (peripheral == null) {
	            peripheral = new TrafficLightCardPeripheral(world, pos, this);
	        }
	        return CAPABILITY_PERIPHERAL.cast(peripheral);
	    }
	    return super.getCapability(capability, facing);
	}



	
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
		compound.setBoolean("isAutoMode", !sensors.isEmpty() || !northSouthPedButtons.isEmpty() || !westEastPedButtons.isEmpty());
		compound.setBoolean("hasNorth", hasNorth);
		compound.setBoolean("hasSouth", hasSouth);
		compound.setBoolean("hasEast", hasEast);
		compound.setBoolean("hasWest", hasWest);

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
			getAutomator().update();
		}
	}
	
	private void enterDarkMode() {
		isInDarkMode = true;
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
	public boolean isFlashOn;

	
	private void flashRedYellowForRecovery() {
	    isFlashingEmergency = true;

	    new Thread(() -> {
	        // Set flashing state once
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

	        try {
	            Thread.sleep(15000); // wait 15 seconds
	        } catch (InterruptedException ignored) {}

	        ((WorldServer) world).addScheduledTask(() -> {
	            getAutomator().reset();
	            isFlashingEmergency = false;
	        });
	    }).start();
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
		
		
		
		public void reset() {
		    lastStage = Stages.Red;
		    lastRightOfWay = RightOfWays.EastWest;
		    hasInitialized = false;
		    nextUpdate = 0;
		    stageStartTime = 0;
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
		            

		            isFlashOn = true;
		        }

		        return;
		    }

		    // Skip everything else if in alternate states
		    if (isInDarkMode || isFlashingEmergency) return;

		    if (!hasInitialized) {
		        initialize();
		    }

		    if (MinecraftServer.getCurrentTimeMillis() < nextUpdate) {
		        return;
		    }

		    if (lastStage == Stages.Red) {
		        lastRightOfWay = lastRightOfWay.getNext();
		    }

		    SensorCheckResult sensorResults = checkSensors(lastRightOfWay);
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
			
			

			//System.out.print("AUTO:" + Automator.this.stageStartTime);
			
			List<BlockPos> trafficLightPosForRightOfWay;
			List<BlockPos> trafficLightPosOpposingRightOfWay;
			List<BaseTrafficLightTileEntity> trafficLightsForRightOfWay;
			List<BaseTrafficLightTileEntity> trafficLightsOpposingRightOfWay;
			EnumFacing direction1;
			EnumFacing direction2;
			
			if (lastRightOfWay == RightOfWays.NorthSouth )
			{
				trafficLightPosForRightOfWay = northSouthLights;
				trafficLightPosOpposingRightOfWay = westEastLights;
				
				direction1 = EnumFacing.NORTH;
				direction2 = EnumFacing.SOUTH;
			}
			 else {
				trafficLightPosForRightOfWay = westEastLights;
				trafficLightPosOpposingRightOfWay = northSouthLights;
				
				direction1 = EnumFacing.EAST;
				direction2 = EnumFacing.WEST;
			}
			
			trafficLightsForRightOfWay = trafficLightPosForRightOfWay
					.stream()
					.map(p ->
					{
						TileEntity te = world.getTileEntity(p);
						if (te instanceof BaseTrafficLightTileEntity)
						{
							return (BaseTrafficLightTileEntity)te;
						}
						
						return null;
					})
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
			
			trafficLightsOpposingRightOfWay = trafficLightPosOpposingRightOfWay
					.stream()
					.map(p ->
					{
						TileEntity te = world.getTileEntity(p);
						if (te instanceof BaseTrafficLightTileEntity)
						{
							return (BaseTrafficLightTileEntity)te;
						}
						
						return null;
					})
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
			
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
							IBlockState tlBs = world.getBlockState(tl.getPos());
							if (CustomAngleCalculator.isRotationFacing(tlBs.getValue(BlockBaseTrafficLight.ROTATION), direction1.getOpposite()))
							{
								tl.powerOff();
								tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft2, true, true);
								tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn2, true, true);
								tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
								
								tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
								
								tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
								
								return;
							}
							
							if (!CustomAngleCalculator.isRotationFacing(tlBs.getValue(BlockBaseTrafficLight.ROTATION), direction1))
							{
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
					
					trafficLightsOpposingRightOfWay
					.stream()
					.forEach(tl -> {
						IBlockState tlBs = world.getBlockState(tl.getPos());
						if (!CustomAngleCalculator.isRotationFacing(tlBs.getValue(BlockBaseTrafficLight.ROTATION), direction1cw))
						{
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
					trafficLightsForRightOfWay
					.stream()
					.forEach(tl -> {
						IBlockState tlBs = world.getBlockState(tl.getPos());
						if (CustomAngleCalculator.isRotationFacing(tlBs.getValue(BlockBaseTrafficLight.ROTATION), direction2.getOpposite()))
						{
							tl.powerOff();
							tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft2, true, true);
							tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn2, true, true);
							tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
							
							tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
							
							return;
						}
						
						if (!CustomAngleCalculator.isRotationFacing(tlBs.getValue(BlockBaseTrafficLight.ROTATION), direction2))
						{
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
					
					trafficLightsOpposingRightOfWay
					.stream()
					.forEach(tl -> {
						IBlockState tlBs = world.getBlockState(tl.getPos());						
						if (!CustomAngleCalculator.isRotationFacing(tlBs.getValue(BlockBaseTrafficLight.ROTATION), direction2cw))
						{
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
					trafficLightsForRightOfWay
					.stream()
					.forEach(tl ->
					{
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
					trafficLightsForRightOfWay
						.stream()
						.forEach(tl -> {
							IBlockState tlBs = world.getBlockState(tl.getPos());
							if (CustomAngleCalculator.isRotationFacing(tlBs.getValue(BlockBaseTrafficLight.ROTATION), direction1.getOpposite()))
							{
								
								tl.powerOff();
								tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft2, true, true);
								tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
								tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
							}
							
							
							
							
							if (!CustomAngleCalculator.isRotationFacing(tlBs.getValue(BlockBaseTrafficLight.ROTATION), direction1))
							{
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
						
						if (CustomAngleCalculator.isRotationFacing(tlBs.getValue(BlockBaseTrafficLight.ROTATION), direction1cw.getOpposite()))
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
						
						if (!CustomAngleCalculator.isRotationFacing(tlBs.getValue(BlockBaseTrafficLight.ROTATION), direction1cw))
						{
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
					trafficLightsForRightOfWay
					.stream()
					.forEach(tl -> {
						IBlockState tlBs = world.getBlockState(tl.getPos());
						if (CustomAngleCalculator.isRotationFacing(tlBs.getValue(BlockBaseTrafficLight.ROTATION), direction2.getOpposite()))
						{
							
							tl.powerOff();
							tl.setActive(EnumTrafficLightBulbTypes.Red, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.Red2, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.StraightRed, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft2, true, true);
							tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
							tl.setActive(EnumTrafficLightBulbTypes.NoLeftTurn, true, false);
						}
						if (!CustomAngleCalculator.isRotationFacing(tlBs.getValue(BlockBaseTrafficLight.ROTATION), direction2))
						{
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
						if (CustomAngleCalculator.isRotationFacing(tlBs.getValue(BlockBaseTrafficLight.ROTATION), direction2cw.getOpposite()))
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
						
						
						if (!CustomAngleCalculator.isRotationFacing(tlBs.getValue(BlockBaseTrafficLight.ROTATION), direction2cw))
						{
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
						tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
						
					});
					
					trafficLightsOpposingRightOfWay
					.stream()
					.forEach(tl -> {
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
					trafficLightsForRightOfWay
					.stream()
					.forEach(tl -> {
						IBlockState tlBs = world.getBlockState(tl.getPos());
						if (!CustomAngleCalculator.isRotationFacing(tlBs.getValue(BlockBaseTrafficLight.ROTATION), direction1))
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
						if (!CustomAngleCalculator.isRotationFacing(tlBs.getValue(BlockBaseTrafficLight.ROTATION), direction1cw))
						{
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
						if (!CustomAngleCalculator.isRotationFacing(tlBs.getValue(BlockBaseTrafficLight.ROTATION), direction2))
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
						if (!CustomAngleCalculator.isRotationFacing(tlBs.getValue(BlockBaseTrafficLight.ROTATION), direction2cw))
						{
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
					trafficLightsForRightOfWay
					.stream()
					.forEach(tl ->
					{
						tl.powerOff();
						tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
						
						tl.setActive(EnumTrafficLightBulbTypes.Yellow, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.StraightYellow, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
					});
					
					trafficLightsOpposingRightOfWay
					.stream()
					.forEach(tl -> {
						tl.powerOff();
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
					});
					break;
				case Green:
				case GreenCross:
				case GreenDontCrossWarning:
					trafficLightsForRightOfWay
					.stream()
					.forEach(tl ->
					{
						tl.powerOff();
						tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft, true, true);
						tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn, true, true);
						tl.setActive(EnumTrafficLightBulbTypes.YellowArrowLeft2, true, true);
						tl.setActive(EnumTrafficLightBulbTypes.YellowArrowUTurn2, true, true);
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowLeft2, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowUTurn2, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.Green, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.GreenArrowLeft2, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.GreenArrowUTurn2, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.GreenArrowRight2, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.StraightGreen, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.RedArrowRight2, true, false);
						tl.setActive(EnumTrafficLightBulbTypes.YellowArrowRight2, true, true);
						tl.setActive(EnumTrafficLightBulbTypes.NoRightTurn, true, false);
						
						if (stage == Stages.GreenCross)
						{
							tl.setActive(EnumTrafficLightBulbTypes.DontCross, false, false);
							tl.setActive(EnumTrafficLightBulbTypes.Cross, true, false);
						}
						else if (stage == Stages.GreenDontCrossWarning)
						{
							tl.setActive(EnumTrafficLightBulbTypes.DontCross, true, true);
						}
					});
					
					trafficLightsOpposingRightOfWay
					.stream()
					.forEach(tl -> {
						tl.powerOff();
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
					});
					break;
					
			}
			
			
			
		
			
			return stage;
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

		        boolean isTripped = world
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
		        case Red:
		            if (sensorResult.Direction1SensorRight) {
		                this.stageStartTime = world.getTotalWorldTime();
		                ticksInStage = 0;
		                setNextUpdate(getAutomator().getRightArrowTime());
		                return Stages.Direction1RightTurnArrow;
		            } else if (sensorResult.Direction2SensorRight) {
		                this.stageStartTime = world.getTotalWorldTime();
		                ticksInStage = 0;
		                setNextUpdate(getAutomator().getRightArrowTime());
		                return Stages.Direction2RightTurnArrow;
		            } else if ((sensorResult.Direction1SensorLeft && sensorResult.Direction2SensorLeft) || arrowMinimum != 0) {
		                ticksInStage = 0;
		                this.stageStartTime = world.getTotalWorldTime();
		                setNextUpdate(arrowMinimum);
		                return Stages.BothTurnArrow;
		            } else if (sensorResult.Direction1SensorLeft) {
		                ticksInStage = 0;
		                this.stageStartTime = world.getTotalWorldTime();
		                setNextUpdate(arrowMinNS);
		                return Stages.Direction1LeftTurnArrow;
		            } else if (sensorResult.Direction2SensorLeft) {
		                ticksInStage = 0;
		                this.stageStartTime = world.getTotalWorldTime();
		                setNextUpdate(arrowMinEW);
		                return Stages.Direction2LeftTurnArrow;
		            }
		            return pedCheckedGreen(currentRightOfWay);

		        case Direction1RightTurnArrow:
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
		            setNextUpdate(sensorResult.Direction2SensorRight ? getAutomator().getRightArrowTime() : arrowMinEW);
		            return Stages.Direction2LeftTurnArrow;

		        case Direction2RightTurnArrow:
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
		            setNextUpdate(sensorResult.Direction2SensorRight ? getAutomator().getRightArrowTime() : arrowMinNS);
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
		        	    return Stages.Direction1LeftTurnArrowYellow;
		        	}
		        	// Normal end: minimum time met and timeExceeded is true
		        	else if (timeExceeded && arrowMinimum != 0 && ticksInStage >= (arrowMinimum * 20)) {
		        	    ticksInStage = 0;
		        	    this.stageStartTime = world.getTotalWorldTime();
		        	    setNextUpdate(yellowTime);
		        	    return Stages.Direction1LeftTurnArrowYellow;
		        	}
		            return Stages.Direction1LeftTurnArrow;


		        case Direction1LeftTurnArrowYellow:
		            ticksInStage = 0;
		            this.stageStartTime = world.getTotalWorldTime();
		            return pedCheckedGreen(currentRightOfWay);

		        case Direction2LeftTurnArrow:
		        	if (arrowMinimum == 0 && ticksInStage >= (arrowMax * 20)) {
		        	    ticksInStage = 0;
		        	    this.stageStartTime = world.getTotalWorldTime();
		        	    setNextUpdate(yellowTime);
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
		            return pedCheckedGreen(currentRightOfWay);

		        case Green:
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
		Direction2RightTurnArrowYellow(14);
		
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
