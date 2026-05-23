package dev.xsuite.core.velocity.internal.messenger;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.ChannelMessageSource;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.xsuite.core.api.messenger.MessageTransport;
import dev.xsuite.core.internal.codec.MessageCodec;
import dev.xsuite.core.internal.messenger.MessageEnvelope;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Proxy-side cross-server transport. Receives envelopes from backends on
 * channel {@code xsuite:envelope}, routes them to the named target (or all
 * peers on broadcast), and accepts envelopes destined for the proxy itself.
 *
 * <p>Outbound proxy→backend traffic still requires at least one player on
 * the target backend (Minecraft plugin-message limitation). If the target
 * has no players, the message is silently dropped and a warning is logged.
 *
 * <p>This class is also registered as a Velocity event listener — register
 * it via {@code proxy.getEventManager().register(plugin, transport)} during
 * the plugin's enable hook.
 */
public final class VelocityPluginMessageTransport implements MessageTransport {

    public static final ChannelIdentifier ENVELOPE_CHANNEL =
            MinecraftChannelIdentifier.from("xsuite:envelope");

    private final ProxyServer proxy;
    private final MessageCodec codec;
    private final Logger log;

    private volatile Consumer<MessageEnvelope> receiver = env -> {};
    private volatile boolean started = false;

    public VelocityPluginMessageTransport(@NotNull ProxyServer proxy,
                                          @NotNull MessageCodec codec,
                                          @NotNull Logger log) {
        this.proxy = proxy;
        this.codec = codec;
        this.log = log;
    }

    @Override
    public void start() {
        if (started) return;
        proxy.getChannelRegistrar().register(ENVELOPE_CHANNEL);
        started = true;
    }

    @Override
    public void stop() {
        if (!started) return;
        proxy.getChannelRegistrar().unregister(ENVELOPE_CHANNEL);
        started = false;
    }

    @Override
    public void send(@NotNull MessageEnvelope envelope) {
        // Proxy is generating this envelope; emitter is the proxy.
        MessageEnvelope stamped = envelope.from().isEmpty()
                ? new MessageEnvelope(MessageEnvelope.PROXY, envelope.to(), envelope.messageType(),
                                      envelope.payload(), envelope.sentAtMillis())
                : envelope;
        route(stamped);
    }

    @Override
    public void setReceiver(@NotNull Consumer<MessageEnvelope> receiver) {
        this.receiver = receiver;
    }

    @Override public @NotNull String selfId() { return MessageEnvelope.PROXY; }
    @Override public boolean isProxyAvailable() { return true; }

    @Override
    public @NotNull Set<String> knownServers() {
        return proxy.getAllServers().stream()
                .map(s -> s.getServerInfo().getName())
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Velocity calls this whenever a backend / player sends a plugin message. */
    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!ENVELOPE_CHANNEL.equals(event.getIdentifier())) return;
        // Consume — don't let Velocity forward this to anywhere else.
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        MessageEnvelope env;
        try {
            env = codec.decodeFrame(event.getData());
        } catch (Throwable t) {
            log.warn("[xCore/messenger/velocity] failed to decode incoming envelope: {}", t.getMessage());
            return;
        }

        // If the backend didn't fill 'from', infer it from the connection.
        ChannelMessageSource src = event.getSource();
        if ((env.from() == null || env.from().isEmpty() || "unknown".equals(env.from()))
                && src instanceof ServerConnection sc) {
            env = new MessageEnvelope(
                    sc.getServerInfo().getName(),
                    env.to(),
                    env.messageType(),
                    env.payload(),
                    env.sentAtMillis());
        }

        route(env);
    }

    private void route(MessageEnvelope env) {
        String to = env.to();
        if (MessageEnvelope.PROXY.equals(to)) {
            receiver.accept(env);
            return;
        }
        if (MessageEnvelope.BROADCAST.equals(to)) {
            for (RegisteredServer server : proxy.getAllServers()) {
                if (server.getServerInfo().getName().equals(env.from())) continue;
                deliverToBackend(server, env);
            }
            // Also dispatch locally for proxy-side subscribers.
            receiver.accept(env);
            return;
        }
        proxy.getServer(to).ifPresentOrElse(
                server -> deliverToBackend(server, env),
                () -> log.warn("[xCore/messenger/velocity] dropping envelope to unknown server '{}'", to));
    }

    private void deliverToBackend(RegisteredServer server, MessageEnvelope env) {
        byte[] frame = codec.encodeFrame(env);
        boolean sent = server.sendPluginMessage(ENVELOPE_CHANNEL, frame);
        if (!sent) {
            log.warn("[xCore/messenger/velocity] no players on '{}' — dropped envelope (type={})",
                    server.getServerInfo().getName(), env.messageType());
        }
    }
}
