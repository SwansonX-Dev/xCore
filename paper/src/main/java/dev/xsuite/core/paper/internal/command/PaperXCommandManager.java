package dev.xsuite.core.paper.internal.command;

import dev.xsuite.core.api.XCore;
import dev.xsuite.core.api.XPluginHandle;
import dev.xsuite.core.api.command.XCommand;
import dev.xsuite.core.api.command.XCommandContext;
import dev.xsuite.core.api.command.XCommandHalt;
import dev.xsuite.core.api.command.XCommandManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class PaperXCommandManager implements XCommandManager {

    private record Entry(XPluginHandle owner, XCommand command, BukkitBridge bridge) {}

    private final Map<String, Entry> registered = new ConcurrentHashMap<>();
    private final Logger log;

    public PaperXCommandManager(@NotNull Logger log) {
        this.log = log;
    }

    @Override
    public void register(@NotNull XPluginHandle owner, @NotNull XCommand command) {
        String key = command.name().toLowerCase(Locale.ROOT);
        if (registered.containsKey(key)) {
            throw new IllegalStateException("Command /" + key + " is already registered");
        }
        BukkitBridge bridge = new BukkitBridge(command, owner);
        CommandMap map = Bukkit.getCommandMap();
        map.register(owner.name().toLowerCase(Locale.ROOT), bridge);
        registered.put(key, new Entry(owner, command, bridge));
    }

    @Override
    public void unregister(@NotNull XCommand command) {
        Entry entry = registered.remove(command.name().toLowerCase(Locale.ROOT));
        if (entry != null) {
            entry.bridge.unregister(Bukkit.getCommandMap());
        }
    }

    @Override
    public void unregisterAll(@NotNull XPluginHandle owner) {
        for (Map.Entry<String, Entry> e : new ArrayList<>(registered.entrySet())) {
            if (e.getValue().owner.equals(owner)) {
                e.getValue().bridge.unregister(Bukkit.getCommandMap());
                registered.remove(e.getKey());
            }
        }
    }

    @Override
    public @NotNull Set<String> registeredNames() {
        return Collections.unmodifiableSet(registered.keySet());
    }

    private final class BukkitBridge extends Command {
        private final XCommand command;
        private final XPluginHandle owner;

        BukkitBridge(XCommand command, XPluginHandle owner) {
            super(command.name(),
                    command.description(),
                    command.usage().isEmpty() ? "/" + command.name() : command.usage(),
                    command.aliases());
            this.command = command;
            this.owner = owner;
            // Intentionally NOT calling setPermission(): on Paper, a command whose
            // permission the sender lacks is hidden from their command tree and
            // resolves as "Unknown command", which hides admin commands from staff
            // who simply haven't been granted the node. We enforce the permission
            // in execute()/tabComplete() instead, so the command stays visible and
            // a clear "no-permission" message is sent.
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
            if (!command.permission().isEmpty() && !sender.hasPermission(command.permission())) {
                XCore.api().messages().sendPrefixed(new PaperXSender(sender), "no-permission");
                return true;
            }
            try {
                dispatch(command, new XCommandContext(new PaperXSender(sender), label, args, owner));
            } catch (XCommandHalt halt) {
                // already messaged
            } catch (Throwable t) {
                log.warn("[xCore/commands] /{} threw", command.name(), t);
            }
            return true;
        }

        @Override
        public @NotNull List<String> tabComplete(@NotNull CommandSender sender,
                                                 @NotNull String alias,
                                                 @NotNull String[] args) {
            if (!command.permission().isEmpty() && !sender.hasPermission(command.permission())) {
                return Collections.emptyList();
            }
            try {
                XCommand current = command;
                XCommandContext ctx = new XCommandContext(new PaperXSender(sender), alias, args, owner);
                while (args.length > 1) {
                    XCommand sub = current.resolveSub(args[0]);
                    if (sub == null) break;
                    current = sub;
                    args = Arrays.copyOfRange(args, 1, args.length);
                    ctx = new XCommandContext(new PaperXSender(sender), alias, args, owner);
                }
                String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
                return current.tabComplete(ctx).stream()
                        .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix))
                        .collect(Collectors.toList());
            } catch (Throwable t) {
                return Collections.emptyList();
            }
        }
    }

    private static void dispatch(XCommand command, XCommandContext ctx) {
        if (ctx.argCount() > 0) {
            XCommand sub = command.resolveSub(ctx.arg(0));
            if (sub != null) {
                dispatch(sub, ctx.shift());
                return;
            }
        }
        command.execute(ctx);
    }
}
