package dev.xsuite.core.paper.internal.guide;

import dev.xsuite.core.paper.api.gui.XMenu;
import dev.xsuite.core.paper.api.gui.XMenuItem;
import dev.xsuite.core.paper.api.guide.GuideEntry;
import dev.xsuite.core.paper.api.guide.GuideRegistry;
import org.bukkit.Material;

import java.util.List;

/**
 * The global {@code /guide} hub. Shows one clickable icon per registered
 * {@link GuideEntry}; clicking opens that plugin's own guide.
 *
 * <p>Lays entries out across the inner area of a 6-row chest (up to 28 entries),
 * which comfortably covers the whole suite. If the suite ever outgrows that,
 * swap the base class for {@code PaginatedXMenu}.
 */
public final class GuideHubMenu extends XMenu {

    /** Inner content slots of a 6-row inventory (rows 1-4, columns 1-7). */
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final GuideRegistry registry;

    public GuideHubMenu(GuideRegistry registry) {
        super("<gradient:#7CFFCB:#5FA8FF><bold>Server Guide</bold></gradient>", 6);
        this.registry = registry;
    }

    @Override
    protected void build() {
        for (int i = 0; i < size(); i++) {
            setItem(i, XMenuItem.filler(Material.BLACK_STAINED_GLASS_PANE));
        }

        setItem(4, XMenuItem.builder(Material.KNOWLEDGE_BOOK)
                .name("<gradient:#7CFFCB:#5FA8FF><bold>Server Guide</bold></gradient>")
                .lore("<gray>Pick a plugin to learn how it works.",
                        "<gray>Each icon opens that plugin's own guide.")
                .hideAttributes()
                .build());

        List<GuideEntry> entries = registry.entries();
        for (int i = 0; i < entries.size() && i < CONTENT_SLOTS.length; i++) {
            GuideEntry entry = entries.get(i);
            String[] lore = new String[entry.summary().size() + 2];
            for (int l = 0; l < entry.summary().size(); l++) {
                lore[l] = entry.summary().get(l);
            }
            lore[lore.length - 2] = "<dark_gray>";
            lore[lore.length - 1] = "<yellow>▶ Click to open this guide";
            setItem(CONTENT_SLOTS[i], XMenuItem.builder(entry.icon())
                    .name(entry.displayName())
                    .lore(lore)
                    .hideAttributes()
                    .onClick(e -> {
                        e.close();
                        // Defer so the hub fully closes before the next inventory opens.
                        entry.open(e.viewer());
                    })
                    .build());
        }

        setItem(49, XMenuItem.builder(Material.BARRIER)
                .name("<red>Close")
                .onClick(e -> e.close())
                .build());
    }
}
