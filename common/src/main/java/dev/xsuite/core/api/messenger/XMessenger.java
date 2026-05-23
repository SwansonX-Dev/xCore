package dev.xsuite.core.api.messenger;

import dev.xsuite.core.api.XPluginHandle;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Cross-server messaging facility.
 *
 * <p>On Velocity, the messenger routes between backends. On Paper, it sends
 * outbound packets through the proxy (when a player is online — see below)
 * and receives inbound packets routed by xCore-Velocity.
 *
 * <h2>Standalone Paper</h2>
 * If no Velocity proxy is reachable, {@link #broadcast} and {@link #sendTo}
 * succeed but reach no peers. {@link #isProxyAvailable()} returns {@code false}
 * and {@link #knownServers()} returns an empty set.
 *
 * <h2>Plugin-message transport caveats</h2>
 * The default transport piggybacks on Minecraft's plugin-message channels,
 * which require at least one player connection on the source server. xCore
 * queues outbound messages until a player is online and flushes them then;
 * messages still in the queue when the JVM exits are dropped. Networks that
 * cannot tolerate this should switch to a Redis backend (planned).
 *
 * <h2>Registration</h2>
 * Receivers MUST {@link #register} their message classes before incoming
 * packets can be deserialized. Sending implicitly registers the class.
 */
public interface XMessenger {

    /** Identifier of this node — backend server name on Paper, {@code "proxy"} on Velocity. */
    @NotNull String selfId();

    boolean isProxyAvailable();

    /** Names of servers reachable through the proxy. Empty when {@link #isProxyAvailable()} is false. */
    @NotNull Set<String> knownServers();

    /**
     * Make {@code type} known to the codec so messages of this type arriving
     * over the wire can be deserialized and dispatched.
     */
    void register(@NotNull XPluginHandle owner, @NotNull Class<? extends NetworkMessage> type);

    <M extends NetworkMessage> @NotNull NetworkSubscription subscribe(
            @NotNull XPluginHandle owner,
            @NotNull Class<M> type,
            @NotNull BiConsumer<MessageContext, M> handler);

    /** Send to a specific server. Use {@code "proxy"} to target the Velocity proxy itself. */
    @NotNull CompletableFuture<Void> sendTo(@NotNull String target, @NotNull NetworkMessage message);

    /** Send to all other servers (not including the sender). */
    @NotNull CompletableFuture<Void> broadcast(@NotNull NetworkMessage message);

    void unsubscribeAll(@NotNull XPluginHandle owner);
}
