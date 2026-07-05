package com.gamearoosdevelopment.realistictrafficcontrol.network;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.SignBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.UUID;

/** C2S payload: port of 1.12.2 {@code PacketUpdateSign}. */
public record UpdateSignPayload(BlockPos pos, int legacyType, int variant, UUID id, ArrayList<String> textLines)
        implements CustomPacketPayload {

    public static final Type<UpdateSignPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, "update_sign"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateSignPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UpdateSignPayload decode(RegistryFriendlyByteBuf buf) {
                    BlockPos pos = buf.readBlockPos();
                    int legacyType = buf.readInt();
                    int variant = buf.readInt();
                    long most = buf.readLong();
                    long least = buf.readLong();
                    UUID signId = (most != 0 || least != 0) ? new UUID(most, least) : null;
                    int count = buf.readInt();
                    ArrayList<String> lines = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        lines.add(buf.readUtf());
                    }
                    return new UpdateSignPayload(pos, legacyType, variant, signId, lines);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, UpdateSignPayload payload) {
                    buf.writeBlockPos(payload.pos);
                    buf.writeInt(payload.legacyType);
                    buf.writeInt(payload.variant);
                    if (payload.id != null) {
                        buf.writeLong(payload.id.getMostSignificantBits());
                        buf.writeLong(payload.id.getLeastSignificantBits());
                    } else {
                        buf.writeLong(0);
                        buf.writeLong(0);
                    }
                    buf.writeInt(payload.textLines != null ? payload.textLines.size() : 0);
                    if (payload.textLines != null) {
                        for (String line : payload.textLines) {
                            buf.writeUtf(line != null ? line : "");
                        }
                    }
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateSignPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (!player.level().isLoaded(payload.pos)) {
                return;
            }
            BlockEntity be = player.level().getBlockEntity(payload.pos);
            if (be instanceof SignBlockEntity sign) {
                sign.applyUpdate(payload.legacyType, payload.variant, payload.id, payload.textLines);
                sign.setChanged();
                BlockState state = player.level().getBlockState(payload.pos);
                player.level().sendBlockUpdated(payload.pos, state, state, 3);
            }
        });
    }
}
