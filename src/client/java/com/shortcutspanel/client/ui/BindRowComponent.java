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

public final class BindRowComponent extends UIComponent {

    private final BindEntry entry;
    private final ShortcutsPanelScreen screen;

    private final boolean removable;

    private final boolean canBind;

    private final boolean excluded;

    private boolean selected;

    private int cachedWidth = -1;
    private String cachedName = "";

    private final List<Component> baseTooltip;

    private boolean tooltipSuppressed;

    public BindRowComponent(BindEntry entry, ShortcutsPanelScreen screen, boolean removable) {
        this.entry = entry;
        this.screen = screen;
        this.removable = removable;
        this.canBind = !entry.isMalilib();
        this.excluded = screen.isExcluded(entry);
        this.cursorStyle(CursorStyle.POINTER);

        if (removable) {
            this.baseTooltip =
                    List.of(
                            Component.literal(entry.name()),
                            Component.literal(entry.category() + "  \u00b7  " + entry.keyText()));
        } else {
            this.baseTooltip = List.of(Component.literal(entry.id()));
            this.mouseEnter(() -> this.screen.selectRow(this));
        }
        this.tooltip(this.baseTooltip);
    }

    @Override
    public boolean canFocus() {
        return true;
    }

    @Override
    protected int contentHeight() {
        return Theme.ROW_HEIGHT;
    }

    @Override
    protected void drawSelf(UiGraphics context, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        int left = this.x();
        int top = this.y();
        int bottom = top + this.height();
        int contentRight = left + this.width() - Theme.SCROLLBAR_RESERVE;
        int contentWidth = this.width() - Theme.SCROLLBAR_RESERVE;

        if (this.removable) {
            context.fill(
                    left,
                    top,
                    contentRight,
                    bottom,
                    this.isHovered() ? Theme.CHIP_HOVER : Theme.CHIP);
            context.outline(
                    left,
                    top,
                    contentWidth,
                    this.height(),
                    this.isHovered() ? Theme.ACCENT : Theme.BORDER);
        } else if (this.selected) {
            context.fill(left, top, contentRight, bottom, Theme.ROW_ACTIVE);
            context.fill(left, top, left + 2, bottom, Theme.ACCENT);
        } else if (this.isHovered()) {
            context.fill(left, top, contentRight, bottom, Theme.ROW_HOVER);
        }

        if (this.excluded) {
            context.fill(left, top, left + 2, bottom, Theme.TEXT_DIM);
        }

        int textY = top + (this.height() - font.lineHeight) / 2 + 1;
        int actionLeft = contentRight - 4 - this.actionArea();
        int bindLeft = actionLeft;
        int starLeft =
                this.canBind ? actionLeft + Theme.BUTTON_SIZE + Theme.BUTTON_GAP : actionLeft;
        int textRight = actionLeft - 6;

        boolean compact = this.removable || contentWidth < 150;
        if (!compact) {
            String category = this.entry.category();
            if (!category.isEmpty()) {
                int categoryWidth = font.width(category);
                context.text(
                        font, category, textRight - categoryWidth, textY, Theme.TEXT_DIM, true);
                textRight -= categoryWidth + 8;
            }
            if (contentWidth >= 190) {
                String keyText = this.entry.keyText();
                int badgeWidth = font.width(keyText) + 8;
                int badgeX = textRight - badgeWidth;
                int badgeY = top + (this.height() - 12) / 2;
                context.outline(badgeX, badgeY, badgeWidth, 12, Theme.BORDER);
                context.centeredText(
                        font, keyText, badgeX + badgeWidth / 2, badgeY + 2, Theme.TEXT_DIM, true);
                textRight -= badgeWidth + 8;
            }
        }

        String nameText = this.nameText(font, Math.max(0, textRight - left - 10));
        int nameX = left + 10;
        int nameColor = this.excluded ? Theme.TEXT_DIM : Theme.TEXT;
        context.text(font, nameText, nameX, textY, nameColor, true);

        if (this.excluded) {
            int strikeY = textY + font.lineHeight / 2;
            context.fill(nameX, strikeY, nameX + font.width(nameText), strikeY + 1, Theme.TEXT_DIM);
        }

        if (!this.isHovered()) return;

        int boxY = top + (this.height() - Theme.BUTTON_SIZE) / 2;
        boolean starHover = this.inBox(mouseX, mouseY, starLeft, boxY);
        boolean bindHover = this.canBind && this.inBox(mouseX, mouseY, bindLeft, boxY);

        if (starHover || bindHover) {
            if (!this.tooltipSuppressed) {
                this.tooltipSuppressed = true;
                this.tooltip(List.of());
            }
        } else if (this.tooltipSuppressed) {
            this.tooltipSuppressed = false;
            this.tooltip(this.baseTooltip);
        }

        if (this.canBind) {
            this.drawActionBox(context, font, bindLeft, boxY, "\u952E", false, bindHover);
        }
        this.drawActionBox(
                context,
                font,
                starLeft,
                boxY,
                "\u2605",
                this.screen.isFavorite(this.entry),
                starHover);
    }

