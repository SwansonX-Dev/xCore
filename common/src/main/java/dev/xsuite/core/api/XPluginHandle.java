package dev.xsuite.core.api;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Platform-neutral "plugin owner" passed to xCore APIs.
 *
 * <p>Both the Paper-side {@code XPaperPlugin} base class and the Velocity-side
 * {@code XVelocityPlugin} base class implement this. xSuite plugin authors pass
 * {@code this} to every API call that asks for an owner, regardless of platform.
 *
 * <p>This is the unit xCore uses for ownership tracking — when a plugin disables,
 * every event subscription, service, command, and messenger registration tied to
 * its handle is torn down automatically.
 */
public interface XPluginHandle {

    @NotNull String name();

    @NotNull String version();

    @NotNull XPlatform platform();

    @NotNull Path dataFolder();

    @NotNull Logger logger();
}
