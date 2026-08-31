package com.gamearoosdevelopment.realistictrafficcontrol.network;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.util.ApproachMovementSettings;
import com.gamearoosdevelopment.realistictrafficcontrol.util.FyaMode;
import com.gamearoosdevelopment.realistictrafficcontrol.util.IdleBulbMode;

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
        boolean sharedTurns,
        boolean noOpposingRightWithLeft,
        byte straightIdle,
        byte leftIdle,
        byte rightIdle,
        byte leftFya,
        byte rightFya) implements CustomPacketPayload {

    public SetApproachMovementPayload(BlockPos pos, Direction facing, ApproachMovementSettings settings) {
        this(pos,
                (byte) facing.get2DDataValue(),
                settings.straightEnabled,
                settings.leftEnabled,
                settings.rightEnabled,
                settings.sharedTurns,
                settings.noOpposingRightWithLeft,
                (byte) settings.straightIdle.ordinal(),
                (byte) settings.leftIdle.ordinal(),
                (byte) settings.rightIdle.ordinal(),
                (byte) settings.leftFya.ordinal(),
                (byte) settings.rightFya.ordinal());
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
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readByte(),
                    buf.readByte(),
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
            buf.writeBoolean(payload.sharedTurns);
            buf.writeBoolean(payload.noOpposingRightWithLeft);
            buf.writeByte(payload.straightIdle);
            buf.writeByte(payload.leftIdle);
            buf.writeByte(payload.rightIdle);
            buf.writeByte(payload.leftFya);
            buf.writeByte(payload.rightFya);
        }
    };

    public Direction facing() {
        return Direction.from2DDataValue(facingIndex & 0x3);
    }

    public ApproachMovementSettings toSettings() {
        ApproachMovementSettings settings = new ApproachMovementSettings();
        settings.straightEnabled = straightEnabled;
        settings.leftEnabled = leftEnabled;
        settings.rightEnabled = rightEnabled;
        settings.sharedTurns = sharedTurns;
        settings.noOpposingRightWithLeft = noOpposingRightWithLeft;
        settings.straightIdle = IdleBulbMode.fromLegacyOrdinal(straightIdle & 0xFF, true);
        settings.leftIdle = IdleBulbMode.fromLegacyOrdinal(leftIdle & 0xFF, false);
        settings.rightIdle = IdleBulbMode.fromLegacyOrdinal(rightIdle & 0xFF, false);
        settings.leftFya = FyaMode.fromOrdinal(leftFya);
        settings.rightFya = FyaMode.fromOrdinal(rightFya);
        return settings;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
