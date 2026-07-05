package com.gamearoosdevelopment.realistictrafficcontrol.network;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S payload: port of 1.12.2 {@code PacketTrafficLightFrameFacingUpdate}. Sets the configured approach
 * facing (2D data value, or -1 for auto) on the frame item held by the sending player.
 */
public record FrameFacingUpdatePayload(int facingIndex) implements CustomPacketPayload {

    public static final Type<FrameFacingUpdatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, "frame_facing_update"));

    public static final StreamCodec<FriendlyByteBuf, FrameFacingUpdatePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.INT, FrameFacingUpdatePayload::facingIndex, FrameFacingUpdatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
