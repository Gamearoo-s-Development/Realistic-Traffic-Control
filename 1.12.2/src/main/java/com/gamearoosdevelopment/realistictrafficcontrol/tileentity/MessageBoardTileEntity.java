package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.util.DisplaySchedule;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Portable message/arrow board with built-in pages, schedule, and optional peer linking. */
public class MessageBoardTileEntity extends SyncableTileEntity implements ITickable {
	public enum FontStyle {
		REGULAR("Regular", ""), BOLD("Bold", "\u00A7l"), ITALIC("Italic", "\u00A7o"),
		BOLD_ITALIC("Bold Italic", "\u00A7l\u00A7o");

		private final String label;
		private final String formatting;

		FontStyle(String label, String formatting) {
			this.label = label;
			this.formatting = formatting;
		}

		public String getLabel() { return label; }
		public String apply(String text) { return formatting + (text == null ? "" : text); }
		public FontStyle next() { return values()[(ordinal() + 1) % values().length]; }

		public static FontStyle fromName(String name) {
			if (name != null) {
				try { return valueOf(name.trim().toUpperCase().replace(' ', '_')); }
				catch (IllegalArgumentException ignored) { }
			}
			return REGULAR;
		}
	}

	public enum DisplayMode {
		TEXT, ARROW_LEFT, ARROW_RIGHT, ARROW_BOTH, CAUTION, OFF;

		public static DisplayMode fromName(String name) {
			if (name != null) {
				String normalized = name.trim().toUpperCase();
				if ("ARROW_MERGE_LEFT".equals(normalized)) return ARROW_LEFT;
				if ("ARROW_MERGE_RIGHT".equals(normalized)) return ARROW_RIGHT;
				if ("ARROW_LEFT_RIGHT".equals(normalized) || "ARROW_COMBINED".equals(normalized)
						|| "BOTH".equals(normalized)) {
					return ARROW_BOTH;
				}
				try { return valueOf(normalized); } catch (IllegalArgumentException ignored) { }
			}
			return TEXT;
		}
	}

	public static final int MAX_LINES = 3;
	public static final int MAX_LINE_LENGTH = 32;
	public static final int MAX_BOARDS = 16;
	public static final int MAX_ROTATION_PAGES = 32;

	private final String[] lines = new String[] { "", "", "" };
	private int color = 0xFFFFA000;
	private float brightness = 1.0F;
	private float textScale = 1.0F;
	private FontStyle fontStyle = FontStyle.REGULAR;
	private DisplayMode mode = DisplayMode.TEXT;

	private final ArrayList<BlockPos> boards = new ArrayList<>();
	private final ArrayList<RotationPage> rotationPages = new ArrayList<>();
	private final DisplaySchedule schedule = new DisplaySchedule();
	private int rotationIndex = -1;

	private static class RotationPage {
		private final String[] lines = new String[] { "", "", "" };
		private DisplayMode mode = DisplayMode.TEXT;
		private float brightness = 1.0F;
		private float textScale = 1.0F;
		private FontStyle fontStyle = FontStyle.REGULAR;
		private int color = 0xFFFFA000;

		private RotationPage(MessageBoardTileEntity board) {
			for (int i = 0; i < lines.length; i++) lines[i] = board.lines[i];
			mode = board.mode;
			brightness = board.brightness;
			textScale = board.textScale;
			fontStyle = board.fontStyle;
			color = board.color;
		}

