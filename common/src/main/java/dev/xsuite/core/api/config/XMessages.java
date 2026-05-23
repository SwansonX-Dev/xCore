package dev.xsuite.core.api.config;

import dev.xsuite.core.api.XPluginHandle;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * MiniMessage-based message catalog. Identical surface on every platform.
 *
 * <p>Keys map to MiniMessage strings; placeholders are passed at call time
 * as {@code <name>} tags.
 */
public interface XMessages {

    @NotNull Component prefix();

    @NotNull Component get(@NotNull String key);

    @NotNull Component get(@NotNull String key, @NotNull Map<String, ?> placeholders);

    /** Alternating key/value placeholders, e.g. {@code get("k", "name", "Alice")}. */
    @NotNull Component get(@NotNull String key, @NotNull Object... placeholders);

    void send(@NotNull Audience audience, @NotNull String key);

    void send(@NotNull Audience audience, @NotNull String key, @NotNull Object... placeholders);

    void sendPrefixed(@NotNull Audience audience, @NotNull String key, @NotNull Object... placeholders);

    void reload();

    /** Returns a catalog scoped to {@code plugin}, falling back to xCore for missing keys. */
    @NotNull XMessages forPlugin(@NotNull XPluginHandle plugin);
}
