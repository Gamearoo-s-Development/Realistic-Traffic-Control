package com.gamearoosdevelopment.realistictrafficcontrol.network;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** C2S: port of 1.12.2 {@code PacketToggleSplitAxis}. */
public record ToggleSplitAxisPayload(BlockPos pos, byte axis, boolean enabled) implements CustomPacketPayload {

    public static final byte AXIS_NS = 0;
    public static final byte AXIS_EW = 1;

    public static final Type<ToggleSplitAxisPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, "toggle_split_axis"));

    public static final StreamCodec<FriendlyByteBuf, ToggleSplitAxisPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ToggleSplitAxisPayload::pos,
            ByteBufCodecs.BYTE, ToggleSplitAxisPayload::axis,
            ByteBufCodecs.BOOL, ToggleSplitAxisPayload::enabled,
            ToggleSplitAxisPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
