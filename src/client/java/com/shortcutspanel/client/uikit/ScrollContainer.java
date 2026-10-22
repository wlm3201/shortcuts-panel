package com.shortcutspanel.client.uikit;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;

public final class ScrollContainer extends ParentComponent {

    private static final int SCROLL_STEP = 12;

    private static final int GRAB_SLACK = 3;

    private final UIComponent content;

    private double scrollOffset;
    private int maxScroll;
    private int scrollbarColor = 0x55FFFFFF;
    private int scrollbarHoverColor = 0xAAFFFFFF;
    private int thickness = 5;

    private boolean scrollbaring;

    private int fastScrollFactor = 1;
    private boolean fastScrollActive;

    public ScrollContainer(Sizing horizontal, Sizing vertical, UIComponent content) {
        this.content = content;
        this.sizing(horizontal, vertical);
        this.child(content);
    }

    public ScrollContainer scrollbarColor(int color) {
        this.scrollbarColor = color;
        return this;
    }

    public ScrollContainer scrollbarThickness(int thickness) {
        this.thickness = Math.max(2, thickness);
        return this;
    }

    public ScrollContainer fastScroll(boolean active, int factor) {
        this.fastScrollActive = active;
        this.fastScrollFactor = Math.max(1, factor);
        return this;
    }

    public double scrollOffset() {
        return this.scrollOffset;
    }

    public int maxScroll() {
        return this.maxScroll;
    }

    public void scrollTo(double offset) {
        this.scrollOffset = Mth.clamp(offset, 0, Math.max(0, this.maxScroll));
    }

    public double progress() {
        return this.maxScroll <= 0 ? 0 : Mth.clamp(this.scrollOffset / this.maxScroll, 0, 1);
    }

    public void restore(double progress) {
        this.scrollTo(progress * Math.max(0, this.maxScroll));
    }

    private int barX() {
        return this.x + this.width - this.thickness - 1;
    }

    private int barHeight() {
        int track = this.innerHeight();
        if (this.maxScroll <= 0) return track;
        return Math.max(8, Math.round(track * (float) track / (track + this.maxScroll)));
    }

    private int barTravel() {
        return Math.max(0, this.innerHeight() - this.barHeight());
    }

    private int barY() {
        if (this.maxScroll <= 0) return this.innerY();
        return this.innerY()
                + Math.round(this.barTravel() * (float) (this.scrollOffset / this.maxScroll));
    }

    private boolean isInScrollbar(int mouseX, int mouseY) {
        if (this.maxScroll <= 0) return false;
        if (!this.isInBoundingBox(mouseX, mouseY)) return false;
        return mouseX >= this.barX() - GRAB_SLACK;
    }

    @Override
    public void inflate(int availableWidth, int availableHeight) {
        super.inflate(availableWidth, availableHeight);

        this.content.inflate(this.innerWidth() - this.thickness - 1, Integer.MAX_VALUE / 4);
    }

    @Override
    protected int contentWidth() {
        return this.content.width();
    }

    @Override
    protected int contentHeight() {
        return this.content.height();
    }

    @Override
    protected void layoutChildren() {
        int barSpace = this.thickness + 1;
        this.content.inflate(this.innerWidth() - barSpace, Integer.MAX_VALUE / 4);
        this.content.moveTo(this.innerX(), this.innerY() - (int) this.scrollOffset);
        this.content.layout();

        this.maxScroll = Math.max(0, this.content.height() - this.innerHeight());
        this.scrollTo(this.scrollOffset);
        this.content.moveTo(this.innerX(), this.innerY() - (int) this.scrollOffset);
        this.content.layout();
    }

    @Override
    public void draw(UiGraphics context, int mouseX, int mouseY) {
        if (this.surface != null) {
            this.surface.draw(context, this.x, this.y, this.width, this.height);
        }

        context.enableScissor(
                this.innerX(),
                this.innerY(),
                this.innerX() + this.innerWidth(),
                this.innerY() + this.innerHeight());
        this.content.draw(context, mouseX, mouseY);
        context.disableScissor();

        this.drawScrollbar(context, mouseX, mouseY);
    }

    private void drawScrollbar(UiGraphics context, int mouseX, int mouseY) {
        if (this.maxScroll <= 0) return;

        boolean hot = this.scrollbaring || this.isInScrollbar(mouseX, mouseY);
        int color = hot ? this.scrollbarHoverColor : this.scrollbarColor;
        int width = hot ? this.thickness + 1 : this.thickness;

        int barX = this.barX();
        int barY = this.barY();
        context.fill(barX, barY, barX + width, barY + this.barHeight(), color);
    }

    @Override
    public UIComponent childAt(int px, int py) {
        if (!this.isInBoundingBox(px, py)) return null;
        if (this.isInScrollbar(px, py)) return this;
        return this.content.childAt(px, py);
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent event, int mouseX, int mouseY) {
        if (this.isInScrollbar(mouseX, mouseY)) {
            this.scrollbaring = true;

            int travel = this.barTravel();
            if (travel > 0) {
                double ratio =
                        Mth.clamp((mouseY - this.innerY() - this.barHeight() / 2.0) / travel, 0, 1);
                this.scrollTo(ratio * this.maxScroll);
                this.layoutChildren();
            }
            return true;
        }

        UIComponent hit = this.childAt(mouseX, mouseY);
        return hit != null && hit != this && hit.onMouseDown(event, mouseX, mouseY);
    }

    @Override
    public boolean onMouseDrag(MouseButtonEvent event, double deltaX, double deltaY) {
        if (this.scrollbaring) {
            int travel = this.barTravel();
            if (travel > 0) {
                double scalar = (double) this.maxScroll / travel;
                this.scrollTo(this.scrollOffset + deltaY * scalar);
                this.layoutChildren();
            }
            return true;
        }
        return this.content.onMouseDrag(event, deltaX, deltaY);
    }

    @Override
    public boolean onMouseUp(MouseButtonEvent event, int mouseX, int mouseY) {
        if (this.scrollbaring) {
            this.scrollbaring = false;
            return true;
        }
        return this.content.onMouseUp(event, mouseX, mouseY);
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        if (!this.isInBoundingBox(mouseX, mouseY)) return false;

        double step = -amount * SCROLL_STEP * (this.fastScrollActive ? this.fastScrollFactor : 1);
        double before = this.scrollOffset;
        this.scrollTo(this.scrollOffset + step);
        if (this.scrollOffset != before) {
            this.layoutChildren();
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyPress(net.minecraft.client.input.KeyEvent input) {
        return this.content.onKeyPress(input);
    }

    public void revealChild(UIComponent child) {
        int top = child.y();
        int bottom = top + child.height();
        int viewTop = this.innerY();
        int viewBottom = viewTop + this.innerHeight();

        double delta = 0;
        if (top < viewTop) delta = top - viewTop;
        else if (bottom > viewBottom) delta = bottom - viewBottom;
        if (delta == 0) return;

        this.scrollTo(this.scrollOffset + delta);
        this.layoutChildren();
    }
}
