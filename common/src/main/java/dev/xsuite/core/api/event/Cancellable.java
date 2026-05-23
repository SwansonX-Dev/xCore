package dev.xsuite.core.api.event;

/**
 * Marker for {@link XEvent} subclasses that handlers may cancel.
 * The bus continues delivering cancelled events to {@link XEventPriority#MONITOR}
 * handlers only — earlier-priority handlers see the cancel state and skip work.
 */
public interface Cancellable {
    boolean isCancelled();

    void setCancelled(boolean cancelled);
}
