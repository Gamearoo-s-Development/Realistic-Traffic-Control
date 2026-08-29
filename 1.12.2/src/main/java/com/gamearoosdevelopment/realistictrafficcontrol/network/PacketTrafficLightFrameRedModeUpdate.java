package com.gamearoosdevelopment.realistictrafficcontrol.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Kept as a registered stub so existing save data with this packet ID does not cause desync. Does nothing. */
public class PacketTrafficLightFrameRedModeUpdate implements IMessage {
	public PacketTrafficLightFrameRedModeUpdate() {}

	@Override public void fromBytes(ByteBuf buf) { buf.readBoolean(); buf.readBoolean(); }
	@Override public void toBytes(ByteBuf buf) { buf.writeBoolean(false); buf.writeBoolean(false); }

	public static class Handler implements IMessageHandler<PacketTrafficLightFrameRedModeUpdate, IMessage> {
		@Override
		public IMessage onMessage(PacketTrafficLightFrameRedModeUpdate message, MessageContext ctx) {
			return null;
		}
	}
}
