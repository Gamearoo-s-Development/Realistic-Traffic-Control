package com.gamearoosdevelopment.realistictrafficcontrol;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * NeoForge {@link ModConfigSpec} port of the 1.12.2 Forge {@code Configuration} based config.
 *
 * <p>Static mirror fields are refreshed from the spec whenever the config is (re)loaded, so the rest
 * of the codebase can keep reading plain static fields exactly like the 1.12.2 version did.
 */
public final class Config {

    public static final ModConfigSpec SPEC;

    // --- general ---
    private static final ModConfigSpec.IntValue PARALLEL_SCANS;
    private static final ModConfigSpec.IntValue ISLAND_TIMEOUT;
    private static final ModConfigSpec.IntValue BORDER_TIMEOUT;
    private static final ModConfigSpec.IntValue BORDER_TICK;
    private static final ModConfigSpec.IntValue CROSSING_BELL_STOP_AFTER_SECONDS;
    private static final ModConfigSpec.IntValue TOOLTIP_CHAR_WRAP_LENGTH;

    // --- traffic lights ---
    private static final ModConfigSpec.ConfigValue<List<? extends String>> SENSOR_CLASSES;
    private static final ModConfigSpec.IntValue SENSOR_SCAN_HEIGHT;
    private static final ModConfigSpec.DoubleValue HAWK_FLASH_YELLOW_SECONDS;
    private static final ModConfigSpec.DoubleValue HAWK_SOLID_YELLOW_SECONDS;
    private static final ModConfigSpec.DoubleValue HAWK_DEFAULT_SOLID_RED_SECONDS;
    private static final ModConfigSpec.DoubleValue HAWK_DEFAULT_FLASH_RED_SECONDS;
    private static final ModConfigSpec.IntValue HAWK_WIGWAG_PERIOD_TICKS;

    // --- computer craft ---
    private static final ModConfigSpec.IntValue TRAFFIC_LIGHT_CARD_T1_CAPACITY;
    private static final ModConfigSpec.IntValue TRAFFIC_LIGHT_CARD_T2_CAPACITY;
    private static final ModConfigSpec.IntValue TRAFFIC_LIGHT_CARD_T3_CAPACITY;
    private static final ModConfigSpec.DoubleValue CC_PERIPHERAL_ENERGY_COST;
    private static final ModConfigSpec.DoubleValue TRAFFIC_LIGHT_CARD_DRAW_PER_BLOCK;

    // --- static mirrors (read across the codebase) ---
    public static int parallelScans = 1;
    public static int islandTimeout = 20;
    public static int borderTimeout = 150;
    public static int borderTick = 10;
    public static int crossingBellStopAfterSeconds = 0;
    public static int tooltipCharWrapLength = 256;
    public static List<? extends String> sensorClasses = List.of(
            "net.minecraft.world.entity.animal.Pig",
            "net.minecraft.world.entity.animal.horse.Horse",
            "net.minecraft.world.entity.animal.horse.Donkey",
            "net.minecraft.world.entity.animal.horse.Mule",
            "net.minecraft.world.entity.animal.horse.SkeletonHorse",
            "net.minecraft.world.entity.animal.horse.ZombieHorse");
    public static int sensorScanHeight = 5;
    public static int trafficLightCardT1Capacity = 20;
    public static int trafficLightCardT2Capacity = 144;
    public static int trafficLightCardT3Capacity = 384;
    public static float ccPeripheralEnergyCost = 0.01F;
    public static float trafficLightCardDrawPerBlock = 0.01F;

    public static float hawkFlashYellowSeconds = 15.0F;
    public static float hawkSolidYellowSeconds = 3.0F;
    public static float hawkDefaultSolidRedSeconds = 5.0F;
    public static float hawkDefaultFlashRedSeconds = 7.0F;
    public static int hawkWigwagPeriodTicks = 20;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("General configuration").push("general");
        ISLAND_TIMEOUT = builder
                .comment("How far (in blocks) should each island shunt scan for the next island shunt?")
                .defineInRange("islandTimeout", 20, 1, 100);
        BORDER_TIMEOUT = builder
                .comment("How far (in blocks) should border shunts scan for the next island shunt?")
                .defineInRange("borderTimeout", 150, 1, 2000);
        BORDER_TICK = builder
                .comment("How far (in blocks) should border shunts scan per tick?")
                .defineInRange("borderTick", 10, 1, 2000);
        PARALLEL_SCANS = builder
                .comment("How many crossing relay boxes should be scanned per tick?")
                .defineInRange("parallelScans", 1, 1, 20);
        CROSSING_BELL_STOP_AFTER_SECONDS = builder
                .comment("Default bell stop time (seconds) for crossing relays. 0 = bells stay on until the crossing clears.")
                .defineInRange("crossingBellStopAfterSeconds", 0, 0, 3600);
        TOOLTIP_CHAR_WRAP_LENGTH = builder
                .comment("How many letters should be rendered in a tooltip before it wraps down to the next line?")
                .defineInRange("tooltipCharWrapLength", 256, 64, 5412);
        builder.pop();

