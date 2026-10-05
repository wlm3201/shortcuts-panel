package com.shortcutspanel.ui;

/** 上下左右的内边距，单位是 GUI 像素 */
public final class Insets {

    public static final Insets NONE = new Insets(0, 0, 0, 0);

    private final int top;
    private final int bottom;
    private final int left;
    private final int right;

    private Insets(int top, int bottom, int left, int right) {
        this.top = top;
        this.bottom = bottom;
        this.left = left;
        this.right = right;
    }

    public static Insets of(int all) {
        return new Insets(all, all, all, all);
    }

    public static Insets of(int top, int bottom, int left, int right) {
        return new Insets(top, bottom, left, right);
    }

    public static Insets both(int vertical, int horizontal) {
        return new Insets(vertical, vertical, horizontal, horizontal);
    }

    public static Insets top(int value) {
        return new Insets(value, 0, 0, 0);
    }

    public static Insets horizontal(int value) {
        return new Insets(0, 0, value, value);
    }

    public int top() {
        return this.top;
    }

    public int bottom() {
        return this.bottom;
    }

    public int left() {
        return this.left;
    }

    public int right() {
        return this.right;
    }
}
