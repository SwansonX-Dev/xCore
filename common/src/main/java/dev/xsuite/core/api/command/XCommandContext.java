package dev.xsuite.core.api.command;

import dev.xsuite.core.api.XCore;
import dev.xsuite.core.api.XPluginHandle;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Per-invocation context passed to {@link XCommand#execute}.
 * Holds the sender, label and args, plus typed parsing helpers.
 */
public final class XCommandContext {

    private final XSender sender;
    private final String label;
    private final String[] args;
    private final XPluginHandle owner;

    public XCommandContext(@NotNull XSender sender,
                           @NotNull String label,
                           @NotNull String[] args,
                           @NotNull XPluginHandle owner) {
        this.sender = sender;
        this.label = label;
        this.args = args;
        this.owner = owner;
    }

    public @NotNull XSender sender() { return sender; }
    public @NotNull String label() { return label; }
    public @NotNull String[] args() { return args; }
    public @NotNull XPluginHandle owner() { return owner; }
    public int argCount() { return args.length; }

    public @Nullable String arg(int index) {
        return index >= 0 && index < args.length ? args[index] : null;
    }

    public @NotNull String argOr(int index, @NotNull String fallback) {
        String v = arg(index);
        return v != null ? v : fallback;
    }

    public @NotNull OptionalInt argInt(int index) {
        String v = arg(index);
        if (v == null) return OptionalInt.empty();
        try {
            return OptionalInt.of(Integer.parseInt(v));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    public @NotNull OptionalLong argLong(int index) {
        String v = arg(index);
        if (v == null) return OptionalLong.empty();
        try {
            return OptionalLong.of(Long.parseLong(v));
        } catch (NumberFormatException e) {
            return OptionalLong.empty();
        }
    }

    public @NotNull String joinFrom(int start) {
        if (start >= args.length) return "";
        return String.join(" ", Arrays.copyOfRange(args, start, args.length));
    }

    public boolean has(@NotNull String permission) {
        return permission.isEmpty() || sender.hasPermission(permission);
    }

    public void requirePermission(@NotNull String permission) {
        if (!has(permission)) {
            XCore.api().messages().sendPrefixed(sender, "no-permission");
            throw new XCommandHalt();
        }
    }

    public void requirePlayer() {
        if (!sender.isPlayer()) {
            XCore.api().messages().sendPrefixed(sender, "player-only");
            throw new XCommandHalt();
        }
    }

    public void reply(@NotNull Component component) {
        sender.sendMessage(component);
    }

    public void reply(@NotNull String key, @NotNull Object... placeholders) {
        XCore.api().messages().sendPrefixed(sender, key, placeholders);
    }

    /** A new context with the leading argument removed (used during subcommand dispatch). */
    public @NotNull XCommandContext shift() {
        String[] rest = args.length == 0 ? args : Arrays.copyOfRange(args, 1, args.length);
        return new XCommandContext(sender, label, rest, owner);
    }
}
