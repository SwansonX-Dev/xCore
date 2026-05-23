package dev.xsuite.core.paper.internal.config;

import dev.xsuite.core.api.XPluginHandle;
import dev.xsuite.core.api.config.XMessages;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class PaperXMessages implements XMessages {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Plugin plugin;
    private final File file;
    private final String resourceName;
    private final Supplier<Component> prefixSupplier;
    private final PaperXMessages fallback;

    private YamlConfiguration yaml;

    public PaperXMessages(@NotNull Plugin plugin,
                          @NotNull File file,
                          @NotNull String resourceName,
                          @NotNull Supplier<Component> prefixSupplier,
                          PaperXMessages fallback) {
        this.plugin = plugin;
        this.file = file;
        this.resourceName = resourceName;
        this.prefixSupplier = prefixSupplier;
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
        String raw = yaml.getString(key);
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
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.getParentFile().mkdirs();
        }
        if (!file.exists() && plugin.getResource(resourceName) != null) {
            plugin.saveResource(resourceName, false);
        }
        yaml = YamlConfiguration.loadConfiguration(file);
        InputStream defaults = plugin.getResource(resourceName);
        if (defaults != null) {
            YamlConfiguration jarYaml = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaults, StandardCharsets.UTF_8));
            yaml.setDefaults(jarYaml);
        }
    }

    @Override
    public @NotNull XMessages forPlugin(@NotNull XPluginHandle other) {
        if (!(other instanceof Plugin otherPlugin)) {
            // Foreign-handle fallback: messages always come from xCore's own catalog.
            return this;
        }
        File scoped = new File(otherPlugin.getDataFolder(), "messages.yml");
        return new PaperXMessages(otherPlugin, scoped, "messages.yml", prefixSupplier, this);
    }
}
