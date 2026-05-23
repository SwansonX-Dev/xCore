package dev.xsuite.core.internal.event;

import dev.xsuite.core.api.XPluginHandle;
import dev.xsuite.core.api.event.Cancellable;
import dev.xsuite.core.api.event.XEvent;
import dev.xsuite.core.api.event.XEventBus;
import dev.xsuite.core.api.event.XEventPriority;
import dev.xsuite.core.api.event.XEventSubscription;
import dev.xsuite.core.api.scheduler.XScheduler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class XEventBusImpl implements XEventBus {

    private final ConcurrentHashMap<Class<? extends XEvent>, CopyOnWriteArrayList<Entry<?>>> handlers =
            new ConcurrentHashMap<>();
    private final Logger log;
    private final boolean debug;
    private final XScheduler scheduler;
    private final XPluginHandle xCoreHandle;

    public XEventBusImpl(@NotNull XPluginHandle xCoreHandle,
                         @NotNull XScheduler scheduler,
                         @NotNull Logger log,
                         boolean debug) {
        this.xCoreHandle = xCoreHandle;
        this.scheduler = scheduler;
        this.log = log;
        this.debug = debug;
    }

    @Override
    public <E extends XEvent> @NotNull XEventSubscription subscribe(
            @NotNull XPluginHandle owner,
            @NotNull Class<E> type,
            @NotNull Consumer<E> handler) {
        return subscribe(owner, type, XEventPriority.NORMAL, handler);
    }

    @Override
    public <E extends XEvent> @NotNull XEventSubscription subscribe(
            @NotNull XPluginHandle owner,
            @NotNull Class<E> type,
            @NotNull XEventPriority priority,
            @NotNull Consumer<E> handler) {
        Entry<E> entry = new Entry<>(owner, type, priority, handler);
        handlers.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(entry);
        return entry;
    }

    @Override
    public <E extends XEvent> @NotNull E post(@NotNull E event) {
        deliver(event);
        return event;
    }

    @Override
    public void postAsync(@NotNull XEvent event) {
        scheduler.runAsync(xCoreHandle, () -> deliver(event));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void deliver(XEvent event) {
        if (debug) {
            log.info("[xCore/events] posting {}", event.getClass().getName());
        }
        Class<?> cls = event.getClass();
        while (cls != null && XEvent.class.isAssignableFrom(cls)) {
            List<Entry<?>> list = handlers.get(cls);
            if (list != null && !list.isEmpty()) {
                List<Entry<?>> sorted = list.stream()
                        .filter(e -> e.active)
                        .sorted(Comparator.comparingInt(e -> e.priority.ordinal()))
                        .toList();
                for (Entry entry : sorted) {
                    boolean cancelled = event instanceof Cancellable c && c.isCancelled();
                    if (cancelled && entry.priority != XEventPriority.MONITOR) continue;
                    try {
                        entry.handler.accept(event);
                    } catch (Throwable t) {
                        log.warn("[xCore/events] handler in {} threw on {}: {}",
                                entry.owner.name(), event.getClass().getSimpleName(), t.getMessage(), t);
                    }
                }
            }
            cls = cls.getSuperclass();
        }
    }

    @Override
    public void unsubscribeAll(@NotNull XPluginHandle owner) {
        handlers.values().forEach(list -> list.removeIf(e -> {
            if (e.owner.equals(owner)) {
                e.active = false;
                return true;
            }
            return false;
        }));
    }

    @Override
    public int subscriberCount(@NotNull Class<? extends XEvent> type) {
        CopyOnWriteArrayList<Entry<?>> list = handlers.get(type);
        return list == null ? 0 : list.size();
    }

    private final class Entry<E extends XEvent> implements XEventSubscription {
        final XPluginHandle owner;
        final Class<E> type;
        final XEventPriority priority;
        final Consumer<E> handler;
        volatile boolean active = true;

        Entry(XPluginHandle owner, Class<E> type, XEventPriority priority, Consumer<E> handler) {
            this.owner = owner;
            this.type = type;
            this.priority = priority;
            this.handler = handler;
        }

        @Override public Class<? extends XEvent> eventType() { return type; }
        @Override public boolean isActive() { return active; }

        @Override
        public void unsubscribe() {
            if (!active) return;
            active = false;
            CopyOnWriteArrayList<Entry<?>> list = handlers.get(type);
            if (list != null) list.remove(this);
        }
    }
}
