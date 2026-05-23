package dev.xsuite.core.paper.internal.messenger;

import dev.xsuite.core.api.messenger.MessageTransport;
import dev.xsuite.core.internal.codec.MessageCodec;
import dev.xsuite.core.internal.messenger.MessageEnvelope;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

/**
 * Paper-side cross-server transport over Minecraft plugin messaging.
 *
 * <p>Outgoing envelopes are framed by {@link MessageCodec#encodeFrame} and
 * sent on channel {@code xsuite:envelope} via any online player. If no
 * player is online they queue until one joins, then flush.
 *
 * <p>Inbound packets on {@code xsuite:envelope} are decoded and handed to
 * the receiver registered through {@link #setReceiver}.
 *
 * <p>Self-id is discovered via the BungeeCord-compatible {@code GetServer}
 * subchannel on first player join; until then it is {@code "unknown"}.
 */
public final class PaperPluginMessageTransport implements MessageTransport, Listener, PluginMessageListener {

    private static final String ENVELOPE_CHANNEL = "xsuite:envelope";
    private static final String BUNGEE_CHANNEL = "BungeeCord";

    private final JavaPlugin plugin;
    private final MessageCodec codec;
    private final Logger log;

    private final Deque<MessageEnvelope> pending = new ArrayDeque<>();
    private final Object pendingLock = new Object();
    private final Set<String> knownServers = new CopyOnWriteArraySet<>();

    private volatile String selfId = "unknown";
    private volatile Consumer<MessageEnvelope> receiver = env -> {};
    private volatile boolean started = false;

    public PaperPluginMessageTransport(@NotNull JavaPlugin plugin,
                                       @NotNull MessageCodec codec,
                                       @NotNull Logger log) {
        this.plugin = plugin;
        this.codec = codec;
        this.log = log;
    }

    @Override
    public void start() {
        if (started) return;
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, ENVELOPE_CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, ENVELOPE_CHANNEL, this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, BUNGEE_CHANNEL, this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        started = true;
    }

    @Override
    public void stop() {
        if (!started) return;
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin);
        started = false;
    }

    @Override
    public void send(@NotNull MessageEnvelope envelope) {
        synchronized (pendingLock) {
            pending.addLast(envelope);
        }
        tryFlush();
    }

    @Override
    public void setReceiver(@NotNull Consumer<MessageEnvelope> receiver) {
        this.receiver = receiver;
    }

    @Override public @NotNull String selfId() { return selfId; }
    @Override public @NotNull Set<String> knownServers() { return Collections.unmodifiableSet(knownServers); }

    @Override
    public boolean isProxyAvailable() {
        // We treat "we learned a server name from BungeeCord" as proof of proxy.
        return !"unknown".equals(selfId);
    }

    private void tryFlush() {
        Player carrier = null;
        Iterator<? extends Player> it = Bukkit.getOnlinePlayers().iterator();
        if (it.hasNext()) carrier = it.next();
        if (carrier == null) return;

        synchronized (pendingLock) {
            while (!pending.isEmpty()) {
                MessageEnvelope env = pending.peekFirst();
                if (!sendThrough(carrier, env)) break;
                pending.pollFirst();
            }
        }
    }

    private boolean sendThrough(Player carrier, MessageEnvelope env) {
        try {
            byte[] frame = codec.encodeFrame(env);
            carrier.sendPluginMessage(plugin, ENVELOPE_CHANNEL, frame);
            return true;
        } catch (Throwable t) {
            log.warn("[xCore/messenger/paper] failed to ship envelope: {}", t.getMessage());
            return false;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        if ("unknown".equals(selfId)) requestServerName(p);
        tryFlush();
    }

    private void requestServerName(Player carrier) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(baos)) {
            out.writeUTF("GetServer");
            carrier.sendPluginMessage(plugin, BUNGEE_CHANNEL, baos.toByteArray());
            // Also request the player list of known servers.
            ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
            DataOutputStream out2 = new DataOutputStream(baos2);
            out2.writeUTF("GetServers");
            carrier.sendPluginMessage(plugin, BUNGEE_CHANNEL, baos2.toByteArray());
        } catch (IOException e) {
            log.warn("[xCore/messenger/paper] couldn't ask proxy for server name: {}", e.getMessage());
        }
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel,
                                        @NotNull Player player,
                                        byte @NotNull [] data) {
        if (ENVELOPE_CHANNEL.equals(channel)) {
            try {
                MessageEnvelope env = codec.decodeFrame(data);
                receiver.accept(env);
            } catch (Throwable t) {
                log.warn("[xCore/messenger/paper] failed to decode incoming envelope: {}", t.getMessage());
            }
            return;
        }
        if (BUNGEE_CHANNEL.equals(channel)) {
            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
                String sub = in.readUTF();
                if ("GetServer".equals(sub)) {
                    String name = in.readUTF();
                    selfId = name;
                    knownServers.add(name);
                    log.info("[xCore/messenger/paper] proxy identified this server as '{}'", name);
                } else if ("GetServers".equals(sub)) {
                    String csv = in.readUTF();
                    for (String s : csv.split(", ")) {
                        if (!s.isEmpty()) knownServers.add(s);
                    }
                }
            } catch (IOException e) {
                log.warn("[xCore/messenger/paper] failed to read BungeeCord reply: {}", e.getMessage());
            }
        }
    }
}
