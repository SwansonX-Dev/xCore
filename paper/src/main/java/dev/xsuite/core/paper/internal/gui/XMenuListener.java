package dev.xsuite.core.paper.internal.gui;

import dev.xsuite.core.paper.api.gui.XMenu;
import dev.xsuite.core.paper.api.gui.XMenuClickEvent;
import dev.xsuite.core.paper.api.gui.XMenuItem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

/** Routes Bukkit inventory events to xCore {@link XMenu} instances. */
public final class XMenuListener implements Listener {

    private final Logger log;

    public XMenuListener(@NotNull Logger log) {
        this.log = log;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof XMenuHolder holder)) return;
        // Block every default interaction — menus are read-only.
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int raw = event.getRawSlot();
        // Clicks in the player's own inventory have raw slots >= menu size.
        if (raw < 0 || raw >= holder.menu().size()) return;

        XMenuItem item = holder.menu().itemAt(raw);
        if (item == null || item.onClick() == null) return;

        try {
            item.onClick().accept(new XMenuClickEvent(player, event.getClick(), raw, holder.menu()));
        } catch (Throwable t) {
            log.warn("[xCore/gui] click handler threw in {}: {}",
                    holder.menu().getClass().getSimpleName(), t.getMessage(), t);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof XMenuHolder)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof XMenuHolder holder)) return;
        if (event.getPlayer() instanceof Player player) {
            try {
                holder.menu().onClose(player);
            } catch (Throwable t) {
                log.warn("[xCore/gui] onClose threw in {}: {}",
                        holder.menu().getClass().getSimpleName(), t.getMessage(), t);
            }
        }
    }
}
