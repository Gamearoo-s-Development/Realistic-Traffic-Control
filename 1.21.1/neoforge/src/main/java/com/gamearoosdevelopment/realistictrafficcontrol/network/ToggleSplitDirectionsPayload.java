package com.gamearoosdevelopment.realistictrafficcontrol.network;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** C2S: port of 1.12.2 {@code PacketToggleSplitDirections}. */
public record ToggleSplitDirectionsPayload(BlockPos pos, boolean enabled) implements CustomPacketPayload {

    public static final Type<ToggleSplitDirectionsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, "toggle_split_directions"));

    public static final StreamCodec<FriendlyByteBuf, ToggleSplitDirectionsPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ToggleSplitDirectionsPayload::pos,
            ByteBufCodecs.BOOL, ToggleSplitDirectionsPayload::enabled,
            ToggleSplitDirectionsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
