package dev.xsuite.core.velocity.internal.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.xsuite.core.api.command.XSender;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public final class VelocityXSender implements XSender, ForwardingAudience.Single {

    private final CommandSource source;

    public VelocityXSender(@NotNull CommandSource source) {
        this.source = source;
    }

    @Override
    public @NotNull String name() {
        if (source instanceof Player p) return p.getUsername();
        if (source instanceof ConsoleCommandSource) return "Console";
        return source.getClass().getSimpleName();
    }

    @Override public boolean isPlayer() { return source instanceof Player; }
    @Override public boolean isConsole() { return source instanceof ConsoleCommandSource; }

    @Override
    public @NotNull Optional<UUID> uuid() {
        return source instanceof Player p ? Optional.of(p.getUniqueId()) : Optional.empty();
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return source.hasPermission(permission);
    }

    @Override public @NotNull Object platformSender() { return source; }
    @Override public @NotNull Audience audience() { return source; }
}
