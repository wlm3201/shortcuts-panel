package com.shortcutspanel.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.shortcutspanel.client.compat.Compat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;

/**
 * 自研 UI 的屏幕基类：管布局、绘制、事件分发、指针样式与 tooltip。
 *
 * <p>坐标一律用 GUI 坐标（MC 传进来的鼠标坐标就是这个坐标系）。
 */
public abstract class UiScreen extends Screen {

    protected FlowLayout root;

    /** 上一次建 root 时的窗口尺寸，用来判断要不要重建 */
    private int lastWidth = -1;
    private int lastHeight = -1;

    private UIComponent focused;
    private UIComponent dragged;
    private boolean pointerCursor;

    protected UiScreen(Component title) {
        super(title);
    }

    /** 子类往 root 里加东西 */
    protected abstract void build(FlowLayout root);

    protected FlowLayout createRoot() {
        return FlowLayout.horizontal(Sizing.fill(100), Sizing.fill(100));
    }

    public FlowLayout root() {
        return this.root;
    }

    @Override
    protected void init() {
        // MC 的 setScreen 会对目标 screen 重新走一遍 init（从改键界面返回就是这样）。
        // 窗口尺寸没变就别重建：否则搜索框内容、滚动位置、焦点全都会丢。
        if (this.root != null && this.lastWidth == this.width && this.lastHeight == this.height) {
            return;
        }

        this.lastWidth = this.width;
        this.lastHeight = this.height;

        this.root = this.createRoot();
        this.build(this.root);
        this.relayout();
    }

    /** 窗口尺寸变化时重新布局 */
    public void relayout() {
        if (this.root == null) return;

        this.root.moveTo(0, 0);
        this.root.inflate(this.width, this.height);
        this.root.layout();
    }

    // ---------------------------------------------------------------- 绘制

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        if (this.root == null) return;

        this.root.updateHovered(mouseX, mouseY);

        var graphics = new UiGraphics(context);
        this.root.inflate(this.width, this.height);
        this.root.layout();
        this.root.draw(graphics, mouseX, mouseY);

        this.queueTooltip(graphics, mouseX, mouseY);
        graphics.drawQueuedTooltip(Minecraft.getInstance().font, this.width, this.height);

        this.updateCursor(mouseX, mouseY);
    }

    private void queueTooltip(UiGraphics graphics, int mouseX, int mouseY) {
        UIComponent hit = this.root.childAt(mouseX, mouseY);
        if (hit == null || hit.tooltip().isEmpty() || !hit.isHovered()) return;

        graphics.queueTooltip(mouseX, mouseY, hit.tooltip());
    }

    private void updateCursor(int mouseX, int mouseY) {
        UIComponent hit = this.root.childAt(mouseX, mouseY);
        boolean pointer = hit != null && hit.cursorStyle() == CursorStyle.POINTER;

        if (pointer != this.pointerCursor) {
            this.pointerCursor = pointer;
            Compat.get().setPointerCursor(pointer);
        }
    }

    // ---------------------------------------------------------------- 事件

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (this.root == null) return false;

        int mouseX = (int) event.x();
        int mouseY = (int) event.y();

        UIComponent hit = this.root.childAt(mouseX, mouseY);
        this.focus(hit);
        if (hit == null) return false;

        this.dragged = hit;
        return hit.onMouseDown(event, mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.root == null) return false;

        int mouseX = (int) event.x();
        int mouseY = (int) event.y();

        // 必须先发给按下时命中的组件：拖完松手时鼠标往往已经离开它了，
        // 只按当前悬停位置分发的话，它会永远收不到释放，状态卡在"拖动中"
        // （滚动条保持高亮、收藏条目拖完放不下）。
        boolean handled = false;
        if (this.dragged != null) {
            handled = this.dragged.onMouseUp(event, mouseX, mouseY);
        }
        if (!handled) {
            handled = this.root.onMouseUp(event, mouseX, mouseY);
        }

        this.dragged = null;
        return handled;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        // 拖拽事件始终发给按下时命中的那个组件，否则拖出边界就断了
        return this.dragged != null && this.dragged.onMouseDrag(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.root == null) return false;

        int guiX = (int) mouseX;
        int guiY = (int) mouseY;
        return this.root.onMouseScroll(guiX, guiY, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (this.root == null) return false;

        if (this.focused != null && this.focused.onKeyPress(input)) return true;
        if (this.root.onKeyPress(input)) return true;
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (this.root == null) return false;

        if (this.focused != null && this.focused.onCharTyped(input)) return true;
        return this.root.onCharTyped(input);
    }

    /** 输入法预编辑：必须转给聚焦的组件，否则中文打不进去 */
    @Override
    public boolean preeditUpdated(PreeditEvent event) {
        if (this.focused != null && this.focused.onPreedit(event)) return true;
        return this.root != null && this.root.onPreedit(event);
    }

    /**
     * 设置焦点。点空白或点到不可聚焦的组件（列表、按钮）等于"取消聚焦"，
     * 否则焦点会永远留在唯一可聚焦的输入框上，聚焦高亮就失去了意义。
     */
    public void focus(UIComponent component) {
        if (component != null && !component.canFocus()) component = null;
        if (this.focused == component) return;

        // 必须把旧组件的焦点关掉，否则输入框会一直闪光标
        if (this.focused != null) this.focused.setFocused(false);
        this.focused = component;
        if (component != null) component.setFocused(true);
    }

    public UIComponent focused() {
        return this.focused;
    }

    public void focusFirstFocusable() {
        this.focused = findFirstFocusable(this.root);
    }

    private UIComponent findFirstFocusable(UIComponent current) {
        if (current == null) return null;
        if (current.canFocus()) return current;

        if (current instanceof ParentComponent parent) {
            for (UIComponent child : parent.children()) {
                UIComponent found = findFirstFocusable(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    @Override
    public void removed() {
        super.removed();
        Compat.get().setPointerCursor(false);
    }

    /** Esc 一次关闭本屏，不做两段确认 */
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    protected Font font() {
        return Minecraft.getInstance().font;
    }

    protected static boolean isAlt(KeyEvent input) {
        return input.key() == InputConstants.KEY_LALT || input.key() == InputConstants.KEY_RALT;
    }
}
