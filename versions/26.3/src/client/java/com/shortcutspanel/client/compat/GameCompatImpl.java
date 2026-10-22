package com.shortcutspanel.client.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;

public final class GameCompatImpl implements GameCompat {

    @Override
    public Screen currentScreen(Minecraft client) {
        return client.gui.screen();
    }

    @Override
    public void setScreen(Minecraft client, Screen screen) {
        client.gui.setScreen(screen);
    }

    @Override
    public void captureScreenshot(Minecraft client) {
        Screenshot.grab(client, false);
    }

    @Override
    public boolean isFriendsKey(Minecraft client, net.minecraft.client.KeyMapping mapping) {
        return mapping == client.options.keyFriends;
    }

    @Override
    public void openSocialScreen(Minecraft client) {
        client.gui.setScreen(
                new net.minecraft.client.gui.screens.social.SocialInteractionsScreen());
    }

    @Override
    public InputConstants.Key keyFromCode(int code) {
        return InputConstants.Type.KEYBOARD.getOrCreate(code);
    }

    @Override
    public InputConstants.Type keyboardType() {
        return InputConstants.Type.KEYBOARD;
    }

    @Override
    public net.minecraft.client.input.KeyEvent keyEvent(int key) {
        return new net.minecraft.client.input.KeyEvent(key, 0, 0);
    }

    @Override
    public void toggleDebugOverlay(Minecraft client) {
        try {
            Object entries = Minecraft.class.getField("debugEntries").get(client);
            entries.getClass().getMethod("toggleDebugOverlay").invoke(entries);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void takeScreenshot(Minecraft client) {
        Screenshot.grab(client, false);
    }

    @Override
    public void toggleFullscreen(Minecraft client) {
        var window = client.getWindow();
        boolean next = !window.isExclusiveFullscreen();
        window.setFullscreen(next);
        client.options.fullscreen().set(next);
        client.options.save();
    }

    @Override
    public void setPointerCursor(boolean pointer) {}
}
