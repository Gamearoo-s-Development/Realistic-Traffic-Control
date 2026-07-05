package com.gamearoosdevelopment.realistictrafficcontrol.network;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.item.BaseItemTrafficLightFrame;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.SyncableBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightControlBoxBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Single {@link PayloadRegistrar} channel for the 1.21.1 port, replacing the 1.12.2
 * {@code SimpleNetworkWrapper} ({@code ModNetworkHandler} + {@code PacketHandler}). Each old
 * {@code IMessage} becomes a {@link net.minecraft.network.protocol.common.custom.CustomPacketPayload}.
 */
public final class RTCNetworking {

    private static final String VERSION = "1";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(ModRealisticTrafficControl.MODID).versioned(VERSION);
        registrar.playToServer(FrameFacingUpdatePayload.TYPE, FrameFacingUpdatePayload.STREAM_CODEC,
                RTCNetworking::handleFrameFacing);
        registrar.playToServer(FrameGuiUpdatePayload.TYPE, FrameGuiUpdatePayload.STREAM_CODEC,
                RTCNetworking::handleFrameGui);
        registrar.playToServer(SyncableTileEntityPayload.TYPE, SyncableTileEntityPayload.STREAM_CODEC,
                RTCNetworking::handleSyncable);
        registrar.playToServer(UpdateSignPayload.TYPE, UpdateSignPayload.STREAM_CODEC, UpdateSignPayload::handle);
        registrar.playToClient(SignPackCheckPayload.TYPE, SignPackCheckPayload.STREAM_CODEC,
                SignPackCheckPayload::handle);

        registrar.playToServer(ToggleNightFlashPayload.TYPE, ToggleNightFlashPayload.STREAM_CODEC,
                RTCNetworking::handleToggleNightFlash);
        registrar.playToServer(ToggleMainPayload.TYPE, ToggleMainPayload.STREAM_CODEC,
                RTCNetworking::handleToggleMain);
        registrar.playToServer(ToggleHawkBeaconPayload.TYPE, ToggleHawkBeaconPayload.STREAM_CODEC,
                RTCNetworking::handleToggleHawkBeacon);
        registrar.playToServer(ToggleSplitDirectionsPayload.TYPE, ToggleSplitDirectionsPayload.STREAM_CODEC,
                RTCNetworking::handleToggleSplitDirections);
        registrar.playToServer(ToggleSplitAxisPayload.TYPE, ToggleSplitAxisPayload.STREAM_CODEC,
                RTCNetworking::handleToggleSplitAxis);
        registrar.playToServer(ToggleApproachEnabledPayload.TYPE, ToggleApproachEnabledPayload.STREAM_CODEC,
                RTCNetworking::handleToggleApproachEnabled);
        registrar.playToServer(ToggleFyaNightOnlyPayload.TYPE, ToggleFyaNightOnlyPayload.STREAM_CODEC,
                RTCNetworking::handleToggleFyaNightOnly);
        registrar.playToServer(SetApproachMovementPayload.TYPE, SetApproachMovementPayload.STREAM_CODEC,
                RTCNetworking::handleSetApproachMovement);
    }

    private static void handleSyncable(SyncableTileEntityPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player.level().isLoaded(payload.pos())) {
                BlockEntity be = player.level().getBlockEntity(payload.pos());
                if (be instanceof SyncableBlockEntity syncable) {
                    syncable.handleClientToServerUpdateTag(payload.data(), player.level().registryAccess());
                }
            }
        });
    }

    private static void handleFrameFacing(FrameFacingUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof BaseItemTrafficLightFrame) {
                Direction facing = payload.facingIndex() < 0 ? null : Direction.from2DDataValue(payload.facingIndex());
                BaseItemTrafficLightFrame.setConfiguredApproachFacing(stack, facing);
            }
        });
    }

    private static void handleFrameGui(FrameGuiUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof BaseItemTrafficLightFrame) {
                BaseItemTrafficLightFrame.setAllowFlash(stack, payload.slotId(), payload.allowFlash());
            }
        });
    }

    private static void handleToggleNightFlash(ToggleNightFlashPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> applyControlBoxToggle(payload.pos(), context, box -> {
            box.setNightFlashEnabled(payload.enabled());
            box.getAutomator().reset();
        }));
    }

    private static void handleToggleMain(ToggleMainPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> applyControlBoxToggle(payload.pos(), context, box -> {
            box.setNorthMainEnabled(payload.enabled());
            box.getAutomator().reset();
        }));
    }

    private static void handleToggleHawkBeacon(ToggleHawkBeaconPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> applyControlBoxToggle(payload.pos(), context, box -> {
            box.setHawkBeaconEnabled(payload.enabled());
            box.getAutomator().reset();
        }));
    }

    private static void handleToggleSplitDirections(ToggleSplitDirectionsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> applyControlBoxToggle(payload.pos(), context, box -> {
            box.setSplitDirectionsEnabled(payload.enabled());
            box.getAutomator().reset();
        }));
    }

    private static void handleToggleSplitAxis(ToggleSplitAxisPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> applyControlBoxToggle(payload.pos(), context, box -> {
            if (payload.axis() == ToggleSplitAxisPayload.AXIS_NS) {
                box.setSplitNorthSouthEnabled(payload.enabled());
            } else if (payload.axis() == ToggleSplitAxisPayload.AXIS_EW) {
                box.setSplitWestEastEnabled(payload.enabled());
            }
            box.getAutomator().reset();
        }));
    }

    private static void handleToggleApproachEnabled(ToggleApproachEnabledPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> applyControlBoxToggle(payload.pos(), context, box -> {
            Direction facing = payload.facing();
            switch (facing) {
                case NORTH -> box.setNorth(payload.enabled());
                case SOUTH -> box.setSouth(payload.enabled());
                case EAST -> box.setEast(payload.enabled());
                case WEST -> box.setWest(payload.enabled());
                default -> {
                }
            }
            notifyBlockUpdate(box);
            box.getAutomator().reset();
        }));
    }

    private static void handleToggleFyaNightOnly(ToggleFyaNightOnlyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> applyControlBoxToggle(payload.pos(), context, box -> {
            box.setFyaNightOnlyEnabled(payload.enabled());
            box.getAutomator().reset();
        }));
    }

    private static void handleSetApproachMovement(SetApproachMovementPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> applyControlBoxToggle(payload.pos(), context, box -> {
            box.setMovementSettings(payload.facing(), payload.toSettings());
            notifyBlockUpdate(box);
            box.getAutomator().reset();
        }));
    }

    private static void applyControlBoxToggle(BlockPos pos, IPayloadContext context,
            java.util.function.Consumer<TrafficLightControlBoxBlockEntity> action) {
        Player player = context.player();
        if (!player.level().isLoaded(pos)) {
            return;
        }
        BlockEntity be = player.level().getBlockEntity(pos);
        if (be instanceof TrafficLightControlBoxBlockEntity box) {
            action.accept(box);
        }
    }

    private static void notifyBlockUpdate(TrafficLightControlBoxBlockEntity box) {
        if (box.getLevel() != null) {
            BlockState state = box.getLevel().getBlockState(box.getBlockPos());
            box.getLevel().sendBlockUpdated(box.getBlockPos(), state, state, 3);
        }
    }

    private RTCNetworking() {
    }
}
