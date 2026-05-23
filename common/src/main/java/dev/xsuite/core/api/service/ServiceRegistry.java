package dev.xsuite.core.api.service;

import dev.xsuite.core.api.XPluginHandle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

/**
 * Typed registry where xSuite plugins publish capabilities ("services") that
 * other plugins can consume without compile-time dependencies.
 */
public interface ServiceRegistry {

    <T> void register(@NotNull Class<T> type, @NotNull T impl, @NotNull XPluginHandle owner);

    <T> void replace(@NotNull Class<T> type, @NotNull T impl, @NotNull XPluginHandle owner);

    <T> @NotNull Optional<T> get(@NotNull Class<T> type);

    <T> @NotNull T require(@NotNull Class<T> type);

    void unregister(@NotNull Class<?> type);

    void unregisterAll(@NotNull XPluginHandle owner);

    @NotNull Set<Class<?>> registeredTypes();

    @Nullable XPluginHandle ownerOf(@NotNull Class<?> type);
}
