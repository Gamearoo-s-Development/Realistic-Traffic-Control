package com.gamearoosdevelopment.realistictrafficcontrol;

import java.util.List;
import java.util.function.Supplier;

import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BellBlock;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockBaseTrafficLight;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockCrossingGateBase;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockCrossingGateGate;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockCrossingGateLamps;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockCrossingRelayNE;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockCrossingRelayNW;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockCrossingRelaySE;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockCrossingRelaySW;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockCrossingRelayTopNE;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockCrossingRelayTopNW;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockCrossingRelayTopSE;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockCrossingRelayTopSW;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockOverheadLamps;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockPedCrossingLamps;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockPedestrianButton;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockRedstoneSensor;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockShuntBorder;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockShuntIsland;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficLightControlBox;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficLight5Upper;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockTrafficRail;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockVerticalWigWag;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockWireAnchor;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockSign;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockStreetLightDouble;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockStreetLightSingle;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockStreetSign;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockType3Barrier;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockType3BarrierRight;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.BlockWigWag;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.HorizontalPoleBlock;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.LightSourceBlock;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.RotatedBlock;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.TrafficSensorBlock;
import com.gamearoosdevelopment.realistictrafficcontrol.blocks.TrafficSensorBlock.SensorKind;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Block registry for the 1.21.1 port. Replaces the 1.12.2 {@code ModBlocks} {@code @ObjectHolder}
 * fields and {@code CommonProxy.registerBlocks} with {@link DeferredRegister}.
 *
 * <p>NOTE: This currently registers the "simple" (non-tile-entity) blocks that have been ported.
 * Tile-entity backed blocks (traffic lights, relays, bells, signs, crossings, sensors, barriers,
 * street lights, wire anchor, ...) are added as their block-entity subsystems are ported.
 */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(ModRealisticTrafficControl.MODID);

    private static final VoxelShape POLE_SHAPE = Block.box(6.92, 0, 6.92, 8.92, 16, 8.92);
    private static final VoxelShape POLE_BASE_SHAPE = Block.box(6, 0, 6, 10, 16, 10);
    private static final VoxelShape CONE_SHAPE = Block.box(4.8, 0, 4.8, 11.2, 16, 11.2);

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of().strength(2f).noOcclusion().sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    private static DeferredBlock<RotatedBlock> rotated(String name, VoxelShape shape) {
        return rotated(name, shape, metal());
    }

    private static DeferredBlock<RotatedBlock> rotated(String name, VoxelShape shape, BlockBehaviour.Properties props) {
        return BLOCKS.register(name, () -> new RotatedBlock(props, shape));
    }

    // --- poles ---
    public static final DeferredBlock<RotatedBlock> POLE = rotated("pole", POLE_SHAPE);
    public static final DeferredBlock<RotatedBlock> WOOD_POLE = rotated("wood_pole", POLE_SHAPE);
    public static final DeferredBlock<RotatedBlock> PLUS_POLE = rotated("plus_pole", POLE_SHAPE);
    public static final DeferredBlock<RotatedBlock> T_POLE = rotated("t_pole", POLE_SHAPE);
    public static final DeferredBlock<RotatedBlock> D_POLE = rotated("d_pole", POLE_SHAPE);
    public static final DeferredBlock<RotatedBlock> DH_POLE = rotated("dh_pole", POLE_SHAPE);
    public static final DeferredBlock<RotatedBlock> C_POLE = rotated("c_pole", POLE_SHAPE);
    public static final DeferredBlock<RotatedBlock> CH_POLE = rotated("ch_pole", POLE_SHAPE);
    public static final DeferredBlock<RotatedBlock> H_POLE = rotated("h_pole", POLE_SHAPE);
    public static final DeferredBlock<RotatedBlock> U_T_POLE = rotated("u_t_pole", POLE_SHAPE);
    public static final DeferredBlock<RotatedBlock> POLE_BASE = rotated("pole_base", POLE_BASE_SHAPE);
    public static final DeferredBlock<RotatedBlock> STAND = rotated("stand", POLE_BASE_SHAPE);
    public static final DeferredBlock<RotatedBlock> GENERATOR = rotated("generator", POLE_BASE_SHAPE);
    public static final DeferredBlock<RotatedBlock> TAG = rotated("tag", POLE_SHAPE);

    public static final DeferredBlock<HorizontalPoleBlock> HORIZONTAL_POLE =
            BLOCKS.register("horizontal_pole", () -> new HorizontalPoleBlock(metal()));

    // --- traffic control markers / decoratives ---
    public static final DeferredBlock<RotatedBlock> CONE =
            rotated("cone", CONE_SHAPE, BlockBehaviour.Properties.of().strength(1f).noOcclusion());
    public static final DeferredBlock<RotatedBlock> CHANNELIZER =
            rotated("channelizer", CONE_SHAPE, BlockBehaviour.Properties.of().strength(1f).noOcclusion());
    public static final DeferredBlock<RotatedBlock> DRUM =
            rotated("drum", CONE_SHAPE, BlockBehaviour.Properties.of().strength(1f).noOcclusion());

    public static final DeferredBlock<LightSourceBlock> LIGHT_SOURCE = BLOCKS.register("light_source",
            () -> new LightSourceBlock(BlockBehaviour.Properties.of()
                    .replaceable().noCollission().instabreak().noOcclusion().lightLevel(s -> 15)));

    // --- simple crossing decoratives (no block entity) ---
    private static final VoxelShape SIGNAL_SHAPE = Block.box(6, 0, 6, 10, 16, 10);

    public static final DeferredBlock<RotatedBlock> CROSSING_GATE_POLE = rotated("crossing_gate_pole", POLE_SHAPE);
    public static final DeferredBlock<RotatedBlock> QUIET_ZONE_SIGNAL = rotated("quiet_zone_signal", SIGNAL_SHAPE);
    public static final DeferredBlock<RotatedBlock> GATE_GUARD = rotated("gate_guard", CONE_SHAPE,
            BlockBehaviour.Properties.of().strength(1f).noOcclusion().sound(SoundType.STONE).requiresCorrectToolForDrops());

    // --- overhead span family + crossbucks (simple rotated decoratives) ---
    private static final VoxelShape OVERHEAD_BEAM = Block.box(0, 10, 6, 16, 16, 10);
    private static final VoxelShape PLATE_SHAPE = Block.box(0, 4, 7, 16, 16, 9);
    private static final VoxelShape CROSSBUCK_SHAPE = Block.box(2, 2, 7, 14, 14, 9);

    public static final DeferredBlock<RotatedBlock> OVERHEAD = rotated("overhead", OVERHEAD_BEAM);
    public static final DeferredBlock<RotatedBlock> OVERHEAD_POLE = rotated("overhead_pole", POLE_SHAPE);
    public static final DeferredBlock<RotatedBlock> OVERHEAD_CROSSBUCK = rotated("overhead_crossbuck", PLATE_SHAPE);
    public static final DeferredBlock<RotatedBlock> CROSSING_GATE_CROSSBUCK =
            rotated("crossing_gate_crossbuck", CROSSBUCK_SHAPE);

    // --- crossing gate / lamps / wig-wag (block-entity backed) ---
    public static final DeferredBlock<BlockCrossingGateBase> CROSSING_GATE_BASE =
            BLOCKS.register("crossing_gate_base", () -> new BlockCrossingGateBase(metal()));
    public static final DeferredBlock<BlockCrossingGateGate> CROSSING_GATE_GATE =
            BLOCKS.register("crossing_gate_gate", () -> new BlockCrossingGateGate(metal()));
    public static final DeferredBlock<BlockCrossingGateLamps> CROSSING_GATE_LAMPS =
            BLOCKS.register("crossing_gate_lamps", () -> new BlockCrossingGateLamps(metal()));
    public static final DeferredBlock<BlockPedCrossingLamps> PED_CROSSING_LAMPS =
            BLOCKS.register("ped_crossing_lamps", () -> new BlockPedCrossingLamps(metal()));
    public static final DeferredBlock<BlockOverheadLamps> OVERHEAD_LAMPS =
            BLOCKS.register("overhead_lamps", () -> new BlockOverheadLamps(metal()));
    public static final DeferredBlock<BlockWigWag> WIG_WAG =
            BLOCKS.register("wig_wag", () -> new BlockWigWag(metal()));
    public static final DeferredBlock<BlockVerticalWigWag> VERTICAL_WIG_WAG =
            BLOCKS.register("vertical_wig_wag", () -> new BlockVerticalWigWag(metal()));

    // --- barriers (dye tinting deferred until textures + color handler are ported) ---
    private static final VoxelShape BARRIER_SHAPE = Block.box(0, 0, 5, 16, 14, 11);

    public static final DeferredBlock<RotatedBlock> CONCRETE_BARRIER = rotated("concrete_barrier", BARRIER_SHAPE,
            BlockBehaviour.Properties.of().strength(2f).noOcclusion().sound(SoundType.STONE).requiresCorrectToolForDrops());

    // --- vehicle detection sensors (queried by the control box) ---
    private static final VoxelShape SENSOR_SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    private static DeferredBlock<TrafficSensorBlock> sensor(String name, SensorKind kind) {
        return BLOCKS.register(name, () -> new TrafficSensorBlock(metal(), SENSOR_SHAPE, kind));
    }

    public static final DeferredBlock<TrafficSensorBlock> TRAFFIC_SENSOR_LEFT = sensor("traffic_sensor_left", SensorKind.LEFT);
    public static final DeferredBlock<TrafficSensorBlock> TRAFFIC_SENSOR_STRAIGHT = sensor("traffic_sensor_straight", SensorKind.STRAIGHT);
    public static final DeferredBlock<TrafficSensorBlock> TRAFFIC_SENSOR_RIGHT = sensor("traffic_sensor_right", SensorKind.RIGHT);

    // --- control box + pedestrian button (automation engine) ---
    public static final DeferredBlock<BlockTrafficLightControlBox> TRAFFIC_LIGHT_CONTROL_BOX =
            BLOCKS.register("traffic_light_control_box", () -> new BlockTrafficLightControlBox(
                    BlockBehaviour.Properties.of().strength(2f).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final DeferredBlock<BlockPedestrianButton> PEDESTRIAN_BUTTON =
            BLOCKS.register("pedestrian_button", () -> new BlockPedestrianButton(metal()));

    // --- traffic-light frame blocks (placed via frame items, share one block entity type) ---
    private static DeferredBlock<BlockBaseTrafficLight> trafficLight(String name, int bulbCount,
            Supplier<? extends Item> frameItem) {
        return trafficLight(name, bulbCount, frameItem, () -> null);
    }

    private static DeferredBlock<BlockBaseTrafficLight> trafficLight(String name, int bulbCount,
            Supplier<? extends Item> frameItem, Supplier<Block> upperHalfBlock) {
        return BLOCKS.register(name, () -> new BlockBaseTrafficLight(
                BlockBehaviour.Properties.of().strength(2f).noOcclusion().sound(SoundType.METAL)
                        .requiresCorrectToolForDrops(),
                bulbCount, frameItem, upperHalfBlock));
    }

    public static final DeferredBlock<BlockBaseTrafficLight> TRAFFIC_LIGHT =
            trafficLight("traffic_light", 3, () -> ModItems.TRAFFIC_LIGHT_FRAME.get());
    public static final DeferredBlock<BlockBaseTrafficLight> TRAFFIC_LIGHT_HOZ =
            trafficLight("traffic_light_hoz", 3, () -> ModItems.TRAFFIC_LIGHT_HOZ_FRAME.get());
    public static final DeferredBlock<BlockBaseTrafficLight> TRAFFIC_LIGHT_1 =
            trafficLight("traffic_light_1", 1, () -> ModItems.TRAFFIC_LIGHT_1_FRAME.get());
    public static final DeferredBlock<BlockBaseTrafficLight> TRAFFIC_LIGHT_2 =
            trafficLight("traffic_light_2", 2, () -> ModItems.TRAFFIC_LIGHT_2_FRAME.get());
    public static final DeferredBlock<BlockBaseTrafficLight> TRAFFIC_LIGHT_2_HOZ =
            trafficLight("traffic_light_2_hoz", 2, () -> ModItems.TRAFFIC_LIGHT_2_HOZ_FRAME.get());
    public static final DeferredBlock<BlockBaseTrafficLight> TRAFFIC_LIGHT_4 =
            trafficLight("traffic_light_4", 4, () -> ModItems.TRAFFIC_LIGHT_4_FRAME.get());
    public static final DeferredBlock<BlockBaseTrafficLight> TRAFFIC_LIGHT_4_HOZ =
            trafficLight("traffic_light_4_hoz", 4, () -> ModItems.TRAFFIC_LIGHT_4_HOZ_FRAME.get());
    public static final DeferredBlock<BlockTrafficLight5Upper> TRAFFIC_LIGHT_5_UPPER =
            BLOCKS.register("traffic_light_5_upper", () -> new BlockTrafficLight5Upper(
                    BlockBehaviour.Properties.of().strength(2f).noOcclusion().sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()));
    public static final DeferredBlock<BlockBaseTrafficLight> TRAFFIC_LIGHT_5 =
            trafficLight("traffic_light_5", 5, () -> ModItems.TRAFFIC_LIGHT_5_FRAME.get(), () -> TRAFFIC_LIGHT_5_UPPER.get());
    public static final DeferredBlock<BlockBaseTrafficLight> TRAFFIC_LIGHT_5_HOZ =
            trafficLight("traffic_light_5_hoz", 5, () -> ModItems.TRAFFIC_LIGHT_5_HOZ_FRAME.get());
    public static final DeferredBlock<BlockBaseTrafficLight> TRAFFIC_LIGHT_DOGHOUSE =
            trafficLight("traffic_light_doghouse", 5, () -> ModItems.TRAFFIC_LIGHT_DOGHOUSE_FRAME.get());
    public static final DeferredBlock<BlockBaseTrafficLight> TRAFFIC_LIGHT_6 =
            trafficLight("traffic_light_6", 4, () -> ModItems.TRAFFIC_LIGHT_6_FRAME.get());
    public static final DeferredBlock<BlockBaseTrafficLight> TRAFFIC_LIGHT_7 =
            trafficLight("traffic_light_7", 3, () -> ModItems.TRAFFIC_LIGHT_7_FRAME.get());
    public static final DeferredBlock<BlockBaseTrafficLight> TRAFFIC_LIGHT_8 =
            trafficLight("traffic_light_8", 4, () -> ModItems.TRAFFIC_LIGHT_8_FRAME.get());

    // --- crossing bells / horns (share one block entity type) ---
    private static final VoxelShape BELL_TALL = Block.box(6, 0, 6, 10, 16, 10);
    private static final VoxelShape BELL_SHORT = Block.box(6, 0, 6, 10, 13, 10);
    private static final VoxelShape BELL_BOX_NS = Block.box(4.8, 0, 6, 11.2, 9.6, 10);
    private static final VoxelShape BELL_BOX_EW = Block.box(6, 0, 4.8, 10, 9.6, 11.2);

    private static DeferredBlock<BellBlock> bell(String name, Holder<SoundEvent> sound, VoxelShape ns, VoxelShape ew) {
        return BLOCKS.register(name, () -> new BellBlock(
                BlockBehaviour.Properties.of().strength(2f).noOcclusion().sound(SoundType.METAL)
                        .requiresCorrectToolForDrops(),
                sound, ns, ew));
    }

    public static final DeferredBlock<BellBlock> WCH_BELL =
            bell("wch_bell", ModSounds.WCH, BELL_TALL, BELL_TALL);
    public static final DeferredBlock<BellBlock> WAYSIDE_HORN_BLOCK =
            bell("wayside_horn", ModSounds.WAYSIDE_HORN, BELL_TALL, BELL_TALL);
    public static final DeferredBlock<BellBlock> WCH_MECHANICAL_BELL =
            bell("wch_mechanical_bell", ModSounds.WCH_MECHANICAL_BELL, BELL_BOX_NS, BELL_BOX_EW);
    public static final DeferredBlock<BellBlock> TEARDROP_BELL =
            bell("teardrop_bell", ModSounds.TEARDROP_BELL, BELL_BOX_NS, BELL_BOX_EW);
    public static final DeferredBlock<BellBlock> SAFETRAN_TYPE_1 =
            bell("safetran_type_1", ModSounds.SAFETRAN_TYPE_1, BELL_BOX_NS, BELL_BOX_EW);
    public static final DeferredBlock<BellBlock> SAFETRAN_TYPE_3 =
            bell("safetran_type_3", ModSounds.SAFETRAN_TYPE_3, BELL_SHORT, BELL_SHORT);
    public static final DeferredBlock<BellBlock> SAFETRAN_MECHANICAL =
            bell("safetran_mechanical", ModSounds.SAFETRAN_MECHANICAL, BELL_BOX_NS, BELL_BOX_EW);

    // --- crossing relay multiblock (eight segments, one block entity type) ---
    public static final DeferredBlock<BlockCrossingRelaySE> CROSSING_RELAY_SE =
            BLOCKS.register("crossing_relay_se", () -> new BlockCrossingRelaySE(metal()));
    public static final DeferredBlock<BlockCrossingRelaySW> CROSSING_RELAY_SW =
            BLOCKS.register("crossing_relay_sw", () -> new BlockCrossingRelaySW(metal()));
    public static final DeferredBlock<BlockCrossingRelayNW> CROSSING_RELAY_NW =
            BLOCKS.register("crossing_relay_nw", () -> new BlockCrossingRelayNW(metal()));
    public static final DeferredBlock<BlockCrossingRelayNE> CROSSING_RELAY_NE =
            BLOCKS.register("crossing_relay_ne", () -> new BlockCrossingRelayNE(metal()));
    public static final DeferredBlock<BlockCrossingRelayTopNE> CROSSING_RELAY_TOP_NE =
            BLOCKS.register("crossing_relay_top_ne", () -> new BlockCrossingRelayTopNE(metal()));
    public static final DeferredBlock<BlockCrossingRelayTopSE> CROSSING_RELAY_TOP_SE =
            BLOCKS.register("crossing_relay_top_se", () -> new BlockCrossingRelayTopSE(metal()));
    public static final DeferredBlock<BlockCrossingRelayTopSW> CROSSING_RELAY_TOP_SW =
            BLOCKS.register("crossing_relay_top_sw", () -> new BlockCrossingRelayTopSW(metal()));
    public static final DeferredBlock<BlockCrossingRelayTopNW> CROSSING_RELAY_TOP_NW =
            BLOCKS.register("crossing_relay_top_nw", () -> new BlockCrossingRelayTopNW(metal()));

    public static final DeferredBlock<BlockShuntIsland> SHUNT_ISLAND =
            BLOCKS.register("shunt_island", () -> new BlockShuntIsland(metal()));
    public static final DeferredBlock<BlockShuntBorder> SHUNT_BORDER =
            BLOCKS.register("shunt_border", () -> new BlockShuntBorder(metal()));

    public static final DeferredBlock<BlockTrafficRail> TRAFFIC_RAIL =
            BLOCKS.register("traffic_rail", () -> new BlockTrafficRail(metal()));
    public static final DeferredBlock<BlockRedstoneSensor> REDSTONE_SENSOR =
            BLOCKS.register("redstone_sensor", () -> new BlockRedstoneSensor(metal()));
    public static final DeferredBlock<BlockWireAnchor> WIRE_ANCHOR =
            BLOCKS.register("wire_anchor", () -> new BlockWireAnchor(metal()));

    public static final DeferredBlock<BlockSign> SIGN =
            BLOCKS.register("sign", () -> new BlockSign(metal()));
    public static final DeferredBlock<BlockStreetSign> STREET_SIGN =
            BLOCKS.register("street_sign", () -> new BlockStreetSign(metal()));
    public static final DeferredBlock<BlockStreetLightSingle> STREET_LIGHT_SINGLE =
            BLOCKS.register("street_light_single", () -> new BlockStreetLightSingle(
                    BlockBehaviour.Properties.of().strength(2f).noOcclusion().sound(SoundType.STONE)
                            .requiresCorrectToolForDrops()));
    public static final DeferredBlock<BlockStreetLightDouble> STREET_LIGHT_DOUBLE =
            BLOCKS.register("street_light_double", () -> new BlockStreetLightDouble(
                    BlockBehaviour.Properties.of().strength(2f).noOcclusion().sound(SoundType.STONE)
                            .requiresCorrectToolForDrops()));
    public static final DeferredBlock<BlockType3Barrier> TYPE_3_BARRIER =
            BLOCKS.register("type_3_barrier", () -> new BlockType3Barrier(
                    BlockBehaviour.Properties.of().strength(1f).noOcclusion().sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()));
    public static final DeferredBlock<BlockType3BarrierRight> TYPE_3_BARRIER_RIGHT =
            BLOCKS.register("type_3_barrier_right", () -> new BlockType3BarrierRight(
                    BlockBehaviour.Properties.of().strength(1f).noOcclusion().sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()));

    /** All bell / horn blocks, used to build the shared bell block entity type. */
    public static List<Block> bellBlocks() {
        return List.of(WCH_BELL.get(), WAYSIDE_HORN_BLOCK.get(), WCH_MECHANICAL_BELL.get(), TEARDROP_BELL.get(),
                SAFETRAN_TYPE_1.get(), SAFETRAN_TYPE_3.get(), SAFETRAN_MECHANICAL.get());
    }

    /** All crossing-relay multiblock segments, used to build the shared relay block entity type. */
    public static List<Block> relayBlocks() {
        return List.of(
                CROSSING_RELAY_SE.get(), CROSSING_RELAY_SW.get(), CROSSING_RELAY_NW.get(), CROSSING_RELAY_NE.get(),
                CROSSING_RELAY_TOP_NE.get(), CROSSING_RELAY_TOP_SE.get(), CROSSING_RELAY_TOP_SW.get(),
                CROSSING_RELAY_TOP_NW.get());
    }

    /** All traffic-light frame blocks, used to build the shared block entity type. */
    public static List<Block> trafficLightBlocks() {
        return List.of(
                TRAFFIC_LIGHT.get(), TRAFFIC_LIGHT_HOZ.get(), TRAFFIC_LIGHT_1.get(), TRAFFIC_LIGHT_2.get(),
                TRAFFIC_LIGHT_2_HOZ.get(), TRAFFIC_LIGHT_4.get(), TRAFFIC_LIGHT_4_HOZ.get(), TRAFFIC_LIGHT_5.get(),
                TRAFFIC_LIGHT_5_HOZ.get(), TRAFFIC_LIGHT_DOGHOUSE.get(), TRAFFIC_LIGHT_6.get(), TRAFFIC_LIGHT_7.get(),
                TRAFFIC_LIGHT_8.get());
    }

    /** All crossing-lamp blocks sharing one block entity type. */
    public static List<Block> lampBlocks() {
        return List.of(
                CROSSING_GATE_LAMPS.get(), PED_CROSSING_LAMPS.get(), OVERHEAD_LAMPS.get());
    }

    /** Type-3 barrier segments sharing one block entity type. */
    public static List<Block> type3BarrierBlocks() {
        return List.of(TYPE_3_BARRIER.get(), TYPE_3_BARRIER_RIGHT.get());
    }

    private ModBlocks() {
    }
}
