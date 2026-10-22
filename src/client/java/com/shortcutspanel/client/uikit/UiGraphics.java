package com.shortcutspanel.client.uikit;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class UiGraphics {

    private final GuiGraphicsExtractor context;

    private int tooltipX = -1;
    private int tooltipY = -1;
    private List<Component> tooltipLines = List.of();

    public UiGraphics(GuiGraphicsExtractor context) {
        this.context = context;
    }

    public GuiGraphicsExtractor vanilla() {
        return this.context;
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        this.context.fill(x1, y1, x2, y2, color);
    }

    public void outline(int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) return;

        this.context.fill(x, y, x + width, y + 1, color);
        this.context.fill(x, y + height - 1, x + width, y + height, color);
        if (height > 2) {
            this.context.fill(x, y + 1, x + 1, y + height - 1, color);
            this.context.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
        }
    }

    public void cross(int centerX, int centerY, int size, int thickness, int color) {
        if (size <= 0 || thickness <= 0) return;

        int span = size + thickness - 1;
        int left = centerX - size / 2;
        int top = centerY - span / 2;

        for (int i = 0; i < size; i++) {
            int y = top + i;
            this.context.fill(left + i, y, left + i + 1, y + thickness, color);
            this.context.fill(left + size - 1 - i, y, left + size - i, y + thickness, color);
        }
    }

    public void text(Font font, String text, int x, int y, int color, boolean shadow) {
        this.context.text(font, text, x, y, color, shadow);
    }

    public void text(Font font, Component text, int x, int y, int color, boolean shadow) {
        this.context.text(font, text, x, y, color, shadow);
    }

    public void centeredText(
            Font font, String text, int centerX, int y, int color, boolean shadow) {
        this.context.text(font, text, centerX - font.width(text) / 2, y, color, shadow);
    }

    public void centeredText(
            Font font, Component text, int centerX, int y, int color, boolean shadow) {
        this.context.text(font, text, centerX - font.width(text) / 2, y, color, shadow);
    }

    public void enableScissor(int x1, int y1, int x2, int y2) {
        this.context.enableScissor(x1, y1, x2, y2);
    }

    public void disableScissor() {
        this.context.disableScissor();
    }

    public void queueTooltip(int mouseX, int mouseY, List<Component> lines) {
        this.tooltipX = mouseX;
        this.tooltipY = mouseY;
        this.tooltipLines = lines;
    }

    public boolean hasTooltip() {
        return !this.tooltipLines.isEmpty();
    }

    public void drawQueuedTooltip(Font font, int screenWidth, int screenHeight) {
        if (this.tooltipLines.isEmpty()) return;

        int padding = 4;
        int lineHeight = font.lineHeight + 1;

        int width = 0;
        for (Component line : this.tooltipLines) {
            width = Math.max(width, font.width(line));
        }
        int boxWidth = width + padding * 2;
        int boxHeight = this.tooltipLines.size() * lineHeight + padding * 2 - 1;

        int x = this.tooltipX + 10;
        int y = this.tooltipY + 10;
        if (x + boxWidth > screenWidth) x = this.tooltipX - boxWidth - 6;
        if (y + boxHeight > screenHeight) y = screenHeight - boxHeight - 2;
        if (x < 2) x = 2;
        if (y < 2) y = 2;

        this.context.fill(x, y, x + boxWidth, y + boxHeight, 0xF0100010);
        this.outline(x, y, boxWidth, boxHeight, 0x50FFFFFF);

        int textY = y + padding;
        for (Component line : this.tooltipLines) {
            this.context.text(font, line, x + padding, textY, 0xFFF2F4F7, true);
            textY += lineHeight;
        }

        this.tooltipLines = List.of();
    }
}
