package com.gamearoosdevelopment.realistictrafficcontrol;

import java.util.function.Supplier;

import com.gamearoosdevelopment.realistictrafficcontrol.item.TrafficLightCardItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Creative tab registry for the 1.21.1 port. Replaces the nine 1.12.2 {@code Tab*} {@code CreativeTabs}
 * subclasses in the {@code Iinvatory} package. The OpenComputers tab is intentionally dropped.
 *
 * <p>Items are added to their tab via {@code buildCreativeModeTabContents}. As more items are ported,
 * add them to the matching tab below.
 */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ModRealisticTrafficControl.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> POLES = TABS.register("poles_tab",
            () -> tab("poles_tab", ModItems.POLE, (params, output) -> {
                output.accept(ModItems.POLE.get());
                output.accept(ModItems.WOOD_POLE.get());
                output.accept(ModItems.PLUS_POLE.get());
                output.accept(ModItems.T_POLE.get());
                output.accept(ModItems.D_POLE.get());
                output.accept(ModItems.DH_POLE.get());
                output.accept(ModItems.C_POLE.get());
                output.accept(ModItems.CH_POLE.get());
                output.accept(ModItems.H_POLE.get());
                output.accept(ModItems.U_T_POLE.get());
                output.accept(ModItems.POLE_BASE.get());
                output.accept(ModItems.STAND.get());
                output.accept(ModItems.GENERATOR.get());
                        output.accept(ModItems.TAG.get());
                        output.accept(ModItems.WIRE_ANCHOR.get());
                        output.accept(ModItems.HORIZONTAL_POLE.get());
                        output.accept(ModItems.SIGN.get());
                        output.accept(ModItems.STREET_SIGN.get());
                        output.accept(ModItems.STREET_LIGHT_SINGLE.get());
                        output.accept(ModItems.STREET_LIGHT_DOUBLE.get());
                        output.accept(ModItems.CROSSING_GATE_POLE.get());
                        output.accept(ModItems.OVERHEAD.get());
                        output.accept(ModItems.OVERHEAD_POLE.get());
                        output.accept(ModItems.OVERHEAD_CROSSBUCK.get());
                    }));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TOOLS = TABS.register("tools_tab",
            () -> tab("tools_tab", ModItems.COVER_HOOK, (params, output) -> {
                output.accept(ModItems.COVER_HOOK.get());
                output.accept(ModItems.WIRE_CUTTER.get());
                output.accept(ModItems.SCREWDRIVER.get());
                output.accept(ModItems.CROSSING_RELAY_TUNER.get());
            }));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CONES = TABS.register("cones_tab",
            () -> tab("cones_tab", ModItems.CONE, (params, output) -> {
                output.accept(ModItems.CONE.get());
                output.accept(ModItems.CHANNELIZER.get());
                output.accept(ModItems.DRUM.get());
            }));

    // Reserved tabs (populated as their content is ported).
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CROSSING = TABS.register("crossing_tab",
            () -> tab("crossing_tab", ModItems.WCH_BELL, (params, output) -> {
                output.accept(ModItems.WCH_BELL.get());
                output.accept(ModItems.WCH_MECHANICAL_BELL.get());
                output.accept(ModItems.TEARDROP_BELL.get());
                output.accept(ModItems.SAFETRAN_TYPE_1.get());
                output.accept(ModItems.SAFETRAN_TYPE_3.get());
                output.accept(ModItems.SAFETRAN_MECHANICAL.get());
                output.accept(ModItems.WAYSIDE_HORN.get());
                output.accept(ModItems.GATE_GUARD.get());
                output.accept(ModItems.QUIET_ZONE_SIGNAL.get());
                output.accept(ModItems.CROSSING_GATE_CROSSBUCK.get());
                output.accept(ModItems.CROSSING_GATE_BASE.get());
                output.accept(ModItems.CROSSING_GATE_GATE.get());
                output.accept(ModItems.CROSSING_GATE_LAMPS.get());
                output.accept(ModItems.PED_CROSSING_LAMPS.get());
                output.accept(ModItems.OVERHEAD_LAMPS.get());
                output.accept(ModItems.WIG_WAG.get());
                output.accept(ModItems.VERTICAL_WIG_WAG.get());
                output.accept(ModItems.CROSSING_RELAY_BOX.get());
                output.accept(ModItems.SHUNT_ISLAND.get());
                output.accept(ModItems.SHUNT_BORDER.get());
                output.accept(ModItems.TRAFFIC_RAIL.get());
            }));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CC_TAB = TABS.register("cc_tab",
            () -> tab("cc_tab", ModItems.TRAFFIC_LIGHT_CARD, (params, output) -> {
                output.accept(TrafficLightCardItem.withTier(ModItems.TRAFFIC_LIGHT_CARD.get(), 0));
                output.accept(TrafficLightCardItem.withTier(ModItems.TRAFFIC_LIGHT_CARD.get(), 1));
                output.accept(TrafficLightCardItem.withTier(ModItems.TRAFFIC_LIGHT_CARD.get(), 2));
                output.accept(TrafficLightCardItem.withTier(ModItems.TRAFFIC_LIGHT_CARD.get(), 3));
            }));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FRAMES = TABS.register("frames_tab",
            () -> tab("frames_tab", ModItems.TRAFFIC_LIGHT_FRAME, (params, output) -> {
                output.accept(ModItems.TRAFFIC_LIGHT_FRAME.get());
                output.accept(ModItems.TRAFFIC_LIGHT_HOZ_FRAME.get());
                output.accept(ModItems.TRAFFIC_LIGHT_1_FRAME.get());
                output.accept(ModItems.TRAFFIC_LIGHT_2_FRAME.get());
                output.accept(ModItems.TRAFFIC_LIGHT_2_HOZ_FRAME.get());
                output.accept(ModItems.TRAFFIC_LIGHT_4_FRAME.get());
                output.accept(ModItems.TRAFFIC_LIGHT_4_HOZ_FRAME.get());
                output.accept(ModItems.TRAFFIC_LIGHT_5_FRAME.get());
                output.accept(ModItems.TRAFFIC_LIGHT_5_HOZ_FRAME.get());
                output.accept(ModItems.TRAFFIC_LIGHT_DOGHOUSE_FRAME.get());
                output.accept(ModItems.TRAFFIC_LIGHT_6_FRAME.get());
                output.accept(ModItems.TRAFFIC_LIGHT_7_FRAME.get());
                output.accept(ModItems.TRAFFIC_LIGHT_8_FRAME.get());
            }));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BULBS = TABS.register("bulbs_tab",
            () -> tab("bulbs_tab", ModItems.TRAFFIC_LIGHT_BULB, (params, output) -> {
                for (com.gamearoosdevelopment.realistictrafficcontrol.util.EnumTrafficLightBulbTypes type
                        : com.gamearoosdevelopment.realistictrafficcontrol.util.EnumTrafficLightBulbTypes.values()) {
                    output.accept(com.gamearoosdevelopment.realistictrafficcontrol.item.TrafficLightBulbItem
                            .of(ModItems.TRAFFIC_LIGHT_BULB.get(), type));
                }
            }));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BARRIERS = TABS.register("b_tab",
            () -> tab("b_tab", ModItems.CONCRETE_BARRIER, (params, output) -> {
                for (int dye = 0; dye < 16; dye++) {
                    output.accept(com.gamearoosdevelopment.realistictrafficcontrol.item.ConcreteBarrierBlockItem
                            .withDye(ModItems.CONCRETE_BARRIER.get(), dye));
                }
                output.accept(ModItems.TYPE_3_BARRIER.get());
                output.accept(ModItems.TYPE_3_BARRIER_RIGHT.get());
            }));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SENSORS = TABS.register("sensor_tab",
            () -> tab("sensor_tab", ModItems.TRAFFIC_SENSOR_STRAIGHT, (params, output) -> {
                output.accept(ModItems.TRAFFIC_SENSOR_LEFT.get());
                output.accept(ModItems.TRAFFIC_SENSOR_STRAIGHT.get());
                output.accept(ModItems.TRAFFIC_SENSOR_RIGHT.get());
                output.accept(ModItems.TRAFFIC_LIGHT_CONTROL_BOX.get());
                output.accept(ModItems.PEDESTRIAN_BUTTON.get());
                output.accept(ModItems.REDSTONE_SENSOR.get());
            }));

    private static CreativeModeTab tab(String key, Supplier<? extends ItemLike> icon,
            CreativeModeTab.DisplayItemsGenerator contents) {
        return CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.realistictrafficcontrol." + key))
                .icon(() -> new ItemStack(icon.get()))
                .displayItems(contents)
                .build();
    }

    private ModCreativeTabs() {
    }
}
