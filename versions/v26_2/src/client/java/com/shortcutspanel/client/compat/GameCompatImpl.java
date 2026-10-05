package com.shortcutspanel.client.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

/** MC 26.2：屏幕统一走 Minecraft.gui */
public final class GameCompatImpl implements GameCompat {

    private static long handCursor;

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
        client.gui.setScreen(new net.minecraft.client.gui.screens.social.SocialInteractionsScreen());
    }

    @Override
    public InputConstants.Key keyFromCode(int code) {
        return InputConstants.Type.KEYSYM.getOrCreate(code);
    }

    @Override
    public InputConstants.Type keyboardType() {
        return InputConstants.Type.KEYSYM;
    }

    @Override
    public net.minecraft.client.input.KeyEvent keyEvent(int key) {
        return new net.minecraft.client.input.KeyEvent(key, 0, 0);
    }

    @Override
    public void toggleDebugOverlay(Minecraft client) {
        // 全反射：字段名与类型名都可能随版本变
        try {
            Object entries = Minecraft.class.getField("debugEntries").get(client);
            entries.getClass().getMethod("toggleDebugOverlay").invoke(entries);
        } catch (Throwable ignored) {
            // 拿不到就当没这个功能
        }
    }

    @Override
    public void takeScreenshot(Minecraft client) {
        Screenshot.grab(client, false);
    }

    @Override
    public void toggleFullscreen(Minecraft client) {
        var window = client.getWindow();
        window.toggleFullScreen();
        client.options.fullscreen().set(window.isFullscreen());
        client.options.save();
    }

    @Override
    public void setPointerCursor(boolean pointer) {
        long handle = Minecraft.getInstance().getWindow().handle();

        if (pointer && handCursor == 0) {
            handCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_POINTING_HAND_CURSOR);
        }
        // 传 0（NULL）会恢复系统默认箭头
        GLFW.glfwSetCursor(handle, pointer ? handCursor : 0);
    }
}
