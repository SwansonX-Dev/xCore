package dev.xsuite.core.paper.internal.config;

import dev.xsuite.core.api.config.XConfig;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Bukkit YamlConfiguration-backed {@link XConfig}. Used for the default
 * config.yml and any additional files opened through XPaperPlugin#config(String).
 */
public final class PaperXConfig implements XConfig {

    private final File file;
    private final String name;
    private YamlConfiguration yaml;

    public PaperXConfig(@NotNull File file, @NotNull String name) {
        this.file = file;
        this.name = name;
        reload();
    }

    @Override public @NotNull String name() { return name; }

    @Override public @Nullable String getString(@NotNull String path) { return yaml.getString(path); }
    @Override public @NotNull String getString(@NotNull String path, @NotNull String def) { return yaml.getString(path, def); }
    @Override public int getInt(@NotNull String path, int def) { return yaml.getInt(path, def); }
    @Override public long getLong(@NotNull String path, long def) { return yaml.getLong(path, def); }
    @Override public double getDouble(@NotNull String path, double def) { return yaml.getDouble(path, def); }
    @Override public boolean getBoolean(@NotNull String path, boolean def) { return yaml.getBoolean(path, def); }
    @Override public @NotNull List<String> getStringList(@NotNull String path) { return yaml.getStringList(path); }
    @Override public @NotNull Set<String> getKeys(@NotNull String path) {
        ConfigurationSection sec = yaml.getConfigurationSection(path);
        return sec == null ? Set.of() : sec.getKeys(false);
    }
    @Override public boolean contains(@NotNull String path) { return yaml.contains(path); }
    @Override public void set(@NotNull String path, @Nullable Object value) { yaml.set(path, value); }

    public @NotNull YamlConfiguration raw() { return yaml; }

    @Override
    public void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save " + name, e);
        }
    }

    @Override
    public void reload() {
        yaml = YamlConfiguration.loadConfiguration(file);
    }
}
