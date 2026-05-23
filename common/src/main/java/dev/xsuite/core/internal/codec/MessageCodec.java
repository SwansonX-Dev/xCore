package dev.xsuite.core.internal.codec;

import com.google.gson.Gson;
import dev.xsuite.core.api.messenger.NetworkMessage;
import dev.xsuite.core.internal.messenger.MessageEnvelope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Encodes {@link NetworkMessage}s as JSON via Gson and packs envelopes into
 * a length-prefixed binary frame for the transport.
 *
 * <p>Frame layout (big-endian, suitable for {@link DataOutputStream}):
 * <pre>
 *   UTF  from
 *   UTF  to
 *   UTF  messageType
 *   LONG sentAtMillis
 *   INT  payloadLength
 *   BYTES payload
 * </pre>
 */
public final class MessageCodec {

    private final Gson gson = new Gson();
    private final Map<String, Class<? extends NetworkMessage>> typeRegistry = new ConcurrentHashMap<>();

    public void register(@NotNull Class<? extends NetworkMessage> type) {
        typeRegistry.put(type.getName(), type);
    }

    public boolean isRegistered(@NotNull String typeName) {
        return typeRegistry.containsKey(typeName);
    }

    public byte[] encodePayload(@NotNull NetworkMessage message) {
        return gson.toJson(message).getBytes(StandardCharsets.UTF_8);
    }

    public @Nullable NetworkMessage decodePayload(@NotNull String typeName, byte[] payload) {
        Class<? extends NetworkMessage> cls = typeRegistry.get(typeName);
        if (cls == null) return null;
        String json = new String(payload, StandardCharsets.UTF_8);
        return gson.fromJson(json, cls);
    }

    public byte[] encodeFrame(@NotNull MessageEnvelope env) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(baos)) {
            out.writeUTF(env.from());
            out.writeUTF(env.to());
            out.writeUTF(env.messageType());
            out.writeLong(env.sentAtMillis());
            out.writeInt(env.payload().length);
            out.write(env.payload());
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode envelope", e);
        }
    }

    public @NotNull MessageEnvelope decodeFrame(byte[] data) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            String from = in.readUTF();
            String to = in.readUTF();
            String type = in.readUTF();
            long sentAt = in.readLong();
            int len = in.readInt();
            byte[] payload = in.readNBytes(len);
            return new MessageEnvelope(from, to, type, payload, sentAt);
        } catch (IOException e) {
            throw new RuntimeException("Failed to decode envelope", e);
        }
    }
}
