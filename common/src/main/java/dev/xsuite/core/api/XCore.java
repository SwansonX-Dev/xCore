package dev.xsuite.core.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Static accessor for the running {@link XCoreAPI} instance.
 *
 * <p>Works the same on Paper and Velocity — call {@link #api()} from your
 * plugin's enable hook. The instance is bound by whichever xCore plugin
 * (Paper or Velocity) the JVM is running.
 */
public final class XCore {

    private static volatile XCoreAPI instance;

    private XCore() {
    }

    public static @NotNull XCoreAPI api() {
        XCoreAPI ref = instance;
        if (ref == null) {
            throw new IllegalStateException(
                    "XCore is not enabled yet. Declare xCore as a dependency and wait for enable.");
        }
        return ref;
    }

    public static @Nullable XCoreAPI apiOrNull() {
        return instance;
    }

    public static boolean isReady() {
        return instance != null;
    }

    public static void bind(@NotNull XCoreAPI api) {
        if (instance != null) {
            throw new IllegalStateException("XCore API already bound");
        }
        instance = api;
    }

    public static void unbind() {
        instance = null;
    }
}
