package com.gamearoosdevelopment.realistictrafficcontrol.client;

import com.gamearoosdevelopment.realistictrafficcontrol.ModBlockEntities;
import com.gamearoosdevelopment.realistictrafficcontrol.ModItems;
import com.gamearoosdevelopment.realistictrafficcontrol.ModMenus;
import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.client.CrossingLampClientModels;
import com.gamearoosdevelopment.realistictrafficcontrol.client.WigWagClientModels;
import com.gamearoosdevelopment.realistictrafficcontrol.client.model.RotatedBlockModelWrapper;
import com.gamearoosdevelopment.realistictrafficcontrol.client.render.CrossingGateGateBlockEntityRenderer;
import com.gamearoosdevelopment.realistictrafficcontrol.client.render.CrossingLampsBlockEntityRenderer;
import com.gamearoosdevelopment.realistictrafficcontrol.client.render.TrafficLightBlockEntityRenderer;
import com.gamearoosdevelopment.realistictrafficcontrol.client.render.VerticalWigWagBlockEntityRenderer;
import com.gamearoosdevelopment.realistictrafficcontrol.client.render.WigWagBlockEntityRenderer;
import com.gamearoosdevelopment.realistictrafficcontrol.client.render.SignBlockEntityRenderer;
import com.gamearoosdevelopment.realistictrafficcontrol.client.render.StreetLightDoubleBlockEntityRenderer;
import com.gamearoosdevelopment.realistictrafficcontrol.client.render.StreetLightSingleBlockEntityRenderer;
import com.gamearoosdevelopment.realistictrafficcontrol.client.render.StreetSignBlockEntityRenderer;
import com.gamearoosdevelopment.realistictrafficcontrol.client.render.Type3BarrierBlockEntityRenderer;
import com.gamearoosdevelopment.realistictrafficcontrol.client.render.WireAnchorBlockEntityRenderer;
import com.gamearoosdevelopment.realistictrafficcontrol.client.render.MessageBoardBlockEntityRenderer;
import com.gamearoosdevelopment.realistictrafficcontrol.item.ConcreteBarrierBlockItem;
import com.gamearoosdevelopment.realistictrafficcontrol.item.TrafficLightBulbItem;
import com.gamearoosdevelopment.realistictrafficcontrol.item.TrafficLightCardItem;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Client-only setup for the 1.21.1 port (replaces the 1.12.2 {@code ClientProxy}). Registers menu screens;
 * block-entity renderers and other client hooks are added in the rendering phase.
 */
