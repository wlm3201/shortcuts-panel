package com.shortcutspanel.client.data;

import java.lang.reflect.Method;
import net.minecraft.client.KeyMapping;

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
