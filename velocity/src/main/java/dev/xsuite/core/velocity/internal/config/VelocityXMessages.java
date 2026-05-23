package dev.xsuite.core.velocity.internal.config;

import dev.xsuite.core.api.XPluginHandle;
import dev.xsuite.core.api.config.XConfig;
import dev.xsuite.core.api.config.XMessages;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class VelocityXMessages implements XMessages {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Path dataDir;
    private final String resourceName;
    private final Supplier<Component> prefixSupplier;
    private final VelocityXMessages fallback;
    private final ClassLoader resourceLoader;

    private VelocityXConfig backing;

    public VelocityXMessages(@NotNull Path dataDir,
                             @NotNull String resourceName,
                             @NotNull Supplier<Component> prefixSupplier,
                             @NotNull ClassLoader resourceLoader,
                             VelocityXMessages fallback) {
        this.dataDir = dataDir;
        this.resourceName = resourceName;
        this.prefixSupplier = prefixSupplier;
        this.resourceLoader = resourceLoader;
        this.fallback = fallback;
        reload();
    }

    @Override public @NotNull Component prefix() { return prefixSupplier.get(); }

    @Override
    public @NotNull Component get(@NotNull String key) {
        return get(key, Map.of());
    }

    @Override
    public @NotNull Component get(@NotNull String key, @NotNull Map<String, ?> placeholders) {
        String raw = backing.getString(key);
        if (raw == null && fallback != null) return fallback.get(key, placeholders);
        if (raw == null) return Component.text("<missing key: " + key + ">");
        TagResolver.Builder resolver = TagResolver.builder();
        placeholders.forEach((k, v) -> resolver.resolver(Placeholder.parsed(k, String.valueOf(v))));
        return MM.deserialize(raw, resolver.build());
    }

    @Override
    public @NotNull Component get(@NotNull String key, @NotNull Object... placeholders) {
        if (placeholders.length == 0) return get(key);
        if ((placeholders.length & 1) != 0) {
            throw new IllegalArgumentException("placeholders must come in key/value pairs");
        }
        Map<String, Object> map = new HashMap<>(placeholders.length / 2);
        for (int i = 0; i < placeholders.length; i += 2) {
            map.put(String.valueOf(placeholders[i]), placeholders[i + 1]);
        }
        return get(key, map);
    }

    @Override public void send(@NotNull Audience audience, @NotNull String key) {
        audience.sendMessage(get(key));
    }

    @Override public void send(@NotNull Audience audience, @NotNull String key, @NotNull Object... placeholders) {
        audience.sendMessage(get(key, placeholders));
    }

    @Override
    public void sendPrefixed(@NotNull Audience audience, @NotNull String key, @NotNull Object... placeholders) {
        Component body = placeholders.length == 0 ? get(key) : get(key, placeholders);
        audience.sendMessage(prefix().append(body));
    }

    @Override
    public void reload() {
        Path target = dataDir.resolve(resourceName);
        try {
            if (!Files.exists(target)) {
                if (!Files.exists(dataDir)) Files.createDirectories(dataDir);
                try (InputStream in = resourceLoader.getResourceAsStream(resourceName)) {
                    if (in != null) Files.copy(in, target);
                }
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to materialize " + resourceName, e);
        }
        backing = new VelocityXConfig(target, resourceName);
    }

    @Override
    public @NotNull XMessages forPlugin(@NotNull XPluginHandle plugin) {
        return new VelocityXMessages(
                plugin.dataFolder(),
                "messages.yml",
                prefixSupplier,
                plugin.getClass().getClassLoader(),
                this);
    }

    /** Escape hatch for internal use. */
    public @NotNull XConfig backing() { return backing; }
}
