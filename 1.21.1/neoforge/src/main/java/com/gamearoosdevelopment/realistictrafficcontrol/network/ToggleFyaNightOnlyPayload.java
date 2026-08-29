package com.gamearoosdevelopment.realistictrafficcontrol.network;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** C2S: port of 1.12.2 {@code PacketToggleFyaNightOnly}. */
public record ToggleFyaNightOnlyPayload(BlockPos pos, boolean enabled) implements CustomPacketPayload {

    public static final Type<ToggleFyaNightOnlyPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, "toggle_fya_night_only"));

    public static final StreamCodec<FriendlyByteBuf, ToggleFyaNightOnlyPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ToggleFyaNightOnlyPayload::pos,
            ByteBufCodecs.BOOL, ToggleFyaNightOnlyPayload::enabled,
            ToggleFyaNightOnlyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
