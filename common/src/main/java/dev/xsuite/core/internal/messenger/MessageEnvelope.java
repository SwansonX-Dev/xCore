package dev.xsuite.core.internal.messenger;

/**
 * Wire format for a single cross-server message. Internal — not part of
 * the public API. Carried in raw bytes by {@code MessageTransport}.
 *
 * @param from         emitter node id (backend server name or "proxy")
 * @param to           recipient: server name, "*" broadcast, or "proxy"
 * @param messageType  class FQN of the {@code NetworkMessage} subclass
 * @param payload      Gson-encoded JSON body
 * @param sentAtMillis wall-clock millis at emission
 */
public record MessageEnvelope(
        String from,
        String to,
        String messageType,
        byte[] payload,
        long sentAtMillis
) {
    public static final String BROADCAST = "*";
    public static final String PROXY = "proxy";
}
