package dev.xsuite.core.velocity.internal.scheduler;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import dev.xsuite.core.api.XPluginHandle;
import dev.xsuite.core.api.scheduler.XScheduler;
import dev.xsuite.core.api.scheduler.XTask;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Velocity has no main-thread / async distinction — every task runs on its
 * own worker. All {@link XScheduler} methods route through Velocity's
 * scheduler with the appropriate delay/repeat durations.
 */
public final class VelocityXScheduler implements XScheduler {

    private final ProxyServer proxy;
    private final PluginContainer xCoreContainer;

    public VelocityXScheduler(@NotNull ProxyServer proxy, @NotNull PluginContainer xCoreContainer) {
        this.proxy = proxy;
        this.xCoreContainer = xCoreContainer;
    }

    @Override
    public @NotNull XTask runNow(@NotNull XPluginHandle owner, @NotNull Runnable task) {
        return wrap(proxy.getScheduler().buildTask(xCoreContainer, task).schedule());
    }

    @Override
    public @NotNull XTask runLater(@NotNull XPluginHandle owner, @NotNull Runnable task, long delayMillis) {
        return wrap(proxy.getScheduler().buildTask(xCoreContainer, task)
                .delay(Duration.ofMillis(Math.max(1, delayMillis)))
                .schedule());
    }

    @Override
    public @NotNull XTask runRepeating(@NotNull XPluginHandle owner,
                                       @NotNull Runnable task,
                                       long delayMillis,
                                       long periodMillis) {
        return wrap(proxy.getScheduler().buildTask(xCoreContainer, task)
                .delay(Duration.ofMillis(Math.max(1, delayMillis)))
                .repeat(Duration.ofMillis(Math.max(1, periodMillis)))
                .schedule());
    }

    @Override
    public @NotNull XTask runAsync(@NotNull XPluginHandle owner, @NotNull Runnable task) {
        return runNow(owner, task);
    }

    @Override
    public @NotNull XTask runAsyncLater(@NotNull XPluginHandle owner, @NotNull Runnable task, long delayMillis) {
        return runLater(owner, task, delayMillis);
    }

    @Override
    public @NotNull XTask runAsyncRepeating(@NotNull XPluginHandle owner,
                                            @NotNull Runnable task,
                                            long delayMillis,
                                            long periodMillis) {
        return runRepeating(owner, task, delayMillis, periodMillis);
    }

    private static XTask wrap(ScheduledTask task) {
        return new XTask() {
            @Override public void cancel() { task.cancel(); }
            @Override public boolean isCancelled() {
                return task.status() == com.velocitypowered.api.scheduler.TaskStatus.CANCELLED
                        || task.status() == com.velocitypowered.api.scheduler.TaskStatus.FINISHED;
            }
        };
    }
}
