package com.gamearoosdevelopment.realistictrafficcontrol.network;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** C2S: port of 1.12.2 {@code PacketToggleApproachEnabled}. */
public record ToggleApproachEnabledPayload(BlockPos pos, byte facingIndex, boolean enabled) implements CustomPacketPayload {

    public ToggleApproachEnabledPayload(BlockPos pos, Direction facing, boolean enabled) {
        this(pos, (byte) facing.ordinal(), enabled);
    }

    public static final Type<ToggleApproachEnabledPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, "toggle_approach_enabled"));

    public static final StreamCodec<FriendlyByteBuf, ToggleApproachEnabledPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ToggleApproachEnabledPayload::pos,
            ByteBufCodecs.BYTE, ToggleApproachEnabledPayload::facingIndex,
            ByteBufCodecs.BOOL, ToggleApproachEnabledPayload::enabled,
            ToggleApproachEnabledPayload::new);

    public Direction facing() {
        return Direction.values()[facingIndex & 0xFF];
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
