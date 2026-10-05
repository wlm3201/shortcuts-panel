package com.shortcutspanel.ui;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.List;

/** 容器基类：管子项的添加与事件分发 */
public abstract class ParentComponent extends UIComponent {

    protected final List<UIComponent> children = new ArrayList<>();

    private Insets padding = Insets.NONE;

    public ParentComponent padding(Insets padding) {
        this.padding = padding;
        return this;
    }

    public Insets padding() {
        return this.padding;
    }

    /** 内容区宽度（去掉内边距） */
    public int innerWidth() {
        return Math.max(0, this.width - this.padding.left() - this.padding.right());
    }

    public int innerHeight() {
        return Math.max(0, this.height - this.padding.top() - this.padding.bottom());
    }

    public int innerX() {
        return this.x + this.padding.left();
    }

    public int innerY() {
        return this.y + this.padding.top();
    }

    public ParentComponent child(UIComponent child) {
        this.children.add(child);
        return this;
    }

    public ParentComponent child(int index, UIComponent child) {
        this.children.add(Math.max(0, Math.min(index, this.children.size())), child);
        return this;
    }

    public ParentComponent removeChild(UIComponent child) {
        this.children.remove(child);
        return this;
    }

    public void clearChildren() {
        this.children.clear();
    }

    public List<UIComponent> children() {
        return this.children;
    }

    @Override
    public void draw(UiGraphics context, int mouseX, int mouseY) {
        super.draw(context, mouseX, mouseY);
        for (UIComponent child : this.children) {
            child.draw(context, mouseX, mouseY);
        }
    }

    @Override
    public UIComponent childAt(int px, int py) {
        if (!this.isInBoundingBox(px, py)) return null;

        for (int i = this.children.size() - 1; i >= 0; i--) {
            UIComponent hit = this.children.get(i).childAt(px, py);
            if (hit != null) return hit;
        }
        return this;
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent event, int mouseX, int mouseY) {
        UIComponent hit = this.childAt(mouseX, mouseY);
        return hit != null && hit != this && hit.onMouseDown(event, mouseX, mouseY);
    }

    @Override
    public boolean onMouseUp(MouseButtonEvent event, int mouseX, int mouseY) {
        for (UIComponent child : this.children) {
            if (child.onMouseUp(event, mouseX, mouseY)) return true;
        }
        return false;
    }

    @Override
    public boolean onMouseDrag(MouseButtonEvent event, double deltaX, double deltaY) {
        for (UIComponent child : this.children) {
            if (child.onMouseDrag(event, deltaX, deltaY)) return true;
        }
        return false;
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        for (UIComponent child : this.children) {
            if (child.isInBoundingBox(mouseX, mouseY) && child.onMouseScroll(mouseX, mouseY, amount)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onKeyPress(KeyEvent input) {
        for (UIComponent child : this.children) {
            if (child.onKeyPress(input)) return true;
        }
        return false;
    }

    @Override
    public boolean onCharTyped(CharacterEvent input) {
        for (UIComponent child : this.children) {
            if (child.onCharTyped(input)) return true;
        }
        return false;
    }

    @Override
    public boolean onPreedit(net.minecraft.client.input.PreeditEvent event) {
        for (UIComponent child : this.children) {
            if (child.onPreedit(event)) return true;
        }
        return false;
    }

    /** 悬停状态每帧重算，用于 hover 效果与指针样式 */
    public void updateHovered(int mouseX, int mouseY) {
        UIComponent hit = this.childAt(mouseX, mouseY);
        this.updateHovered(this, hit);
    }

    private void updateHovered(UIComponent current, UIComponent hit) {
        current.setHovered(current == hit);
        if (current instanceof ParentComponent parent) {
            for (UIComponent child : parent.children) {
                updateHovered(child, hit);
            }
        }
    }

    @Override
    public void layout() {
        this.layoutChildren();
    }

    protected abstract void layoutChildren();
}
