package com.gamearoosdevelopment.realistictrafficcontrol.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.minecraftforge.fml.relauncher.Side;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightControlBoxTileEntity;

public class PacketToggleNightFlash implements IMessage {
    private BlockPos pos;
    private boolean enabled;

    public PacketToggleNightFlash() {}

    public PacketToggleNightFlash(BlockPos pos, boolean enabled) {
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

    public static class Handler implements IMessageHandler<PacketToggleNightFlash, IMessage> {
        @Override
        public IMessage onMessage(PacketToggleNightFlash msg, MessageContext ctx) {
            MinecraftServer server = ctx.getServerHandler().player.getServer();
            server.addScheduledTask(() -> {
                World world = ctx.getServerHandler().player.world;
                TileEntity te = world.getTileEntity(msg.pos);
                if (te instanceof TrafficLightControlBoxTileEntity) {
                    ((TrafficLightControlBoxTileEntity) te).setNightFlashEnabled(msg.enabled);
                }
            });
            return null;
        }
    }
}
