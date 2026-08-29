package com.gamearoosdevelopment.realistictrafficcontrol.network;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** C2S: port of 1.12.2 {@code PacketToggleMain}. */
public record ToggleMainPayload(BlockPos pos, boolean enabled) implements CustomPacketPayload {

    public static final Type<ToggleMainPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, "toggle_main"));

    public static final StreamCodec<FriendlyByteBuf, ToggleMainPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ToggleMainPayload::pos,
            ByteBufCodecs.BOOL, ToggleMainPayload::enabled,
            ToggleMainPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
