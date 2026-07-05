package com.gamearoosdevelopment.realistictrafficcontrol.item;

import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockBaseTrafficLight;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockPedestrianButton;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.TrafficSensorBlock;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.ShuntBorderBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.ShuntIslandBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.crossing.ICrossingGateBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.crossing.ICrossingLampBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.crossing.IWigWagBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.BellBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.PedestrianButtonBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.RelayBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightControlBoxBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.util.CustomAngleCalculator;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.VerticalWigWagBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Port of 1.12.2 {@code ItemCrossingRelayTuner}. Pairs bells (and stores gate/lamp/wigwag/shunt pairings
 * on the relay) for crossing-relay and traffic-light control-box orchestration.
 */
public class CrossingRelayTunerItem extends Item {

    public CrossingRelayTunerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        BlockPos pos = context.getClickedPos();
        BlockEntity selected = level.getBlockEntity(pos);
        ItemStack stack = context.getItemInHand();

        if (!performPairCheck(player, stack, selected, level)) {
            return InteractionResult.SUCCESS;
        }

        int[] relayPosArray = getPairingPos(stack);
        if (relayPosArray == null) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pairedPos = new BlockPos(relayPosArray[0], relayPosArray[1], relayPosArray[2]);
        BlockEntity paired = level.getBlockEntity(pairedPos);

        if (selected != null) {
            checkUseOnBlockEntity(level, selected, paired, player);
        } else {
            checkUseOnBlock(level, pos, paired, player);
        }

