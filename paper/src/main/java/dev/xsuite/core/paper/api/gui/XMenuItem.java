package dev.xsuite.core.paper.api.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Immutable item button used inside an {@link XMenu}. Holds the ItemStack
 * shown in the slot plus the click handler invoked when a viewer clicks it.
 */
public final class XMenuItem {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final ItemStack icon;
    private final @Nullable Consumer<XMenuClickEvent> onClick;

    XMenuItem(@NotNull ItemStack icon, @Nullable Consumer<XMenuClickEvent> onClick) {
        this.icon = icon;
        this.onClick = onClick;
    }

    public @NotNull ItemStack icon() {
        return icon.clone();
    }

    public @Nullable Consumer<XMenuClickEvent> onClick() {
        return onClick;
    }

    public static @NotNull XMenuItem of(@NotNull ItemStack icon) {
        return new XMenuItem(icon, null);
    }

    public static @NotNull XMenuItem of(@NotNull ItemStack icon, @NotNull Consumer<XMenuClickEvent> onClick) {
        return new XMenuItem(icon, onClick);
    }

    public static @NotNull Builder builder(@NotNull Material material) {
        return new Builder(material);
    }

    /** Convenience: a 1×1 filler pane with no click handler — useful for menu borders. */
    public static @NotNull XMenuItem filler(@NotNull Material material) {
        return builder(material).name("<reset>").build();
    }

    public static final class Builder {
        private final ItemStack stack;
        private final ItemMeta meta;
        private @Nullable Consumer<XMenuClickEvent> onClick;

        Builder(@NotNull Material material) {
            this.stack = new ItemStack(material);
            this.meta = stack.getItemMeta();
        }

        public @NotNull Builder name(@NotNull String miniMessage) {
            meta.displayName(MM.deserialize(miniMessage).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            return this;
        }

        public @NotNull Builder name(@NotNull Component name) {
            meta.displayName(name);
            return this;
        }

        public @NotNull Builder lore(@NotNull String... miniMessageLines) {
            List<Component> lines = new ArrayList<>(miniMessageLines.length);
            for (String l : miniMessageLines) {
                lines.add(MM.deserialize(l).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            }
            meta.lore(lines);
            return this;
        }

        public @NotNull Builder lore(@NotNull List<Component> lines) {
            meta.lore(new ArrayList<>(lines));
            return this;
        }

        public @NotNull Builder amount(int amount) {
            stack.setAmount(Math.max(1, Math.min(64, amount)));
            return this;
        }

        public @NotNull Builder hideAttributes() {
            meta.addItemFlags(ItemFlag.values());
            return this;
        }

        public @NotNull Builder onClick(@NotNull Consumer<XMenuClickEvent> handler) {
            this.onClick = handler;
            return this;
        }

        public @NotNull XMenuItem build() {
            stack.setItemMeta(meta);
            return new XMenuItem(stack, onClick);
        }
    }
}
