package com.gamearoosdevelopment.realistictrafficcontrol.cc;

import com.gamearoosdevelopment.realistictrafficcontrol.ModRealisticTrafficControl;
import com.gamearoosdevelopment.realistictrafficcontrol.tileentity.TrafficLightControlBoxBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Registers the traffic-light control box as a CC:Tweaked peripheral when ComputerCraft is installed.
 * Uses reflection so the mod compiles without CC:T on the classpath.
 */
public final class TrafficLightPeripheralProvider {

    private TrafficLightPeripheralProvider() {
    }

    public static void register() {
        if (!ModRealisticTrafficControl.CC_INSTALLED) {
            return;
        }
        try {
            Class<?> apiClass = Class.forName("dan200.computercraft.api.ComputerCraftAPI");
            Class<?> providerClass = Class.forName("dan200.computercraft.api.peripheral.IPeripheralProvider");
            Class<?> peripheralClass = Class.forName("dan200.computercraft.api.peripheral.IPeripheral");
            Class<?> luaExceptionClass = Class.forName("dan200.computercraft.api.lua.LuaException");

            Object provider = java.lang.reflect.Proxy.newProxyInstance(
                    providerClass.getClassLoader(),
                    new Class<?>[] { providerClass },
                    (proxy, method, args) -> {
                        if ("getPeripheral".equals(method.getName()) && args != null && args.length == 3) {
                            Level level = (Level) args[0];
                            BlockPos pos = (BlockPos) args[1];
                            BlockEntity tile = level.getBlockEntity(pos);
                            if (tile instanceof TrafficLightControlBoxBlockEntity controlBox) {
                                return wrapPeripheral(new TrafficLightCardPeripheral(level, pos, controlBox),
                                        peripheralClass, luaExceptionClass);
                            }
                            return null;
                        }
                        return null;
                    });
            apiClass.getMethod("registerPeripheralProvider", providerClass).invoke(null, provider);
        } catch (ReflectiveOperationException ex) {
            ModRealisticTrafficControl.LOGGER.warn("Failed to register CC:T peripheral provider", ex);
        }
    }

    private static Object wrapPeripheral(TrafficLightCardPeripheral delegate, Class<?> peripheralClass,
            Class<?> luaExceptionClass) {
        return java.lang.reflect.Proxy.newProxyInstance(
                peripheralClass.getClassLoader(),
                new Class<?>[] { peripheralClass },
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getType".equals(name)) {
                        return delegate.getType();
                    }
                    if ("equals".equals(name) && args != null && args.length == 1) {
                        return proxy == args[0];
                    }
                    if ("getMethodNames".equals(name)) {
                        return delegate.getMethodNames();
                    }
                    if ("callMethod".equals(name) && args != null && args.length == 3) {
                        try {
                            return delegate.callMethod((Integer) args[2], (Object[]) args[3]);
                        } catch (TrafficLightCardPeripheral.CcLuaException ex) {
                            throw (RuntimeException) luaExceptionClass.getConstructor(String.class)
                                    .newInstance(ex.getMessage());
                        }
                    }
                    return null;
                });
    }
}
