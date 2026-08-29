package com.gamearoosdevelopment.realistictrafficcontrol;

import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockShuntBorder;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockShuntIsland;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.BellBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.CrossingGateGateBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.CrossingLampsBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.PedestrianButtonBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.RelayBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.RedstoneSensorBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.ShuntBaseBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.ShuntBorderBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.ShuntIslandBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightControlBoxBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.VerticalWigWagBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.WigWagBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.SignBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.StreetLightDoubleBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.StreetLightSingleBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.StreetSignBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.Type3BarrierBlockEntity;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.WireAnchorBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Block-entity type registry for the 1.21.1 port. Replaces the 1.12.2
 * {@code GameRegistry.registerTileEntity(...)} calls. Additional types (relays, bells, signs, crossings,
 * sensors, barriers, street lights, wire anchor) are added as their subsystems are ported.
 */
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ModRealisticTrafficControl.MODID);

    /**
     * Single shared type for every traffic-light frame block (1.12.2 had one tile-entity subclass per
     * bulb count; here the bulb count is read from the block).
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TrafficLightBlockEntity>> TRAFFIC_LIGHT =
            BLOCK_ENTITIES.register("traffic_light", () -> BlockEntityType.Builder.of(
                    TrafficLightBlockEntity::new, ModBlocks.trafficLightBlocks().toArray(new Block[0])).build(null));

    /** Single shared type for every crossing bell / horn block. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BellBlockEntity>> BELL =
            BLOCK_ENTITIES.register("bell", () -> BlockEntityType.Builder.of(
                    BellBlockEntity::new, ModBlocks.bellBlocks().toArray(new Block[0])).build(null));

    /** Traffic-light control box automation engine. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TrafficLightControlBoxBlockEntity>> TRAFFIC_LIGHT_CONTROL_BOX =
            BLOCK_ENTITIES.register("traffic_light_control_box", () -> BlockEntityType.Builder.of(
                    TrafficLightControlBoxBlockEntity::new, ModBlocks.TRAFFIC_LIGHT_CONTROL_BOX.get()).build(null));

    /** Pedestrian call button. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PedestrianButtonBlockEntity>> PEDESTRIAN_BUTTON =
            BLOCK_ENTITIES.register("pedestrian_button", () -> BlockEntityType.Builder.of(
                    PedestrianButtonBlockEntity::new, ModBlocks.PEDESTRIAN_BUTTON.get()).build(null));

    /** Crossing-relay multiblock orchestrator (eight block segments share one type). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RelayBlockEntity>> RELAY =
            BLOCK_ENTITIES.register("relay", () -> BlockEntityType.Builder.of(
                    RelayBlockEntity::new, ModBlocks.relayBlocks().toArray(new Block[0])).build(null));

    /** IR shunt blocks (island + border share one type). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShuntBaseBlockEntity>> SHUNT =
            BLOCK_ENTITIES.register("shunt", () -> BlockEntityType.Builder.of(
                    (pos, state) -> state.getBlock() instanceof BlockShuntIsland
                            ? new ShuntIslandBlockEntity(pos, state)
                            : new ShuntBorderBlockEntity(pos, state),
                    ModBlocks.SHUNT_ISLAND.get(), ModBlocks.SHUNT_BORDER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrossingGateGateBlockEntity>> CROSSING_GATE_GATE =
            BLOCK_ENTITIES.register("crossing_gate_gate", () -> BlockEntityType.Builder.of(
                    CrossingGateGateBlockEntity::new, ModBlocks.CROSSING_GATE_GATE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrossingLampsBlockEntity>> CROSSING_LAMPS =
            BLOCK_ENTITIES.register("crossing_lamps", () -> BlockEntityType.Builder.of(
                    CrossingLampsBlockEntity::new, ModBlocks.lampBlocks().toArray(new Block[0])).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WigWagBlockEntity>> WIG_WAG =
            BLOCK_ENTITIES.register("wig_wag", () -> BlockEntityType.Builder.of(
                    WigWagBlockEntity::new, ModBlocks.WIG_WAG.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VerticalWigWagBlockEntity>> VERTICAL_WIG_WAG =
            BLOCK_ENTITIES.register("vertical_wig_wag", () -> BlockEntityType.Builder.of(
                    VerticalWigWagBlockEntity::new, ModBlocks.VERTICAL_WIG_WAG.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RedstoneSensorBlockEntity>> REDSTONE_SENSOR =
            BLOCK_ENTITIES.register("redstone_sensor", () -> BlockEntityType.Builder.of(
                    RedstoneSensorBlockEntity::new, ModBlocks.REDSTONE_SENSOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WireAnchorBlockEntity>> WIRE_ANCHOR =
            BLOCK_ENTITIES.register("wire_anchor", () -> BlockEntityType.Builder.of(
                    WireAnchorBlockEntity::new, ModBlocks.WIRE_ANCHOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SignBlockEntity>> SIGN =
            BLOCK_ENTITIES.register("sign", () -> BlockEntityType.Builder.of(
                    SignBlockEntity::new, ModBlocks.SIGN.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StreetSignBlockEntity>> STREET_SIGN =
            BLOCK_ENTITIES.register("street_sign", () -> BlockEntityType.Builder.of(
                    StreetSignBlockEntity::new, ModBlocks.STREET_SIGN.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StreetLightSingleBlockEntity>> STREET_LIGHT_SINGLE =
            BLOCK_ENTITIES.register("street_light_single", () -> BlockEntityType.Builder.of(
                    StreetLightSingleBlockEntity::new, ModBlocks.STREET_LIGHT_SINGLE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StreetLightDoubleBlockEntity>> STREET_LIGHT_DOUBLE =
            BLOCK_ENTITIES.register("street_light_double", () -> BlockEntityType.Builder.of(
                    StreetLightDoubleBlockEntity::new, ModBlocks.STREET_LIGHT_DOUBLE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Type3BarrierBlockEntity>> TYPE_3_BARRIER =
            BLOCK_ENTITIES.register("type_3_barrier", () -> BlockEntityType.Builder.of(
                    Type3BarrierBlockEntity::new, ModBlocks.type3BarrierBlocks().toArray(new Block[0])).build(null));

    private ModBlockEntities() {
    }
}