		private RotationPage(NBTTagCompound compound) {
			for (int i = 0; i < lines.length; i++) lines[i] = compound.getString("line" + i);
			mode = DisplayMode.fromName(compound.getString("mode"));
			if (compound.hasKey("brightness")) brightness = compound.getFloat("brightness");
			if (compound.hasKey("textScale")) textScale = Math.max(0.5F, Math.min(1.5F, compound.getFloat("textScale")));
			fontStyle = FontStyle.fromName(compound.getString("fontStyle"));
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

	public String getLine(int index) {
		return index >= 0 && index < MAX_LINES ? lines[index] : "";
	}

	public String getStyledLine(int index) {
		return fontStyle.apply(getLine(index));
	}

	public int getColor() { return color; }
	public float getBrightness() { return brightness; }
	public float getTextScale() { return textScale; }
	public FontStyle getFontStyle() { return fontStyle; }
	public DisplayMode getMode() { return mode; }

	public List<BlockPos> getLinkedBoards() {
		return Collections.unmodifiableList(boards);
	}

	public boolean linkBoard(BlockPos pos) {
		if (pos == null || pos.equals(getPos()) || boards.contains(pos) || boards.size() >= MAX_BOARDS) {
			return false;
		}
		TileEntity tile = world == null ? null : world.getTileEntity(pos);
		if (!(tile instanceof MessageBoardTileEntity)) return false;
		boards.add(pos);
		copyDisplayTo((MessageBoardTileEntity) tile);
		markDirtyAndNotify();
		return true;
	}

	public boolean unlinkBoard(BlockPos pos) {
		boolean removed = boards.remove(pos);
		if (removed) markDirtyAndNotify();
		return removed;
	}

	public int getRotationPageCount() { return rotationPages.size(); }
	public int getRotationIndex() { return rotationIndex; }

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
		pushDisplayToLinked();
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

	public DisplaySchedule.Mode getScheduleMode() { return schedule.getMode(); }

	public void setScheduleMode(DisplaySchedule.Mode mode) {
		schedule.setMode(mode);
		markDirtyAndNotify();
	}

	public int getScheduleIntervalAmount() { return schedule.getIntervalAmount(); }

	public void setScheduleIntervalAmount(int amount) {
		schedule.setIntervalAmount(amount);
		markDirtyAndNotify();
	}

	public String getScheduleTimesText() { return schedule.getGameTimesText(); }

	public void setScheduleTimes(String times) {
		schedule.setGameTimesFromText(times);
		markDirtyAndNotify();
	}

	/** Sets a line on this board and any linked boards. */
	public int setText(int line, String value) {
		if (line < 0 || line >= MAX_LINES) return 0;
		value = value == null ? "" : value.substring(0, Math.min(value.length(), MAX_LINE_LENGTH));
		lines[line] = value;
		int updated = 1;
		updated += pushDisplayToLinked();
		markDirtyAndNotify();
		return updated;
	}

	public int clearBoards() {
		for (int i = 0; i < lines.length; i++) lines[i] = "";
		int updated = 1;
		updated += pushDisplayToLinked();
		markDirtyAndNotify();
		return updated;
	}

	public int setColor(int color) {
		this.color = color & 0xFFFFFF;
		int updated = 1 + pushDisplayToLinked();
		markDirtyAndNotify();
		return updated;
	}

	public int setBrightness(float brightness) {
		this.brightness = Math.max(0.1F, Math.min(1.0F, brightness));
		int updated = 1 + pushDisplayToLinked();
		markDirtyAndNotify();
		return updated;
	}

	public int setTextScale(float textScale) {
		this.textScale = Math.max(0.5F, Math.min(1.5F, textScale));
		int updated = 1 + pushDisplayToLinked();
		markDirtyAndNotify();
		return updated;
	}

	public int setFontStyle(FontStyle fontStyle) {
		this.fontStyle = fontStyle == null ? FontStyle.REGULAR : fontStyle;
		int updated = 1 + pushDisplayToLinked();
		markDirtyAndNotify();
		return updated;
	}

	public int setMode(DisplayMode mode) {
		this.mode = mode == null ? DisplayMode.TEXT : mode;
		int updated = 1 + pushDisplayToLinked();
		markDirtyAndNotify();
		return updated;
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
		pushDisplayToLinked();
		markDirtyAndNotify();
	}

	private int pushDisplayToLinked() {
		int updated = 0;
		for (BlockPos pos : new ArrayList<>(boards)) {
			TileEntity tile = world == null ? null : world.getTileEntity(pos);
			if (!(tile instanceof MessageBoardTileEntity) || tile == this) {
				boards.remove(pos);
				continue;
			}
			copyDisplayTo((MessageBoardTileEntity) tile);
			updated++;
		}
		return updated;
	}

	private void copyDisplayTo(MessageBoardTileEntity board) {
		for (int i = 0; i < MAX_LINES; i++) {
			board.lines[i] = lines[i];
		}
		board.mode = mode;
		board.brightness = brightness;
		board.textScale = textScale;
		board.fontStyle = fontStyle;
		board.color = color;
		board.markDirtyAndNotify();
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
			world.markBlockRangeForRenderUpdate(getPos(), getPos());
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		for (int i = 0; i < MAX_LINES; i++) lines[i] = compound.getString("line" + i);
		if (compound.hasKey("color")) color = compound.getInteger("color");
		if (compound.hasKey("brightness")) brightness = compound.getFloat("brightness");
		textScale = compound.hasKey("textScale")
				? Math.max(0.5F, Math.min(1.5F, compound.getFloat("textScale"))) : 1.0F;
		fontStyle = FontStyle.fromName(compound.getString("fontStyle"));
		mode = DisplayMode.fromName(compound.getString("mode"));

		boards.clear();
		for (int i = 0; i < MAX_BOARDS; i++) {
			if (compound.hasKey("messageBoard" + i)) {
				boards.add(BlockPos.fromLong(compound.getLong("messageBoard" + i)));
			}
		}
		rotationPages.clear();
		int pageCount = Math.min(MAX_ROTATION_PAGES, compound.getInteger("rotationPageCount"));
		for (int i = 0; i < pageCount; i++) {
			if (compound.hasKey("rotationPage" + i)) {
				rotationPages.add(new RotationPage(compound.getCompoundTag("rotationPage" + i)));
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
		for (int i = 0; i < MAX_LINES; i++) compound.setString("line" + i, lines[i]);
		compound.setInteger("color", color);
		compound.setFloat("brightness", brightness);
		compound.setFloat("textScale", textScale);
		compound.setString("fontStyle", fontStyle.name());
		compound.setString("mode", mode.name());
		for (int i = 0; i < boards.size(); i++) {
			compound.setLong("messageBoard" + i, boards.get(i).toLong());
		}
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
		pushDisplayToLinked();
		markDirtyAndNotify();
	}

	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
		return newState.getBlock() != ModBlocks.message_board;
	}

	@Override
	public double getMaxRenderDistanceSquared() {
		return ModRealisticTrafficControl.MAX_RENDER_DISTANCE;
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return new AxisAlignedBB(pos.add(-2, 0, -2), pos.add(3, 4, 3));
	}
}
