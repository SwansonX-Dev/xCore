package dev.xsuite.core.velocity.internal.command;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.xsuite.core.api.XCore;
import dev.xsuite.core.api.XPluginHandle;
import dev.xsuite.core.api.command.XCommand;
import dev.xsuite.core.api.command.XCommandContext;
import dev.xsuite.core.api.command.XCommandHalt;
import dev.xsuite.core.api.command.XCommandManager;
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

public final class VelocityXCommandManager implements XCommandManager {

    private record Entry(XPluginHandle owner, XCommand command, CommandMeta meta) {}

    private final Map<String, Entry> registered = new ConcurrentHashMap<>();
    private final ProxyServer proxy;
    private final PluginContainer xCoreContainer;
    private final Logger log;

    public VelocityXCommandManager(@NotNull ProxyServer proxy,
                                   @NotNull PluginContainer xCoreContainer,
                                   @NotNull Logger log) {
        this.proxy = proxy;
        this.xCoreContainer = xCoreContainer;
        this.log = log;
    }

    @Override
    public void register(@NotNull XPluginHandle owner, @NotNull XCommand command) {
        String key = command.name().toLowerCase(Locale.ROOT);
        if (registered.containsKey(key)) {
            throw new IllegalStateException("Command /" + key + " is already registered");
        }
        CommandManager cm = proxy.getCommandManager();
        CommandMeta meta = cm.metaBuilder(command.name())
                .aliases(command.aliases().toArray(new String[0]))
                .plugin(xCoreContainer)
                .build();
        SimpleCommand bridge = new BridgeCommand(command, owner);
        cm.register(meta, bridge);
        registered.put(key, new Entry(owner, command, meta));
    }

    @Override
    public void unregister(@NotNull XCommand command) {
        Entry entry = registered.remove(command.name().toLowerCase(Locale.ROOT));
        if (entry != null) {
            proxy.getCommandManager().unregister(entry.meta);
        }
    }

    @Override
    public void unregisterAll(@NotNull XPluginHandle owner) {
        for (Map.Entry<String, Entry> e : new ArrayList<>(registered.entrySet())) {
            if (e.getValue().owner.equals(owner)) {
                proxy.getCommandManager().unregister(e.getValue().meta);
                registered.remove(e.getKey());
            }
        }
    }

    @Override
    public @NotNull Set<String> registeredNames() {
        return Collections.unmodifiableSet(registered.keySet());
    }

    private final class BridgeCommand implements SimpleCommand {
        private final XCommand command;
        private final XPluginHandle owner;

        BridgeCommand(XCommand command, XPluginHandle owner) {
            this.command = command;
            this.owner = owner;
        }

        @Override
        public void execute(@NotNull Invocation invocation) {
            VelocityXSender sender = new VelocityXSender(invocation.source());
            if (!command.permission().isEmpty() && !sender.hasPermission(command.permission())) {
                XCore.api().messages().sendPrefixed(sender, "no-permission");
                return;
            }
            try {
                dispatch(command, new XCommandContext(sender, command.name(), invocation.arguments(), owner));
            } catch (XCommandHalt halt) {
                // already messaged
            } catch (Throwable t) {
                log.warn("[xCore/commands] /{} threw", command.name(), t);
            }
        }

        @Override
        public @NotNull List<String> suggest(@NotNull Invocation invocation) {
            try {
                VelocityXSender sender = new VelocityXSender(invocation.source());
                String[] args = invocation.arguments();
                XCommand current = command;
                XCommandContext ctx = new XCommandContext(sender, command.name(), args, owner);
                while (args.length > 1) {
                    XCommand sub = current.resolveSub(args[0]);
                    if (sub == null) break;
                    current = sub;
                    args = Arrays.copyOfRange(args, 1, args.length);
                    ctx = new XCommandContext(sender, command.name(), args, owner);
                }
                String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
                return current.tabComplete(ctx).stream()
                        .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix))
                        .collect(Collectors.toList());
            } catch (Throwable t) {
                return Collections.emptyList();
            }
        }

        @Override
        public boolean hasPermission(@NotNull Invocation invocation) {
            return command.permission().isEmpty()
                    || invocation.source().hasPermission(command.permission());
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
