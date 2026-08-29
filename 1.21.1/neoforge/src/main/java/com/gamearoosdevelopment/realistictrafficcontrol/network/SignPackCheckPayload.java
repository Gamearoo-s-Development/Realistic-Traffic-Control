package com.gamearoosdevelopment.realistictrafficcontrol.network;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** S2C payload: port of 1.12.2 {@code PacketSignPackCheck}. */
public record SignPackCheckPayload(Map<UUID, String> signPacks) implements CustomPacketPayload {

    public static final Type<SignPackCheckPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, "sign_pack_check"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SignPackCheckPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public SignPackCheckPayload decode(RegistryFriendlyByteBuf buf) {
                    int packs = buf.readInt();
                    Map<UUID, String> map = HashMap.newHashMap(packs);
                    for (int i = 0; i < packs; i++) {
                        map.put(new UUID(buf.readLong(), buf.readLong()), buf.readUtf());
                    }
                    return new SignPackCheckPayload(map);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, SignPackCheckPayload payload) {
                    buf.writeInt(payload.signPacks.size());
                    for (Map.Entry<UUID, String> entry : payload.signPacks.entrySet()) {
                        buf.writeLong(entry.getKey().getMostSignificantBits());
                        buf.writeLong(entry.getKey().getLeastSignificantBits());
                        buf.writeUtf(entry.getValue());
                    }
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SignPackCheckPayload payload,
            net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            var localPacks = ModRealisticTrafficControl.signRepo.getPacksByID();
            for (Map.Entry<UUID, String> serverPack : payload.signPacks.entrySet()) {
                if (!localPacks.containsKey(serverPack.getKey())) {
                    Minecraft.getInstance().player.displayClientMessage(Component.literal(
                            "You are missing Realistic Traffic Control signpack " + serverPack.getValue()
                                    + "! Some signs may display as ERROR."), false);
                }
            }
        });
    }
}
