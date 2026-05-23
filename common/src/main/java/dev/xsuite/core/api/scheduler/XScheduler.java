package dev.xsuite.core.api.scheduler;

import dev.xsuite.core.api.XPluginHandle;
import org.jetbrains.annotations.NotNull;

/**
 * Platform-neutral scheduler. Times are in milliseconds so callers don't
 * have to think about ticks-vs-real-time differences between Paper and
 * Velocity.
 *
 * <p>The Paper-side implementation additionally exposes tick- and
 * region-aware overloads through {@code XPaperScheduler}.
 *
 * <p>"now" means "as soon as the scheduler can dispatch" — on Paper that's
 * the next global tick; on Velocity it's immediate on its event loop.
 */
public interface XScheduler {

    @NotNull XTask runNow(@NotNull XPluginHandle owner, @NotNull Runnable task);

    @NotNull XTask runLater(@NotNull XPluginHandle owner, @NotNull Runnable task, long delayMillis);

    @NotNull XTask runRepeating(@NotNull XPluginHandle owner,
                                @NotNull Runnable task,
                                long delayMillis,
                                long periodMillis);

    @NotNull XTask runAsync(@NotNull XPluginHandle owner, @NotNull Runnable task);

    @NotNull XTask runAsyncLater(@NotNull XPluginHandle owner, @NotNull Runnable task, long delayMillis);

    @NotNull XTask runAsyncRepeating(@NotNull XPluginHandle owner,
                                     @NotNull Runnable task,
                                     long delayMillis,
                                     long periodMillis);
}
