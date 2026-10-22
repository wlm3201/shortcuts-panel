package com.shortcutspanel.client.data;

import com.shortcutspanel.client.compat.Compat;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class BindTrigger {

    private static final long HOLD_MS = 100;

    private static BindEntry pending;
    private static KeyMapping holding;
    private static long releaseAt;

    private BindTrigger() {}

    public static void request(BindEntry entry) {
        if (entry == null) return;

        Minecraft client = Minecraft.getInstance();
        pending = entry;
        Compat.get().setScreen(client, null);
    }

    public static void onTick(Minecraft client) {
        if (holding != null && System.currentTimeMillis() >= releaseAt) {
            holding.setDown(false);
            holding = null;
        }

        if (pending == null || Compat.get().currentScreen(client) != null) return;

        BindEntry entry = pending;
        pending = null;

        if (entry.isMalilib()) {
            MalilibBridge.trigger(entry.hotkey());
            return;
        }

        KeyMapping mapping = entry.mapping();

        if (mapping == client.options.keyScreenshot) {
            Compat.get().takeScreenshot(client);
            return;
        }
        if (mapping == client.options.keyFullscreen) {
            Compat.get().toggleFullscreen(client);
            return;
        }
        if (Compat.get().isFriendsKey(client, mapping)) {
            Compat.get().openSocialScreen(client);
            return;
        }

        if (holding != null) {
            holding.setDown(false);
            holding = null;
        }

        if (mapping == client.options.keyDebugOverlay) {
            Compat.get().toggleDebugOverlay(client);
            return;
        }

        if (mapping.getCategory() == KeyMapping.Category.DEBUG
                && DebugKeyTrigger.trigger(client, mapping)) {
            return;
        }

        if (AmecsBridge.trigger(mapping)) return;

        mapping.clickCount = 1;
        mapping.setDown(true);
        holding = mapping;
        releaseAt = System.currentTimeMillis() + HOLD_MS;
    }
}