        builder.comment("Stuff Related To Traffic Lights").push("traffic_lights");
        SENSOR_CLASSES = builder
                .comment("What entity classes will activate the traffic signal sensors?")
                .defineListAllowEmpty("sensorClasses", List.copyOf(sensorClasses), () -> "", o -> o instanceof String);
        SENSOR_SCAN_HEIGHT = builder
                .comment("How far up (in blocks) should traffic signal sensors scan for entities? [Min = 0, Max = 10, Default = 5]")
                .defineInRange("sensorScanHeight", 5, 0, 10);
        HAWK_FLASH_YELLOW_SECONDS = builder
                .comment("HAWK beacon: flashing yellow duration (seconds) before solid yellow.")
                .defineInRange("hawkFlashYellowSeconds", 15.0, 0.0, 600.0);
        HAWK_SOLID_YELLOW_SECONDS = builder
                .comment("HAWK beacon: solid yellow duration (seconds) before solid red.")
                .defineInRange("hawkSolidYellowSeconds", 3.0, 0.0, 60.0);
        HAWK_DEFAULT_SOLID_RED_SECONDS = builder
                .comment("Default for new controllers: HAWK solid-red WALK duration (seconds). Existing controllers keep their saved value.")
                .defineInRange("hawkDefaultSolidRedSeconds", 5.0, 0.0, 600.0);
        HAWK_DEFAULT_FLASH_RED_SECONDS = builder
                .comment("Default for new controllers: HAWK flashing-red clearance duration (seconds). Existing controllers keep their saved value.")
                .defineInRange("hawkDefaultFlashRedSeconds", 7.0, 0.0, 600.0);
        HAWK_WIGWAG_PERIOD_TICKS = builder
                .comment("HAWK beacon: wigwag alternation period (ticks). 20 ticks = 1 second.")
                .defineInRange("hawkWigwagPeriodTicks", 20, 1, 200);
        builder.pop();

        builder.comment("ComputerCraft integration").push("computer_craft");
        TRAFFIC_LIGHT_CARD_T1_CAPACITY = builder
                .comment("Maximum traffic lights on a tier-1 CC card.")
                .defineInRange("trafficLightCardT1Capacity", 20, 1, 1000);
        TRAFFIC_LIGHT_CARD_T2_CAPACITY = builder
                .comment("Maximum traffic lights on a tier-2 CC card.")
                .defineInRange("trafficLightCardT2Capacity", 144, 1, 1000);
        TRAFFIC_LIGHT_CARD_T3_CAPACITY = builder
                .comment("Maximum traffic lights on a tier-3 CC card.")
                .defineInRange("trafficLightCardT3Capacity", 384, 1, 10000);
        CC_PERIPHERAL_ENERGY_COST = builder
                .comment("How much energy (simulated) does a CC peripheral method call consume?")
                .defineInRange("ccPeripheralEnergyCost", 0.01, 0.0, Double.MAX_VALUE);
        TRAFFIC_LIGHT_CARD_DRAW_PER_BLOCK = builder
                .comment("How much OC energy the traffic-light card consumes times squared block distance.")
                .defineInRange("trafficLightCardDrawPerBlock", 0.01, 0.0, Double.MAX_VALUE);
        builder.pop();

        SPEC = builder.build();
    }

    private Config() {
    }

    /** Refresh the static mirror fields from the loaded spec. Called on config load/reload events. */
    public static void refresh() {
        parallelScans = PARALLEL_SCANS.get();
        islandTimeout = ISLAND_TIMEOUT.get();
        borderTimeout = BORDER_TIMEOUT.get();
        borderTick = BORDER_TICK.get();
        crossingBellStopAfterSeconds = CROSSING_BELL_STOP_AFTER_SECONDS.get();
        tooltipCharWrapLength = TOOLTIP_CHAR_WRAP_LENGTH.get();
        sensorClasses = SENSOR_CLASSES.get();
        sensorScanHeight = SENSOR_SCAN_HEIGHT.get();
        trafficLightCardT1Capacity = TRAFFIC_LIGHT_CARD_T1_CAPACITY.get();
        trafficLightCardT2Capacity = TRAFFIC_LIGHT_CARD_T2_CAPACITY.get();
        trafficLightCardT3Capacity = TRAFFIC_LIGHT_CARD_T3_CAPACITY.get();
        ccPeripheralEnergyCost = CC_PERIPHERAL_ENERGY_COST.get().floatValue();
        trafficLightCardDrawPerBlock = TRAFFIC_LIGHT_CARD_DRAW_PER_BLOCK.get().floatValue();
        hawkFlashYellowSeconds = HAWK_FLASH_YELLOW_SECONDS.get().floatValue();
        hawkSolidYellowSeconds = HAWK_SOLID_YELLOW_SECONDS.get().floatValue();
        hawkDefaultSolidRedSeconds = HAWK_DEFAULT_SOLID_RED_SECONDS.get().floatValue();
        hawkDefaultFlashRedSeconds = HAWK_DEFAULT_FLASH_RED_SECONDS.get().floatValue();
        hawkWigwagPeriodTicks = HAWK_WIGWAG_PERIOD_TICKS.get();
    }
}
