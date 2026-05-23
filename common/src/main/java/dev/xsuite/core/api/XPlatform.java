package dev.xsuite.core.api;

/**
 * Which Minecraft platform xCore is currently running on.
 * Code that needs to branch on platform (rare) can check {@link XCoreAPI#platform()}.
 */
public enum XPlatform {
    /** Paper / Folia-compatible backend server. */
    PAPER,
    /** Velocity proxy. */
    VELOCITY
}
