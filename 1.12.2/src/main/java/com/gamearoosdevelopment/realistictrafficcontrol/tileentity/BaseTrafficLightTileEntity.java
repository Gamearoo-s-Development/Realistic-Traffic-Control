package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import java.util.HashMap;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.util.EnumTrafficLightBulbTypes;

import net.minecraft.entity.passive.EntityPig;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;

public class BaseTrafficLightTileEntity extends TileEntity implements ITickable {

	private final int BULB_COUNT;
	private HashMap<Integer, EnumTrafficLightBulbTypes> bulbsBySlot = new HashMap<>();
	private HashMap<Integer, EnumTrafficLightBulbTypes> secondaryBulbsBySlot = new HashMap<>();
	private HashMap<Integer, EnumTrafficLightBulbTypes> activeBulbSelectionBySlot = new HashMap<>();
	private HashMap<Integer, Boolean> activeBySlot = new HashMap<>();
	private HashMap<Integer, Boolean> flashBySlot = new HashMap<>();
	private HashMap<Integer, Integer> flashTimeBySlot = new HashMap<>();
	private HashMap<Integer, Boolean> flashCurrent = new HashMap<>();
	private HashMap<Integer, Boolean> allowFlashBySlot = new HashMap<>();

	private int isPigAboveDelay;
	private boolean isPigAbove;
	private boolean hasCover;
	private boolean hasPole;
	private boolean suppressHorizontalBar;

	public BaseTrafficLightTileEntity(int bulbCount) {
		super();
		BULB_COUNT = bulbCount;
		hasCover = true;
		hasPole = false;
		suppressHorizontalBar = false;
		for (int i = 0; i < BULB_COUNT; i++) {
			activeBySlot.put(i, false);
			flashBySlot.put(i, false);
			allowFlashBySlot.put(i, true);
		}
	}

	public int getBulbCount() {
		return BULB_COUNT;
	}

	public boolean hasCover() {
		return hasCover;
	}

	public boolean hasPole() {
		return hasPole;
	}

	public void setCover(boolean hasCover) {
		this.hasCover = hasCover;
		markDirtyAndSync();
	}

	public void setPole(boolean hasPole) {
		this.hasPole = hasPole;
		markDirtyAndSync();
	}

	public boolean isHorizontalBarSuppressed() {
		return suppressHorizontalBar;
	}

	public void setHorizontalBarSuppressed(boolean suppress) {
		this.suppressHorizontalBar = suppress;
		markDirtyAndSync();
	}

	public void setBulbType(int slot, EnumTrafficLightBulbTypes newType) {
		setBulbType(slot, 0, newType);
	}

	public void setBulbType(int slot, int layer, EnumTrafficLightBulbTypes newType) {
		if (slot < 0 || slot >= BULB_COUNT) {
			return;
		}

		HashMap<Integer, EnumTrafficLightBulbTypes> target = layer == 1 ? secondaryBulbsBySlot : bulbsBySlot;
		if (newType == null) {
			target.remove(slot);
		} else {
			target.put(slot, newType);
		}

		EnumTrafficLightBulbTypes selection = activeBulbSelectionBySlot.get(slot);
		if (selection != null && !matchesSlotBulb(slot, selection)) {
			activeBulbSelectionBySlot.remove(slot);
		}

		markDirtyAndSync();
	}

	public void setBulbsBySlot(HashMap<Integer, EnumTrafficLightBulbTypes> primary,
			HashMap<Integer, EnumTrafficLightBulbTypes> secondary) {
		bulbsBySlot = new HashMap<>(primary);
		secondaryBulbsBySlot = new HashMap<>(secondary);
		activeBulbSelectionBySlot.clear();
		for (int i = 0; i < BULB_COUNT; i++) {
			activeBySlot.put(i, false);
			flashBySlot.put(i, false);
		}
		markDirtyAndSync();
	}

	public void setAllowFlashBySlot(HashMap<Integer, Boolean> allowFlashBySlot) {
		this.allowFlashBySlot = new HashMap<>();
		for (int i = 0; i < BULB_COUNT; i++) {
			boolean allow = allowFlashBySlot.containsKey(i) ? allowFlashBySlot.get(i) : true;
			this.allowFlashBySlot.put(i, allow);
		}
		markDirtyAndSync();
	}

	public boolean getAllowFlashBySlot(int slot) {
		if (allowFlashBySlot.containsKey(slot)) {
			return allowFlashBySlot.get(slot);
		}
		return true;
	}

