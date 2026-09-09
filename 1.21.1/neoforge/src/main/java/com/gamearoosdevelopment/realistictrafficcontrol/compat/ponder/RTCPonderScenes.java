package com.gamearoosdevelopment.realistictrafficcontrol.compat.ponder;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class RTCPonderScenes {

    private RTCPonderScenes() {
    }

    static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(RTCPonderPlugin.ITEMS)
                .addStoryBoard("pole_mount", RTCPonderScenes::poleMount, RTCPonderPlugin.TAG);
        helper.forComponents(
                RTCPonderPlugin.id("screwdriver"),
                RTCPonderPlugin.id("pole"),
                RTCPonderPlugin.id("traffic_light_frame"))
                .addStoryBoard("screwdriver_pair", RTCPonderScenes::screwdriverPair, RTCPonderPlugin.TAG);
        helper.forComponents(
                RTCPonderPlugin.id("horizontal_pole"),
                RTCPonderPlugin.id("traffic_light_frame"),
                RTCPonderPlugin.id("traffic_light_doghouse_frame"),
                RTCPonderPlugin.id("traffic_light_hoz_frame"),
                RTCPonderPlugin.id("sign"))
                .addStoryBoard("hoz_attach", RTCPonderScenes::hozAttach, RTCPonderPlugin.TAG);
    }

    public static void poleMount(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("pole_mount", "Mounting on a pole");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos poleBase = util.grid().at(2, 1, 2);
        BlockPos pole = util.grid().at(2, 2, 2);
        BlockPos frame = util.grid().at(2, 2, 3);

        scene.world().showSection(util.select().fromTo(poleBase, pole), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(70)
                .text("Click a pole face. The piece sits on that face and copies the extra 22.5°.")
                .pointAt(util.vector().blockSurface(pole, Direction.SOUTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(20);

        scene.overlay().showOutline(PonderPalette.OUTPUT, pole, util.select().position(pole), 40);
        scene.idle(40);

        scene.world().showSection(util.select().position(frame), Direction.NORTH);
        scene.idle(15);

        scene.overlay().showText(70)
                .text("Off-90° poles keep that extra angle, so the frame stays flush.")
                .pointAt(util.vector().centerOf(frame))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(80);
        scene.markAsFinished();
    }

    public static void screwdriverPair(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("screwdriver_pair", "Turning an assembly");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos pole = util.grid().at(2, 1, 2);
        BlockPos frame = util.grid().at(2, 1, 3);
        scene.world().showSection(util.select().fromTo(pole, frame), Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(70)
                .text("A screwdriver turns the pole and every mounted piece as one assembly.")
                .pointAt(util.vector().centerOf(pole))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(40);

        scene.world().modifyBlock(pole, state -> state.setValue(
                com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties.ROTATION, 3), false);
        scene.world().modifyBlock(frame, state -> state.setValue(
                com.gamearoosdevelopment.realistictrafficcontrol.blocks.RTCProperties.ROTATION, 3), false);
        scene.idle(20);

        scene.overlay().showText(70)
                .text("The extra 22.5° is shared, so the frame stays on the clicked face.")
                .pointAt(util.vector().centerOf(frame))
                .placeNearTarget();
        scene.idle(80);
        scene.markAsFinished();
    }

    public static void hozAttach(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("hoz_attach", "Horizontal poles, frames, and signs");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos poleBase = util.grid().at(1, 1, 2);
        BlockPos pole = util.grid().at(1, 2, 2);
        BlockPos bar = util.grid().at(2, 2, 2);
        BlockPos bar2 = util.grid().at(3, 2, 2);
        BlockPos frame = util.grid().at(2, 2, 3);
        BlockPos sign = util.grid().at(3, 2, 3);

        scene.world().showSection(util.select().fromTo(poleBase, pole), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(70)
                .text("Click a pole face with a horizontal pole. The bar sits on that face.")
                .pointAt(util.vector().blockSurface(pole, Direction.EAST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(20);

        scene.world().showSection(util.select().position(bar), Direction.WEST);
        scene.idle(40);

        scene.overlay().showText(70)
                .text("Click the end of the bar with another hoz to extend the run.")
                .pointAt(util.vector().centerOf(bar))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(20);

        scene.world().showSection(util.select().position(bar2), Direction.WEST);
        scene.idle(50);

        scene.overlay().showText(70)
                .text("Click the side of the bar with a frame or sign. It hangs on that face.")
                .pointAt(util.vector().centerOf(bar))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(20);

        scene.world().showSection(util.select().fromTo(frame, sign), Direction.NORTH);
        scene.idle(80);
        scene.markAsFinished();
    }
}
