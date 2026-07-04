package com.gamearoosdevelopment.realistictrafficcontrol.network;

import com.gamearoosdevelopment.realistictrafficcontrol.item.BaseItemTrafficLightFrame;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketTrafficLightFrameFacingUpdate implements IMessage {
	public byte facingOrdinal;

	public PacketTrafficLightFrameFacingUpdate() {}

	public PacketTrafficLightFrameFacingUpdate(EnumFacing facing) {
		this.facingOrdinal = (byte) (facing == null ? -1 : facing.getHorizontalIndex());
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		facingOrdinal = buf.readByte();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeByte(facingOrdinal);
	}

	public static class Handler implements IMessageHandler<PacketTrafficLightFrameFacingUpdate, IMessage> {
		@Override
		public IMessage onMessage(PacketTrafficLightFrameFacingUpdate message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> handle(message, ctx));
			return null;
		}

		private void handle(PacketTrafficLightFrameFacingUpdate packet, MessageContext ctx) {
			ItemStack stack = ctx.getServerHandler().player.getHeldItemMainhand();
			if (!(stack.getItem() instanceof BaseItemTrafficLightFrame)) {
				return;
			}

			BaseItemTrafficLightFrame frameItem = (BaseItemTrafficLightFrame) stack.getItem();
			EnumFacing facing = packet.facingOrdinal < 0 ? null : EnumFacing.getHorizontal(packet.facingOrdinal);
			frameItem.setConfiguredApproachFacing(stack, facing);
		}
	}
}
