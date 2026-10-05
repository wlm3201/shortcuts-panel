package com.shortcutspanel.client.data;

import net.minecraft.client.KeyMapping;

import java.lang.reflect.Method;

/**
 * amecs「回调型」按键绑定的桥接层（全部走反射，不引入编译期依赖）。
 *
 * <p>amecs 的 {@code AmecsPriorityKeyMapping} 这类绑定不走原版的 {@code clickCount / isDown}
 * ——动作写在 {@code onPressedPriority()} 里，由 amecs 自己 mixin 进
 * {@code KeyboardHandler} / {@code Mouse} 后在真实按键事件到达时调用。
 * 所以我们注入 {@code clickCount} 对它无效（没人读那两个字段），必须直接调回调。
 *
 * <p>采用接口而不是具体类来判断，一个分支就能覆盖所有 amecs 系 mod：
 * Mouse Wheelie 的整理物品 / 滚动 / 选工具 / 打开配置，以及其它用 amecs 的 mod。
 *
 * <p>没装 amecs 时 {@link #isAvailable()} 为 false，原路径照旧。
 */
public final class AmecsBridge {

    private static final String INTERFACE =
            "de.siphalor.amecs.priority_key_mappings.api.AmecsPriorityKeyMapping";

    private static boolean checked;
    private static Class<?> priorityInterface;
    private static Method onPressedPriority;

    private AmecsBridge() {}

    public static boolean isAvailable() {
        if (checked) return priorityInterface != null;
        checked = true;

        try {
            priorityInterface = Class.forName(INTERFACE);
            onPressedPriority = priorityInterface.getMethod("onPressedPriority");
        } catch (Throwable ignored) {
            priorityInterface = null;
        }

        return priorityInterface != null;
    }

    /**
     * 触发一个 amecs 回调型绑定。
     *
     * @return 是否由 amecs 处理掉了。返回 false 有两种情况：它根本不是 amecs 绑定，
     *         或者它的前置条件没满足（比如要求当前打开的是容器界面）——两种情况都该
     *         回退到原版的 {@code clickCount} 路径。
     */
    public static boolean trigger(KeyMapping mapping) {
        if (!isAvailable() || mapping == null || !priorityInterface.isInstance(mapping)) {
            return false;
        }

        try {
            Object consumed = onPressedPriority.invoke(mapping);
            return Boolean.TRUE.equals(consumed);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
