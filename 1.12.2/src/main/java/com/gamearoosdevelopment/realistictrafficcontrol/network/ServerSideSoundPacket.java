package com.gamearoosdevelopment.realistictrafficcontrol.network;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.proxy.ClientProxy;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ServerSideSoundPacket implements IMessage
{
	public BlockPos pos;
	public String modID = ModRealisticTrafficControl.MODID;
	public String soundName;
	public float volume = 1F;
	public float pitch = 1F;
	
	@Override
	public void fromBytes(ByteBuf buf) 
	{
		pos = BlockPos.fromLong(buf.readLong());
		modID = ByteBufUtils.readUTF8String(buf);
		soundName = ByteBufUtils.readUTF8String(buf);
		volume = buf.readFloat();
		pitch = buf.readFloat();
	}
	
	@Override
	public void toBytes(ByteBuf buf) 
	{
		buf.writeLong(pos.toLong());
		ByteBufUtils.writeUTF8String(buf, modID);
		ByteBufUtils.writeUTF8String(buf, soundName);
		buf.writeFloat(volume);
		buf.writeFloat(pitch);
	}
	
	public static class Handler implements IMessageHandler<ServerSideSoundPacket, IMessage>
	{
		@Override
		public IMessage onMessage(ServerSideSoundPacket message, MessageContext ctx)
		{
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> handle(message, ctx));
			return null;
		}
		
		private void handle(ServerSideSoundPacket message, MessageContext ctx)
		{
			try
			{
				ClientProxy.playSoundHandler(message, (javax.xml.ws.handler.MessageContext) ctx);
			}
			catch(NullPointerException ex)
			{
				ModRealisticTrafficControl.logger.error("[" + ModRealisticTrafficControl.MODID + "] An error occurred in " + Handler.class.getName());
				ModRealisticTrafficControl.logger.error(ex);
				ModRealisticTrafficControl.logger.error("[" + ModRealisticTrafficControl.MODID + "] Please report this error to us.");
			}
		}
	}
}
