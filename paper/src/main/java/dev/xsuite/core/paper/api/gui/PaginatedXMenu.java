package dev.xsuite.core.paper.api.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * An {@link XMenu} that paginates a list of content items across the top
 * {@code rows - 1} rows, reserving the last row for navigation.
 *
 * <p>Subclasses override {@link #contentItems()} to supply the items. The
 * base class places previous-page, page-indicator and next-page buttons
 * in slots {@code last-row[0]}, {@code last-row[4]} and {@code last-row[8]}
 * respectively, and exposes {@link #fillBackground(XMenuItem)} for the
 * remaining bottom-row slots.
 */
public abstract class PaginatedXMenu extends XMenu {

    private int page = 0;

    protected PaginatedXMenu(@NotNull Component title, int rows) {
        super(title, Math.max(2, rows));
    }

    protected PaginatedXMenu(@NotNull String titleMiniMessage, int rows) {
        super(titleMiniMessage, Math.max(2, rows));
    }

    /** All items to be paginated. Called each {@link #build()} so subclasses may compute lazily. */
    protected abstract @NotNull List<XMenuItem> contentItems();

    public final int page() { return page; }

    public final int itemsPerPage() {
        return (rows() - 1) * 9;
    }

    public final int pageCount() {
        int total = contentItems().size();
        return Math.max(1, (total + itemsPerPage() - 1) / itemsPerPage());
    }

    /** Optional background filler for the bottom row's empty slots. */
    protected @NotNull XMenuItem backgroundFiller() {
        return XMenuItem.filler(Material.GRAY_STAINED_GLASS_PANE);
    }

    /** Optional decorator for the page-indicator paper at the centre of the nav row. */
    protected @NotNull XMenuItem pageIndicator(int page, int total) {
        return XMenuItem.builder(Material.PAPER)
                .name("<gray>Page <yellow>" + (page + 1) + "<gray>/<yellow>" + total)
                .build();
    }

    /** Slot index of the first cell in the bottom nav row. */
    protected final int navRow() {
        return (rows() - 1) * 9;
    }

    /**
     * Called at the end of {@link #build()} so subclasses can place extra
     * buttons (e.g. a back button) in the unused nav-row slots
     * ({@code navRow() + 1..3, 5..7}). Slots 0, 4 and 8 are reserved for
     * previous-page, page-indicator and next-page respectively.
     */
    protected void decoratePageNav() {
        // no-op
    }

    @Override
    protected final void build() {
        List<XMenuItem> all = contentItems();
        int per = itemsPerPage();
        int start = page * per;
        int end = Math.min(start + per, all.size());

        int slot = 0;
        for (int i = start; i < end; i++) {
            setItem(slot++, all.get(i));
        }

        int lastRow = navRow();
        for (int i = 0; i < 9; i++) setItem(lastRow + i, backgroundFiller());

        if (page > 0) {
            setItem(lastRow, XMenuItem.builder(Material.ARROW)
                    .name("<yellow>« Previous page")
                    .onClick(e -> { page--; refresh(); })
                    .build());
        }
        if (page < pageCount() - 1) {
            setItem(lastRow + 8, XMenuItem.builder(Material.ARROW)
                    .name("<yellow>Next page »")
                    .onClick(e -> { page++; refresh(); })
                    .build());
        }
        setItem(lastRow + 4, pageIndicator(page, pageCount()));
        decoratePageNav();
    }
}
