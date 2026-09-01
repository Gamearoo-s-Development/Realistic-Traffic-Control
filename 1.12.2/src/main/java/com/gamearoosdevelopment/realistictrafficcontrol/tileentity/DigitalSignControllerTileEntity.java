package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
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

	private static class RotationPage {
		private UUID signId = Sign.DEFAULT_BLANK_SIGN;
		private Integer gameTime = null;
		private final ArrayList<String> textLines = new ArrayList<>();

		private RotationPage(UUID signId) {
			this.signId = signId == null ? Sign.DEFAULT_BLANK_SIGN : signId;
			ensureTextLineCount(this);
		}

		private RotationPage(NBTTagCompound compound) {
			if (compound.hasKey("signId")) {
				signId = NBTUtil.getUUIDFromTag(compound.getCompoundTag("signId"));
			}
			if (compound.hasKey("gameTime")) {
				gameTime = DisplaySchedule.normalizeDayTime(compound.getInteger("gameTime"));
			}
			textLines.clear();
			for (int i = 0; compound.hasKey("text" + i); i++) {
				textLines.add(compound.getString("text" + i));
			}
			ensureTextLineCount(this);
		}

		private NBTTagCompound writeToNBT() {
			NBTTagCompound compound = new NBTTagCompound();
			compound.setTag("signId", NBTUtil.createUUIDTag(signId));
			if (gameTime != null) compound.setInteger("gameTime", gameTime);
			for (int i = 0; i < textLines.size(); i++) {
				compound.setString("text" + i, textLines.get(i) == null ? "" : textLines.get(i));
			}
			return compound;
		}

		private RotationPage copy() {
			RotationPage copy = new RotationPage(signId);
			copy.gameTime = gameTime;
			copy.textLines.clear();
			for (String line : textLines) copy.textLines.add(line == null ? "" : line);
			return copy;
		}
	}

	private final ArrayList<BlockPos> signs = new ArrayList<>();
	private final ArrayList<BlockPos> syncedControllers = new ArrayList<>();
	private final ArrayList<RotationPage> rotationPages = new ArrayList<>();
	private final DisplaySchedule schedule = new DisplaySchedule();
	private UUID selectedSign = Sign.DEFAULT_BLANK_SIGN;
	private int rotationIndex = -1;
	private BlockPos syncMaster = null;
	private boolean applyingSync;

	private static void ensureTextLineCount(RotationPage page) {
		Sign sign = ModRealisticTrafficControl.instance.signRepo.getSignByID(page.signId);
		int required = sign == null ? 0 : sign.getTextLines().size();
		while (page.textLines.size() < required) page.textLines.add("");
		while (page.textLines.size() > required) page.textLines.remove(page.textLines.size() - 1);
	}

	public List<BlockPos> getLinkedSigns() {
		return Collections.unmodifiableList(signs);
	}

	public List<BlockPos> getSyncedControllers() {
		return Collections.unmodifiableList(syncedControllers);
	}

	public BlockPos getSyncMaster() {
		return syncMaster;
	}

	public boolean isSyncFollower() {
		return syncMaster != null;
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
		if (removed) markDirtyAndNotify();
		return removed;
	}

	public boolean linkSyncedController(BlockPos otherPos) {
		if (world == null || otherPos == null || otherPos.equals(getPos())) return false;
		TileEntity otherTile = world.getTileEntity(otherPos);
		if (!(otherTile instanceof DigitalSignControllerTileEntity)) return false;
		DigitalSignControllerTileEntity follower = (DigitalSignControllerTileEntity) otherTile;

		if (syncedControllers.contains(otherPos)) {
			syncedControllers.remove(otherPos);
			if (otherPos.equals(follower.syncMaster)) follower.syncMaster = null;
			follower.markDirtyAndNotify();
			markDirtyAndNotify();
			return false;
		}

		if (follower.ownsSyncOf(getPos()) || isInMasterChain(otherPos)) return false;

		syncedControllers.add(otherPos);
		follower.syncMaster = getPos();
		follower.applySyncFrom(this);
		follower.markDirtyAndNotify();
		markDirtyAndNotify();
		return true;
	}

	private boolean ownsSyncOf(BlockPos pos) {
		if (syncedControllers.contains(pos)) return true;
		for (BlockPos followerPos : new ArrayList<>(syncedControllers)) {
			TileEntity tile = world == null ? null : world.getTileEntity(followerPos);
			if (tile instanceof DigitalSignControllerTileEntity
					&& ((DigitalSignControllerTileEntity) tile).ownsSyncOf(pos)) {
				return true;
			}
		}
		return false;
	}

	private boolean isInMasterChain(BlockPos pos) {
		HashSet<BlockPos> visited = new HashSet<>();
		BlockPos current = syncMaster;
		while (current != null && visited.add(current)) {
			if (current.equals(pos)) return true;
			TileEntity tile = world == null ? null : world.getTileEntity(current);
			if (!(tile instanceof DigitalSignControllerTileEntity)) break;
			current = ((DigitalSignControllerTileEntity) tile).syncMaster;
		}
		return false;
	}

	public int getRotationPageCount() {
		return rotationPages.size();
	}

	public List<UUID> getRotationSigns() {
		ArrayList<UUID> ids = new ArrayList<>();
		for (RotationPage page : rotationPages) ids.add(page.signId);
		return Collections.unmodifiableList(ids);
	}

	public UUID getPageSignId(int index) {
		return index >= 0 && index < rotationPages.size() ? rotationPages.get(index).signId : selectedSign;
	}

	public String getPageTextLine(int pageIndex, int lineIndex) {
		if (pageIndex < 0 || pageIndex >= rotationPages.size()) return "";
		RotationPage page = rotationPages.get(pageIndex);
		return lineIndex >= 0 && lineIndex < page.textLines.size()
				? (page.textLines.get(lineIndex) == null ? "" : page.textLines.get(lineIndex)) : "";
	}

	public void setPageTextLine(int pageIndex, int lineIndex, String text) {
		if (pageIndex < 0 || pageIndex >= rotationPages.size() || lineIndex < 0) return;
		RotationPage page = rotationPages.get(pageIndex);
		ensureTextLineCount(page);
		if (lineIndex >= page.textLines.size()) return;
		page.textLines.set(lineIndex, text == null ? "" : text);
		if (pageIndex == rotationIndex) applySelectedToLinkedSigns();
		markDirtyAndNotify();
	}

	public boolean addRotationSign(UUID id) {
		if (id == null || rotationPages.size() >= MAX_ROTATION_SIGNS) return false;
		rotationPages.add(new RotationPage(id));
		rotationIndex = rotationPages.size() - 1;
		setSelectedSign(id);
		markDirtyAndNotify();
		return true;
	}

	public int getRotationIndex() {
		return rotationIndex;
	}

	public boolean selectRotationSign(int index) {
		if (index < 0 || index >= rotationPages.size()) return false;
		rotationIndex = index;
		setSelectedSign(rotationPages.get(index).signId);
		return true;
	}

	public boolean updateRotationSign(int index, UUID id) {
		if (id == null || index < 0 || index >= rotationPages.size()) return false;
		RotationPage page = rotationPages.get(index);
		if (!id.equals(page.signId)) {
			page.signId = id;
			page.textLines.clear();
			ensureTextLineCount(page);
		}
		rotationIndex = index;
		setSelectedSign(id);
		markDirtyAndNotify();
		return true;
	}

	public void saveRotationPage(int index, UUID id, List<String> textLines) {
		if (index < 0 || index >= rotationPages.size()) return;
		RotationPage page = rotationPages.get(index);
		page.signId = id == null ? Sign.DEFAULT_BLANK_SIGN : id;
		page.textLines.clear();
		if (textLines != null) {
			for (String line : textLines) page.textLines.add(line == null ? "" : line);
		}
		ensureTextLineCount(page);
		rotationIndex = index;
		selectedSign = page.signId;
		applySelectedToLinkedSigns();
		markDirtyAndNotify();
		pushSyncToFollowers();
	}

	public boolean removeRotationSign(int index) {
		if (index < 0 || index >= rotationPages.size()) return false;
		rotationPages.remove(index);
		if (rotationPages.isEmpty()) {
			rotationIndex = -1;
			selectedSign = Sign.DEFAULT_BLANK_SIGN;
			applySelectedToLinkedSigns();
		} else {
			rotationIndex = Math.min(index, rotationPages.size() - 1);
			setSelectedSign(rotationPages.get(rotationIndex).signId);
		}
		markDirtyAndNotify();
		pushSyncToFollowers();
		return true;
	}

	public boolean setRotationSignTime(UUID id, String timeText) {
		if (isSyncFollower()) return false;
		int index = findPageIndexForSign(id);
		if (index < 0) return false;
		RotationPage page = rotationPages.get(index);
		if (timeText == null || timeText.trim().isEmpty()) {
			page.gameTime = null;
		} else {
			int time = DisplaySchedule.parseGameTime(timeText);
			if (time < 0) return false;
			page.gameTime = time;
		}
		markDirtyAndNotify();
		pushSyncToFollowers();
		return true;
	}

	public String getRotationSignTimeText(UUID id) {
		int index = findPageIndexForSign(id);
		return getRotationPageTimeText(index);
	}

	public String getRotationPageTimeText(int index) {
		if (index < 0 || index >= rotationPages.size()) return "";
		Integer time = rotationPages.get(index).gameTime;
		return time == null ? "" : DisplaySchedule.formatGameTime(time);
	}

	public boolean setRotationPageTime(int index, String timeText) {
		if (isSyncFollower()) return false;
		if (index < 0 || index >= rotationPages.size()) return false;
		RotationPage page = rotationPages.get(index);
		if (timeText == null || timeText.trim().isEmpty()) {
			page.gameTime = null;
		} else {
			int time = DisplaySchedule.parseGameTime(timeText);
			if (time < 0) return false;
			page.gameTime = time;
		}
		markDirtyAndNotify();
		pushSyncToFollowers();
		return true;
	}

	private int findPageIndexForSign(UUID id) {
		if (id == null) return -1;
		if (rotationIndex >= 0 && rotationIndex < rotationPages.size()
				&& id.equals(rotationPages.get(rotationIndex).signId)) {
			return rotationIndex;
		}
		for (int i = 0; i < rotationPages.size(); i++) {
			if (id.equals(rotationPages.get(i).signId)) return i;
		}
		return -1;
	}

	public void clearRotationSigns() {
		rotationPages.clear();
		rotationIndex = -1;
		markDirtyAndNotify();
		pushSyncToFollowers();
	}

	public DisplaySchedule.Mode getScheduleMode() {
		return schedule.getMode();
	}

	public void setScheduleMode(DisplaySchedule.Mode mode) {
		if (isSyncFollower()) return;
		schedule.setMode(mode);
		markDirtyAndNotify();
		pushSyncToFollowers();
	}

	public int getScheduleIntervalAmount() {
		return schedule.getIntervalAmount();
	}

	public void setScheduleIntervalAmount(int amount) {
		if (isSyncFollower()) return;
		schedule.setIntervalAmount(amount);
		markDirtyAndNotify();
		pushSyncToFollowers();
	}

	public String getScheduleTimesText() {
		return schedule.getGameTimesText();
	}

	public void setScheduleTimes(String times) {
		if (isSyncFollower()) return;
		schedule.setGameTimesFromText(times);
		markDirtyAndNotify();
		pushSyncToFollowers();
	}

	public int setSelectedSign(UUID id) {
		selectedSign = id == null ? Sign.DEFAULT_BLANK_SIGN : id;
		if (rotationIndex >= 0 && rotationIndex < rotationPages.size()) {
			selectedSign = rotationPages.get(rotationIndex).signId;
		}
		int updated = applySelectedToLinkedSigns();
		markDirtyAndNotify();
		pushSyncToFollowers();
		return updated;
	}

	private RotationPage getActivePage() {
		return rotationIndex >= 0 && rotationIndex < rotationPages.size()
				? rotationPages.get(rotationIndex) : null;
	}

	private int applySelectedToLinkedSigns() {
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
		return updated;
	}

	public UUID getSelectedSign() {
		return selectedSign;
	}

	private void applyTo(DigitalSignTileEntity sign) {
		// Like a normal SignBlock: only the paired panel is updated. Neighboring digital
		// cabinets stay independent; adjacency is only for seamless multiblock bezels.
		RotationPage page = getActivePage();
		UUID signId = page == null ? selectedSign : page.signId;
		sign.setTypeLegacy(-1);
		sign.setVariantLegacy(-1);
		sign.setID(signId);
		sign.clearTextLines();
		if (page != null) {
			ensureTextLineCount(page);
			for (int i = 0; i < page.textLines.size(); i++) {
				String line = page.textLines.get(i);
				sign.setTextLine(i, line == null ? "" : line);
			}
		}
		sign.markDirty();
		BlockPos signPos = sign.getPos();
		IBlockState state = world.getBlockState(signPos);
		world.notifyBlockUpdate(signPos, state, state, 3);
		world.markBlockRangeForRenderUpdate(signPos, signPos);
	}

	public void applySyncFrom(DigitalSignControllerTileEntity master) {
		if (master == null || master == this) return;
		applyingSync = true;
		try {
			// Timing only: keep this controller's own pages/signs, but rotate on the master's clock.
			schedule.copyTimingFrom(master.schedule);
			applyRotationIndex(master.rotationIndex, false);
		} finally {
			applyingSync = false;
		}
	}

	/** Apply a page index to this controller's own pages (does not push sync). */
	private void applyRotationIndex(int index, boolean allowEmpty) {
		if (rotationPages.isEmpty()) {
			rotationIndex = -1;
			if (allowEmpty) {
				selectedSign = Sign.DEFAULT_BLANK_SIGN;
				applySelectedToLinkedSigns();
			}
			return;
		}
		int idx = index < 0 ? 0 : index % rotationPages.size();
		rotationIndex = idx;
		selectedSign = rotationPages.get(rotationIndex).signId;
		applySelectedToLinkedSigns();
	}

	private void pushSyncToFollowers() {
		if (applyingSync || world == null || world.isRemote || syncedControllers.isEmpty()) return;
		for (BlockPos pos : new ArrayList<>(syncedControllers)) {
			TileEntity tile = world.getTileEntity(pos);
			if (!(tile instanceof DigitalSignControllerTileEntity)) {
				syncedControllers.remove(pos);
				continue;
			}
			DigitalSignControllerTileEntity follower = (DigitalSignControllerTileEntity) tile;
			follower.syncMaster = getPos();
			follower.applySyncFrom(this);
			follower.markDirtyAndNotify();
		}
	}

	@Override
	public void update() {
		if (world == null || world.isRemote) return;
		if (syncMaster != null) {
			TileEntity masterTile = world.getTileEntity(syncMaster);
			if (!(masterTile instanceof DigitalSignControllerTileEntity)) {
				syncMaster = null;
				markDirtyAndNotify();
				return;
			}
			// Pull master's page index so followers stay aligned even if a push was missed.
			followMasterTiming((DigitalSignControllerTileEntity) masterTile);
			return;
		}

		boolean advanced = false;
		if (schedule.getMode() == DisplaySchedule.Mode.GAME_TIMES) {
			advanced = selectSignForCurrentGameTime();
		} else if (schedule.getMode().isInterval() && rotationPages.size() > 1 && schedule.update(world)) {
			int next = rotationIndex < 0 ? 0 : (rotationIndex + 1) % rotationPages.size();
			applyRotationIndex(next, true);
			markDirtyAndNotify();
			advanced = true;
		}
		if (advanced) pushSyncToFollowers();
	}

	private void followMasterTiming(DigitalSignControllerTileEntity master) {
		int target = rotationPages.isEmpty() ? -1
				: (master.rotationIndex < 0 ? 0 : master.rotationIndex) % Math.max(1, rotationPages.size());
		boolean scheduleDiff = schedule.getMode() != master.schedule.getMode()
				|| schedule.getIntervalAmount() != master.schedule.getIntervalAmount()
				|| !schedule.getGameTimes().equals(master.schedule.getGameTimes());
		boolean indexDiff = rotationPages.isEmpty() ? rotationIndex != -1 : rotationIndex != target;
		if (!scheduleDiff && !indexDiff) return;
		applySyncFrom(master);
		markDirtyAndNotify();
	}

	private boolean selectSignForCurrentGameTime() {
		if (rotationPages.isEmpty()) return false;
		int current = DisplaySchedule.normalizeDayTime(world.getWorldTime());
		int bestIndex = -1;
		int bestElapsed = Integer.MAX_VALUE;
		for (int i = 0; i < rotationPages.size(); i++) {
			Integer configured = rotationPages.get(i).gameTime;
			if (configured == null) continue;
			int elapsed = (current - configured + 24000) % 24000;
			if (elapsed < bestElapsed) {
				bestElapsed = elapsed;
				bestIndex = i;
			}
		}
		if (bestIndex >= 0 && rotationIndex != bestIndex) {
			applyRotationIndex(bestIndex, true);
			markDirtyAndNotify();
			return true;
		}
		return false;
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
			if (compound.hasKey("digitalSign" + i)) signs.add(BlockPos.fromLong(compound.getLong("digitalSign" + i)));
		}
		syncedControllers.clear();
		int syncCount = Math.max(0, compound.getInteger("syncedControllerCount"));
		for (int i = 0; i < syncCount; i++) {
			if (compound.hasKey("syncedController" + i)) {
				syncedControllers.add(BlockPos.fromLong(compound.getLong("syncedController" + i)));
			}
		}
		syncMaster = compound.hasKey("syncMaster") ? BlockPos.fromLong(compound.getLong("syncMaster")) : null;
		selectedSign = compound.hasKey("selectedSign")
				? NBTUtil.getUUIDFromTag(compound.getCompoundTag("selectedSign")) : Sign.DEFAULT_BLANK_SIGN;

		rotationPages.clear();
		if (compound.hasKey("rotationPageCount")) {
			int pageCount = Math.min(MAX_ROTATION_SIGNS, compound.getInteger("rotationPageCount"));
			for (int i = 0; i < pageCount; i++) {
				if (compound.hasKey("rotationPage" + i)) {
					rotationPages.add(new RotationPage(compound.getCompoundTag("rotationPage" + i)));
				}
			}
		} else {
			int rotationCount = Math.min(MAX_ROTATION_SIGNS, compound.getInteger("rotationSignCount"));
			for (int i = 0; i < rotationCount; i++) {
				if (compound.hasKey("rotationSign" + i)) {
					UUID id = NBTUtil.getUUIDFromTag(compound.getCompoundTag("rotationSign" + i));
					RotationPage page = new RotationPage(id);
					if (compound.hasKey("rotationSignTime" + i)) {
						page.gameTime = DisplaySchedule.normalizeDayTime(compound.getInteger("rotationSignTime" + i));
					}
					rotationPages.add(page);
				}
			}
		}
		rotationIndex = compound.getInteger("rotationIndex");
		if (rotationIndex < 0 || rotationIndex >= rotationPages.size()) {
			rotationIndex = rotationPages.isEmpty() ? -1 : 0;
		}
		schedule.readFromNBT(compound, "schedule");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		for (int i = 0; i < signs.size(); i++) compound.setLong("digitalSign" + i, signs.get(i).toLong());
		compound.setInteger("syncedControllerCount", syncedControllers.size());
		for (int i = 0; i < syncedControllers.size(); i++) {
			compound.setLong("syncedController" + i, syncedControllers.get(i).toLong());
		}
		if (syncMaster != null) compound.setLong("syncMaster", syncMaster.toLong());
		if (selectedSign != null) compound.setTag("selectedSign", NBTUtil.createUUIDTag(selectedSign));
		compound.setInteger("rotationPageCount", rotationPages.size());
		for (int i = 0; i < rotationPages.size(); i++) {
			compound.setTag("rotationPage" + i, rotationPages.get(i).writeToNBT());
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
		pushSyncToFollowers();
	}

	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
		return newState.getBlock() != ModBlocks.digital_sign_controller;
	}
}
