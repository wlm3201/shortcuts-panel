package com.shortcutspanel.ui;

/** 组件背景：纯色填充、描边，或两者的组合 */
public interface Surface {

    void draw(UiGraphics context, int x, int y, int width, int height);

    default Surface and(Surface other) {
        Surface self = this;
        return (context, x, y, width, height) -> {
            self.draw(context, x, y, width, height);
            other.draw(context, x, y, width, height);
        };
    }

    static Surface flat(int color) {
        return (context, x, y, width, height) -> context.fill(x, y, x + width, y + height, color);
    }

    static Surface outline(int color) {
        return (context, x, y, width, height) -> context.outline(x, y, width, height, color);
    }
}
