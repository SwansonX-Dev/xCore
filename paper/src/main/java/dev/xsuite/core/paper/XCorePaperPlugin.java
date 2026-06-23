package dev.xsuite.core.paper;

import dev.xsuite.core.api.XCore;
import dev.xsuite.core.api.XPlatform;
import dev.xsuite.core.api.XPluginHandle;
import dev.xsuite.core.internal.XCoreAPIImpl;
import dev.xsuite.core.internal.codec.MessageCodec;
import dev.xsuite.core.internal.event.XEventBusImpl;
import dev.xsuite.core.internal.messenger.XMessengerImpl;
import dev.xsuite.core.internal.service.ServiceRegistryImpl;
import dev.xsuite.core.paper.api.guide.GuideEntry;
import dev.xsuite.core.paper.api.guide.GuideRegistry;
import dev.xsuite.core.paper.internal.command.PaperXCommandManager;
import dev.xsuite.core.paper.internal.commands.GuideCommand;
import dev.xsuite.core.paper.internal.commands.PaperXCoreCommand;
import dev.xsuite.core.paper.internal.config.PaperXMessages;
import dev.xsuite.core.paper.internal.gui.XMenuListener;
import dev.xsuite.core.paper.internal.guide.GuideRegistryImpl;
import dev.xsuite.core.paper.internal.messenger.PaperPluginMessageTransport;
import dev.xsuite.core.paper.internal.scheduler.XPaperSchedulerImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;

/**
 * Paper-side main class for xCore. Boots all subsystems, binds {@link XCore},
 * and exposes the API to other x plugins on this server.
 *
 * <p>This class itself implements {@link XPluginHandle} so it can be the
 * owner of its own command and any internally registered events/services.
 */
public final class XCorePaperPlugin extends JavaPlugin implements XPluginHandle {

    private XPaperSchedulerImpl scheduler;
    private ServiceRegistryImpl services;
    private XEventBusImpl events;
    private PaperXMessages messages;
    private PaperXCommandManager commands;
    private XMessengerImpl messenger;
    private PaperPluginMessageTransport transport;
    private GuideRegistryImpl guides;
    private XCoreAPIImpl api;

    // ---- XPluginHandle ----

    @Override public @NotNull String name() { return getName(); }
    @Override public @NotNull String version() { return getPluginMeta().getVersion(); }
    @Override public @NotNull XPlatform platform() { return XPlatform.PAPER; }
    @Override public @NotNull Path dataFolder() { return getDataFolder().toPath(); }
    @Override public @NotNull Logger logger() { return getSLF4JLogger(); }

    // ---- lifecycle ----

    @Override
    public void onLoad() {
        saveDefaultConfig();
        boolean debug = getConfig().getBoolean("debug-events", false);
        boolean debugServices = getConfig().getBoolean("debug-services", false);

        scheduler = new XPaperSchedulerImpl(this);
        services = new ServiceRegistryImpl(getSLF4JLogger(), debugServices);
        events = new XEventBusImpl(this, scheduler, getSLF4JLogger(), debug);

        messages = new PaperXMessages(
                this,
                new File(getDataFolder(), "messages.yml"),
                "messages.yml",
                this::loadPrefix,
                null);

        commands = new PaperXCommandManager(getSLF4JLogger());

        MessageCodec codec = new MessageCodec();
        transport = new PaperPluginMessageTransport(this, codec, getSLF4JLogger());
        messenger = new XMessengerImpl(transport, getSLF4JLogger());

        api = new XCoreAPIImpl(
                XPlatform.PAPER,
                getPluginMeta().getVersion(),
                services,
                events,
                messages,
                commands,
                scheduler,
                messenger);

        XCore.bind(api);
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new XMenuListener(getSLF4JLogger()), this);
        commands.register(this, new PaperXCoreCommand(this));

        // Global /guide hub. Seed externals from config (forks / non-xCore plugins);
        // in-process plugins override these by registering a real GuideEntry on enable.
        guides = new GuideRegistryImpl(getSLF4JLogger(), loadExternalGuides());
        services.register(GuideRegistry.class, guides, this);
        commands.register(this, new GuideCommand(guides));

        messenger.start();
        getSLF4JLogger().info("xCore (Paper) {} ready.", getPluginMeta().getVersion());
    }

    /** Parse the {@code guide.external} config list into command-dispatch guide entries. */
    private java.util.List<GuideEntry> loadExternalGuides() {
        java.util.List<GuideEntry> out = new java.util.ArrayList<>();
        for (java.util.Map<?, ?> raw : getConfig().getMapList("guide.external")) {
            try {
                String id = String.valueOf(raw.get("id"));
                String name = String.valueOf(raw.get("name"));
                String command = String.valueOf(raw.get("command"));
                if (id == null || name == null || command == null
                        || id.equals("null") || name.equals("null") || command.equals("null")) {
                    continue;
                }
                Object iconRaw = raw.get("icon");
                org.bukkit.Material icon = iconRaw == null
                        ? org.bukkit.Material.BOOK
                        : org.bukkit.Material.matchMaterial(String.valueOf(iconRaw));
                if (icon == null) icon = org.bukkit.Material.BOOK;

                java.util.List<String> summary = new java.util.ArrayList<>();
                Object loreObj = raw.get("summary");
                if (loreObj instanceof java.util.List<?> list) {
                    for (Object line : list) summary.add(String.valueOf(line));
                }
                out.add(GuideEntry.ofCommand(id, name, icon, summary, command));
            } catch (Throwable t) {
                getSLF4JLogger().warn("[xCore/guide] skipping malformed guide.external entry: {}", t.toString());
            }
        }
        return out;
    }

    @Override
    public void onDisable() {
        try {
            messenger.stop();
            commands.unregisterAll(this);
        } finally {
            XCore.unbind();
        }
    }

    /** Reload xCore's own config + messages. Per-plugin reload is run by the command. */
    public void reloadXCore() {
        reloadConfig();
        messages.reload();
    }

    private Component loadPrefix() {
        String raw = getConfig().getString("prefix", "");
        return raw.isEmpty() ? Component.empty() : MiniMessage.miniMessage().deserialize(raw);
    }
}