        return InteractionResult.SUCCESS;
    }

    private void checkUseOnBlock(Level level, BlockPos pos, BlockEntity paired, Player player) {
        if (paired instanceof TrafficLightControlBoxBlockEntity controlBox) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof TrafficSensorBlock) {
                player.displayClientMessage(Component.literal(controlBox.addOrRemoveSensor(pos)
                        ? "Paired sensor to Traffic Light Control Box"
                        : "Unpaired sensor from Traffic Light Control Box"), false);
            }
        }
    }

    private boolean performPairCheck(Player player, ItemStack stack, BlockEntity te, Level level) {
        int[] pairingPos = getPairingPos(stack);

        if (pairingPos == null) {
            if (!(te instanceof RelayBlockEntity || te instanceof TrafficLightControlBoxBlockEntity)) {
                return false;
            }

            BlockPos relayPos;
            String typeOfPairing;
            if (te instanceof RelayBlockEntity relay) {
                RelayBlockEntity master = relay.getMaster(level);
                if (master == null) {
                    return false;
                }
                relayPos = master.getBlockPos();
                typeOfPairing = "Relay Box";
            } else {
                relayPos = te.getBlockPos();
                typeOfPairing = "Traffic Light Control Box";
            }

            setPairingPos(stack, relayPos);
            player.displayClientMessage(Component.literal("Started pairing with " + typeOfPairing + " at "
                    + relayPos.getX() + ", " + relayPos.getY() + ", " + relayPos.getZ()), false);
        } else {
            if (te instanceof RelayBlockEntity || te instanceof TrafficLightControlBoxBlockEntity) {
                BlockPos relayPos;
                String typeOfPairing;
                if (te instanceof RelayBlockEntity relayTE) {
                    RelayBlockEntity master = relayTE.getMaster(level);
                    if (master == null) {
                        return false;
                    }
                    relayPos = master.getBlockPos();
                    typeOfPairing = "Relay Box";
                } else {
                    relayPos = te.getBlockPos();
                    typeOfPairing = "Traffic Light Control Box";
                }

                clearPairingPos(stack);
                player.displayClientMessage(Component.literal("Stopped pairing with " + typeOfPairing + " at "
                        + pairingPos[0] + ", " + pairingPos[1] + ", " + pairingPos[2]), false);

                if (pairingPos[0] == relayPos.getX() && pairingPos[1] == relayPos.getY()
                        && pairingPos[2] == relayPos.getZ()) {
                    return false;
                }

                if (te instanceof RelayBlockEntity relayTE) {
                    RelayBlockEntity master = relayTE.getMaster(level);
                    if (master != null) {
                        setPairingPos(stack, master.getBlockPos());
                    }
                } else {
                    setPairingPos(stack, relayPos);
                }

                pairingPos = getPairingPos(stack);
                if (pairingPos != null) {
                    player.displayClientMessage(Component.literal("Started pairing with " + typeOfPairing + " at "
                            + pairingPos[0] + ", " + pairingPos[1] + ", " + pairingPos[2]), false);
                }
            } else if (te != null) {
                BlockPos pos = new BlockPos(pairingPos[0], pairingPos[1], pairingPos[2]);
                BlockEntity teAtPairingPos = level.getBlockEntity(pos);
                if (!(teAtPairingPos instanceof RelayBlockEntity
                        || teAtPairingPos instanceof TrafficLightControlBoxBlockEntity)) {
                    clearPairingPos(stack);
                    player.displayClientMessage(Component.literal("Could not find pair at "
                            + pairingPos[0] + ", " + pairingPos[1] + ", " + pairingPos[2] + ". Unpaired."), false);
                    return false;
                }
            }
        }

        return true;
    }

    private void checkUseOnBlockEntity(Level level, BlockEntity te, BlockEntity pairedTE, Player player) {
        if (pairedTE instanceof RelayBlockEntity relay) {
            RelayBlockEntity master = relay.getMaster(level);
            if (master == null) {
                return;
            }
            relay = master;

            if (te instanceof ICrossingGateBlockEntity) {
                player.displayClientMessage(Component.literal(relay.addOrRemoveCrossingGateGate(te.getBlockPos())
                        ? "Paired Crossing Gate to Relay Box"
                        : "Unpaired Crossing Gate from Relay Box"), false);
            }

            if (te instanceof IWigWagBlockEntity && isVerticalWigWag(te)) {
                player.displayClientMessage(Component.literal(relay.addOrRemoveVerticalWigWag(te.getBlockPos())
                        ? "Paired Vertical Wig Wag to Relay Box"
                        : "Unpaired Vertical Wig Wag from Relay Box"), false);
            } else if (te instanceof IWigWagBlockEntity) {
                player.displayClientMessage(Component.literal(relay.addOrRemoveWigWag(te.getBlockPos())
                        ? "Paired Wig Wag to Relay Box"
                        : "Unpaired Wig Wag from Relay Box"), false);
            }

            if (te instanceof BellBlockEntity) {
                player.displayClientMessage(Component.literal(relay.addOrRemoveBell(te.getBlockPos())
                        ? "Paired Bell to Relay Box"
                        : "Unpaired Bell from Relay Box"), false);
            }

            if (te instanceof ICrossingLampBlockEntity) {
                player.displayClientMessage(Component.literal(relay.addOrRemoveCrossingGateLamp(te.getBlockPos())
                        ? "Paired Crossing Lamps to Relay Box"
                        : "Unpaired Crossing Lamps from Relay Box"), false);
            }

            if (te instanceof ShuntBorderBlockEntity shuntBorder) {
                BlockState borderBlock = level.getBlockState(te.getBlockPos());
                Direction borderFacing = borderBlock.getValue(BlockStateProperties.HORIZONTAL_FACING);
                if (relay.addOrRemoveShuntBorder(shuntBorder.getTrackOrigin(), borderFacing)) {
                    shuntBorder.addPairedRelayBox(relay.getBlockPos());
                    player.displayClientMessage(Component.literal("Paired Border Shunt to Relay Box"), false);
                } else {
                    shuntBorder.removePairedRelayBox(relay.getBlockPos());
                    player.displayClientMessage(Component.literal("Unpaired Border Shunt from Relay Box"), false);
                }
            }

            if (te instanceof ShuntIslandBlockEntity shuntIsland) {
                BlockState islandBlock = level.getBlockState(te.getBlockPos());
                Direction islandFacing = islandBlock.getValue(BlockStateProperties.HORIZONTAL_FACING);
                if (relay.addOrRemoveShuntIsland(shuntIsland.getTrackOrigin(), islandFacing)) {
                    shuntIsland.addPairedRelayBox(relay.getBlockPos());
                    player.displayClientMessage(Component.literal("Paired Island Shunt to Relay Box"), false);
                } else {
                    shuntIsland.removePairedRelayBox(relay.getBlockPos());
                    player.displayClientMessage(Component.literal("Unpaired Island Shunt from Relay Box"), false);
                }
            }
        }

        if (pairedTE instanceof TrafficLightControlBoxBlockEntity controlBox) {
            if (te instanceof TrafficLightBlockEntity) {
                BlockState state = level.getBlockState(te.getBlockPos());
                if (state.getBlock() instanceof BlockBaseTrafficLight) {
                    int rotation = state.getValue(RTCProperties.ROTATION);
                    boolean pairedLight;
                    if (CustomAngleCalculator.isEast(rotation) || CustomAngleCalculator.isWest(rotation)) {
                        pairedLight = controlBox.addOrRemoveWestEastTrafficLight(te.getBlockPos());
                    } else {
                        pairedLight = controlBox.addOrRemoveNorthSouthTrafficLight(te.getBlockPos());
                    }
                    player.displayClientMessage(Component.literal(pairedLight
                            ? "Paired Traffic Light to Traffic Light Control Box"
                            : "Unpaired Traffic Light from Traffic Light Control Box"), false);
                }
            }

            if (te instanceof PedestrianButtonBlockEntity pedTE) {
                BlockState state = level.getBlockState(te.getBlockPos());
                if (state.getBlock() instanceof BlockPedestrianButton) {
                    int rotation = state.getValue(RTCProperties.ROTATION);
                    boolean operationResult;
                    if (!CustomAngleCalculator.isNorthSouth(rotation)) {
                        operationResult = controlBox.addOrRemoveNorthSouthPedButton(te.getBlockPos());
                    } else {
                        operationResult = controlBox.addOrRemoveWestEastPedButton(te.getBlockPos());
                    }
                    if (operationResult) {
                        pedTE.addPairedBox(controlBox.getBlockPos());
                        player.displayClientMessage(Component.literal("Paired Pedestrian Button to Traffic Light Control Box"), false);
                    } else {
                        pedTE.removePairedBox(controlBox.getBlockPos());
                        player.displayClientMessage(Component.literal("Unpaired Pedestrian Button from Traffic Light Control Box"), false);
                    }
                }
            }
        }
    }

    /** Distinguish vertical vs horizontal wig-wags. */
    private static boolean isVerticalWigWag(BlockEntity te) {
        return te instanceof VerticalWigWagBlockEntity;
    }

    private static int[] getPairingPos(ItemStack stack) {
        var tag = stack.get(com.gamearoosdevelopment.realistictrafficcontrol.RTCDataComponents.TUNER_PAIRING_DATA.get());
        if (tag == null || !tag.contains("pairingpos")) {
            return null;
        }
        return tag.getIntArray("pairingpos");
    }

    private static void setPairingPos(ItemStack stack, BlockPos pos) {
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putIntArray("pairingpos", new int[] { pos.getX(), pos.getY(), pos.getZ() });
        stack.set(com.gamearoosdevelopment.realistictrafficcontrol.RTCDataComponents.TUNER_PAIRING_DATA.get(), tag);
    }

    private static void clearPairingPos(ItemStack stack) {
        stack.remove(com.gamearoosdevelopment.realistictrafficcontrol.RTCDataComponents.TUNER_PAIRING_DATA.get());
    }
}