    private void drawActionBox(
            UiGraphics context,
            Font font,
            int x,
            int y,
            String glyph,
            boolean active,
            boolean hovered) {
        int size = Theme.BUTTON_SIZE;
        context.fill(x, y, x + size, y + size, hovered ? Theme.BUTTON_HOVER : Theme.BUTTON_BOX);
        context.outline(x, y, size, size, (hovered || active) ? Theme.ACCENT : Theme.BORDER);
        context.centeredText(
                font,
                glyph,
                x + size / 2,
                y + 4,
                active ? Theme.ACCENT : (hovered ? Theme.TEXT : Theme.TEXT_DIM),
                true);
    }

    private String nameText(Font font, int width) {
        if (this.cachedWidth != width) {
            this.cachedWidth = width;
            this.cachedName = fit(font, this.entry.name(), width);
        }
        return this.cachedName;
    }

    private static String fit(Font font, String text, int maxWidth) {
        if (maxWidth <= 8) return "";
        if (font.width(text) <= maxWidth) return text;

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (font.width(builder.toString() + c + "\u2026") > maxWidth) break;
            builder.append(c);
        }
        return builder + "\u2026";
    }

    private int actionArea() {
        return this.canBind ? Theme.BUTTON_SIZE * 2 + Theme.BUTTON_GAP : Theme.BUTTON_SIZE;
    }

    private boolean inBox(double mouseX, double mouseY, int boxX, int boxY) {
        return mouseX >= boxX
                && mouseX <= boxX + Theme.BUTTON_SIZE
                && mouseY >= boxY
                && mouseY <= boxY + Theme.BUTTON_SIZE;
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent event, int mouseX, int mouseY) {
        if (mouseX - this.x() > this.width() - Theme.SCROLLBAR_RESERVE) return false;

        if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            if (this.removable) this.screen.removeHistory(this.entry, this);
            else this.screen.toggleExcluded(this.entry);
            return true;
        }
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;

        int boxY = this.y() + (this.height() - Theme.BUTTON_SIZE) / 2;
        int actionLeft = this.x() + this.width() - Theme.SCROLLBAR_RESERVE - 4 - this.actionArea();

        if (this.canBind && this.inBox(mouseX, mouseY, actionLeft, boxY)) {
            this.screen.startKeyCapture(this.entry);
            return true;
        }
        int starLeft =
                this.canBind ? actionLeft + Theme.BUTTON_SIZE + Theme.BUTTON_GAP : actionLeft;
        if (this.inBox(mouseX, mouseY, starLeft, boxY)) {
            this.screen.toggleFavorite(this.entry);
            return true;
        }

        this.screen.triggerEntry(this.entry);
        return true;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public BindEntry entry() {
        return this.entry;
    }
}
