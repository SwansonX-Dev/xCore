package dev.xsuite.core.paper.internal.commands;

import dev.xsuite.core.api.XCore;
import dev.xsuite.core.api.XPluginHandle;
import dev.xsuite.core.api.command.XCommand;
import dev.xsuite.core.api.command.XCommandContext;
import dev.xsuite.core.paper.XCorePaperPlugin;
import dev.xsuite.core.paper.api.XPaperPlugin;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Administrative {@code /xcore} command on the Paper side. */
public final class PaperXCoreCommand extends XCommand {

    private final XCorePaperPlugin plugin;

    public PaperXCoreCommand(@NotNull XCorePaperPlugin plugin) {
        super("xcore", "Manage xCore.", "xcore.admin", "/xcore <reload|plugins|services|messenger|version>", List.of("xc"));
        this.plugin = plugin;
        addSubcommand("reload", new Reload(plugin));
        addSubcommand("plugins", new Plugins());
        addSubcommand("services", new Services());
        addSubcommand("messenger", new Messenger());
        addSubcommand("version", new Version(plugin));
        addSubcommand("help", new Help());
    }

    @Override
    public void execute(@NotNull XCommandContext ctx) {
        ctx.requirePermission("xcore.admin");
        if (ctx.argCount() == 0) {
            new Help().execute(ctx);
            return;
        }
        ctx.reply("unknown-subcommand", "sub", ctx.arg(0));
    }

    private static final class Reload extends XCommand {
        private final XCorePaperPlugin plugin;
        Reload(XCorePaperPlugin plugin) {
            super("reload", "Reload xCore config.", "xcore.admin", "/xcore reload", List.of());
            this.plugin = plugin;
        }
        @Override public void execute(@NotNull XCommandContext ctx) {
            ctx.requirePermission("xcore.admin");
            long start = System.currentTimeMillis();
            plugin.reloadXCore();
            for (XPluginHandle x : XCore.api().registeredPlugins()) {
                if (x instanceof XPaperPlugin xp) {
                    try { xp.onXReload(); } catch (Throwable t) {
                        plugin.getSLF4JLogger().warn("Reload of {} threw", x.name(), t);
                    }
                }
            }
            ctx.reply("reloaded", "ms", String.valueOf(System.currentTimeMillis() - start));
        }
    }

    private static final class Plugins extends XCommand {
        Plugins() {
            super("plugins", "List registered xSuite plugins.", "xcore.admin", "/xcore plugins", List.of("list"));
        }
        @Override public void execute(@NotNull XCommandContext ctx) {
            ctx.requirePermission("xcore.admin");
            var registered = XCore.api().registeredPlugins();
            ctx.reply("plugins-header", "count", String.valueOf(registered.size()));
            for (XPluginHandle p : registered) {
                ctx.reply("plugins-entry", "name", p.name(), "version", p.version());
            }
        }
    }

    private static final class Services extends XCommand {
        Services() {
            super("services", "List registered services.", "xcore.admin", "/xcore services", List.of());
        }
        @Override public void execute(@NotNull XCommandContext ctx) {
            ctx.requirePermission("xcore.admin");
            var types = XCore.api().services().registeredTypes();
            ctx.reply("services-header", "count", String.valueOf(types.size()));
            for (Class<?> type : types) {
                XPluginHandle owner = XCore.api().services().ownerOf(type);
                ctx.reply("services-entry", "type", type.getSimpleName(),
                        "owner", owner == null ? "?" : owner.name());
            }
        }
    }

    private static final class Messenger extends XCommand {
        Messenger() {
            super("messenger", "Show messenger state.", "xcore.admin", "/xcore messenger", List.of("net"));
        }
        @Override public void execute(@NotNull XCommandContext ctx) {
            ctx.requirePermission("xcore.admin");
            var m = XCore.api().messenger();
            ctx.reply("messenger-status",
                    "self", m.selfId(),
                    "proxy", String.valueOf(m.isProxyAvailable()),
                    "peers", String.join(", ", m.knownServers()));
        }
    }

    private static final class Version extends XCommand {
        private final XCorePaperPlugin plugin;
        Version(XCorePaperPlugin plugin) {
            super("version", "Show xCore version.", "", "/xcore version", List.of("ver"));
            this.plugin = plugin;
        }
        @Override public void execute(@NotNull XCommandContext ctx) {
            String platform = "Paper " + Bukkit.getMinecraftVersion();
            ctx.reply("version", "version", plugin.getPluginMeta().getVersion(), "platform", platform);
        }
    }

    private static final class Help extends XCommand {
        Help() {
            super("help", "Show xCore commands.", "xcore.admin", "/xcore help", List.of("?"));
        }
        @Override public void execute(@NotNull XCommandContext ctx) {
            ctx.sender().sendMessage(XCore.api().messages().prefix()
                    .append(net.kyori.adventure.text.Component.text(
                            "Available: reload, plugins, services, messenger, version, help")));
        }
    }
}
