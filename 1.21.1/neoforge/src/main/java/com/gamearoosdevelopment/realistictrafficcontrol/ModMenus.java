package com.gamearoosdevelopment.realistictrafficcontrol;

import com.gamearoosdevelopment.realistictrafficcontrol.menu.CrossingGateGateMenu;
import com.gamearoosdevelopment.realistictrafficcontrol.menu.CrossingLampsMenu;
import com.gamearoosdevelopment.realistictrafficcontrol.menu.TrafficLightControlBoxMenu;
import com.gamearoosdevelopment.realistictrafficcontrol.menu.TrafficLightFrameMenu;
import com.gamearoosdevelopment.realistictrafficcontrol.menu.SignMenu;
import com.gamearoosdevelopment.realistictrafficcontrol.menu.StreetSignMenu;
import com.gamearoosdevelopment.realistictrafficcontrol.menu.Type3BarrierMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Menu type registry for the 1.21.1 port. Replaces the 1.12.2 {@code IGuiHandler}/{@code GuiProxy}
 * GUI-id system with {@link MenuType}s opened via {@code Player.openMenu}.
 */
public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ModRealisticTrafficControl.MODID);

    /** Shared menu for all traffic-light frames (bulb count is read from the opening packet buffer). */
    public static final DeferredHolder<MenuType<?>, MenuType<TrafficLightFrameMenu>> TRAFFIC_LIGHT_FRAME =
            MENUS.register("traffic_light_frame",
                    () -> IMenuTypeExtension.create(TrafficLightFrameMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<TrafficLightControlBoxMenu>> TRAFFIC_LIGHT_CONTROL_BOX =
            MENUS.register("traffic_light_control_box",
                    () -> IMenuTypeExtension.create(TrafficLightControlBoxMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CrossingGateGateMenu>> CROSSING_GATE_GATE =
            MENUS.register("crossing_gate_gate",
                    () -> IMenuTypeExtension.create(CrossingGateGateMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CrossingLampsMenu>> CROSSING_LAMPS =
            MENUS.register("crossing_lamps",
                    () -> IMenuTypeExtension.create(CrossingLampsMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SignMenu>> SIGN =
            MENUS.register("sign", () -> IMenuTypeExtension.create(SignMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<StreetSignMenu>> STREET_SIGN =
            MENUS.register("street_sign", () -> IMenuTypeExtension.create(StreetSignMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<Type3BarrierMenu>> TYPE_3_BARRIER =
            MENUS.register("type_3_barrier", () -> IMenuTypeExtension.create(Type3BarrierMenu::new));

    private ModMenus() {
    }
}
