package com.shortcutspanel.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/** 一行文字 */
public final class LabelComponent extends UIComponent {

    private Component text;
    private int color = 0xFFF2F4F7;
    private boolean shadow = true;
    private Align.Horizontal alignment = Align.Horizontal.LEFT;

    public LabelComponent(Component text) {
        this.text = text == null ? Component.empty() : text;
    }

    public LabelComponent text(Component text) {
        this.text = text == null ? Component.empty() : text;
        return this;
    }

    public LabelComponent color(int color) {
        this.color = color;
        return this;
    }

    public LabelComponent shadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    public LabelComponent align(Align.Horizontal alignment) {
        this.alignment = alignment;
        return this;
    }

    @Override
    protected int contentWidth() {
        return Minecraft.getInstance().font.width(this.text);
    }

    @Override
    protected int contentHeight() {
        return Minecraft.getInstance().font.lineHeight;
    }

    @Override
    protected void drawSelf(UiGraphics context, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        int y = this.y + (this.height - font.lineHeight) / 2 + 1;

        switch (this.alignment) {
            case LEFT -> context.text(font, this.text, this.x, y, this.color, this.shadow);
            case CENTER -> context.centeredText(font, this.text, this.x + this.width / 2, y, this.color, this.shadow);
            case RIGHT -> context.text(font, this.text, this.x + this.width - font.width(this.text), y,
                    this.color, this.shadow);
        }
    }
}
