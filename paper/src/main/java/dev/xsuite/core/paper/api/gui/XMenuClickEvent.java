package dev.xsuite.core.paper.api.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;

/** Click event delivered to an {@link XMenuItem}'s onClick handler. */
public final class XMenuClickEvent {

    private final Player viewer;
    private final ClickType clickType;
    private final int slot;
    private final XMenu menu;

    public XMenuClickEvent(@NotNull Player viewer,
                           @NotNull ClickType clickType,
                           int slot,
                           @NotNull XMenu menu) {
        this.viewer = viewer;
        this.clickType = clickType;
        this.slot = slot;
        this.menu = menu;
    }

    public @NotNull Player viewer() { return viewer; }
    public @NotNull ClickType clickType() { return clickType; }
    public int slot() { return slot; }
    public @NotNull XMenu menu() { return menu; }

    public boolean isLeftClick() { return clickType.isLeftClick(); }
    public boolean isRightClick() { return clickType.isRightClick(); }
    public boolean isShiftClick() { return clickType.isShiftClick(); }

    /** Close the menu on the next tick. */
    public void close() {
        viewer.closeInventory();
    }

    /** Re-run {@link XMenu#build()} and push updated items to the open inventory. */
    public void refresh() {
        menu.refresh();
    }
}
