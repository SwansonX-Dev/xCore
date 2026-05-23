package dev.xsuite.core.api.scheduler;

/** Handle to a task scheduled through {@link XScheduler}. {@link #cancel()} is idempotent. */
public interface XTask {
    void cancel();

    boolean isCancelled();
}
