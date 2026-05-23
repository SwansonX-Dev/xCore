package dev.xsuite.core.internal.messenger;

import dev.xsuite.core.api.XPluginHandle;
import dev.xsuite.core.api.messenger.MessageContext;
import dev.xsuite.core.api.messenger.MessageTransport;
import dev.xsuite.core.api.messenger.NetworkMessage;
import dev.xsuite.core.api.messenger.NetworkSubscription;
import dev.xsuite.core.api.messenger.XMessenger;
import dev.xsuite.core.internal.codec.MessageCodec;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

public final class XMessengerImpl implements XMessenger {

    private final MessageTransport transport;
    private final MessageCodec codec = new MessageCodec();
    private final ConcurrentHashMap<Class<? extends NetworkMessage>, CopyOnWriteArrayList<Sub<?>>> subs =
            new ConcurrentHashMap<>();
    private final Logger log;

    public XMessengerImpl(@NotNull MessageTransport transport, @NotNull Logger log) {
        this.transport = transport;
        this.log = log;
        transport.setReceiver(this::onReceive);
    }

    public void start() {
        transport.start();
    }

    public void stop() {
        transport.stop();
    }

    @Override
    public @NotNull String selfId() {
        return transport.selfId();
    }

    @Override
    public boolean isProxyAvailable() {
        return transport.isProxyAvailable();
    }

    @Override
    public @NotNull Set<String> knownServers() {
        return transport.knownServers();
    }

    @Override
    public void register(@NotNull XPluginHandle owner, @NotNull Class<? extends NetworkMessage> type) {
        codec.register(type);
    }

    @Override
    public <M extends NetworkMessage> @NotNull NetworkSubscription subscribe(
            @NotNull XPluginHandle owner,
            @NotNull Class<M> type,
            @NotNull BiConsumer<MessageContext, M> handler) {
        codec.register(type);
        Sub<M> sub = new Sub<>(owner, type, handler);
        subs.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(sub);
        return sub;
    }

    @Override
    public @NotNull CompletableFuture<Void> sendTo(@NotNull String target, @NotNull NetworkMessage message) {
        return dispatch(target, message);
    }

    @Override
    public @NotNull CompletableFuture<Void> broadcast(@NotNull NetworkMessage message) {
        return dispatch(MessageEnvelope.BROADCAST, message);
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<Void> dispatch(String target, NetworkMessage message) {
        codec.register((Class<? extends NetworkMessage>) message.getClass());
        try {
            byte[] payload = codec.encodePayload(message);
            MessageEnvelope env = new MessageEnvelope(
                    transport.selfId(),
                    target,
                    message.getClass().getName(),
                    payload,
                    System.currentTimeMillis());
            transport.send(env);
            return CompletableFuture.completedFuture(null);
        } catch (Throwable t) {
            return CompletableFuture.failedFuture(t);
        }
    }

    @Override
    public void unsubscribeAll(@NotNull XPluginHandle owner) {
        subs.values().forEach(list -> list.removeIf(s -> {
            if (s.owner.equals(owner)) {
                s.active = false;
                return true;
            }
            return false;
        }));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void onReceive(MessageEnvelope env) {
        if (!codec.isRegistered(env.messageType())) {
            log.debug("[xCore/messenger] dropping unknown message type {} from {}", env.messageType(), env.from());
            return;
        }
        NetworkMessage msg;
        try {
            msg = codec.decodePayload(env.messageType(), env.payload());
        } catch (Throwable t) {
            log.warn("[xCore/messenger] failed to decode {} from {}: {}",
                    env.messageType(), env.from(), t.getMessage());
            return;
        }
        if (msg == null) return;

        CopyOnWriteArrayList<Sub<?>> list = subs.get(msg.getClass());
        if (list == null || list.isEmpty()) return;

        MessageContext ctx = new MessageContext(env.from(), env.to(), env.sentAtMillis());
        for (Sub sub : list) {
            if (!sub.active) continue;
            try {
                sub.handler.accept(ctx, msg);
            } catch (Throwable t) {
                log.warn("[xCore/messenger] {} handler threw on {}: {}",
                        sub.owner.name(), msg.getClass().getSimpleName(), t.getMessage(), t);
            }
        }
    }

    private final class Sub<M extends NetworkMessage> implements NetworkSubscription {
        final XPluginHandle owner;
        final Class<M> type;
        final BiConsumer<MessageContext, M> handler;
        volatile boolean active = true;

        Sub(XPluginHandle owner, Class<M> type, BiConsumer<MessageContext, M> handler) {
            this.owner = owner;
            this.type = type;
            this.handler = handler;
        }

        @Override public @NotNull Class<? extends NetworkMessage> messageType() { return type; }
        @Override public boolean isActive() { return active; }

        @Override
        public void unsubscribe() {
            if (!active) return;
            active = false;
            CopyOnWriteArrayList<Sub<?>> list = subs.get(type);
            if (list != null) list.remove(this);
        }
    }
}
