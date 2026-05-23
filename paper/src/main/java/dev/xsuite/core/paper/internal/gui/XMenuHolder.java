package dev.xsuite.core.paper.internal.gui;

import dev.xsuite.core.paper.api.gui.XMenu;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * InventoryHolder marker so {@code XMenuListener} can detect xCore-owned
 * inventories without falling back to title-string comparisons.
 */
public final class XMenuHolder implements InventoryHolder {

    private final XMenu menu;

    public XMenuHolder(@NotNull XMenu menu) {
        this.menu = menu;
    }

    public @NotNull XMenu menu() { return menu; }

    @Override
    public @NotNull Inventory getInventory() {
        // Required by Bukkit's interface but we never read the inventory back from the
        // holder — the menu's open() flow constructs and tracks it directly.
        throw new UnsupportedOperationException("XMenuHolder is a marker; obtain the inventory via XMenu.open()");
    }
}
