package com.gamearoosdevelopment.realistictrafficcontrol.network;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S payload: port of 1.12.2 {@code PacketTrafficLightFrameGuiUpdate}. Toggles the per-slot
 * "allow flash" flag on the frame item held by the sending player.
 */
public record FrameGuiUpdatePayload(int slotId, boolean allowFlash) implements CustomPacketPayload {

    public static final Type<FrameGuiUpdatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, "frame_gui_update"));

    public static final StreamCodec<FriendlyByteBuf, FrameGuiUpdatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FrameGuiUpdatePayload::slotId,
            ByteBufCodecs.BOOL, FrameGuiUpdatePayload::allowFlash,
            FrameGuiUpdatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
