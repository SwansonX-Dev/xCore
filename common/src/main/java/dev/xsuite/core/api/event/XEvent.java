package dev.xsuite.core.api.event;

/**
 * Base class for all events posted on the xCore {@link XEventBus}.
 *
 * <p>Subclasses that should be cancellable additionally implement
 * {@link Cancellable}. These are NOT Bukkit events — they live in xCore and
 * dispatch independently of the Bukkit event system.
 */
public abstract class XEvent {

    private final long createdAtNanos = System.nanoTime();

    public long createdAtNanos() {
        return createdAtNanos;
    }
}
