package dev.xsuite.core.velocity.internal.config;

import dev.xsuite.core.api.config.XConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SnakeYAML-backed XConfig for Velocity. Uses dotted paths to navigate a
 * nested map, mirroring the Bukkit YamlConfiguration behavior.
 *
 * <p>SnakeYAML is brought in transitively by velocity-api.
 */
public final class VelocityXConfig implements XConfig {

    private final Path file;
    private final String name;
    private Map<String, Object> root;

    public VelocityXConfig(@NotNull Path file, @NotNull String name) {
        this.file = file;
        this.name = name;
        reload();
    }

    @Override public @NotNull String name() { return name; }

    @SuppressWarnings("unchecked")
    private Object resolve(String path) {
        String[] parts = path.split("\\.");
        Object node = root;
        for (String part : parts) {
            if (!(node instanceof Map<?, ?> map)) return null;
            node = ((Map<String, Object>) map).get(part);
            if (node == null) return null;
        }
        return node;
    }

    @SuppressWarnings("unchecked")
    private void assign(String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> node = root;
        for (int i = 0; i < parts.length - 1; i++) {
            Object child = node.get(parts[i]);
            if (!(child instanceof Map<?, ?>)) {
                child = new LinkedHashMap<String, Object>();
                node.put(parts[i], child);
            }
            node = (Map<String, Object>) child;
        }
        if (value == null) node.remove(parts[parts.length - 1]);
        else node.put(parts[parts.length - 1], value);
    }

    @Override public @Nullable String getString(@NotNull String path) {
        Object v = resolve(path);
        return v == null ? null : v.toString();
    }

    @Override public @NotNull String getString(@NotNull String path, @NotNull String def) {
        String v = getString(path);
        return v != null ? v : def;
    }

    @Override public int getInt(@NotNull String path, int def) {
        Object v = resolve(path);
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        return def;
    }

    @Override public long getLong(@NotNull String path, long def) {
        Object v = resolve(path);
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s) try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
        return def;
    }

    @Override public double getDouble(@NotNull String path, double def) {
        Object v = resolve(path);
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) try { return Double.parseDouble(s); } catch (NumberFormatException ignored) {}
        return def;
    }

    @Override public boolean getBoolean(@NotNull String path, boolean def) {
        Object v = resolve(path);
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return Boolean.parseBoolean(s);
        return def;
    }

    @Override
    public @NotNull List<String> getStringList(@NotNull String path) {
        Object v = resolve(path);
        if (!(v instanceof List<?> raw)) return Collections.emptyList();
        List<String> out = new ArrayList<>(raw.size());
        for (Object o : raw) out.add(String.valueOf(o));
        return out;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull Set<String> getKeys(@NotNull String path) {
        Object v = resolve(path);
        if (!(v instanceof Map<?, ?> map)) return Set.of();
        return new LinkedHashSet<>(((Map<String, Object>) map).keySet());
    }

    @Override public boolean contains(@NotNull String path) { return resolve(path) != null; }
    @Override public void set(@NotNull String path, @Nullable Object value) { assign(path, value); }

    @Override
    public void save() {
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            DumperOptions opts = new DumperOptions();
            opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            opts.setPrettyFlow(true);
            Yaml yaml = new Yaml(opts);
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                yaml.dump(root, w);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save " + name, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void reload() {
        if (!Files.exists(file)) {
            root = new LinkedHashMap<>();
            return;
        }
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Object loaded = new Yaml().load(r);
            root = loaded instanceof Map ? new LinkedHashMap<>((Map<String, Object>) loaded) : new LinkedHashMap<>();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + name, e);
        }
    }
}
