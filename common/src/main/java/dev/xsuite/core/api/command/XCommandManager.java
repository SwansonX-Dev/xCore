package dev.xsuite.core.api.command;

import dev.xsuite.core.api.XPluginHandle;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Registry of {@link XCommand} instances. Each platform's implementation
 * wires the underlying command system at register time so subclasses don't
 * need to touch platform-specific command APIs themselves.
 */
public interface XCommandManager {

    void register(@NotNull XPluginHandle owner, @NotNull XCommand command);

    void unregister(@NotNull XCommand command);

    void unregisterAll(@NotNull XPluginHandle owner);

    @NotNull Set<String> registeredNames();
}
