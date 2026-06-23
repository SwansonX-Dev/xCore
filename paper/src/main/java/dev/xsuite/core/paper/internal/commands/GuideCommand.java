package dev.xsuite.core.paper.internal.commands;

import dev.xsuite.core.api.command.XCommand;
import dev.xsuite.core.api.command.XCommandContext;
import dev.xsuite.core.paper.api.guide.GuideRegistry;
import dev.xsuite.core.paper.internal.guide.GuideHubMenu;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The global {@code /guide} command. Opens a hub listing every player-facing
 * plugin that has published a {@link dev.xsuite.core.paper.api.guide.GuideEntry}.
 */
public final class GuideCommand extends XCommand {

    private final GuideRegistry registry;

    public GuideCommand(@NotNull GuideRegistry registry) {
        super("guide", "Open the server guide hub.", "", "/guide", List.of("guides", "serverguide"));
        this.registry = registry;
    }

    @Override
    public void execute(@NotNull XCommandContext ctx) {
        ctx.requirePlayer();
        Player player = (Player) ctx.sender().platformSender();
        new GuideHubMenu(registry).open(player);
    }
}
