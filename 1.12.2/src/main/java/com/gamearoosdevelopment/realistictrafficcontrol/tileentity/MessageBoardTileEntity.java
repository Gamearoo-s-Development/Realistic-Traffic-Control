package com.gamearoosdevelopment.realistictrafficcontrol.tileentity;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlocks;
import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

public class MessageBoardTileEntity extends SyncableTileEntity {
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
		TEXT, ARROW_LEFT, ARROW_RIGHT, CAUTION, OFF;

		public static DisplayMode fromName(String name) {
			if (name != null) {
				String normalized = name.trim().toUpperCase();
				// Migrate pages saved before the merge modes were removed.
				if ("ARROW_MERGE_LEFT".equals(normalized)) return ARROW_LEFT;
				if ("ARROW_MERGE_RIGHT".equals(normalized)) return ARROW_RIGHT;
				try { return valueOf(normalized); } catch (IllegalArgumentException ignored) { }
			}
			return TEXT;
		}
	}
	public static final int MAX_LINES = 3;
	public static final int MAX_LINE_LENGTH = 32;

	private final String[] lines = new String[] { "", "", "" };
	private int color = 0xFFFFA000;
	private float brightness = 1.0F;
	private float textScale = 1.0F;
	private FontStyle fontStyle = FontStyle.REGULAR;
	private DisplayMode mode = DisplayMode.TEXT;

	public String getLine(int index) {
		return index >= 0 && index < MAX_LINES ? lines[index] : "";
	}

	public void setLine(int index, String value) {
		if (index < 0 || index >= MAX_LINES) {
			return;
		}
		if (value == null) {
			value = "";
		}
		lines[index] = value.substring(0, Math.min(MAX_LINE_LENGTH, value.length()));
		markDirtyAndNotify();
	}

	public void clear() {
		for (int i = 0; i < MAX_LINES; i++) {
			lines[i] = "";
		}
		markDirtyAndNotify();
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color & 0xFFFFFF;
		markDirtyAndNotify();
	}

	public float getBrightness() {
		return brightness;
	}

	public float getTextScale() { return textScale; }
	public FontStyle getFontStyle() { return fontStyle; }
	public String getStyledLine(int index) { return fontStyle.apply(getLine(index)); }

	public DisplayMode getMode() { return mode; }

	public void setMode(DisplayMode mode) {
		this.mode = mode == null ? DisplayMode.TEXT : mode;
		markDirtyAndNotify();
	}

	public void setBrightness(float brightness) {
		this.brightness = Math.max(0.1F, Math.min(1.0F, brightness));
		markDirtyAndNotify();
	}

	public void setTextScale(float textScale) {
		this.textScale = Math.max(0.5F, Math.min(1.5F, textScale));
		markDirtyAndNotify();
	}

	public void setFontStyle(FontStyle fontStyle) {
		this.fontStyle = fontStyle == null ? FontStyle.REGULAR : fontStyle;
		markDirtyAndNotify();
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
		for (int i = 0; i < MAX_LINES; i++) {
			lines[i] = compound.getString("line" + i);
		}
		if (compound.hasKey("color")) {
			color = compound.getInteger("color");
		}
		if (compound.hasKey("brightness")) {
			brightness = compound.getFloat("brightness");
		}
		textScale = compound.hasKey("textScale")
				? Math.max(0.5F, Math.min(1.5F, compound.getFloat("textScale"))) : 1.0F;
		fontStyle = FontStyle.fromName(compound.getString("fontStyle"));
		mode = DisplayMode.fromName(compound.getString("mode"));
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		for (int i = 0; i < MAX_LINES; i++) {
			compound.setString("line" + i, lines[i]);
		}
		compound.setInteger("color", color);
		compound.setFloat("brightness", brightness);
		compound.setFloat("textScale", textScale);
		compound.setString("fontStyle", fontStyle.name());
		compound.setString("mode", mode.name());
		return super.writeToNBT(compound);
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return writeToNBT(super.getUpdateTag());
	}

	@Override
	public void handleUpdateTag(NBTTagCompound tag) {
		super.handleUpdateTag(tag);
		for (int i = 0; i < MAX_LINES; i++) {
			lines[i] = tag.getString("line" + i);
		}
		color = tag.getInteger("color");
		brightness = tag.getFloat("brightness");
		textScale = tag.hasKey("textScale")
				? Math.max(0.5F, Math.min(1.5F, tag.getFloat("textScale"))) : 1.0F;
		fontStyle = FontStyle.fromName(tag.getString("fontStyle"));
		mode = DisplayMode.fromName(tag.getString("mode"));
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
