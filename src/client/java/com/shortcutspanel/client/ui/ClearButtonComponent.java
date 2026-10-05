package com.shortcutspanel.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.shortcutspanel.ui.CursorStyle;
import com.shortcutspanel.ui.UIComponent;
import com.shortcutspanel.ui.UiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * 清空按钮：要<strong>点两次</strong>。
 *
 * <p>第一次点击只是进入"待确认"（描边和文字变红，提示再点一次），再点一次才真的执行。
 * 清空是不可撤销的，一步到位太容易误触。
 *
 * <p>进入待确认后如果一段时间没动作会自动退回，免得一直红着。
 */
public final class ClearButtonComponent extends UIComponent {

    /** 待确认状态保留多久后自动取消 */
    private static final long CONFIRM_WINDOW_MS = 3000;

    private static final Component LABEL_IDLE = Component.translatable("shortcuts_panel.clear");
    private static final Component LABEL_ARMED = Component.translatable("shortcuts_panel.clear.confirm");

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
        context.fill(left, top, left + this.width(), top + this.height(),
                hovered ? Theme.BUTTON_HOVER : Theme.BUTTON);

        // × 跟边框同一个配色：待确认才红，悬停青绿，平时和边框一样半透明
        int ink = this.armed ? Theme.DANGER : (hovered ? Theme.ACCENT : Theme.BORDER);
        context.outline(left, top, this.width(), this.height(), ink);

        // × 和边框一样用矩形拼：字体里的 × 字形在这个尺寸下太小，看着不像按钮
        context.cross(left + this.width() / 2, top + this.height() / 2,
                Theme.CLEAR_ICON_SIZE, Theme.CLEAR_ICON_THICKNESS, ink);
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
        // 不抢焦点：点它之后搜索框应该还能继续打字
        return false;
    }
}
