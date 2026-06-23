package dev.xsuite.core.paper.internal.guide;

import dev.xsuite.core.api.XPluginHandle;
import dev.xsuite.core.paper.api.guide.GuideEntry;
import dev.xsuite.core.paper.api.guide.GuideRegistry;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Default {@link GuideRegistry}. Entries are keyed by id and kept in insertion
 * order; config-seeded externals are added first, then plugins register their
 * own (replacing a same-id external fallback).
 */
public final class GuideRegistryImpl implements GuideRegistry {

    private final Map<String, GuideEntry> entries = new LinkedHashMap<>();
    private final Logger log;

    public GuideRegistryImpl(@NotNull Logger log, @NotNull Collection<GuideEntry> externalSeed) {
        this.log = log;
        for (GuideEntry e : externalSeed) {
            entries.put(e.id(), e);
        }
    }

    @Override
    public void register(@NotNull XPluginHandle owner, @NotNull GuideEntry entry) {
        entries.put(entry.id(), entry);
        log.info("[xCore/guide] {} registered guide entry '{}'", owner.name(), entry.id());
    }

    @Override
    public void unregister(@NotNull String id) {
        entries.remove(id.toLowerCase(java.util.Locale.ROOT));
    }

    @Override
    public @NotNull List<GuideEntry> entries() {
        return new ArrayList<>(entries.values());
    }

    @Override
    public @NotNull Optional<GuideEntry> get(@NotNull String id) {
        return Optional.ofNullable(entries.get(id.toLowerCase(java.util.Locale.ROOT)));
    }
}
