package com.gamearoosdevelopment.realistictrafficcontrol.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightControlBoxTileEntity;

public class PacketToggleHawkBeacon implements IMessage {
    private BlockPos pos;
    private boolean enabled;

    public PacketToggleHawkBeacon() {}

    public PacketToggleHawkBeacon(BlockPos pos, boolean enabled) {
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

    public static class Handler implements IMessageHandler<PacketToggleHawkBeacon, IMessage> {
        @Override
        public IMessage onMessage(PacketToggleHawkBeacon msg, MessageContext ctx) {
            MinecraftServer server = ctx.getServerHandler().player.getServer();
            server.addScheduledTask(() -> {
                World world = ctx.getServerHandler().player.world;
                TileEntity te = world.getTileEntity(msg.pos);
                if (te instanceof TrafficLightControlBoxTileEntity) {
                    ((TrafficLightControlBoxTileEntity) te).setHawkBeaconEnabled(msg.enabled);
                }
            });
            return null;
        }
    }
}
