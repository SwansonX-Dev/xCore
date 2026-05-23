package dev.xsuite.core.velocity.api;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.xsuite.core.api.XCore;
import dev.xsuite.core.api.XCoreAPI;
import dev.xsuite.core.api.XPlatform;
import dev.xsuite.core.api.XPluginHandle;
import dev.xsuite.core.api.command.XCommandManager;
import dev.xsuite.core.api.config.XConfig;
import dev.xsuite.core.api.config.XMessages;
import dev.xsuite.core.api.event.XEventBus;
import dev.xsuite.core.api.messenger.XMessenger;
import dev.xsuite.core.api.scheduler.XScheduler;
import dev.xsuite.core.api.service.ServiceRegistry;
import dev.xsuite.core.velocity.internal.config.VelocityXConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class every Velocity-side xSuite plugin should extend.
 *
 * <p>Usage:
 * <pre>{@code
 * @Plugin(id="xchat-velocity", name="xChat", version="1.0",
 *         dependencies = {@Dependency(id="xcore")})
 * public class XChatVelocity extends XVelocityPlugin {
 *     @Inject
 *     public XChatVelocity(ProxyServer proxy, Logger logger,
 *                          @DataDirectory Path dataDir, PluginContainer container) {
 *         super(proxy, logger, dataDir, container);
 *     }
 *
 *     @Override protected void onXEnable() { ... }
 * }
 * }</pre>
 *
 * <p>{@link #onXEnable()} is called from {@link ProxyInitializeEvent}; the
 * parent class subscribes to the lifecycle events for you.
 */
public abstract class XVelocityPlugin implements XPluginHandle {

    protected final ProxyServer proxy;
    protected final Logger log;
    protected final Path dataDirectory;
    protected final PluginContainer container;

    private final Map<String, XConfig> openConfigs = new HashMap<>();
    private XMessages scopedMessages;

    protected XVelocityPlugin(@NotNull ProxyServer proxy,
                              @NotNull Logger log,
                              @NotNull Path dataDirectory,
                              @NotNull PluginContainer container) {
        this.proxy = proxy;
        this.log = log;
        this.dataDirectory = dataDirectory;
        this.container = container;
    }

    // ---- XPluginHandle ----

    @Override public @NotNull String name() { return container.getDescription().getName().orElse(container.getDescription().getId()); }
    @Override public @NotNull String version() { return container.getDescription().getVersion().orElse("0.0.0"); }
    @Override public @NotNull XPlatform platform() { return XPlatform.VELOCITY; }
    @Override public @NotNull Path dataFolder() { return dataDirectory; }
    @Override public @NotNull Logger logger() { return log; }

    public final @NotNull PluginContainer container() { return container; }
    public final @NotNull ProxyServer proxy() { return proxy; }

    // ---- lifecycle ----

    @Subscribe
    public final void onProxyInit(ProxyInitializeEvent event) {
        if (!XCore.isReady()) {
            log.error("xCore is not loaded — {} cannot enable.", name());
            return;
        }
        try {
            XCore.api().registerPlugin(this);
            onXEnable();
        } catch (Throwable t) {
            log.error("Failed to enable {}", name(), t);
        }
    }

    @Subscribe
    public final void onProxyShutdown(ProxyShutdownEvent event) {
        try {
            onXDisable();
        } catch (Throwable t) {
            log.warn("Error during {} onXDisable", name(), t);
        } finally {
            XCoreAPI api = XCore.apiOrNull();
            if (api != null) {
                api.detach(this);
                api.unregisterPlugin(this);
            }
            openConfigs.clear();
            scopedMessages = null;
        }
    }

    protected abstract void onXEnable();

    protected void onXDisable() {}

    /** Invoked when an operator runs {@code /xcore reload}. */
    public void onXReload() {}

    // ---- subsystem accessors ----

    protected final @NotNull XCoreAPI core() { return XCore.api(); }
    protected final @NotNull ServiceRegistry services() { return core().services(); }
    protected final @NotNull XEventBus events() { return core().events(); }
    protected final @NotNull XCommandManager commands() { return core().commands(); }
    protected final @NotNull XScheduler scheduler() { return core().scheduler(); }
    protected final @NotNull XMessenger messenger() { return core().messenger(); }

    protected final @NotNull XMessages messages() {
        if (scopedMessages == null) scopedMessages = core().messages().forPlugin(this);
        return scopedMessages;
    }

    // ---- config helpers ----

    public final @NotNull XConfig config() {
        return config("config.yml");
    }

    public final @NotNull XConfig config(@NotNull String fileName) {
        return openConfigs.computeIfAbsent(fileName, name -> new VelocityXConfig(dataDirectory.resolve(name), name));
    }
}
