package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.util.DisplaySchedule;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MessageBoardControllerTileEntity extends SyncableTileEntity implements ITickable {
	public static final int MAX_BOARDS = 16;
	public static final int MAX_ROTATION_PAGES = 32;
	private final ArrayList<BlockPos> boards = new ArrayList<>();
	private final ArrayList<RotationPage> rotationPages = new ArrayList<>();
	private final DisplaySchedule schedule = new DisplaySchedule();
	private final String[] lines = new String[] { "", "", "" };
	private MessageBoardTileEntity.DisplayMode mode = MessageBoardTileEntity.DisplayMode.TEXT;
	private float brightness = 1.0F;
	private float textScale = 1.0F;
	private MessageBoardTileEntity.FontStyle fontStyle = MessageBoardTileEntity.FontStyle.REGULAR;
	private int color = 0xFFFFA000;
	private int rotationIndex = -1;

	private static class RotationPage {
		private final String[] lines = new String[] { "", "", "" };
		private MessageBoardTileEntity.DisplayMode mode = MessageBoardTileEntity.DisplayMode.TEXT;
		private float brightness = 1.0F;
		private float textScale = 1.0F;
		private MessageBoardTileEntity.FontStyle fontStyle = MessageBoardTileEntity.FontStyle.REGULAR;
		private int color = 0xFFFFA000;

		private RotationPage(MessageBoardControllerTileEntity controller) {
			for (int i = 0; i < lines.length; i++) lines[i] = controller.lines[i];
			mode = controller.mode;
			brightness = controller.brightness;
			textScale = controller.textScale;
			fontStyle = controller.fontStyle;
			color = controller.color;
		}

		private RotationPage(NBTTagCompound compound) {
			for (int i = 0; i < lines.length; i++) lines[i] = compound.getString("line" + i);
			mode = MessageBoardTileEntity.DisplayMode.fromName(compound.getString("mode"));
			if (compound.hasKey("brightness")) brightness = compound.getFloat("brightness");
			if (compound.hasKey("textScale")) textScale = Math.max(0.5F, Math.min(1.5F, compound.getFloat("textScale")));
			fontStyle = MessageBoardTileEntity.FontStyle.fromName(compound.getString("fontStyle"));
			if (compound.hasKey("color")) color = compound.getInteger("color");
		}

		private NBTTagCompound writeToNBT() {
			NBTTagCompound compound = new NBTTagCompound();
			for (int i = 0; i < lines.length; i++) compound.setString("line" + i, lines[i]);
			compound.setString("mode", mode.name());
			compound.setFloat("brightness", brightness);
			compound.setFloat("textScale", textScale);
			compound.setString("fontStyle", fontStyle.name());
			compound.setInteger("color", color);
			return compound;
		}
	}

	public List<BlockPos> getLinkedBoards() {
		return Collections.unmodifiableList(boards);
	}

	public boolean linkBoard(BlockPos pos) {
		if (pos == null || boards.contains(pos) || boards.size() >= MAX_BOARDS
				|| !(world.getTileEntity(pos) instanceof MessageBoardTileEntity)) {
			return false;
		}
		boards.add(pos);
		applyTo((MessageBoardTileEntity) world.getTileEntity(pos));
		markDirtyAndNotify();
		return true;
	}

	public boolean unlinkBoard(BlockPos pos) {
		boolean removed = boards.remove(pos);
		if (removed) {
			markDirtyAndNotify();
		}
		return removed;
	}

	public int getRotationPageCount() {
		return rotationPages.size();
	}

	public int getRotationIndex() {
		return rotationIndex;
	}

	public boolean selectRotationPage(int index) {
		if (rotationPages.isEmpty()) return false;
		rotationIndex = (index % rotationPages.size() + rotationPages.size()) % rotationPages.size();
		applyRotationPage(rotationIndex);
		return true;
	}

	public boolean addCurrentPage() {
		if (rotationPages.size() >= MAX_ROTATION_PAGES) return false;
		rotationPages.add(new RotationPage(this));
		rotationIndex = rotationPages.size() - 1;
		markDirtyAndNotify();
		return true;
	}

	public boolean updateCurrentPage() {
		return updateRotationPage(rotationIndex);
	}

	public boolean updateRotationPage(int index) {
		if (index < 0 || index >= rotationPages.size()) return false;
		rotationIndex = index;
		rotationPages.set(index, new RotationPage(this));
		markDirtyAndNotify();
		return true;
	}

	public boolean removeCurrentPage() {
		if (rotationIndex < 0 || rotationIndex >= rotationPages.size()) return false;
		rotationPages.remove(rotationIndex);
		if (rotationPages.isEmpty()) {
			rotationIndex = -1;
			markDirtyAndNotify();
		} else {
			rotationIndex = Math.min(rotationIndex, rotationPages.size() - 1);
			applyRotationPage(rotationIndex);
		}
		return true;
	}

	public void clearRotationPages() {
		rotationPages.clear();
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

	public int setText(int line, String value) {
		if (line < 0 || line >= MessageBoardTileEntity.MAX_LINES) return 0;
		value = value == null ? "" : value.substring(0, Math.min(value.length(), MessageBoardTileEntity.MAX_LINE_LENGTH));
		lines[line] = value;
		int updated = 0;
		for (BlockPos pos : new ArrayList<>(boards)) {
			TileEntity tile = world.getTileEntity(pos);
			if (!(tile instanceof MessageBoardTileEntity)) {
				boards.remove(pos);
				continue;
			}
			((MessageBoardTileEntity) tile).setLine(line, value);
			updated++;
		}
		markDirtyAndNotify();
		return updated;
	}

	public int clearBoards() {
		for (int i = 0; i < lines.length; i++) lines[i] = "";
		int updated = 0;
		for (BlockPos pos : new ArrayList<>(boards)) {
			TileEntity tile = world.getTileEntity(pos);
			if (!(tile instanceof MessageBoardTileEntity)) {
				boards.remove(pos);
				continue;
			}
			((MessageBoardTileEntity) tile).clear();
			updated++;
		}
		markDirtyAndNotify();
		return updated;
	}

	public int setColor(int color) {
		this.color = color & 0xFFFFFF;
		int updated = 0;
		for (BlockPos pos : new ArrayList<>(boards)) {
			TileEntity tile = world.getTileEntity(pos);
			if (!(tile instanceof MessageBoardTileEntity)) {
				boards.remove(pos);
				continue;
			}
			((MessageBoardTileEntity) tile).setColor(color);
			updated++;
		}
		markDirtyAndNotify();
		return updated;
	}

	public int setBrightness(float brightness) {
		this.brightness = Math.max(0.1F, Math.min(1.0F, brightness));
		int updated = 0;
		for (BlockPos pos : new ArrayList<>(boards)) {
			TileEntity tile = world.getTileEntity(pos);
			if (!(tile instanceof MessageBoardTileEntity)) {
				boards.remove(pos);
				continue;
			}
			((MessageBoardTileEntity) tile).setBrightness(brightness);
			updated++;
		}
		markDirtyAndNotify();
		return updated;
	}

	public int setTextScale(float textScale) {
		this.textScale = Math.max(0.5F, Math.min(1.5F, textScale));
		int updated = 0;
		for (BlockPos pos : new ArrayList<>(boards)) {
			TileEntity tile = world.getTileEntity(pos);
			if (!(tile instanceof MessageBoardTileEntity)) { boards.remove(pos); continue; }
			((MessageBoardTileEntity) tile).setTextScale(this.textScale);
			updated++;
		}
		markDirtyAndNotify();
		return updated;
	}

	public int setFontStyle(MessageBoardTileEntity.FontStyle fontStyle) {
		this.fontStyle = fontStyle == null ? MessageBoardTileEntity.FontStyle.REGULAR : fontStyle;
		int updated = 0;
		for (BlockPos pos : new ArrayList<>(boards)) {
			TileEntity tile = world.getTileEntity(pos);
			if (!(tile instanceof MessageBoardTileEntity)) { boards.remove(pos); continue; }
			((MessageBoardTileEntity) tile).setFontStyle(this.fontStyle);
			updated++;
		}
		markDirtyAndNotify();
		return updated;
	}

	public String getLine(int line) { return line >= 0 && line < lines.length ? lines[line] : ""; }
	public MessageBoardTileEntity.DisplayMode getMode() { return mode; }
	public float getBrightness() { return brightness; }
	public float getTextScale() { return textScale; }
	public MessageBoardTileEntity.FontStyle getFontStyle() { return fontStyle; }
	public int getColor() { return color; }

	public int setMode(MessageBoardTileEntity.DisplayMode mode) {
		this.mode = mode == null ? MessageBoardTileEntity.DisplayMode.TEXT : mode;
		int updated = 0;
		for (BlockPos pos : new ArrayList<>(boards)) {
			TileEntity tile = world.getTileEntity(pos);
			if (!(tile instanceof MessageBoardTileEntity)) { boards.remove(pos); continue; }
			((MessageBoardTileEntity) tile).setMode(this.mode);
			updated++;
		}
		markDirtyAndNotify();
		return updated;
	}

	private void applyTo(MessageBoardTileEntity board) {
		for (int i = 0; i < lines.length; i++) board.setLine(i, lines[i]);
		board.setMode(mode);
		board.setBrightness(brightness);
		board.setTextScale(textScale);
		board.setFontStyle(fontStyle);
		board.setColor(color);
	}

	private void applyRotationPage(int index) {
		if (index < 0 || index >= rotationPages.size()) return;
		RotationPage page = rotationPages.get(index);
		for (int i = 0; i < lines.length; i++) lines[i] = page.lines[i];
		mode = page.mode;
		brightness = page.brightness;
		textScale = page.textScale;
		fontStyle = page.fontStyle;
		color = page.color;
		for (BlockPos pos : new ArrayList<>(boards)) {
			TileEntity tile = world.getTileEntity(pos);
			if (!(tile instanceof MessageBoardTileEntity)) {
				boards.remove(pos);
				continue;
			}
			applyTo((MessageBoardTileEntity) tile);
		}
		markDirtyAndNotify();
	}

	@Override
	public void update() {
		if (world == null || world.isRemote) return;
		if (schedule.update(world) && rotationPages.size() > 1) {
			rotationIndex = (rotationIndex + 1 + rotationPages.size()) % rotationPages.size();
			applyRotationPage(rotationIndex);
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
		boards.clear();
		for (int i = 0; i < MAX_BOARDS; i++) {
			if (compound.hasKey("messageBoard" + i)) {
				boards.add(BlockPos.fromLong(compound.getLong("messageBoard" + i)));
			}
		}
		for (int i = 0; i < lines.length; i++) lines[i] = compound.getString("line" + i);
		mode = MessageBoardTileEntity.DisplayMode.fromName(compound.getString("mode"));
		if (compound.hasKey("brightness")) brightness = compound.getFloat("brightness");
		textScale = compound.hasKey("textScale")
				? Math.max(0.5F, Math.min(1.5F, compound.getFloat("textScale"))) : 1.0F;
		fontStyle = MessageBoardTileEntity.FontStyle.fromName(compound.getString("fontStyle"));
		if (compound.hasKey("color")) color = compound.getInteger("color");
		rotationPages.clear();
		int pageCount = Math.min(MAX_ROTATION_PAGES, compound.getInteger("rotationPageCount"));
		for (int i = 0; i < pageCount; i++) {
			if (compound.hasKey("rotationPage" + i)) {
				rotationPages.add(new RotationPage(compound.getCompoundTag("rotationPage" + i)));
			}
		}
		rotationIndex = compound.getInteger("rotationIndex");
		if (rotationIndex < 0 || rotationIndex >= rotationPages.size()) rotationIndex = rotationPages.isEmpty() ? -1 : 0;
		schedule.readFromNBT(compound, "schedule");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		for (int i = 0; i < boards.size(); i++) {
			compound.setLong("messageBoard" + i, boards.get(i).toLong());
		}
		for (int i = 0; i < lines.length; i++) compound.setString("line" + i, lines[i]);
		compound.setString("mode", mode.name());
		compound.setFloat("brightness", brightness);
		compound.setFloat("textScale", textScale);
		compound.setString("fontStyle", fontStyle.name());
		compound.setInteger("color", color);
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
		for (BlockPos pos : new ArrayList<>(boards)) {
			TileEntity tile = world.getTileEntity(pos);
			if (tile instanceof MessageBoardTileEntity) applyTo((MessageBoardTileEntity) tile);
		}
		markDirtyAndNotify();
	}

	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
		return newState.getBlock() != ModBlocks.message_board_controller;
	}
}
