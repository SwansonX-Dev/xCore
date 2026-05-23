package dev.xsuite.core.paper.api;

import dev.xsuite.core.api.XCore;
import dev.xsuite.core.api.XCoreAPI;
import dev.xsuite.core.api.XPlatform;
import dev.xsuite.core.api.XPluginHandle;
import dev.xsuite.core.api.command.XCommandManager;
import dev.xsuite.core.api.config.XConfig;
import dev.xsuite.core.api.config.XMessages;
import dev.xsuite.core.api.event.XEventBus;
import dev.xsuite.core.api.messenger.XMessenger;
import dev.xsuite.core.api.service.ServiceRegistry;
import dev.xsuite.core.paper.api.scheduler.XPaperScheduler;
import dev.xsuite.core.paper.internal.config.PaperXConfig;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class every Paper-side xSuite plugin should extend instead of
 * {@link JavaPlugin}.
 *
 * <p>Auto-registers with xCore on enable, detaches on disable, and provides
 * typed accessors for every shared subsystem. Subclasses must declare
 * {@code xCore} in their plugin.yml ({@code depend: [xCore]}) so load order
 * is enforced.
 *
 * <p>Override {@link #onXEnable()} / {@link #onXDisable()} / {@link #onXLoad()}
 * rather than the JavaPlugin lifecycle methods.
 */
public abstract class XPaperPlugin extends JavaPlugin implements XPluginHandle {

    private final Map<String, XConfig> openConfigs = new HashMap<>();
    private XMessages scopedMessages;

    // ---- XPluginHandle ----

    @Override public @NotNull String name() { return getName(); }
    @Override public @NotNull String version() { return getPluginMeta().getVersion(); }
    @Override public @NotNull XPlatform platform() { return XPlatform.PAPER; }
    @Override public @NotNull Path dataFolder() { return getDataFolder().toPath(); }
    @Override public @NotNull Logger logger() { return getSLF4JLogger(); }

    // ---- JavaPlugin lifecycle (sealed off; use the onX* hooks) ----

    @Override
    public final void onLoad() {
        try {
            onXLoad();
        } catch (Throwable t) {
            getSLF4JLogger().error("Error during {} onXLoad", getName(), t);
        }
    }

    @Override
    public final void onEnable() {
        if (!XCore.isReady()) {
            getSLF4JLogger().error("xCore is not loaded — disabling {}.", getName());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        try {
            XCore.api().registerPlugin(this);
            onXEnable();
        } catch (Throwable t) {
            getSLF4JLogger().error("Failed to enable {}", getName(), t);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public final void onDisable() {
        try {
            onXDisable();
        } catch (Throwable t) {
            getSLF4JLogger().warn("Error during {} onXDisable", getName(), t);
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

    // ---- hooks ----

    protected void onXLoad() {}

    protected abstract void onXEnable();

    protected void onXDisable() {}

    /** Invoked when an operator runs {@code /xcore reload}. */
    public void onXReload() {}

    // ---- subsystem accessors (public so other classes in the plugin can use plugin.messages() etc.) ----

    public final @NotNull XCoreAPI core() { return XCore.api(); }
    public final @NotNull ServiceRegistry services() { return core().services(); }
    public final @NotNull XEventBus events() { return core().events(); }
    public final @NotNull XCommandManager commands() { return core().commands(); }
    public final @NotNull XMessenger messenger() { return core().messenger(); }

    public final @NotNull XPaperScheduler scheduler() {
        return (XPaperScheduler) core().scheduler();
    }

    public final @NotNull XMessages messages() {
        if (scopedMessages == null) scopedMessages = core().messages().forPlugin(this);
        return scopedMessages;
    }

    // ---- config helpers ----

    public final @NotNull XConfig xconfig() {
        return xconfig("config.yml");
    }

    /**
     * Open (or return the cached handle for) a YAML file relative to this
     * plugin's data folder. The file is created from the JAR resource of the
     * same name if such a resource exists.
     *
     * <p>Named {@code xconfig} (not {@code config}) so it doesn't collide
     * with {@link JavaPlugin#getConfig()}.
     */
    public final @NotNull XConfig xconfig(@NotNull String fileName) {
        return openConfigs.computeIfAbsent(fileName, this::loadConfig);
    }

    private XConfig loadConfig(String fileName) {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getSLF4JLogger().warn("Could not create data folder for {}", getName());
        }
        File file = new File(getDataFolder(), fileName);
        if (!file.exists() && getResource(fileName) != null) {
            saveResource(fileName, false);
        }
        return new PaperXConfig(file, fileName);
    }
}
