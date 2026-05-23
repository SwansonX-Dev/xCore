package dev.xsuite.core.api;

import dev.xsuite.core.api.command.XCommandManager;
import dev.xsuite.core.api.config.XMessages;
import dev.xsuite.core.api.event.XEventBus;
import dev.xsuite.core.api.messenger.XMessenger;
import dev.xsuite.core.api.scheduler.XScheduler;
import dev.xsuite.core.api.service.ServiceRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * The public, stable API exposed by xCore on every platform.
 *
 * <p>Anything outside the {@code dev.xsuite.core.api} package is internal
 * and may change between versions. Only program against this surface.
 */
public interface XCoreAPI {

    @NotNull XPlatform platform();

    @NotNull String version();

    @NotNull ServiceRegistry services();

    @NotNull XEventBus events();

    @NotNull XMessages messages();

    @NotNull XCommandManager commands();

    @NotNull XScheduler scheduler();

    /** Cross-server messaging. On standalone Paper this still works locally; cross-server reach requires a connected Velocity proxy. */
    @NotNull XMessenger messenger();

    /** All currently registered xSuite plugins on this node. */
    @NotNull Collection<XPluginHandle> registeredPlugins();

    void registerPlugin(@NotNull XPluginHandle plugin);

    void unregisterPlugin(@NotNull XPluginHandle plugin);

    /** Unsubscribe events, unregister services, commands and messenger subscriptions owned by {@code plugin}. */
    void detach(@NotNull XPluginHandle plugin);
}
