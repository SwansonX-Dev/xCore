package dev.xsuite.core.api.event;

import dev.xsuite.core.api.XPluginHandle;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Cross-plugin event bus. Local to this node — for cross-server events,
 * see {@link dev.xsuite.core.api.messenger.XMessenger}.
 */
public interface XEventBus {

    <E extends XEvent> @NotNull XEventSubscription subscribe(
            @NotNull XPluginHandle owner,
            @NotNull Class<E> type,
            @NotNull Consumer<E> handler);

    <E extends XEvent> @NotNull XEventSubscription subscribe(
            @NotNull XPluginHandle owner,
            @NotNull Class<E> type,
            @NotNull XEventPriority priority,
            @NotNull Consumer<E> handler);

    /** Deliver {@code event} synchronously on the caller's thread. */
    <E extends XEvent> @NotNull E post(@NotNull E event);

    /** Deliver {@code event} on xCore's async pool. */
    void postAsync(@NotNull XEvent event);

    void unsubscribeAll(@NotNull XPluginHandle owner);

    int subscriberCount(@NotNull Class<? extends XEvent> type);
}
