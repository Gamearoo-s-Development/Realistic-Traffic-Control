package com.gamearoosdevelopment.realistictrafficcontrol.network;

import com.gamearoosdevelopment.realistictrafficcontrol.gui.BaseTrafficLightFrameContainer;
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
			net.minecraft.entity.player.EntityPlayerMP player = ctx.getServerHandler().player;
			EnumFacing facing = packet.facingOrdinal < 0 ? null : EnumFacing.getHorizontal(packet.facingOrdinal);

			ItemStack stack = ItemStack.EMPTY;
			if (player.openContainer instanceof BaseTrafficLightFrameContainer) {
				stack = ((BaseTrafficLightFrameContainer) player.openContainer).getFrameStack();
			}
			if (stack.isEmpty()) {
				stack = player.inventory.getCurrentItem();
			}
			if (stack.isEmpty() || !(stack.getItem() instanceof BaseItemTrafficLightFrame)) {
				return;
			}

			BaseItemTrafficLightFrame frameItem = (BaseItemTrafficLightFrame) stack.getItem();
			frameItem.setConfiguredApproachFacing(stack, facing);

			// Keep held stack references in sync with the edited frame stack.
			ItemStack main = player.getHeldItemMainhand();
			if (!main.isEmpty() && main.getItem() instanceof BaseItemTrafficLightFrame) {
				frameItem.setConfiguredApproachFacing(main, facing);
			}
			ItemStack off = player.getHeldItemOffhand();
			if (!off.isEmpty() && off.getItem() instanceof BaseItemTrafficLightFrame) {
				((BaseItemTrafficLightFrame) off.getItem()).setConfiguredApproachFacing(off, facing);
			}

			player.inventory.markDirty();
			if (player.openContainer != null) {
				player.openContainer.detectAndSendChanges();
			}
		}
	}
}
