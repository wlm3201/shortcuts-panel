package com.shortcutspanel.client.ui;

import com.shortcutspanel.client.uikit.CursorStyle;
import com.shortcutspanel.client.uikit.UIComponent;
import com.shortcutspanel.client.uikit.UiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;

public final class EditBoxComponent extends UIComponent {

    private static final long DOUBLE_CLICK_MS = 250;

    private int textOffset() {
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

    public EditBoxComponent onFocusChanged(java.util.function.Consumer<Boolean> listener) {
        this.focusListener = listener == null ? value -> {} : listener;
        return this;
    }

    public String getValue() {
        return this.box.getValue();
    }

    public void setValue(String value) {
        this.syncGeometry();
        this.box.setValue(value == null ? "" : value);
        this.box.moveCursorToEnd(false);
        this.box.setHighlightPos(this.box.getCursorPosition());
    }

    public void clear() {
        this.box.setValue("");
    }

    public boolean isFocused() {
        return this.focused;
    }

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

    private void syncGeometry() {
        this.box.setX(this.x);
        this.box.setY(this.y + this.textOffset());
        this.box.setWidth(this.width);
        this.box.setHeight(this.height);
    }

    @Override
    protected void drawSelf(UiGraphics context, int mouseX, int mouseY) {
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

    @Override
    public boolean onPreedit(PreeditEvent event) {
        if (!this.focused) return false;
        return this.box.preeditUpdated(event);
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent event, int mouseX, int mouseY) {
        if (event.button() != com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT)
            return false;

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

    private static final class Box extends EditBox {

        Box(Font font, Component message) {
            super(font, 0, 0, 0, 0, message);
        }

        void paint(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            this.extractWidgetRenderState(context, mouseX, mouseY, delta);
        }
    }
}
