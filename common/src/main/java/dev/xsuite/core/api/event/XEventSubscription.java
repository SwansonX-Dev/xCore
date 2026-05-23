package dev.xsuite.core.api.event;

public interface XEventSubscription {
    Class<? extends XEvent> eventType();

    void unsubscribe();

    boolean isActive();
}
