package com.gamearoosdevelopment.realistictrafficcontrol.compat;

import java.lang.reflect.InvocationTargetException;

/**
 * Classloader-safe bridge to the optional OpenComputers integration. This class has no OC types in its
 * constant pool, so loading RTC without OpenComputers installed cannot resolve OC API classes.
 */
public final class OpenComputersCompat {
    private static final String INTEGRATION_CLASS =
            "com.gamearoosdevelopment.realistictrafficcontrol.oc.OpenComputersIntegration";

    private OpenComputersCompat() {
    }

    public static void register() {
        try {
            Class.forName(INTEGRATION_CLASS).getMethod("register").invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException exception) {
            throw new IllegalStateException("OpenComputers integration entry point is unavailable", exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("OpenComputers integration registration failed", cause);
        }
    }
}
