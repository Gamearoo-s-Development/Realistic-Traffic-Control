package com.gamearoosdevelopment.realistictrafficcontrol.network;

import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightControlBoxTileEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketToggleApproachEnabled implements IMessage {
	private BlockPos pos;
	private byte facingOrdinal;
	private boolean enabled;

	public PacketToggleApproachEnabled() {}

	public PacketToggleApproachEnabled(BlockPos pos, EnumFacing facing, boolean enabled) {
		this.pos = pos;
		this.facingOrdinal = (byte) facing.ordinal();
		this.enabled = enabled;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeLong(pos.toLong());
		buf.writeByte(facingOrdinal);
		buf.writeBoolean(enabled);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		pos = BlockPos.fromLong(buf.readLong());
		facingOrdinal = buf.readByte();
		enabled = buf.readBoolean();
	}

	public static class Handler implements IMessageHandler<PacketToggleApproachEnabled, IMessage> {
		@Override
		public IMessage onMessage(PacketToggleApproachEnabled msg, MessageContext ctx) {
			MinecraftServer server = ctx.getServerHandler().player.getServer();
			server.addScheduledTask(() -> {
				World world = ctx.getServerHandler().player.world;
				TileEntity te = world.getTileEntity(msg.pos);
				if (!(te instanceof TrafficLightControlBoxTileEntity)) {
					return;
				}

				TrafficLightControlBoxTileEntity box = (TrafficLightControlBoxTileEntity) te;
				EnumFacing facing = EnumFacing.getFront((int) msg.facingOrdinal);
				switch (facing) {
					case NORTH:
						box.setNorth(msg.enabled);
						break;
					case SOUTH:
						box.setSouth(msg.enabled);
						break;
					case EAST:
						box.setEast(msg.enabled);
						break;
					case WEST:
						box.setWest(msg.enabled);
						break;
					default:
						break;
				}
				box.markDirty();
				world.notifyBlockUpdate(box.getPos(), world.getBlockState(box.getPos()), world.getBlockState(box.getPos()), 3);
				box.getAutomator().reset();
			});
			return null;
		}
	}
}
