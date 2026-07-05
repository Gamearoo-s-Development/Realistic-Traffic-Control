package com.gamearoosdevelopment.realistictrafficcontrol.network;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.util.ApproachMovementSettings;
import com.gamearoosdevelopment.realistictrafficcontrol.util.IdleBulbState;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** C2S: port of 1.12.2 {@code PacketSetApproachMovement}. */
public record SetApproachMovementPayload(
        BlockPos pos,
        byte facingIndex,
        boolean straightEnabled,
        boolean leftEnabled,
        boolean rightEnabled,
        byte straightIdle,
        byte leftIdle,
        byte rightIdle) implements CustomPacketPayload {

    public SetApproachMovementPayload(BlockPos pos, Direction facing, ApproachMovementSettings settings) {
        this(pos,
                (byte) facing.ordinal(),
                settings.straightEnabled,
                settings.leftEnabled,
                settings.rightEnabled,
                (byte) settings.straightIdle.ordinal(),
                (byte) settings.leftIdle.ordinal(),
                (byte) settings.rightIdle.ordinal());
    }

    public static final Type<SetApproachMovementPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, "set_approach_movement"));

    public static final StreamCodec<FriendlyByteBuf, SetApproachMovementPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SetApproachMovementPayload decode(FriendlyByteBuf buf) {
            return new SetApproachMovementPayload(
                    buf.readBlockPos(),
                    buf.readByte(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readByte(),
                    buf.readByte(),
                    buf.readByte());
        }

        @Override
        public void encode(FriendlyByteBuf buf, SetApproachMovementPayload payload) {
            buf.writeBlockPos(payload.pos);
            buf.writeByte(payload.facingIndex);
            buf.writeBoolean(payload.straightEnabled);
            buf.writeBoolean(payload.leftEnabled);
            buf.writeBoolean(payload.rightEnabled);
            buf.writeByte(payload.straightIdle);
            buf.writeByte(payload.leftIdle);
            buf.writeByte(payload.rightIdle);
        }
    };

    public Direction facing() {
        return Direction.values()[facingIndex & 0xFF];
    }

    public ApproachMovementSettings toSettings() {
        ApproachMovementSettings settings = new ApproachMovementSettings();
        settings.straightEnabled = straightEnabled;
        settings.leftEnabled = leftEnabled;
        settings.rightEnabled = rightEnabled;
        settings.straightIdle = IdleBulbState.fromOrdinal(straightIdle);
        settings.leftIdle = IdleBulbState.fromOrdinal(leftIdle);
        settings.rightIdle = IdleBulbState.fromOrdinal(rightIdle);
        return settings;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
