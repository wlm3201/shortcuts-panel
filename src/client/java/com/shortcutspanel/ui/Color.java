package com.shortcutspanel.ui;

/** 颜色就是 0xAARRGGBB，留这个类只是为了调用处读起来自然 */
public final class Color {

    private Color() {}

    public static int ofArgb(int argb) {
        return argb;
    }
}
