package dev.xsuite.core.paper.api.guide;

import dev.xsuite.core.api.XPluginHandle;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Registry of {@link GuideEntry} objects backing the global {@code /guide} hub.
 *
 * <p>Published by xCore as a service: consume it with
 * {@code core().services().get(GuideRegistry.class)} and register your plugin's
 * guide in {@code onXEnable}:
 *
 * <pre>{@code
 * core().services().get(GuideRegistry.class).ifPresent(reg ->
 *     reg.register(this, new GuideEntry(
 *         "teams", "<aqua>Teams", Material.WHITE_BANNER,
 *         List.of("<gray>Group up, share a bank,", "<gray>rank up for perks."),
 *         player -> new TeamGuideMenu(this).open(player))));
 * }</pre>
 *
 * <p>Registering an entry whose {@link GuideEntry#id()} already exists replaces
 * the previous one (so a real in-process entry overrides a config-seeded
 * external fallback of the same id).
 */
public interface GuideRegistry {

    /** Add or replace (by id) a guide entry owned by {@code owner}. */
    void register(@NotNull XPluginHandle owner, @NotNull GuideEntry entry);

    /** Remove the entry with the given id, if present. */
    void unregister(@NotNull String id);

    /** All entries in registration order (config-seeded externals first). */
    @NotNull List<GuideEntry> entries();

    /** Look up a single entry by id. */
    @NotNull Optional<GuideEntry> get(@NotNull String id);
}
