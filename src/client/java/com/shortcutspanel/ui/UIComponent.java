package com.shortcutspanel.ui;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 自研 UI 的组件基类。
 *
 * <p>坐标是绝对屏幕（GUI）坐标，布局阶段由父容器写入 {@link #moveTo}。
 * 事件从 UiScreen 一路分发下来，命中测试默认是矩形判定。
 */
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

    // ---------------------------------------------------------------- 配置

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

    // ---------------------------------------------------------------- 布局

    public void moveTo(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * 计算自身尺寸。
     *
     * @param availableWidth  父容器给的最大宽度
     * @param availableHeight 父容器给的最大高度
     */
    public void inflate(int availableWidth, int availableHeight) {
        this.width = resolve(this.horizontalSizing, availableWidth, this.contentWidth());
        this.height = resolve(this.verticalSizing, availableHeight, this.contentHeight());
    }

    /** 内容决定的固有尺寸，子类按需覆盖 */
    protected int contentWidth() {
        return 0;
    }

    protected int contentHeight() {
        return 0;
    }

    /** 按给定可用空间解析某个轴的尺寸；容器在交叉轴上也会直接用它 */
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

    /** 子项定位，容器覆盖 */
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
        return px >= this.x && px <= this.x + this.width
                && py >= this.y && py <= this.y + this.height;
    }

    // ---------------------------------------------------------------- 绘制

    public void draw(UiGraphics context, int mouseX, int mouseY) {
        if (this.surface != null) {
            this.surface.draw(context, this.x, this.y, this.width, this.height);
        }
        this.drawSelf(context, mouseX, mouseY);
    }

    /** 组件自己的内容，子类覆盖 */
    protected void drawSelf(UiGraphics context, int mouseX, int mouseY) {}

    // ---------------------------------------------------------------- 事件

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

    /** 字符输入（含中文输入法提交） */
    public boolean onCharTyped(CharacterEvent input) {
        return false;
    }

    /**
     * 输入法预编辑（正在拼、还没上屏的文字）。
     * 消费它才能让中文输入法正常工作，否则预编辑无处显示，看起来就是"打不进去"。
     */
    public boolean onPreedit(PreeditEvent event) {
        return false;
    }

    /**
     * 焦点变化。输入类组件要覆盖它，把焦点同步给自己内部的原版输入控件，
     * 否则光标不会闪、也收不到输入法事件。
     */
    public void setFocused(boolean focused) {}

    /** 当前是否持有焦点 */
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
