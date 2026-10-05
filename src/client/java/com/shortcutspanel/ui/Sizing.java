package com.shortcutspanel.ui;

/** 尺寸策略：按父容器可用空间的百分比 / 固定像素 / 内容决定 / 吃掉剩余空间 */
public final class Sizing {

    private final Mode mode;
    private final int value;

    private Sizing(Mode mode, int value) {
        this.mode = mode;
        this.value = value;
    }

    public enum Mode { FILL, FIXED, CONTENT, EXPAND }

    /** 占父容器可用空间的百分比 */
    public static Sizing fill(int percent) {
        return new Sizing(Mode.FILL, percent);
    }

    public static Sizing fixed(int pixels) {
        return new Sizing(Mode.FIXED, pixels);
    }

    public static Sizing content() {
        return new Sizing(Mode.CONTENT, 0);
    }

    /** 同一容器里其它子项排完后，剩下的空间都给它 */
    public static Sizing expand() {
        return new Sizing(Mode.EXPAND, 0);
    }

    public Mode mode() {
        return this.mode;
    }

    public int value() {
        return this.value;
    }
}
