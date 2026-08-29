package com.gamearoosdevelopment.realistictrafficcontrol.network;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** C2S: port of 1.12.2 {@code PacketToggleHawkBeacon}. */
public record ToggleHawkBeaconPayload(BlockPos pos, boolean enabled) implements CustomPacketPayload {

    public static final Type<ToggleHawkBeaconPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, "toggle_hawk_beacon"));

    public static final StreamCodec<FriendlyByteBuf, ToggleHawkBeaconPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ToggleHawkBeaconPayload::pos,
            ByteBufCodecs.BOOL, ToggleHawkBeaconPayload::enabled,
            ToggleHawkBeaconPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
