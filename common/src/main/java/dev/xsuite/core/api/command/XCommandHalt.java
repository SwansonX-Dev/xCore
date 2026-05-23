package dev.xsuite.core.api.command;

/**
 * Thrown by {@link XCommandContext#requirePlayer()} / {@link XCommandContext#requirePermission}
 * to short-circuit the rest of an {@link XCommand#execute} call after the user
 * has been told why. Caught silently by {@link XCommandManager}.
 */
public final class XCommandHalt extends RuntimeException {
    public XCommandHalt() {
        super(null, null, false, false);
    }
}