@EventBusSubscriber(modid = ModRealisticTrafficControl.MODID, value = Dist.CLIENT)
public final class RTCClient {

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.TRAFFIC_LIGHT_FRAME.get(), TrafficLightFrameScreen::new);
        event.register(ModMenus.TRAFFIC_LIGHT_CONTROL_BOX.get(), TrafficLightControlBoxScreen::new);
        event.register(ModMenus.CROSSING_GATE_GATE.get(), CrossingGateGateScreen::new);
        event.register(ModMenus.CROSSING_LAMPS.get(), CrossingLampsScreen::new);
        event.register(ModMenus.SIGN.get(), SignScreen::new);
        event.register(ModMenus.STREET_SIGN.get(), StreetSignScreen::new);
        event.register(ModMenus.TYPE_3_BARRIER.get(), Type3BarrierScreen::new);
        event.register(ModMenus.DISPLAY.get(), DisplayScreen::new);
    }

    @SubscribeEvent
    public static void onClientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ModRealisticTrafficControl.signRepo.initClientTextures();
            ItemProperties.register(
                    ModItems.TRAFFIC_LIGHT_CARD.get(),
                    ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, "card_tier"),
                    (stack, level, entity, seed) -> TrafficLightCardItem.getTier(stack));
            ItemProperties.register(
                    ModItems.TRAFFIC_LIGHT_BULB.get(),
                    ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, "bulb_type"),
                    (stack, level, entity, seed) -> TrafficLightBulbItem.getType(stack));
            ItemProperties.register(
                    ModItems.CONCRETE_BARRIER.get(),
                    ResourceLocation.fromNamespaceAndPath(ModRealisticTrafficControl.MODID, "dye"),
                    (stack, level, entity, seed) -> ConcreteBarrierBlockItem.getDye(stack) / 15.0F);
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLoadComplete(FMLLoadCompleteEvent event) {
        if (ModRealisticTrafficControl.CREATE_INSTALLED) {
            event.enqueueWork(com.gamearoosdevelopment.realistictrafficcontrol.compat.ponder.RTCPonderClient::reload);
        }
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.TRAFFIC_LIGHT.get(), TrafficLightBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CROSSING_GATE_GATE.get(),
                CrossingGateGateBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CROSSING_LAMPS.get(),
                CrossingLampsBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.WIG_WAG.get(), WigWagBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.VERTICAL_WIG_WAG.get(),
                VerticalWigWagBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.WIRE_ANCHOR.get(), WireAnchorBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SIGN.get(), SignBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DIGITAL_SIGN.get(), SignBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MESSAGE_BOARD.get(), MessageBoardBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.STREET_SIGN.get(), StreetSignBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.STREET_LIGHT_SINGLE.get(),
                StreetLightSingleBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.STREET_LIGHT_DOUBLE.get(),
                StreetLightDoubleBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.TYPE_3_BARRIER.get(),
                Type3BarrierBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        CrossingLampClientModels.registerAdditional(event);
        WigWagClientModels.registerAdditional(event);
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        CrossingLampClientModels.aliasLampStateVariants(event.getModels());
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        List<ModelResourceLocation> toWrap = new ArrayList<>();
        for (ModelResourceLocation loc : models.keySet()) {
            if (!shouldWrapRotatedModel(loc)) {
                continue;
            }
            toWrap.add(loc);
        }
        for (ModelResourceLocation loc : toWrap) {
            models.put(loc, new RotatedBlockModelWrapper(models.get(loc), isPoleMountedModel(loc)));
        }
    }

    /** Wrap block models that read {@link com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties#ROTATION} at bake time. */
    private static boolean shouldWrapRotatedModel(ModelResourceLocation loc) {
        if (!loc.id().getNamespace().equals(ModRealisticTrafficControl.MODID)) {
            return false;
        }
        String variant = loc.getVariant();
        // Standalone models are rendered and rotated by their BER. Actual lamp
        // blockstates contain both "rotation=" and "state=", and must be wrapped.
        if (variant.contains("inventory") || variant.contains("standalone")) {
            return false;
        }
        String path = loc.id().getPath();
        // BER-only blocks: wrapping an empty model is wasted work and can fight the TESR.
        if (path.startsWith("street_light") || path.equals("street_sign") || path.equals("light_source")) {
            return false;
        }
        if (variant.contains("rotation=")) {
            return true;
        }
        // Multipart blockstate model locations use the block registry path, not "block/<model>".
        return path.startsWith("traffic_light")
                || path.startsWith("crossing_gate") || path.startsWith("wig_wag")
                || path.equals("ped_crossing_lamps") || path.equals("crossing_gate_lamps")
                || path.equals("overhead_lamps") || path.equals("horizontal_pole")
                || path.equals("overhead") || path.equals("overhead_pole")
                || path.equals("overhead_crossbuck") || path.equals("tag")
                || path.equals("sign") || path.equals("digital_sign")
                || path.endsWith("_pole") || path.equals("pole");
    }

    /** Frames and signs orbit the pole; hoz bars spin in-cell like a Create shaft. */
    private static boolean isPoleMountedModel(ModelResourceLocation loc) {
        String path = loc.id().getPath();
        if (path.startsWith("block/")) {
            path = path.substring("block/".length());
        }
        if (path.contains("control")) {
            return false;
        }
        return path.equals("sign") || path.equals("digital_sign")
                || path.startsWith("traffic_light");
    }

    private RTCClient() {
    }
}
