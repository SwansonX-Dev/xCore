package dev.xsuite.core.paper.api.guide;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * One plugin's entry in the global {@code /guide} hub.
 *
 * <p>Each player-facing xSuite plugin publishes a {@code GuideEntry} to the
 * {@link GuideRegistry} on enable. The hub menu renders one icon per entry and,
 * when clicked, invokes {@link #open(Player)} — which typically opens that
 * plugin's own guide menu.
 *
 * <p>Plugins that can't depend on xCore at compile time (the forks, third-party
 * plugins) are surfaced instead through xCore's {@code config.yml}
 * {@code guide.external} list, each turned into a command-dispatch entry via
 * {@link #ofCommand}.
 *
 * @param id          stable lowercase id used for de-duplication (e.g. {@code "teams"})
 * @param displayName MiniMessage display name shown on the hub icon
 * @param icon        the Material rendered in the hub
 * @param summary     MiniMessage lore lines describing the plugin
 * @param opener      action run when a player clicks this entry in the hub
 */
public record GuideEntry(@NotNull String id,
                         @NotNull String displayName,
                         @NotNull Material icon,
                         @NotNull List<String> summary,
                         @NotNull Consumer<Player> opener) {

    public GuideEntry {
        id = id.toLowerCase(Locale.ROOT);
        summary = List.copyOf(summary);
    }

    /** Run this entry's opener for {@code viewer}. */
    public void open(@NotNull Player viewer) {
        opener.accept(viewer);
    }

    /**
     * Build an entry whose opener simply makes the player run a command — used for
     * plugins that aren't compiled against xCore (forks/externals) but expose their
     * own {@code guide} command.
     *
     * @param command the command to run, with or without a leading slash
     */
    public static @NotNull GuideEntry ofCommand(@NotNull String id,
                                                @NotNull String displayName,
                                                @NotNull Material icon,
                                                @NotNull List<String> summary,
                                                @NotNull String command) {
        String normalized = command.startsWith("/") ? command.substring(1) : command;
        return new GuideEntry(id, displayName, icon, summary, player -> {
            player.closeInventory();
            player.performCommand(normalized);
        });
    }
}
