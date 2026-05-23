package dev.xsuite.core.paper.internal.scheduler;

import dev.xsuite.core.api.XPluginHandle;
import dev.xsuite.core.api.scheduler.XTask;
import dev.xsuite.core.paper.api.scheduler.XPaperScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Routes through Paper's modern schedulers (Global / Async / Region / Entity),
 * which exist on both Paper and Folia. Never touches {@code Bukkit.getScheduler()}.
 */
public final class XPaperSchedulerImpl implements XPaperScheduler {

    private static final long MS_PER_TICK = 50L;

    private final JavaPlugin xCorePlugin;

    public XPaperSchedulerImpl(@NotNull JavaPlugin xCorePlugin) {
        this.xCorePlugin = xCorePlugin;
    }

    /**
     * xCore's own JavaPlugin is used as the {@link Plugin} owner of every
     * scheduled task; xSuite plugins identify themselves at the API layer
     * through {@link XPluginHandle}, but the platform scheduler still needs
     * a JavaPlugin reference to schedule against.
     */
    private Plugin platformOwner(XPluginHandle handle) {
        if (handle instanceof JavaPlugin jp) return jp;
        return xCorePlugin;
    }

    @Override
    public @NotNull XTask runNow(@NotNull XPluginHandle owner, @NotNull Runnable task) {
        return wrap(Bukkit.getGlobalRegionScheduler().run(platformOwner(owner), t -> task.run()));
    }

    @Override
    public @NotNull XTask runLater(@NotNull XPluginHandle owner, @NotNull Runnable task, long delayMillis) {
        long ticks = Math.max(1, delayMillis / MS_PER_TICK);
        return runLaterTicks(owner, task, ticks);
    }

    @Override
    public @NotNull XTask runRepeating(@NotNull XPluginHandle owner,
                                       @NotNull Runnable task,
                                       long delayMillis,
                                       long periodMillis) {
        long d = Math.max(1, delayMillis / MS_PER_TICK);
        long p = Math.max(1, periodMillis / MS_PER_TICK);
        return runRepeatingTicks(owner, task, d, p);
    }

    @Override
    public @NotNull XTask runLaterTicks(@NotNull XPluginHandle owner, @NotNull Runnable task, long delayTicks) {
        long safe = Math.max(1, delayTicks);
        return wrap(Bukkit.getGlobalRegionScheduler().runDelayed(platformOwner(owner), t -> task.run(), safe));
    }

    @Override
    public @NotNull XTask runRepeatingTicks(@NotNull XPluginHandle owner,
                                            @NotNull Runnable task,
                                            long delayTicks,
                                            long periodTicks) {
        long d = Math.max(1, delayTicks);
        long p = Math.max(1, periodTicks);
        return wrap(Bukkit.getGlobalRegionScheduler().runAtFixedRate(platformOwner(owner), t -> task.run(), d, p));
    }

    @Override
    public @NotNull XTask runAsync(@NotNull XPluginHandle owner, @NotNull Runnable task) {
        return wrap(Bukkit.getAsyncScheduler().runNow(platformOwner(owner), t -> task.run()));
    }

    @Override
    public @NotNull XTask runAsyncLater(@NotNull XPluginHandle owner, @NotNull Runnable task, long delayMillis) {
        long safe = Math.max(1, delayMillis);
        return wrap(Bukkit.getAsyncScheduler()
                .runDelayed(platformOwner(owner), t -> task.run(), safe, TimeUnit.MILLISECONDS));
    }

    @Override
    public @NotNull XTask runAsyncRepeating(@NotNull XPluginHandle owner,
                                            @NotNull Runnable task,
                                            long delayMillis,
                                            long periodMillis) {
        long d = Math.max(1, delayMillis);
        long p = Math.max(1, periodMillis);
        return wrap(Bukkit.getAsyncScheduler()
                .runAtFixedRate(platformOwner(owner), t -> task.run(), d, p, TimeUnit.MILLISECONDS));
    }

    @Override
    public @NotNull XTask runAt(@NotNull XPluginHandle owner, @NotNull Location location, @NotNull Runnable task) {
        return wrap(Bukkit.getRegionScheduler().run(platformOwner(owner), location, t -> task.run()));
    }

    @Override
    public @NotNull XTask runAt(@NotNull XPluginHandle owner, @NotNull Entity entity, @NotNull Runnable task) {
        ScheduledTask scheduled = entity.getScheduler().run(platformOwner(owner), t -> task.run(), null);
        if (scheduled == null) {
            return new XTask() {
                @Override public void cancel() {}
                @Override public boolean isCancelled() { return true; }
            };
        }
        return wrap(scheduled);
    }

    private static XTask wrap(ScheduledTask task) {
        return new XTask() {
            @Override public void cancel() { task.cancel(); }
            @Override public boolean isCancelled() { return task.isCancelled(); }
        };
    }
}
