package com.shortcutspanel.client.uikit;

import java.util.List;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;

public abstract class UIComponent {

    protected int x;
    protected int y;
    protected int width;
    protected int height;

    private Sizing horizontalSizing = Sizing.content();
    private Sizing verticalSizing = Sizing.content();
    private Insets margins = Insets.NONE;
    protected Surface surface;
    private CursorStyle cursorStyle = CursorStyle.ARROW;
    private List<Component> tooltip = List.of();
    private boolean hovered;

    private Runnable onMouseEnter = () -> {};
    private Runnable onMouseLeave = () -> {};

    public UIComponent sizing(Sizing horizontal, Sizing vertical) {
        this.horizontalSizing = horizontal;
        this.verticalSizing = vertical;
        return this;
    }

    public UIComponent horizontalSizing(Sizing sizing) {
        this.horizontalSizing = sizing;
        return this;
    }

    public UIComponent verticalSizing(Sizing sizing) {
        this.verticalSizing = sizing;
        return this;
    }

    public Sizing horizontalSizing() {
        return this.horizontalSizing;
    }

    public Sizing verticalSizing() {
        return this.verticalSizing;
    }

    public UIComponent margins(Insets margins) {
        this.margins = margins;
        return this;
    }

    public Insets margins() {
        return this.margins;
    }

    public UIComponent surface(Surface surface) {
        this.surface = surface;
        return this;
    }

    public UIComponent cursorStyle(CursorStyle style) {
        this.cursorStyle = style;
        return this;
    }

    public CursorStyle cursorStyle() {
        return this.cursorStyle;
    }

    public UIComponent tooltip(Component tooltip) {
        this.tooltip = tooltip == null ? List.of() : List.of(tooltip);
        return this;
    }

    public UIComponent tooltip(List<Component> tooltip) {
        this.tooltip = tooltip == null ? List.of() : tooltip;
        return this;
    }

    public List<Component> tooltip() {
        return this.tooltip;
    }

    public UIComponent mouseEnter(Runnable action) {
        this.onMouseEnter = action;
        return this;
    }

    public UIComponent mouseLeave(Runnable action) {
        this.onMouseLeave = action;
        return this;
    }

    public void moveTo(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void inflate(int availableWidth, int availableHeight) {
        this.width = resolve(this.horizontalSizing, availableWidth, this.contentWidth());
        this.height = resolve(this.verticalSizing, availableHeight, this.contentHeight());
    }

    protected int contentWidth() {
        return 0;
    }

    protected int contentHeight() {
        return 0;
    }

    public static int resolve(Sizing sizing, int available, int content) {
        return switch (sizing.mode()) {
            case FIXED -> sizing.value();
            case FILL -> Math.round(available * sizing.value() / 100f);
            case EXPAND -> available;
            case CONTENT -> Math.max(content, 0);
        };
    }

    public int contentWidthForLayout() {
        return this.contentWidth();
    }

    public int contentHeightForLayout() {
        return this.contentHeight();
    }

    public void layout() {}

    public int x() {
        return this.x;
    }

    public int y() {
        return this.y;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public boolean isInBoundingBox(double px, double py) {
        return px >= this.x
                && px <= this.x + this.width
                && py >= this.y
                && py <= this.y + this.height;
    }

    public void draw(UiGraphics context, int mouseX, int mouseY) {
        if (this.surface != null) {
            this.surface.draw(context, this.x, this.y, this.width, this.height);
        }
        this.drawSelf(context, mouseX, mouseY);
    }

    protected void drawSelf(UiGraphics context, int mouseX, int mouseY) {}

    public UIComponent childAt(int px, int py) {
        return this.isInBoundingBox(px, py) ? this : null;
    }

    public boolean onMouseDown(MouseButtonEvent event, int mouseX, int mouseY) {
        return false;
    }

    public boolean onMouseUp(MouseButtonEvent event, int mouseX, int mouseY) {
        return false;
    }

    public boolean onMouseDrag(MouseButtonEvent event, double deltaX, double deltaY) {
        return false;
    }

    public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        return false;
    }

    public boolean onKeyPress(KeyEvent input) {
        return false;
    }

    public boolean onCharTyped(CharacterEvent input) {
        return false;
    }

    public boolean onPreedit(PreeditEvent event) {
        return false;
    }

    public void setFocused(boolean focused) {}

    public boolean isFocused() {
        return false;
    }

    public boolean canFocus() {
        return false;
    }

    public void setHovered(boolean hovered) {
        if (this.hovered == hovered) return;

        this.hovered = hovered;
        if (hovered) this.onMouseEnter.run();
        else this.onMouseLeave.run();
    }

    public boolean isHovered() {
        return this.hovered;
    }
}
