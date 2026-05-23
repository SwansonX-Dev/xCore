package dev.xsuite.core.api.event;

/** Delivery order for {@link XEventBus} handlers. Lower ordinal runs first. */
public enum XEventPriority {
    LOWEST,
    LOW,
    NORMAL,
    HIGH,
    HIGHEST,
    MONITOR
}
