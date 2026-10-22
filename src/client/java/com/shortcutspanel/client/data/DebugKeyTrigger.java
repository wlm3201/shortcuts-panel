package com.shortcutspanel.client.data;

import com.mojang.blaze3d.platform.InputConstants;
import com.shortcutspanel.client.compat.Compat;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

public final class DebugKeyTrigger {

    private static final String METHOD = "handleDebugKeys";

    private static int virtualKeyCode(Minecraft client) {
        int max = 0;
        for (KeyMapping mapping : client.options.keyMappings) {
            max = Math.max(max, KeyMappingHelper.getBoundKeyOf(mapping).getValue());
        }
        return max + 1;
    }

    private DebugKeyTrigger() {}

    public static boolean trigger(Minecraft client, KeyMapping mapping) {
        try {
            InputConstants.Key original = KeyMappingHelper.getBoundKeyOf(mapping);
            int virtualKey = virtualKeyCode(client);
            InputConstants.Key virtual = Compat.get().keyboardType().getOrCreate(virtualKey);

            mapping.setKey(virtual);
            try {
                KeyEvent event = Compat.get().keyEvent(virtualKey);
                Object handler = client.keyboardHandler;

                var method = handler.getClass().getDeclaredMethod(METHOD, KeyEvent.class);
                method.setAccessible(true);

                Object consumed = method.invoke(handler, event);
                return Boolean.TRUE.equals(consumed);
            } finally {
                mapping.setKey(original);
            }
        } catch (Throwable ignored) {
            return false;
        }
    }
}
