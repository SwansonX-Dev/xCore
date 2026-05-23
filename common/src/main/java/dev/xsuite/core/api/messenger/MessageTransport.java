package dev.xsuite.core.api.messenger;

import dev.xsuite.core.internal.messenger.MessageEnvelope;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.function.Consumer;

/**
 * SPI: a pluggable transport that ships {@link MessageEnvelope}s between
 * xCore nodes. Default implementation uses Minecraft plugin messaging via
 * the connected Velocity proxy. Future implementations may add Redis,
 * NATS, etc.
 *
 * <p>Lifecycle: {@link #setReceiver} is called once before {@link #start},
 * which begins delivery. {@link #stop} is called on shutdown.
 */
public interface MessageTransport {

    /** Begin sending/receiving. Called once during xCore enable. */
    void start();

    /** Stop sending/receiving; release any underlying resources. */
    void stop();

    /** Outbound: encode envelope and ship to the appropriate destination(s). */
    void send(@NotNull MessageEnvelope envelope);

    /** Inbound: register the callback invoked on each received envelope. */
    void setReceiver(@NotNull Consumer<MessageEnvelope> receiver);

    @NotNull String selfId();

    @NotNull Set<String> knownServers();

    boolean isProxyAvailable();
}
