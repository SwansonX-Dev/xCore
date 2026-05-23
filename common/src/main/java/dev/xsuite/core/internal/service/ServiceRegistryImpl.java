package dev.xsuite.core.internal.service;

import dev.xsuite.core.api.XPluginHandle;
import dev.xsuite.core.api.service.ServiceRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ServiceRegistryImpl implements ServiceRegistry {

    private record Registration(Object impl, XPluginHandle owner) {}

    private final ConcurrentHashMap<Class<?>, Registration> services = new ConcurrentHashMap<>();
    private final Logger log;
    private final boolean debug;

    public ServiceRegistryImpl(@NotNull Logger log, boolean debug) {
        this.log = log;
        this.debug = debug;
    }

    @Override
    public <T> void register(@NotNull Class<T> type, @NotNull T impl, @NotNull XPluginHandle owner) {
        if (services.putIfAbsent(type, new Registration(impl, owner)) != null) {
            throw new IllegalStateException("Service " + type.getName() + " is already registered");
        }
        if (debug) log.info("[xCore/services] {} registered {}", owner.name(), type.getName());
    }

    @Override
    public <T> void replace(@NotNull Class<T> type, @NotNull T impl, @NotNull XPluginHandle owner) {
        Registration prev = services.put(type, new Registration(impl, owner));
        if (debug) {
            log.info("[xCore/services] {} replaced {}{}",
                    owner.name(), type.getName(),
                    prev == null ? "" : " (was owned by " + prev.owner.name() + ")");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @NotNull Optional<T> get(@NotNull Class<T> type) {
        Registration reg = services.get(type);
        return reg == null ? Optional.empty() : Optional.of((T) reg.impl);
    }

    @Override
    public <T> @NotNull T require(@NotNull Class<T> type) {
        return this.<T>get(type).orElseThrow(() ->
                new IllegalStateException("No xSuite service registered for " + type.getName()));
    }

    @Override
    public void unregister(@NotNull Class<?> type) {
        Registration removed = services.remove(type);
        if (debug && removed != null) {
            log.info("[xCore/services] unregistered {}", type.getName());
        }
    }

    @Override
    public void unregisterAll(@NotNull XPluginHandle owner) {
        services.entrySet().removeIf(e -> {
            boolean match = e.getValue().owner.equals(owner);
            if (match && debug) {
                log.info("[xCore/services] {} unregistered (cascade) {}",
                        owner.name(), e.getKey().getName());
            }
            return match;
        });
    }

    @Override
    public @NotNull Set<Class<?>> registeredTypes() {
        return Collections.unmodifiableSet(new HashSet<>(services.keySet()));
    }

    @Override
    public @Nullable XPluginHandle ownerOf(@NotNull Class<?> type) {
        Registration reg = services.get(type);
        return reg == null ? null : reg.owner;
    }
}
