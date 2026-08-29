package com.gamearoosdevelopment.realistictrafficcontrol.network;

import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightControlBoxTileEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.ApproachMovementSettings;
import com.gamearoosdevelopment.realistictrafficcontrol.util.FyaMode;
import com.gamearoosdevelopment.realistictrafficcontrol.util.IdleBulbMode;

import io.netty.buffer.ByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSetApproachMovement implements IMessage {
	private BlockPos pos;
	private byte facingOrdinal;
	private boolean straightEnabled;
	private boolean leftEnabled;
	private boolean rightEnabled;
	private boolean sharedTurns;
	private byte straightIdle;
	private byte leftIdle;
	private byte rightIdle;
	private byte leftFya;
	private byte rightFya;

	public PacketSetApproachMovement() {}

	public PacketSetApproachMovement(BlockPos pos, EnumFacing facing, ApproachMovementSettings settings) {
		this.pos = pos;
		// Horizontal index only (N=0,S=1,W=2,E=3) — approach settings are per cardinal direction.
		this.facingOrdinal = (byte) facing.getHorizontalIndex();
		this.straightEnabled = settings.straightEnabled;
		this.leftEnabled = settings.leftEnabled;
		this.rightEnabled = settings.rightEnabled;
		this.sharedTurns = settings.sharedTurns;
		this.straightIdle = (byte) settings.straightIdle.ordinal();
		this.leftIdle = (byte) settings.leftIdle.ordinal();
		this.rightIdle = (byte) settings.rightIdle.ordinal();
		this.leftFya = (byte) settings.leftFya.ordinal();
		this.rightFya = (byte) settings.rightFya.ordinal();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeLong(pos.toLong());
		buf.writeByte(facingOrdinal);
		buf.writeBoolean(straightEnabled);
		buf.writeBoolean(leftEnabled);
		buf.writeBoolean(rightEnabled);
		buf.writeByte(straightIdle);
		buf.writeByte(leftIdle);
		buf.writeByte(rightIdle);
		buf.writeByte(leftFya);
		buf.writeByte(rightFya);
		buf.writeBoolean(sharedTurns);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		pos = BlockPos.fromLong(buf.readLong());
		facingOrdinal = buf.readByte();
		straightEnabled = buf.readBoolean();
		leftEnabled = buf.readBoolean();
		rightEnabled = buf.readBoolean();
		straightIdle = buf.readByte();
		leftIdle = buf.readByte();
		rightIdle = buf.readByte();
		if (buf.isReadable()) {
			leftFya = buf.readByte();
			rightFya = buf.readByte();
		} else {
			leftFya = (byte) FyaMode.ALWAYS.ordinal();
			rightFya = (byte) FyaMode.ALWAYS.ordinal();
		}
		if (buf.isReadable()) {
			sharedTurns = buf.readBoolean();
		} else {
			sharedTurns = false;
		}
	}

	public static class Handler implements IMessageHandler<PacketSetApproachMovement, IMessage> {
		@Override
		public IMessage onMessage(PacketSetApproachMovement msg, MessageContext ctx) {
			MinecraftServer server = ctx.getServerHandler().player.getServer();
			server.addScheduledTask(() -> {
				World world = ctx.getServerHandler().player.world;
				TileEntity te = world.getTileEntity(msg.pos);
				if (!(te instanceof TrafficLightControlBoxTileEntity)) {
					return;
				}

				TrafficLightControlBoxTileEntity box = (TrafficLightControlBoxTileEntity) te;
				EnumFacing facing = EnumFacing.getHorizontal(msg.facingOrdinal & 0x3);
				ApproachMovementSettings settings = new ApproachMovementSettings();
				settings.straightEnabled = msg.straightEnabled;
				settings.leftEnabled = msg.leftEnabled;
				settings.rightEnabled = msg.rightEnabled;
				settings.sharedTurns = msg.sharedTurns;
				settings.straightIdle = IdleBulbMode.fromLegacyOrdinal(msg.straightIdle & 0xFF, true);
				settings.leftIdle = IdleBulbMode.fromLegacyOrdinal(msg.leftIdle & 0xFF, false);
				settings.rightIdle = IdleBulbMode.fromLegacyOrdinal(msg.rightIdle & 0xFF, false);
				settings.leftFya = FyaMode.fromOrdinal(msg.leftFya);
				settings.rightFya = FyaMode.fromOrdinal(msg.rightFya);
				box.setMovementSettings(facing, settings);
				box.markDirty();
				world.notifyBlockUpdate(box.getPos(), world.getBlockState(box.getPos()), world.getBlockState(box.getPos()), 3);
				box.getAutomator().reset();
			});
			return null;
		}
	}
}
