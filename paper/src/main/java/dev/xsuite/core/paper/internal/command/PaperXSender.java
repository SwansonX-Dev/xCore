package dev.xsuite.core.paper.internal.command;

import dev.xsuite.core.api.command.XSender;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public final class PaperXSender implements XSender, ForwardingAudience.Single {

    private final CommandSender source;

    public PaperXSender(@NotNull CommandSender source) {
        this.source = source;
    }

    @Override public @NotNull String name() { return source.getName(); }
    @Override public boolean isPlayer() { return source instanceof Player; }
    @Override public boolean isConsole() { return !(source instanceof Player); }

    @Override
    public @NotNull Optional<UUID> uuid() {
        return source instanceof Player p ? Optional.of(p.getUniqueId()) : Optional.empty();
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return source.hasPermission(permission);
    }

    @Override
    public @NotNull Object platformSender() {
        return source;
    }

    @Override
    public @NotNull Audience audience() {
        return source;
    }
}
