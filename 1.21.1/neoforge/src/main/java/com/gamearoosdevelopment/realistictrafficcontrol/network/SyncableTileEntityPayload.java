package com.gamearoosdevelopment.realistictrafficcontrol.network;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S payload: port of 1.12.2 {@code PacketSyncableTileEntity}. Carries a block position and an
 * NBT blob authored by a GUI on the client, applied to the {@code SyncableBlockEntity} at that position.
 */
public record SyncableTileEntityPayload(BlockPos pos, net.minecraft.nbt.CompoundTag data)
        implements CustomPacketPayload {

    public static final Type<SyncableTileEntityPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, "syncable_tile_entity"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncableTileEntityPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SyncableTileEntityPayload::pos,
                    ByteBufCodecs.COMPOUND_TAG, SyncableTileEntityPayload::data,
                    SyncableTileEntityPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
