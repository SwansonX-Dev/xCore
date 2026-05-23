package dev.xsuite.core.api.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Platform-neutral YAML config wrapper. Each platform provides its own
 * implementation (Bukkit's YamlConfiguration on Paper, SnakeYAML directly
 * on Velocity), but the surface is identical.
 */
public interface XConfig {

    @NotNull String name();

    @Nullable String getString(@NotNull String path);
    @NotNull String getString(@NotNull String path, @NotNull String def);

    int getInt(@NotNull String path, int def);
    long getLong(@NotNull String path, long def);
    double getDouble(@NotNull String path, double def);
    boolean getBoolean(@NotNull String path, boolean def);

    @NotNull List<String> getStringList(@NotNull String path);

    boolean contains(@NotNull String path);

    void set(@NotNull String path, @Nullable Object value);

    void save();

    void reload();
}