	public void setActive(EnumTrafficLightBulbTypes bulbType, boolean active, boolean flash) {
		boolean changed = false;
		for (int slot = 0; slot < BULB_COUNT; slot++) {
			EnumTrafficLightBulbTypes primary = bulbsBySlot.get(slot);
			EnumTrafficLightBulbTypes secondary = secondaryBulbsBySlot.get(slot);
			if (primary == bulbType || secondary == bulbType) {
				activeBySlot.put(slot, active);
				flashBySlot.put(slot, flash);
				if (active) {
					activeBulbSelectionBySlot.put(slot, bulbType);
				} else if (activeBulbSelectionBySlot.get(slot) == bulbType) {
					activeBulbSelectionBySlot.remove(slot);
				}
				changed = true;
			}
		}

		if (changed) {
			markDirtyAndSync();
		}
	}

	public void powerOff() {
		for (int i = 0; i < BULB_COUNT; i++) {
			activeBySlot.put(i, false);
			flashBySlot.put(i, false);
			activeBulbSelectionBySlot.remove(i);
		}
		setActive(EnumTrafficLightBulbTypes.DontCross, true, false);
		markDirtyAndSync();
	}

	public boolean hasBulb(EnumTrafficLightBulbTypes bulbType) {
		if (bulbType == null) {
			return false;
		}
		for (int slot = 0; slot < BULB_COUNT; slot++) {
			if (matchesSlotBulb(slot, bulbType)) {
				return true;
			}
		}
		return false;
	}

	public EnumTrafficLightBulbTypes getBulbTypeBySlot(int slot) {
		return getDisplayedBulbForSlot(slot);
	}

	public EnumTrafficLightBulbTypes getBulbTypeBySlot(int slot, int layer) {
		if (layer == 0) {
			return bulbsBySlot.get(slot);
		}
		if (layer == 1) {
			return secondaryBulbsBySlot.get(slot);
		}
		return null;
	}

	public EnumTrafficLightBulbTypes[] getBulbTypesBySlot(int slot) {
		EnumTrafficLightBulbTypes[] result = new EnumTrafficLightBulbTypes[2];
		result[0] = bulbsBySlot.get(slot);
		result[1] = secondaryBulbsBySlot.get(slot);
		return result;
	}

	public EnumTrafficLightBulbTypes getDisplayedBulbForSlot(int slot) {
		EnumTrafficLightBulbTypes selected = activeBulbSelectionBySlot.get(slot);
		if (selected != null) {
			return selected;
		}
		return getFirstAvailableBulb(slot);
	}

	public boolean getActiveBySlot(int slot) {
		if (activeBySlot.containsKey(slot)) {
			return activeBySlot.get(slot);
		}
		return false;
	}

	public boolean getFlashBySlot(int slot) {
		if (flashBySlot.containsKey(slot)) {
			return flashBySlot.get(slot);
		}
		return false;
	}

	public boolean getFlashCurrentBySlot(int slot) {
		if (flashCurrent.containsKey(slot)) {
			return flashCurrent.get(slot);
		}
		return true;
	}

