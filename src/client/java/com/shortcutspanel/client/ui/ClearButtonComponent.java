package com.shortcutspanel.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.shortcutspanel.client.uikit.CursorStyle;
import com.shortcutspanel.client.uikit.UIComponent;
import com.shortcutspanel.client.uikit.UiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class ClearButtonComponent extends UIComponent {

    private static final long CONFIRM_WINDOW_MS = 3000;

    private static final Component LABEL_IDLE = Component.translatable("shortcuts_panel.clear");
    private static final Component LABEL_ARMED =
            Component.translatable("shortcuts_panel.clear.confirm");

    private Runnable onConfirm = () -> {};
    private boolean armed;
    private long armedAt;

    public ClearButtonComponent() {
        this.cursorStyle(CursorStyle.POINTER);
        this.tooltip(LABEL_IDLE);
    }

    public ClearButtonComponent onConfirm(Runnable onConfirm) {
        this.onConfirm = onConfirm == null ? () -> {} : onConfirm;
        return this;
    }

    @Override
    protected void drawSelf(UiGraphics context, int mouseX, int mouseY) {
        int left = this.x();
        int top = this.y();

        if (this.armed && System.currentTimeMillis() - this.armedAt > CONFIRM_WINDOW_MS) {
            this.disarm();
        }

        boolean hovered = this.isHovered();
        context.fill(
                left,
                top,
                left + this.width(),
                top + this.height(),
                hovered ? Theme.BUTTON_HOVER : Theme.BUTTON);

        int ink = this.armed ? Theme.DANGER : (hovered ? Theme.ACCENT : Theme.BORDER);
        context.outline(left, top, this.width(), this.height(), ink);

        context.cross(
                left + this.width() / 2,
                top + this.height() / 2,
                Theme.CLEAR_ICON_SIZE,
                Theme.CLEAR_ICON_THICKNESS,
                ink);
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent event, int mouseX, int mouseY) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;

        if (!this.armed) {
            this.armed = true;
            this.armedAt = System.currentTimeMillis();
            this.tooltip(LABEL_ARMED);
            return true;
        }

        this.disarm();
        this.onConfirm.run();
        return true;
    }

    private void disarm() {
        this.armed = false;
        this.tooltip(LABEL_IDLE);
    }

    @Override
    public boolean canFocus() {
        return false;
    }
}
