package com.shortcutspanel.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.shortcutspanel.client.uikit.CursorStyle;
import com.shortcutspanel.client.uikit.UIComponent;
import com.shortcutspanel.client.uikit.UiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class ToggleButtonComponent extends UIComponent {

    private Component label = Component.empty();
    private Runnable onPress = () -> {};

    private boolean active;

    public ToggleButtonComponent() {
        this.cursorStyle(CursorStyle.POINTER);
    }

    public ToggleButtonComponent label(Component label) {
        this.label = label == null ? Component.empty() : label;
        return this;
    }

    public ToggleButtonComponent onPress(Runnable onPress) {
        this.onPress = onPress;
        return this;
    }

    public ToggleButtonComponent active(boolean active) {
        this.active = active;
        return this;
    }

    @Override
    protected void drawSelf(UiGraphics context, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        int left = this.x();
        int top = this.y();

        context.fill(
                left,
                top,
                left + this.width(),
                top + this.height(),
                this.isHovered() ? Theme.BUTTON_HOVER : Theme.BUTTON);
        context.outline(
                left,
                top,
                this.width(),
                this.height(),
                (this.isHovered() || this.active) ? Theme.ACCENT : Theme.BORDER);

        String text = this.label.getString();
        int textX = left + (this.width() - font.width(text)) / 2;
        context.text(
                font,
                text,
                textX,
                top + (this.height() - font.lineHeight) / 2 + 1,
                this.active ? Theme.ACCENT : Theme.TEXT,
                true);
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent event, int mouseX, int mouseY) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;

        this.onPress.run();
        return true;
    }

    @Override
    public boolean canFocus() {
        return true;
    }
}
