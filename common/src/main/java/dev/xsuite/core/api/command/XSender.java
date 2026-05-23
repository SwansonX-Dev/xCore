package dev.xsuite.core.api.command;

import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * Platform-neutral command sender. Wraps Bukkit's CommandSender on Paper
 * and Velocity's CommandSource on the proxy, and exposes the bits of behavior
 * x plugins actually need.
 *
 * <p>Implements {@link Audience} so any Adventure-based code (e.g. MiniMessage)
 * can render straight to the sender.
 */
public interface XSender extends Audience {

    @NotNull String name();

    boolean isPlayer();

    boolean isConsole();

    /** Player UUID if this sender is a player, otherwise empty. */
    @NotNull Optional<UUID> uuid();

    boolean hasPermission(@NotNull String permission);

    /** The underlying platform sender (Bukkit {@code CommandSender} or Velocity {@code CommandSource}). */
    @NotNull Object platformSender();
}
