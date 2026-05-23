package dev.xsuite.core.api.messenger;

import org.jetbrains.annotations.NotNull;

/**
 * Metadata accompanying every received {@link NetworkMessage}.
 */
public final class MessageContext {

    private final String from;
    private final String to;
    private final long sentAtMillis;

    public MessageContext(@NotNull String from, @NotNull String to, long sentAtMillis) {
        this.from = from;
        this.to = to;
        this.sentAtMillis = sentAtMillis;
    }

    /** Server that emitted the message (e.g. "lobby", "survival1", or "proxy"). */
    public @NotNull String from() { return from; }

    /** Intended recipient: a specific server name or {@code "*"} for broadcast. */
    public @NotNull String to() { return to; }

    /** Wall-clock millis when the sender emitted it. */
    public long sentAtMillis() { return sentAtMillis; }
}
