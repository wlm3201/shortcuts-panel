package com.shortcutspanel.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.shortcutspanel.client.ShortcutsPanelScreen;
import com.shortcutspanel.client.data.BindEntry;
import com.shortcutspanel.client.uikit.CursorStyle;
import com.shortcutspanel.client.uikit.UIComponent;
import com.shortcutspanel.client.uikit.UiGraphics;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class FavoriteChipComponent extends UIComponent {

    private static final int DRAG_THRESHOLD = 4;

    private final BindEntry entry;
    private final ShortcutsPanelScreen screen;

    private boolean pressed;
    private boolean dragging;
    private int pressY;

    public FavoriteChipComponent(BindEntry entry, ShortcutsPanelScreen screen) {
        this.entry = entry;
        this.screen = screen;
        this.cursorStyle(CursorStyle.POINTER);
        this.tooltip(
                List.of(
                        Component.literal(entry.name()),
                        Component.literal(entry.category() + "  \u00b7  " + entry.keyText())));
    }

    @Override
    protected int contentHeight() {
        return Theme.ROW_HEIGHT;
    }

    public String entryId() {
        return this.entry.id();
    }

    @Override
    protected void drawSelf(UiGraphics context, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        int left = this.x();
        int top = this.y();
        int right = left + this.width() - Theme.SCROLLBAR_RESERVE;

        context.fill(
                left,
                top,
                right,
                top + this.height(),
                this.dragging
                        ? Theme.ROW_ACTIVE
                        : (this.isHovered() ? Theme.CHIP_HOVER : Theme.CHIP));
        context.outline(
                left,
                top,
                right - left,
                this.height(),
                (this.isHovered() || this.dragging) ? Theme.ACCENT : Theme.BORDER);
        context.text(
                font,
                this.label(font),
                left + 10,
                top + (this.height() - font.lineHeight) / 2 + 1,
                Theme.TEXT,
                true);
    }

    private String label(Font font) {
        int max = this.width() - 10 - Theme.SCROLLBAR_RESERVE;
        String text = this.entry.name();

        if (font.width(text) <= max) return text;

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (font.width(builder.toString() + text.charAt(i) + "\u2026") > max) break;
            builder.append(text.charAt(i));
        }
        return builder + "\u2026";
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent event, int mouseX, int mouseY) {
        int localX = mouseX - this.x();
        if (localX > this.width() - Theme.SCROLLBAR_RESERVE) return false;

        if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            this.screen.removeFavorite(this.entry);
            return true;
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            this.pressed = true;
            this.dragging = false;
            this.pressY = mouseY;
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseUp(MouseButtonEvent event, int mouseX, int mouseY) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;

        if (this.dragging) {
            this.pressed = false;
            this.dragging = false;
            this.screen.endDrag(this);
            return true;
        }
        if (this.pressed) {
            this.pressed = false;
            this.screen.triggerEntry(this.entry);
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseDrag(MouseButtonEvent event, double deltaX, double deltaY) {
        if (!this.pressed || event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;

        int mouseY = (int) event.y();

        if (!this.dragging) {
            if (Math.abs(mouseY - this.pressY) < DRAG_THRESHOLD) return true;
            this.dragging = true;
            this.screen.beginDrag(this);
        }

        this.screen.dragTo(this, mouseY);
        return true;
    }

    @Override
    public boolean canFocus() {
        return true;
    }
}
