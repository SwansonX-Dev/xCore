package dev.xsuite.core.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.xsuite.core.api.XCore;
import dev.xsuite.core.api.XPlatform;
import dev.xsuite.core.api.XPluginHandle;
import dev.xsuite.core.api.config.XConfig;
import dev.xsuite.core.internal.XCoreAPIImpl;
import dev.xsuite.core.internal.codec.MessageCodec;
import dev.xsuite.core.internal.event.XEventBusImpl;
import dev.xsuite.core.internal.messenger.XMessengerImpl;
import dev.xsuite.core.internal.service.ServiceRegistryImpl;
import dev.xsuite.core.velocity.internal.command.VelocityXCommandManager;
import dev.xsuite.core.velocity.internal.commands.VelocityXCoreCommand;
import dev.xsuite.core.velocity.internal.config.VelocityXConfig;
import dev.xsuite.core.velocity.internal.config.VelocityXMessages;
import dev.xsuite.core.velocity.internal.messenger.VelocityPluginMessageTransport;
import dev.xsuite.core.velocity.internal.scheduler.VelocityXScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Velocity-side main class. Boots the platform-specific subsystems, binds
 * {@link XCore}, and routes plugin messaging between backends.
 */
@Plugin(
        id = "xcore",
        name = "xCore",
        version = "0.1.0-SNAPSHOT",
        authors = {"xSuite"},
        description = "Backbone proxy plugin for the xSuite. Routes cross-server messaging and exposes the shared API on Velocity."
)
public final class XCoreVelocityPlugin implements XPluginHandle {

    private final ProxyServer proxy;
    private final Logger log;
    private final Path dataDirectory;
    private final PluginContainer container;

    private ServiceRegistryImpl services;
    private XEventBusImpl events;
    private VelocityXMessages messages;
    private VelocityXCommandManager commands;
    private VelocityXScheduler scheduler;
    private XMessengerImpl messenger;
    private VelocityPluginMessageTransport transport;
    private VelocityXConfig coreConfig;
    private XCoreAPIImpl api;

    @Inject
    public XCoreVelocityPlugin(@NotNull ProxyServer proxy,
                               @NotNull Logger log,
                               @NotNull @DataDirectory Path dataDirectory,
                               @NotNull PluginContainer container) {
        this.proxy = proxy;
        this.log = log;
        this.dataDirectory = dataDirectory;
        this.container = container;
    }

    // ---- XPluginHandle ----

    @Override public @NotNull String name() { return "xCore"; }
    @Override public @NotNull String version() { return container.getDescription().getVersion().orElse("0.0.0"); }
    @Override public @NotNull XPlatform platform() { return XPlatform.VELOCITY; }
    @Override public @NotNull Path dataFolder() { return dataDirectory; }
    @Override public @NotNull Logger logger() { return log; }

    public @NotNull ProxyServer proxy() { return proxy; }
    public @NotNull PluginContainer container() { return container; }

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        coreConfig = materializeCoreConfig();

        scheduler = new VelocityXScheduler(proxy, container);
        services = new ServiceRegistryImpl(log, coreConfig.getBoolean("debug-services", false));
        events = new XEventBusImpl(this, scheduler, log, coreConfig.getBoolean("debug-events", false));

        messages = new VelocityXMessages(
                dataDirectory,
                "messages.yml",
                this::loadPrefix,
                getClass().getClassLoader(),
                null);

        commands = new VelocityXCommandManager(proxy, container, log);

        MessageCodec codec = new MessageCodec();
        transport = new VelocityPluginMessageTransport(proxy, codec, log);
        messenger = new XMessengerImpl(transport, log);

        api = new XCoreAPIImpl(
                XPlatform.VELOCITY,
                version(),
                services,
                events,
                messages,
                commands,
                scheduler,
                messenger);
        XCore.bind(api);

        // Register the transport as an event listener so it sees PluginMessageEvents.
        proxy.getEventManager().register(this, transport);
        messenger.start();
        commands.register(this, new VelocityXCoreCommand(this));

        log.info("xCore (Velocity) {} ready.", version());
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        try {
            if (messenger != null) messenger.stop();
            if (commands != null) commands.unregisterAll(this);
        } finally {
            XCore.unbind();
        }
    }

    public void reloadXCore() {
        if (coreConfig != null) coreConfig.reload();
        if (messages != null) messages.reload();
    }

    private VelocityXConfig materializeCoreConfig() {
        try {
            if (!java.nio.file.Files.exists(dataDirectory)) {
                java.nio.file.Files.createDirectories(dataDirectory);
            }
            Path target = dataDirectory.resolve("config.yml");
            if (!java.nio.file.Files.exists(target)) {
                try (var in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                    if (in != null) java.nio.file.Files.copy(in, target);
                }
            }
        } catch (java.io.IOException e) {
            log.warn("Failed to materialize xCore config: {}", e.getMessage());
        }
        return new VelocityXConfig(dataDirectory.resolve("config.yml"), "config.yml");
    }

    private Component loadPrefix() {
        String raw = coreConfig == null ? "" : coreConfig.getString("prefix", "");
        return raw.isEmpty() ? Component.empty() : MiniMessage.miniMessage().deserialize(raw);
    }

    /** Internal: expose the core config for the admin command. */
    public @NotNull XConfig coreConfig() { return coreConfig; }
}
