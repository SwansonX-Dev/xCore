package dev.xsuite.core.api.messenger;

/**
 * Base class for cross-server messages dispatched through {@link XMessenger}.
 *
 * <p>Subclasses are plain data carriers: public, non-transient fields are
 * serialized via Gson on send and reconstituted on receive. There are no
 * required overrides.
 *
 * <p>The message type identifier is the fully-qualified class name. Both
 * sides (sender and receiver) must use the same class on the same package —
 * register your message class once via
 * {@link XMessenger#register(dev.xsuite.core.api.XPluginHandle, Class)} or
 * implicitly by subscribing or sending it.
 */
public abstract class NetworkMessage {
}
