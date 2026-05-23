package dev.xsuite.core.internal;

import dev.xsuite.core.api.XCoreAPI;
import dev.xsuite.core.api.XPlatform;
import dev.xsuite.core.api.XPluginHandle;
import dev.xsuite.core.api.command.XCommandManager;
import dev.xsuite.core.api.config.XMessages;
import dev.xsuite.core.api.event.XEventBus;
import dev.xsuite.core.api.messenger.XMessenger;
import dev.xsuite.core.api.scheduler.XScheduler;
import dev.xsuite.core.api.service.ServiceRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Platform-neutral {@link XCoreAPI} container. Both the Paper and Velocity
 * variants of xCore wire their concrete subsystems into this object.
 */
public final class XCoreAPIImpl implements XCoreAPI {

    private final XPlatform platform;
    private final String version;
    private final ServiceRegistry services;
    private final XEventBus events;
    private final XMessages messages;
    private final XCommandManager commands;
    private final XScheduler scheduler;
    private final XMessenger messenger;

    private final Set<XPluginHandle> plugins = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public XCoreAPIImpl(@NotNull XPlatform platform,
                        @NotNull String version,
                        @NotNull ServiceRegistry services,
                        @NotNull XEventBus events,
                        @NotNull XMessages messages,
                        @NotNull XCommandManager commands,
                        @NotNull XScheduler scheduler,
                        @NotNull XMessenger messenger) {
        this.platform = platform;
        this.version = version;
        this.services = services;
        this.events = events;
        this.messages = messages;
        this.commands = commands;
        this.scheduler = scheduler;
        this.messenger = messenger;
    }

    @Override public @NotNull XPlatform platform() { return platform; }
    @Override public @NotNull String version() { return version; }
    @Override public @NotNull ServiceRegistry services() { return services; }
    @Override public @NotNull XEventBus events() { return events; }
    @Override public @NotNull XMessages messages() { return messages; }
    @Override public @NotNull XCommandManager commands() { return commands; }
    @Override public @NotNull XScheduler scheduler() { return scheduler; }
    @Override public @NotNull XMessenger messenger() { return messenger; }

    @Override
    public @NotNull Collection<XPluginHandle> registeredPlugins() {
        return Set.copyOf(plugins);
    }

    @Override
    public void registerPlugin(@NotNull XPluginHandle plugin) {
        plugins.add(plugin);
    }

    @Override
    public void unregisterPlugin(@NotNull XPluginHandle plugin) {
        plugins.remove(plugin);
    }

    @Override
    public void detach(@NotNull XPluginHandle plugin) {
        events.unsubscribeAll(plugin);
        services.unregisterAll(plugin);
        commands.unregisterAll(plugin);
        messenger.unsubscribeAll(plugin);
    }
}
