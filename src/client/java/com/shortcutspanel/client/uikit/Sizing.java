package com.shortcutspanel.client.uikit;

public final class Sizing {

    private final Mode mode;
    private final int value;

    private Sizing(Mode mode, int value) {
        this.mode = mode;
        this.value = value;
    }

    public enum Mode {
        FILL,
        FIXED,
        CONTENT,
        EXPAND
    }

    public static Sizing fill(int percent) {
        return new Sizing(Mode.FILL, percent);
    }

    public static Sizing fixed(int pixels) {
        return new Sizing(Mode.FIXED, pixels);
    }

    public static Sizing content() {
        return new Sizing(Mode.CONTENT, 0);
    }

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
