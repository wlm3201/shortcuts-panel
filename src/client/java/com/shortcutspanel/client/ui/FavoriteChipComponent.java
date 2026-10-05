package com.shortcutspanel.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.shortcutspanel.client.ShortcutsPanelScreen;
import com.shortcutspanel.client.data.BindEntry;
import com.shortcutspanel.ui.CursorStyle;
import com.shortcutspanel.ui.UIComponent;
import com.shortcutspanel.ui.UiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 收藏栏里的一个按钮：左键触发，右键移除，按住上下拖动可排序。
 *
 * <p>拖拽事件由 UiScreen 固定发给"按下时命中的组件"，所以拖出收藏栏也不会断。
 */
public final class FavoriteChipComponent extends UIComponent {

    /** 鼠标移动超过这个距离才算拖动，避免手抖把点击变成拖拽 */
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
        // 一行一个 Component：literal 里的 \n 不会被当成换行，会画成缺字的方框
        this.tooltip(List.of(
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
        // 与历史栏的行保持一致：右边给滚动条让位
        int right = left + this.width() - Theme.SCROLLBAR_RESERVE;

        context.fill(left, top, right, top + this.height(),
                this.dragging ? Theme.ROW_ACTIVE : (this.isHovered() ? Theme.CHIP_HOVER : Theme.CHIP));
        context.outline(left, top, right - left, this.height(),
                (this.isHovered() || this.dragging) ? Theme.ACCENT : Theme.BORDER);
        context.text(font, this.label(font), left + 10, top + (this.height() - font.lineHeight) / 2 + 1,
                Theme.TEXT, true);
    }

    /** 条目名按可用宽度截断，尽量占满整行 */
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
        // 落在滚动条上的点击不做任何事
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
