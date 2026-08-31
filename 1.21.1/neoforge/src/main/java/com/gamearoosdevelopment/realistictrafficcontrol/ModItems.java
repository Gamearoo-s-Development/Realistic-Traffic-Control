package com.gamearoosdevelopment.realistictrafficcontrol;

import com.gamearoosdevelopment.realistictrafficcontrol.gui.FrameGuiType;
import com.gamearoosdevelopment.realistictrafficcontrol.item.BaseItemTrafficLightFrame;
import com.gamearoosdevelopment.realistictrafficcontrol.item.CoverHookItem;
import com.gamearoosdevelopment.realistictrafficcontrol.item.CrossingRelayBoxItem;
import com.gamearoosdevelopment.realistictrafficcontrol.item.CrossingRelayTunerItem;
import com.gamearoosdevelopment.realistictrafficcontrol.item.ItemCone;
import com.gamearoosdevelopment.realistictrafficcontrol.item.ItemStreetSign;
import com.gamearoosdevelopment.realistictrafficcontrol.item.ConcreteBarrierBlockItem;
import com.gamearoosdevelopment.realistictrafficcontrol.item.ScrewdriverItem;
import com.gamearoosdevelopment.realistictrafficcontrol.item.TrafficLightBulbItem;
import com.gamearoosdevelopment.realistictrafficcontrol.item.TrafficLightCardItem;
import com.gamearoosdevelopment.realistictrafficcontrol.item.WireCutterItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Item registry for the 1.21.1 port. Replaces the 1.12.2 {@code ModItems} {@code @ObjectHolder} fields
 * and {@code CommonProxy.registerItems} with {@link DeferredRegister}.
 *
 * <p>OpenComputers items are intentionally omitted. CC:Tweaked traffic-light cards are registered when
 * ComputerCraft is present.
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ModRealisticTrafficControl.MODID);

    // --- tools ---
    public static final DeferredItem<CoverHookItem> COVER_HOOK =
            ITEMS.register("cover_hook", () -> new CoverHookItem(new Item.Properties()));
    public static final DeferredItem<WireCutterItem> WIRE_CUTTER =
            ITEMS.register("wire_cutter", () -> new WireCutterItem(new Item.Properties()));
    public static final DeferredItem<ScrewdriverItem> SCREWDRIVER =
            ITEMS.register("screwdriver", () -> new ScrewdriverItem(new Item.Properties()));
    public static final DeferredItem<CrossingRelayTunerItem> CROSSING_RELAY_TUNER =
            ITEMS.register("crossing_relay_tuner", () -> new CrossingRelayTunerItem(new Item.Properties()));
    public static final DeferredItem<CrossingRelayBoxItem> CROSSING_RELAY_BOX =
            ITEMS.register("crossing_relay_box", () -> new CrossingRelayBoxItem(new Item.Properties()));

    public static final DeferredItem<TrafficLightCardItem> TRAFFIC_LIGHT_CARD =
            ITEMS.register("traffic_light_card", () -> new TrafficLightCardItem(new Item.Properties()));

    // --- block items for the simple blocks ---
    public static final DeferredItem<BlockItem> POLE = simpleBlockItem("pole", ModBlocks.POLE);
    public static final DeferredItem<BlockItem> WOOD_POLE = simpleBlockItem("wood_pole", ModBlocks.WOOD_POLE);
    public static final DeferredItem<BlockItem> PLUS_POLE = simpleBlockItem("plus_pole", ModBlocks.PLUS_POLE);
    public static final DeferredItem<BlockItem> T_POLE = simpleBlockItem("t_pole", ModBlocks.T_POLE);
    public static final DeferredItem<BlockItem> D_POLE = simpleBlockItem("d_pole", ModBlocks.D_POLE);
    public static final DeferredItem<BlockItem> DH_POLE = simpleBlockItem("dh_pole", ModBlocks.DH_POLE);
    public static final DeferredItem<BlockItem> C_POLE = simpleBlockItem("c_pole", ModBlocks.C_POLE);
    public static final DeferredItem<BlockItem> CH_POLE = simpleBlockItem("ch_pole", ModBlocks.CH_POLE);
    public static final DeferredItem<BlockItem> H_POLE = simpleBlockItem("h_pole", ModBlocks.H_POLE);
    public static final DeferredItem<BlockItem> U_T_POLE = simpleBlockItem("u_t_pole", ModBlocks.U_T_POLE);
    public static final DeferredItem<BlockItem> POLE_BASE = simpleBlockItem("pole_base", ModBlocks.POLE_BASE);
    public static final DeferredItem<BlockItem> STAND = simpleBlockItem("stand", ModBlocks.STAND);
    public static final DeferredItem<BlockItem> GENERATOR = simpleBlockItem("generator", ModBlocks.GENERATOR);
    public static final DeferredItem<BlockItem> TAG = simpleBlockItem("tag", ModBlocks.TAG);
    public static final DeferredItem<BlockItem> HORIZONTAL_POLE = simpleBlockItem("horizontal_pole", ModBlocks.HORIZONTAL_POLE);

    public static final DeferredItem<ItemCone> CONE =
            ITEMS.register("cone", () -> new ItemCone(ModBlocks.CONE.get(), new Item.Properties()));
    public static final DeferredItem<ItemCone> CHANNELIZER =
            ITEMS.register("channelizer", () -> new ItemCone(ModBlocks.CHANNELIZER.get(), new Item.Properties()));
    public static final DeferredItem<ItemCone> DRUM =
            ITEMS.register("drum", () -> new ItemCone(ModBlocks.DRUM.get(), new Item.Properties()));

    // --- traffic-light bulb (single item; variant carried in the bulb_type component) ---
    public static final DeferredItem<TrafficLightBulbItem> TRAFFIC_LIGHT_BULB =
            ITEMS.register("traffic_light_bulb", () -> new TrafficLightBulbItem(new Item.Properties()));

    // --- traffic-light frame items (carry bulb config via the frame_data component) ---
    private static DeferredItem<BaseItemTrafficLightFrame> frame(String name, FrameGuiType layout,
            java.util.function.Supplier<? extends net.minecraft.world.level.block.Block> baseBlock, String typeLabel) {
        return frame(name, layout, baseBlock, typeLabel, false);
    }

    private static DeferredItem<BaseItemTrafficLightFrame> frame(String name, FrameGuiType layout,
            java.util.function.Supplier<? extends net.minecraft.world.level.block.Block> baseBlock, String typeLabel,
            boolean placesUpperHalf) {
        return ITEMS.register(name, () -> new BaseItemTrafficLightFrame(new Item.Properties(), layout.getBulbCount(),
                baseBlock, typeLabel, layout, placesUpperHalf));
    }

    public static final DeferredItem<BaseItemTrafficLightFrame> TRAFFIC_LIGHT_FRAME =
            frame("traffic_light_frame", FrameGuiType.STANDARD_3, () -> ModBlocks.TRAFFIC_LIGHT.get(), "Standard");
    public static final DeferredItem<BaseItemTrafficLightFrame> TRAFFIC_LIGHT_HOZ_FRAME =
            frame("traffic_light_hoz_frame", FrameGuiType.HORIZONTAL_3, () -> ModBlocks.TRAFFIC_LIGHT_HOZ.get(), "Horizontal");
    public static final DeferredItem<BaseItemTrafficLightFrame> TRAFFIC_LIGHT_1_FRAME =
            frame("traffic_light_1_frame", FrameGuiType.STANDARD_1, () -> ModBlocks.TRAFFIC_LIGHT_1.get(), "Standard");
    public static final DeferredItem<BaseItemTrafficLightFrame> TRAFFIC_LIGHT_2_FRAME =
            frame("traffic_light_2_frame", FrameGuiType.STANDARD_2, () -> ModBlocks.TRAFFIC_LIGHT_2.get(), "Standard");
    public static final DeferredItem<BaseItemTrafficLightFrame> TRAFFIC_LIGHT_2_HOZ_FRAME =
            frame("traffic_light_2_hoz_frame", FrameGuiType.HORIZONTAL_2, () -> ModBlocks.TRAFFIC_LIGHT_2_HOZ.get(), "Horizontal");
    public static final DeferredItem<BaseItemTrafficLightFrame> TRAFFIC_LIGHT_4_FRAME =
            frame("traffic_light_4_frame", FrameGuiType.STANDARD_4, () -> ModBlocks.TRAFFIC_LIGHT_4.get(), "Standard");
    public static final DeferredItem<BaseItemTrafficLightFrame> TRAFFIC_LIGHT_4_HOZ_FRAME =
            frame("traffic_light_4_hoz_frame", FrameGuiType.HORIZONTAL_4, () -> ModBlocks.TRAFFIC_LIGHT_4_HOZ.get(), "Horizontal");
    public static final DeferredItem<BaseItemTrafficLightFrame> TRAFFIC_LIGHT_5_FRAME =
            frame("traffic_light_5_frame", FrameGuiType.STANDARD_5, () -> ModBlocks.TRAFFIC_LIGHT_5.get(), "Standard", true);
    public static final DeferredItem<BaseItemTrafficLightFrame> TRAFFIC_LIGHT_5_HOZ_FRAME =
            frame("traffic_light_5_hoz_frame", FrameGuiType.HORIZONTAL_5, () -> ModBlocks.TRAFFIC_LIGHT_5_HOZ.get(), "Horizontal");
    public static final DeferredItem<BaseItemTrafficLightFrame> TRAFFIC_LIGHT_DOGHOUSE_FRAME =
            frame("traffic_light_doghouse_frame", FrameGuiType.DOGHOUSE_5, () -> ModBlocks.TRAFFIC_LIGHT_DOGHOUSE.get(), "Doghouse");
    public static final DeferredItem<BaseItemTrafficLightFrame> TRAFFIC_LIGHT_6_FRAME =
            frame("traffic_light_6_frame", FrameGuiType.T_4, () -> ModBlocks.TRAFFIC_LIGHT_6.get(), "T");
    public static final DeferredItem<BaseItemTrafficLightFrame> TRAFFIC_LIGHT_7_FRAME =
            frame("traffic_light_7_frame", FrameGuiType.HAWK_3, () -> ModBlocks.TRAFFIC_LIGHT_7.get(), "HAWK Beacon");
    public static final DeferredItem<BaseItemTrafficLightFrame> TRAFFIC_LIGHT_8_FRAME =
            frame("traffic_light_8_frame", FrameGuiType.UPSIDE_DOWN_T_4, () -> ModBlocks.TRAFFIC_LIGHT_8.get(), "Upside-down T");

    // --- simple crossing decoratives ---
    public static final DeferredItem<BlockItem> CROSSING_GATE_POLE = simpleBlockItem("crossing_gate_pole", ModBlocks.CROSSING_GATE_POLE);
    public static final DeferredItem<BlockItem> QUIET_ZONE_SIGNAL = simpleBlockItem("quiet_zone_signal", ModBlocks.QUIET_ZONE_SIGNAL);
    public static final DeferredItem<BlockItem> GATE_GUARD = simpleBlockItem("gate_guard", ModBlocks.GATE_GUARD);

    // --- overhead span family + crossbucks ---
    public static final DeferredItem<BlockItem> OVERHEAD = simpleBlockItem("overhead", ModBlocks.OVERHEAD);
    public static final DeferredItem<BlockItem> OVERHEAD_POLE = simpleBlockItem("overhead_pole", ModBlocks.OVERHEAD_POLE);
    public static final DeferredItem<BlockItem> OVERHEAD_CROSSBUCK = simpleBlockItem("overhead_crossbuck", ModBlocks.OVERHEAD_CROSSBUCK);
    public static final DeferredItem<BlockItem> CROSSING_GATE_CROSSBUCK = simpleBlockItem("crossing_gate_crossbuck", ModBlocks.CROSSING_GATE_CROSSBUCK);

    // --- crossing gate / lamps / wig-wag ---
    public static final DeferredItem<BlockItem> CROSSING_GATE_BASE = simpleBlockItem("crossing_gate_base", ModBlocks.CROSSING_GATE_BASE);
    public static final DeferredItem<BlockItem> CROSSING_GATE_GATE = simpleBlockItem("crossing_gate_gate", ModBlocks.CROSSING_GATE_GATE);
    public static final DeferredItem<BlockItem> CROSSING_GATE_LAMPS = simpleBlockItem("crossing_gate_lamps", ModBlocks.CROSSING_GATE_LAMPS);
    public static final DeferredItem<BlockItem> PED_CROSSING_LAMPS = simpleBlockItem("ped_crossing_lamps", ModBlocks.PED_CROSSING_LAMPS);
    public static final DeferredItem<BlockItem> OVERHEAD_LAMPS = simpleBlockItem("overhead_lamps", ModBlocks.OVERHEAD_LAMPS);
    public static final DeferredItem<BlockItem> WIG_WAG = simpleBlockItem("wig_wag", ModBlocks.WIG_WAG);
    public static final DeferredItem<BlockItem> VERTICAL_WIG_WAG = simpleBlockItem("vertical_wig_wag", ModBlocks.VERTICAL_WIG_WAG);

    // --- barriers ---
    public static final DeferredItem<ConcreteBarrierBlockItem> CONCRETE_BARRIER =
            ITEMS.register("concrete_barrier",
                    () -> new ConcreteBarrierBlockItem(ModBlocks.CONCRETE_BARRIER.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> TYPE_3_BARRIER = simpleBlockItem("type_3_barrier", ModBlocks.TYPE_3_BARRIER);
    public static final DeferredItem<BlockItem> TYPE_3_BARRIER_RIGHT =
            simpleBlockItem("type_3_barrier_right", ModBlocks.TYPE_3_BARRIER_RIGHT);

    // --- signs / street signs / street lights ---
    public static final DeferredItem<BlockItem> SIGN = simpleBlockItem("sign", ModBlocks.SIGN);
    public static final DeferredItem<BlockItem> DIGITAL_SIGN = simpleBlockItem("digital_sign", ModBlocks.DIGITAL_SIGN);
    public static final DeferredItem<BlockItem> DIGITAL_SIGN_CONTROLLER =
            simpleBlockItem("digital_sign_controller", ModBlocks.DIGITAL_SIGN_CONTROLLER);
    public static final DeferredItem<BlockItem> MESSAGE_BOARD = simpleBlockItem("message_board", ModBlocks.MESSAGE_BOARD);
    public static final DeferredItem<BlockItem> MESSAGE_BOARD_CONTROLLER =
            simpleBlockItem("message_board_controller", ModBlocks.MESSAGE_BOARD_CONTROLLER);
    public static final DeferredItem<ItemStreetSign> STREET_SIGN =
            ITEMS.register("street_sign", () -> new ItemStreetSign(ModBlocks.STREET_SIGN.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> STREET_LIGHT_SINGLE =
            simpleBlockItem("street_light_single", ModBlocks.STREET_LIGHT_SINGLE);
    public static final DeferredItem<BlockItem> STREET_LIGHT_DOUBLE =
            simpleBlockItem("street_light_double", ModBlocks.STREET_LIGHT_DOUBLE);

    // --- vehicle detection sensors ---
    public static final DeferredItem<BlockItem> TRAFFIC_SENSOR_LEFT = simpleBlockItem("traffic_sensor_left", ModBlocks.TRAFFIC_SENSOR_LEFT);
    public static final DeferredItem<BlockItem> TRAFFIC_SENSOR_STRAIGHT = simpleBlockItem("traffic_sensor_straight", ModBlocks.TRAFFIC_SENSOR_STRAIGHT);
    public static final DeferredItem<BlockItem> TRAFFIC_SENSOR_RIGHT = simpleBlockItem("traffic_sensor_right", ModBlocks.TRAFFIC_SENSOR_RIGHT);

    public static final DeferredItem<BlockItem> SHUNT_ISLAND = simpleBlockItem("shunt_island", ModBlocks.SHUNT_ISLAND);
    public static final DeferredItem<BlockItem> SHUNT_BORDER = simpleBlockItem("shunt_border", ModBlocks.SHUNT_BORDER);

    // --- control box + pedestrian button ---
    public static final DeferredItem<BlockItem> TRAFFIC_LIGHT_CONTROL_BOX = simpleBlockItem("traffic_light_control_box", ModBlocks.TRAFFIC_LIGHT_CONTROL_BOX);
    public static final DeferredItem<BlockItem> PEDESTRIAN_BUTTON = simpleBlockItem("pedestrian_button", ModBlocks.PEDESTRIAN_BUTTON);

    // --- bell / horn block items ---
    public static final DeferredItem<BlockItem> WCH_BELL = simpleBlockItem("wch_bell", ModBlocks.WCH_BELL);
    public static final DeferredItem<BlockItem> WAYSIDE_HORN = simpleBlockItem("wayside_horn", ModBlocks.WAYSIDE_HORN_BLOCK);
    public static final DeferredItem<BlockItem> WCH_MECHANICAL_BELL = simpleBlockItem("wch_mechanical_bell", ModBlocks.WCH_MECHANICAL_BELL);
    public static final DeferredItem<BlockItem> TEARDROP_BELL = simpleBlockItem("teardrop_bell", ModBlocks.TEARDROP_BELL);
    public static final DeferredItem<BlockItem> SAFETRAN_TYPE_1 = simpleBlockItem("safetran_type_1", ModBlocks.SAFETRAN_TYPE_1);
    public static final DeferredItem<BlockItem> SAFETRAN_TYPE_3 = simpleBlockItem("safetran_type_3", ModBlocks.SAFETRAN_TYPE_3);
    public static final DeferredItem<BlockItem> SAFETRAN_MECHANICAL = simpleBlockItem("safetran_mechanical", ModBlocks.SAFETRAN_MECHANICAL);

    public static final DeferredItem<BlockItem> TRAFFIC_RAIL = simpleBlockItem("traffic_rail", ModBlocks.TRAFFIC_RAIL);
    public static final DeferredItem<BlockItem> REDSTONE_SENSOR = simpleBlockItem("redstone_sensor", ModBlocks.REDSTONE_SENSOR);
    public static final DeferredItem<BlockItem> WIRE_ANCHOR = simpleBlockItem("wire_anchor", ModBlocks.WIRE_ANCHOR);

    private static DeferredItem<BlockItem> simpleBlockItem(String name,
            net.neoforged.neoforge.registries.DeferredBlock<?> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private ModItems() {
    }
}
