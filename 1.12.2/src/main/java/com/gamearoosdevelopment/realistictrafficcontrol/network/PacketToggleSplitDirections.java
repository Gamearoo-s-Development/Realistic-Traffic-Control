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

public class PacketToggleSplitDirections implements IMessage {
	private BlockPos pos;
	private boolean enabled;

	public PacketToggleSplitDirections() {}

	public PacketToggleSplitDirections(BlockPos pos, boolean enabled) {
		this.pos = pos;
		this.enabled = enabled;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeLong(pos.toLong());
		buf.writeBoolean(enabled);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		pos = BlockPos.fromLong(buf.readLong());
		enabled = buf.readBoolean();
	}

	public static class Handler implements IMessageHandler<PacketToggleSplitDirections, IMessage> {
		@Override
		public IMessage onMessage(PacketToggleSplitDirections msg, MessageContext ctx) {
			MinecraftServer server = ctx.getServerHandler().player.getServer();
			server.addScheduledTask(() -> {
				World world = ctx.getServerHandler().player.world;
				TileEntity te = world.getTileEntity(msg.pos);
				if (te instanceof TrafficLightControlBoxTileEntity) {
					((TrafficLightControlBoxTileEntity) te).setSplitDirectionsEnabled(msg.enabled);
				}
			});
			return null;
		}
	}
}