	public boolean anyActive() {
		for (int i = 0; i < BULB_COUNT; i++) {
			if (getActiveBySlot(i) && (!getFlashBySlot(i) || getFlashCurrentBySlot(i))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void update() {
		if (world == null || !world.isRemote) {
			return;
		}

		isPigAboveDelay++;
		if (isPigAboveDelay >= 20) {
			isPigAbove = world.getEntitiesWithinAABB(EntityPig.class,
					new AxisAlignedBB(getPos(), getPos().up(2)).expand(0.5, 0, 0.5)).size() > 0;
			isPigAboveDelay = 0;
		}

		for (int i = 0; i < BULB_COUNT; i++) {
			if (getFlashBySlot(i) && getAllowFlashBySlot(i)) {
				flashTimeBySlot.putIfAbsent(i, 0);
				flashCurrent.putIfAbsent(i, false);
				flashTimeBySlot.put(i, flashTimeBySlot.get(i) + 1);
				if (flashTimeBySlot.get(i) > 20) {
					flashCurrent.put(i, !flashCurrent.get(i));
					flashTimeBySlot.put(i, 0);
					world.checkLight(getPos());
				}
			} else if (getFlashBySlot(i) && !getAllowFlashBySlot(i)) {
				flashCurrent.put(i, false);
			}
		}
	}

	public boolean getIsPigAbove() {
		return isPigAbove;
	}

	@Override
	public double getMaxRenderDistanceSquared() {
		return ModRealisticTrafficControl.MAX_RENDER_DISTANCE;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		writeStateToTag(compound);
		return super.writeToNBT(compound);
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		readStateFromTag(compound);
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		NBTTagCompound tag = super.getUpdateTag();
		writeStateToTag(tag);
		return tag;
	}

	@Override
	public void handleUpdateTag(NBTTagCompound tag) {
		super.handleUpdateTag(tag);
		readStateFromTag(tag);
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(getPos(), 0, getUpdateTag());
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		handleUpdateTag(pkt.getNbtCompound());
		if (world != null) {
			world.markBlockRangeForRenderUpdate(getPos(), getPos());
		}
	}

	private void markDirtyAndSync() {
		markDirty();
		if (world != null) {
			world.notifyBlockUpdate(getPos(), world.getBlockState(getPos()), world.getBlockState(getPos()), 3);
			world.markBlockRangeForRenderUpdate(getPos(), getPos());
		}
	}

	private void writeStateToTag(NBTTagCompound tag) {
		int[] primaryBulbTypes = new int[BULB_COUNT];
		int[] secondaryBulbTypes = new int[BULB_COUNT];
		int[] activeSelections = new int[BULB_COUNT];
		for (int i = 0; i < BULB_COUNT; i++) {
			EnumTrafficLightBulbTypes primary = bulbsBySlot.get(i);
			EnumTrafficLightBulbTypes secondary = secondaryBulbsBySlot.get(i);
			EnumTrafficLightBulbTypes selection = activeBulbSelectionBySlot.get(i);
			primaryBulbTypes[i] = primary != null ? primary.getIndex() : -1;
			secondaryBulbTypes[i] = secondary != null ? secondary.getIndex() : -1;
			activeSelections[i] = selection != null ? selection.getIndex() : -1;
			tag.setBoolean("active" + i, getActiveBySlot(i));
			tag.setBoolean("flash" + i, getFlashBySlot(i));
			tag.setBoolean("allowflash" + i, getAllowFlashBySlot(i));
		}
		tag.setIntArray("bulbTypes", primaryBulbTypes);
		tag.setIntArray("secondaryBulbTypes", secondaryBulbTypes);
		tag.setIntArray("activeBulbSelections", activeSelections);
		tag.setBoolean("cover", hasCover());
		tag.setBoolean("pole", hasPole());
		tag.setBoolean("suppressHorizontalBar", suppressHorizontalBar);
	}

	private void readStateFromTag(NBTTagCompound tag) {
		bulbsBySlot = new HashMap<>();
		secondaryBulbsBySlot = new HashMap<>();
		activeBulbSelectionBySlot = new HashMap<>();
		activeBySlot = new HashMap<>();
		flashBySlot = new HashMap<>();
		allowFlashBySlot = new HashMap<>();

		int[] primaryBulbTypes = tag.getIntArray("bulbTypes");
		int[] secondaryBulbTypes = tag.hasKey("secondaryBulbTypes") ? tag.getIntArray("secondaryBulbTypes") : new int[0];
		int[] activeSelections = tag.hasKey("activeBulbSelections") ? tag.getIntArray("activeBulbSelections") : new int[0];
		for (int i = 0; i < BULB_COUNT; i++) {
			if (i < primaryBulbTypes.length) {
				EnumTrafficLightBulbTypes primary = EnumTrafficLightBulbTypes.get(primaryBulbTypes[i]);
				if (primary != null) {
					bulbsBySlot.put(i, primary);
				}
			}
			if (i < secondaryBulbTypes.length) {
				EnumTrafficLightBulbTypes secondary = EnumTrafficLightBulbTypes.get(secondaryBulbTypes[i]);
				if (secondary != null) {
					secondaryBulbsBySlot.put(i, secondary);
				}
			}
			if (i < activeSelections.length) {
				EnumTrafficLightBulbTypes selection = EnumTrafficLightBulbTypes.get(activeSelections[i]);
				if (selection != null) {
					activeBulbSelectionBySlot.put(i, selection);
				}
			}
			activeBySlot.put(i, tag.getBoolean("active" + i));
			flashBySlot.put(i, tag.getBoolean("flash" + i));
			allowFlashBySlot.put(i, tag.hasKey("allowflash" + i) ? tag.getBoolean("allowflash" + i) : true);
		}
		hasCover = tag.getBoolean("cover");
		hasPole = tag.getBoolean("pole");
		suppressHorizontalBar = tag.getBoolean("suppressHorizontalBar");
	}

	private boolean matchesSlotBulb(int slot, EnumTrafficLightBulbTypes type) {
		if (type == null) {
			return false;
		}
		EnumTrafficLightBulbTypes primary = bulbsBySlot.get(slot);
		EnumTrafficLightBulbTypes secondary = secondaryBulbsBySlot.get(slot);
		return type == primary || type == secondary;
	}

	private EnumTrafficLightBulbTypes getFirstAvailableBulb(int slot) {
		EnumTrafficLightBulbTypes primary = bulbsBySlot.get(slot);
		if (primary != null) {
			return primary;
		}
		return secondaryBulbsBySlot.get(slot);
	}
}
