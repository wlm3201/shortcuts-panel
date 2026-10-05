package com.shortcutspanel.client.compat;

/**
 * 加载当前 MC 版本提供的 {@link GameCompat} 实现。
 * 用反射按约定类名加载，这样共享源码里不需要引用任何版本专属类。
 */
public final class Compat {

    private static final String IMPL_CLASS = "com.shortcutspanel.client.compat.GameCompatImpl";

    private static GameCompat instance;

    private Compat() {}

    public static GameCompat get() {
        if (instance == null) {
            try {
                var implClass = Class.forName(IMPL_CLASS);
                instance = (GameCompat) implClass.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("缺少 " + IMPL_CLASS + "（该 MC 版本没有提供兼容实现）", error);
            }
        }
        return instance;
    }
}
