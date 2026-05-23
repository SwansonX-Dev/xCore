package dev.xsuite.core.paper.api.gui;

import dev.xsuite.core.paper.internal.gui.XMenuHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for inventory-backed GUIs. Subclasses implement {@link #build()}
 * to populate items; xCore's {@code XMenuListener} cancels every player drag,
 * intercepts clicks, and routes them to the corresponding {@link XMenuItem}.
 *
 * <p>Typical usage:
 * <pre>{@code
 * public final class HomesMenu extends XMenu {
 *     public HomesMenu(List<Home> homes) { super("<dark_gray>Homes", 6); ... }
 *
 *     @Override protected void build() {
 *         for (Home h : homes) setItem(slot++, XMenuItem.builder(Material.GRASS_BLOCK)
 *             .name("<gold>" + h.name())
 *             .onClick(e -> { teleport(e.viewer(), h); e.close(); })
 *             .build());
 *     }
 * }
 *
 * new HomesMenu(homes).open(player);
 * }</pre>
 */
public abstract class XMenu {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Component title;
    private final int rows;
    private final Map<Integer, XMenuItem> items = new HashMap<>();

    private @Nullable Inventory inventory;
    private @Nullable Player viewer;

    protected XMenu(@NotNull Component title, int rows) {
        this.title = title;
        this.rows = Math.max(1, Math.min(6, rows));
    }

    protected XMenu(@NotNull String titleMiniMessage, int rows) {
        this(MM.deserialize(titleMiniMessage), rows);
    }

    public final @NotNull Component title() { return title; }
    public final int rows() { return rows; }
    public final int size() { return rows * 9; }

    /** Called by xCore right before the inventory is opened, and again on {@link #refresh()}. */
    protected abstract void build();

    public final void setItem(int slot, @NotNull XMenuItem item) {
        if (slot < 0 || slot >= size()) return;
        items.put(slot, item);
        if (inventory != null) inventory.setItem(slot, item.icon());
    }

    public final void removeItem(int slot) {
        items.remove(slot);
        if (inventory != null) inventory.setItem(slot, null);
    }

    public final void clear() {
        items.clear();
        if (inventory != null) inventory.clear();
    }

    /** @return the item registered at {@code slot}, or {@code null}. */
    public final @Nullable XMenuItem itemAt(int slot) {
        return items.get(slot);
    }

    /** Open for {@code viewer}. Closes any previously open inventory the viewer had. */
    public final void open(@NotNull Player viewer) {
        this.viewer = viewer;
        items.clear();
        build();
        inventory = Bukkit.createInventory(new XMenuHolder(this), size(), title);
        for (Map.Entry<Integer, XMenuItem> e : items.entrySet()) {
            inventory.setItem(e.getKey(), e.getValue().icon());
        }
        viewer.openInventory(inventory);
    }

    public final void close() {
        if (viewer != null) viewer.closeInventory();
    }

    /** Rebuild items and push them into the open inventory without re-opening it. */
    public final void refresh() {
        if (inventory == null) return;
        items.clear();
        inventory.clear();
        build();
        for (Map.Entry<Integer, XMenuItem> e : items.entrySet()) {
            inventory.setItem(e.getKey(), e.getValue().icon());
        }
    }

    /** Called by the internal listener when the viewer closes the inventory. */
    public void onClose(@NotNull Player viewer) {
        // Override to react.
    }

    public final @Nullable Player currentViewer() { return viewer; }
}
