package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.signs.Sign;
import com.gamearoosdevelopment.realistictrafficcontrol.util.DisplaySchedule;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class DigitalSignControllerTileEntity extends SyncableTileEntity implements ITickable {
	public static final int MAX_SIGNS = 16;
	public static final int MAX_ROTATION_SIGNS = 32;
	private final ArrayList<BlockPos> signs = new ArrayList<>();
	private final ArrayList<UUID> rotationSigns = new ArrayList<>();
	private final HashMap<UUID, Integer> rotationSignTimes = new HashMap<>();
	private final DisplaySchedule schedule = new DisplaySchedule();
	private UUID selectedSign = Sign.DEFAULT_BLANK_SIGN;
	private int rotationIndex = -1;

	public List<BlockPos> getLinkedSigns() {
		return Collections.unmodifiableList(signs);
	}

	public boolean linkSign(BlockPos pos) {
		TileEntity tile = pos == null || world == null ? null : world.getTileEntity(pos);
		if (signs.contains(pos) || signs.size() >= MAX_SIGNS || !(tile instanceof DigitalSignTileEntity)) {
			return false;
		}
		signs.add(pos);
		applyTo((DigitalSignTileEntity) tile);
		markDirtyAndNotify();
		return true;
	}

	public boolean unlinkSign(BlockPos pos) {
		boolean removed = signs.remove(pos);
		if (removed) {
			markDirtyAndNotify();
		}
		return removed;
	}

	public List<UUID> getRotationSigns() {
		return Collections.unmodifiableList(rotationSigns);
	}

	public Map<UUID, Integer> getRotationSignTimes() {
		return Collections.unmodifiableMap(rotationSignTimes);
	}

	public boolean addRotationSign(UUID id) {
		if (id == null || rotationSigns.size() >= MAX_ROTATION_SIGNS) return false;
		int existing = rotationSigns.indexOf(id);
		if (existing >= 0) {
			rotationIndex = existing;
			setSelectedSign(id);
			markDirtyAndNotify();
			return false;
		}
		rotationSigns.add(id);
		rotationIndex = rotationSigns.size() - 1;
		setSelectedSign(id);
		markDirtyAndNotify();
		return true;
	}

	public int getRotationIndex() {
		return rotationIndex;
	}

	public boolean selectRotationSign(int index) {
		if (index < 0 || index >= rotationSigns.size()) return false;
		rotationIndex = index;
		setSelectedSign(rotationSigns.get(index));
		return true;
	}

	public boolean updateRotationSign(int index, UUID id) {
		if (id == null || index < 0 || index >= rotationSigns.size()) return false;
		int existing = rotationSigns.indexOf(id);
		if (existing >= 0 && existing != index) return false;
		rotationSigns.set(index, id);
		rotationIndex = index;
		setSelectedSign(id);
		markDirtyAndNotify();
		return true;
	}

	public boolean removeRotationSign(int index) {
		if (index < 0 || index >= rotationSigns.size()) return false;
		UUID removed = rotationSigns.remove(index);
		rotationSignTimes.remove(removed);
		if (rotationSigns.isEmpty()) {
			rotationIndex = -1;
			selectedSign = Sign.DEFAULT_BLANK_SIGN;
		} else {
			rotationIndex = Math.min(index, rotationSigns.size() - 1);
			setSelectedSign(rotationSigns.get(rotationIndex));
		}
		markDirtyAndNotify();
		return true;
	}

	public boolean setRotationSignTime(UUID id, String timeText) {
		if (id == null || !rotationSigns.contains(id)) return false;
		if (timeText == null || timeText.trim().isEmpty()) {
			rotationSignTimes.remove(id);
			markDirtyAndNotify();
			return true;
		}
		int time = DisplaySchedule.parseGameTime(timeText);
		if (time < 0) return false;
		rotationSignTimes.put(id, time);
		markDirtyAndNotify();
		return true;
	}

	public String getRotationSignTimeText(UUID id) {
		Integer time = id == null ? null : rotationSignTimes.get(id);
		return time == null ? "" : DisplaySchedule.formatGameTime(time);
	}

	public void clearRotationSigns() {
		rotationSigns.clear();
		rotationSignTimes.clear();
		rotationIndex = -1;
		markDirtyAndNotify();
	}

	public DisplaySchedule.Mode getScheduleMode() {
		return schedule.getMode();
	}

	public void setScheduleMode(DisplaySchedule.Mode mode) {
		schedule.setMode(mode);
		markDirtyAndNotify();
	}

	public int getScheduleIntervalAmount() {
		return schedule.getIntervalAmount();
	}

	public void setScheduleIntervalAmount(int amount) {
		schedule.setIntervalAmount(amount);
		markDirtyAndNotify();
	}

	public String getScheduleTimesText() {
		return schedule.getGameTimesText();
	}

	public void setScheduleTimes(String times) {
		schedule.setGameTimesFromText(times);
		markDirtyAndNotify();
	}

	public int setSelectedSign(UUID id) {
		selectedSign = id == null ? Sign.DEFAULT_BLANK_SIGN : id;
		int selectedIndex = rotationSigns.indexOf(selectedSign);
		if (selectedIndex >= 0) rotationIndex = selectedIndex;
		int updated = 0;
		for (BlockPos pos : new ArrayList<>(signs)) {
			TileEntity tile = world.getTileEntity(pos);
			if (!(tile instanceof DigitalSignTileEntity)) {
				signs.remove(pos);
				continue;
			}
			applyTo((DigitalSignTileEntity) tile);
			updated++;
		}
		markDirtyAndNotify();
		return updated;
	}

	public UUID getSelectedSign() {
		return selectedSign;
	}

	private void applyTo(DigitalSignTileEntity sign) {
		sign.setTypeLegacy(-1);
		sign.setVariantLegacy(-1);
		sign.setID(selectedSign);
		sign.markDirty();
		IBlockState state = world.getBlockState(sign.getPos());
		world.notifyBlockUpdate(sign.getPos(), state, state, 3);
	}

	@Override
	public void update() {
		if (world == null || world.isRemote) return;
		if (schedule.getMode() == DisplaySchedule.Mode.GAME_TIMES) {
			selectSignForCurrentGameTime();
		} else if (schedule.getMode().isInterval() && schedule.update(world) && rotationSigns.size() > 1) {
			rotationIndex = (rotationIndex + 1 + rotationSigns.size()) % rotationSigns.size();
			setSelectedSign(rotationSigns.get(rotationIndex));
		}
	}

	private void selectSignForCurrentGameTime() {
		if (rotationSigns.isEmpty() || rotationSignTimes.isEmpty()) return;
		int current = DisplaySchedule.normalizeDayTime(world.getWorldTime());
		int bestIndex = -1;
		int bestElapsed = Integer.MAX_VALUE;
		for (int i = 0; i < rotationSigns.size(); i++) {
			Integer configured = rotationSignTimes.get(rotationSigns.get(i));
			if (configured == null) continue;
			int elapsed = (current - configured + 24000) % 24000;
			if (elapsed < bestElapsed) {
				bestElapsed = elapsed;
				bestIndex = i;
			}
		}
		if (bestIndex >= 0 && (rotationIndex != bestIndex
				|| !rotationSigns.get(bestIndex).equals(selectedSign))) {
			rotationIndex = bestIndex;
			setSelectedSign(rotationSigns.get(bestIndex));
		}
	}

	private void markDirtyAndNotify() {
		markDirty();
		if (world != null) {
			IBlockState state = world.getBlockState(getPos());
			world.notifyBlockUpdate(getPos(), state, state, 3);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		signs.clear();
		for (int i = 0; i < MAX_SIGNS; i++) {
			if (compound.hasKey("digitalSign" + i)) {
				signs.add(BlockPos.fromLong(compound.getLong("digitalSign" + i)));
			}
		}
		if (compound.hasKey("selectedSign")) {
			selectedSign = NBTUtil.getUUIDFromTag(compound.getCompoundTag("selectedSign"));
		} else {
			selectedSign = Sign.DEFAULT_BLANK_SIGN;
		}
		rotationSigns.clear();
		rotationSignTimes.clear();
		boolean usesPerSignTimes = compound.hasKey("rotationSignTimesVersion");
		int rotationCount = Math.min(MAX_ROTATION_SIGNS, compound.getInteger("rotationSignCount"));
		for (int i = 0; i < rotationCount; i++) {
			if (compound.hasKey("rotationSign" + i)) {
				UUID id = NBTUtil.getUUIDFromTag(compound.getCompoundTag("rotationSign" + i));
				rotationSigns.add(id);
				if (compound.hasKey("rotationSignTime" + i)) {
					rotationSignTimes.put(id, DisplaySchedule.normalizeDayTime(compound.getInteger("rotationSignTime" + i)));
				}
			}
		}
		rotationIndex = compound.getInteger("rotationIndex");
		if (rotationIndex < 0 || rotationIndex >= rotationSigns.size()) rotationIndex = rotationSigns.isEmpty() ? -1 : 0;
		schedule.readFromNBT(compound, "schedule");
		// Migrate the old global time list once by pairing its entries with the
		// signs that existed in that save. New saves always store time by UUID.
		if (!usesPerSignTimes) {
			List<Integer> oldTimes = schedule.getGameTimes();
			for (int i = 0; i < Math.min(rotationSigns.size(), oldTimes.size()); i++) {
				rotationSignTimes.put(rotationSigns.get(i), oldTimes.get(i));
			}
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		for (int i = 0; i < signs.size(); i++) {
			compound.setLong("digitalSign" + i, signs.get(i).toLong());
		}
		if (selectedSign != null) {
			compound.setTag("selectedSign", NBTUtil.createUUIDTag(selectedSign));
		}
		compound.setInteger("rotationSignCount", rotationSigns.size());
		compound.setInteger("rotationSignTimesVersion", 1);
		for (int i = 0; i < rotationSigns.size(); i++) {
			UUID id = rotationSigns.get(i);
			compound.setTag("rotationSign" + i, NBTUtil.createUUIDTag(id));
			Integer time = rotationSignTimes.get(id);
			if (time != null) compound.setInteger("rotationSignTime" + i, time);
		}
		compound.setInteger("rotationIndex", rotationIndex);
		schedule.writeToNBT(compound, "schedule");
		return super.writeToNBT(compound);
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return writeToNBT(super.getUpdateTag());
	}

	@Override
	public void handleUpdateTag(NBTTagCompound tag) {
		super.handleUpdateTag(tag);
		readFromNBT(tag);
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(getPos(), 0, getUpdateTag());
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
		super.onDataPacket(net, packet);
		handleUpdateTag(packet.getNbtCompound());
	}

	@Override
	public NBTTagCompound getClientToServerUpdateTag() {
		return getUpdateTag();
	}

	@Override
	public void handleClientToServerUpdateTag(NBTTagCompound tag) {
		handleUpdateTag(tag);
		setSelectedSign(selectedSign);
		markDirtyAndNotify();
	}

	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
		return newState.getBlock() != ModBlocks.digital_sign_controller;
	}
}
