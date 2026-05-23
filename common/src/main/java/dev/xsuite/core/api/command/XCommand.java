package dev.xsuite.core.api.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Base class for commands registered through {@link XCommandManager}.
 *
 * <p>Identical on Paper and Velocity. Subclasses override {@link #execute}
 * for behavior and optionally {@link #tabComplete} for completions.
 * Subcommands are registered via {@link #addSubcommand(String, XCommand)}
 * and dispatched automatically.
 */
public abstract class XCommand {

    private final String name;
    private final List<String> aliases;
    private final String description;
    private final String permission;
    private final String usage;

    private final Map<String, XCommand> subcommands = new LinkedHashMap<>();

    protected XCommand(@NotNull String name) {
        this(name, "", "", "", Collections.emptyList());
    }

    protected XCommand(@NotNull String name,
                       @NotNull String description,
                       @NotNull String permission,
                       @NotNull String usage,
                       @NotNull List<String> aliases) {
        this.name = name.toLowerCase(Locale.ROOT);
        this.description = description;
        this.permission = permission;
        this.usage = usage;
        this.aliases = List.copyOf(aliases);
    }

    public final @NotNull String name() { return name; }
    public final @NotNull List<String> aliases() { return aliases; }
    public final @NotNull String description() { return description; }
    public final @NotNull String permission() { return permission; }
    public final @NotNull String usage() { return usage; }

    public final @NotNull Map<String, XCommand> subcommands() {
        return Collections.unmodifiableMap(subcommands);
    }

    protected final void addSubcommand(@NotNull String label, @NotNull XCommand sub) {
        subcommands.put(label.toLowerCase(Locale.ROOT), sub);
    }

    public final @Nullable XCommand resolveSub(@NotNull String label) {
        String key = label.toLowerCase(Locale.ROOT);
        XCommand direct = subcommands.get(key);
        if (direct != null) return direct;
        for (XCommand sub : subcommands.values()) {
            if (sub.aliases.contains(key)) return sub;
        }
        return null;
    }

    public abstract void execute(@NotNull XCommandContext ctx);

    public @NotNull List<String> tabComplete(@NotNull XCommandContext ctx) {
        if (ctx.args().length <= 1 && !subcommands.isEmpty()) {
            return List.copyOf(subcommands.keySet());
        }
        return Collections.emptyList();
    }
}
