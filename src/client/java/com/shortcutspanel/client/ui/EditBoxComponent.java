package com.shortcutspanel.client.ui;

import com.shortcutspanel.ui.CursorStyle;
import com.shortcutspanel.ui.UIComponent;
import com.shortcutspanel.ui.UiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;

/**
 * 搜索输入框：包一层原版 {@link EditBox}，文本与光标都交给它画。
 *
 * <p>这样做的好处是把输入体验全部对齐原版：输入法预编辑（中文）、Ctrl+A、Shift+方向选择、
 * Home/End、按词移动、双击选词、Ctrl+C/X/V——都不用自己维护一套选区模型。
 * owo 的 TextBoxComponent 也是直接继承原版 EditBox，只是外面套了自己的渲染。
 *
 * <p>背景与描边由父容器（搜索行）自己画，所以这里 {@code setBordered(false)}。
 */
public final class EditBoxComponent extends UIComponent {

    private static final long DOUBLE_CLICK_MS = 250;

    /**
     * 文本纵向偏移。
     *
     * <p>原版 EditBox 不按"整块高度居中"放文本，而是把它自己的内部文本区（高度按字体行高算）
     * 顶在 {@code getY()} 上，所以直接让它和装饰框同高时文字会偏上（实测偏上约 6px）。
     * 这里把内部框下移 {@code (高度 - 字体行高) / 2}，文本基线就和表头、结果行完全对齐了。
     */
    private int textOffset() {
        // 后一个 +1 是实测出来的：原版内部文本区本身比它自己的 getY() 还高一点点，
        // 补掉这段偏移后，搜索框文字的墨迹才和表头 / 结果行的文字落在同一条线上。
        return Math.max(0, (this.height - Minecraft.getInstance().font.lineHeight) / 2 + 1);
    }

    private final Box box;
    private boolean focused;
    private long lastClickTime;
    private java.util.function.Consumer<Boolean> focusListener = value -> {};

    public EditBoxComponent() {
        this.box = new Box(Minecraft.getInstance().font, Component.empty());
        this.box.setBordered(false);
        this.box.setTextColor(Theme.TEXT);
        this.cursorStyle(CursorStyle.TEXT);
    }

    public EditBoxComponent hint(Component hint) {
        this.box.setHint(hint == null ? Component.empty() : hint);
        return this;
    }

    public EditBoxComponent onChanged(Runnable onChanged) {
        this.box.setResponder(value -> onChanged.run());
        return this;
    }

    /** 焦点变化时回调，容器用它改自己的装饰（比如边框颜色） */
    public EditBoxComponent onFocusChanged(java.util.function.Consumer<Boolean> listener) {
        this.focusListener = listener == null ? value -> {} : listener;
        return this;
    }

    public String getValue() {
        return this.box.getValue();
    }

    public void setValue(String value) {
        // 先把几何同步进去：原版 EditBox 的显示起点（displayPos）是按"当时的宽度"算的，
        // 组件还没布过局时宽度是 0，算出来的显示区间是空串——文本画不出来，
        // 要等玩家点一下触发重算才出现。
        this.syncGeometry();
        this.box.setValue(value == null ? "" : value);
        // 必须再动一下光标：原版 EditBox 靠它重算显示起点，
        // 光 setValue 的话文本可能还是画不出来（要等下一次交互才出现）
        this.box.moveCursorToEnd(false);
        this.box.setHighlightPos(this.box.getCursorPosition());
    }

    public void clear() {
        this.box.setValue("");
    }

    public boolean isFocused() {
        return this.focused;
    }

    /**
     * 选中全部（Ctrl+A 之外的入口）。
     *
     * <p>重开面板时用它把回填的搜索词全选：直接打字就整段覆盖，
     * 不动它也还能接着看上次的结果（原版 EditBox 的 insertText 会替换选区）。
     */
    public void selectAll() {
        this.syncGeometry();
        this.box.moveCursorTo(0, false);
        this.box.moveCursorTo(this.getValue().length(), true);
    }

    @Override
    public void setFocused(boolean focused) {
        if (this.focused == focused) return;
        this.focused = focused;
        this.box.setFocused(focused);
        this.focusListener.accept(focused);
    }

    @Override
    public boolean canFocus() {
        return true;
    }

    /** 把组件当前的几何同步给内部 EditBox，每帧画之前和 setValue 之前都要做一次 */
    private void syncGeometry() {
        // 注意要用这几个单独的 setter，EditBox 把 setRectangle 重写成了别的语义。
        this.box.setX(this.x);
        this.box.setY(this.y + this.textOffset());
        this.box.setWidth(this.width);
        this.box.setHeight(this.height);
    }

    @Override
    protected void drawSelf(UiGraphics context, int mouseX, int mouseY) {
        // 每帧同步一次位置：原版 EditBox 内部靠自己的坐标算文本与光标。
        this.syncGeometry();
        this.box.paint(context.vanilla(), mouseX, mouseY, 0f);
    }

    @Override
    public boolean onKeyPress(KeyEvent input) {
        return this.box.keyPressed(input);
    }

    @Override
    public boolean onCharTyped(CharacterEvent input) {
        return this.box.charTyped(input);
    }

    /** 输入法预编辑：必须转给原版 EditBox，中文才能正常上屏 */
    @Override
    public boolean onPreedit(PreeditEvent event) {
        if (!this.focused) return false;
        return this.box.preeditUpdated(event);
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent event, int mouseX, int mouseY) {
        if (event.button() != com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) return false;

        long now = System.currentTimeMillis();
        boolean doubled = now - this.lastClickTime < DOUBLE_CLICK_MS;
        this.lastClickTime = now;

        return this.box.mouseClicked(event, doubled);
    }

    @Override
    public boolean onMouseDrag(MouseButtonEvent event, double deltaX, double deltaY) {
        return this.box.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean onMouseUp(MouseButtonEvent event, int mouseX, int mouseY) {
        return this.box.mouseReleased(event);
    }

    /** 只为了把受保护的绘制入口暴露出来 */
    private static final class Box extends EditBox {

        Box(Font font, Component message) {
            super(font, 0, 0, 0, 0, message);
        }

        void paint(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            this.extractWidgetRenderState(context, mouseX, mouseY, delta);
        }
    }
}
