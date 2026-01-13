package com.gamearoosdevelopment.realistictrafficcontrol.network;

import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightControlBoxTileEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketToggleSplitAxis implements IMessage {
	public static final byte AXIS_NS = 0;
	public static final byte AXIS_EW = 1;

	private BlockPos pos;
	private byte axis;
	private boolean enabled;

	public PacketToggleSplitAxis() {}

	public PacketToggleSplitAxis(BlockPos pos, byte axis, boolean enabled) {
		this.pos = pos;
		this.axis = axis;
		this.enabled = enabled;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeLong(pos.toLong());
		buf.writeByte(axis);
		buf.writeBoolean(enabled);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		pos = BlockPos.fromLong(buf.readLong());
		axis = buf.readByte();
		enabled = buf.readBoolean();
	}

	public static class Handler implements IMessageHandler<PacketToggleSplitAxis, IMessage> {
		@Override
		public IMessage onMessage(PacketToggleSplitAxis msg, MessageContext ctx) {
			MinecraftServer server = ctx.getServerHandler().player.getServer();
			server.addScheduledTask(() -> {
				World world = ctx.getServerHandler().player.world;
				TileEntity te = world.getTileEntity(msg.pos);
				if (!(te instanceof TrafficLightControlBoxTileEntity)) {
					return;
				}

				TrafficLightControlBoxTileEntity box = (TrafficLightControlBoxTileEntity) te;
				if (msg.axis == AXIS_NS) {
					box.setSplitNorthSouthEnabled(msg.enabled);
				} else if (msg.axis == AXIS_EW) {
					box.setSplitWestEastEnabled(msg.enabled);
				}
			});
			return null;
		}
	}
}
