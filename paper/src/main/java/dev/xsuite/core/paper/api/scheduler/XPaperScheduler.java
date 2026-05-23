package dev.xsuite.core.paper.api.scheduler;

import dev.xsuite.core.api.XPluginHandle;
import dev.xsuite.core.api.scheduler.XScheduler;
import dev.xsuite.core.api.scheduler.XTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * Paper-specific scheduler. Adds tick-based and region-aware overloads on
 * top of the common millisecond surface. Folia-safe.
 */
public interface XPaperScheduler extends XScheduler {

    @NotNull XTask runLaterTicks(@NotNull XPluginHandle owner, @NotNull Runnable task, long delayTicks);

    @NotNull XTask runRepeatingTicks(@NotNull XPluginHandle owner,
                                     @NotNull Runnable task,
                                     long delayTicks,
                                     long periodTicks);

    /** Run on the region that owns {@code location}. */
    @NotNull XTask runAt(@NotNull XPluginHandle owner, @NotNull Location location, @NotNull Runnable task);

    /** Run on the region that owns {@code entity}. No-op if the entity is already removed. */
    @NotNull XTask runAt(@NotNull XPluginHandle owner, @NotNull Entity entity, @NotNull Runnable task);
}
