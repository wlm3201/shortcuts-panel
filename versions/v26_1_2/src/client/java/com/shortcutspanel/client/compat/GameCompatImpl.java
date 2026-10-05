package com.shortcutspanel.client.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;

/** MC 26.1.2：屏幕走 Minecraft 自己的字段/方法 */
public final class GameCompatImpl implements GameCompat {

    private static long handCursor;

    @Override
    public Screen currentScreen(Minecraft client) {
        return client.screen;
    }

    @Override
    public void setScreen(Minecraft client, Screen screen) {
        client.setScreen(screen);
    }

    @Override
    public void captureScreenshot(Minecraft client) {
        Screenshot.grab(client.gameDirectory, client.getMainRenderTarget(), message -> {});
    }

    @Override
    public boolean isFriendsKey(Minecraft client, net.minecraft.client.KeyMapping mapping) {
        // 26.1.2 的 Options 里没有 keyFriends
        return false;
    }

    @Override
    public void openSocialScreen(Minecraft client) {
        client.setScreen(new net.minecraft.client.gui.screens.social.SocialInteractionsScreen());
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
        try {
            Object entries = Minecraft.class.getField("debugEntries").get(client);
            entries.getClass().getMethod("toggleDebugOverlay").invoke(entries);
        } catch (Throwable ignored) {
            // 26.1.2 的调试入口如果不同就静默跳过
        }
    }

    @Override
    public void takeScreenshot(Minecraft client) {
        // 26.1.2 没有 Screenshot.grab(Minecraft, boolean)，自己传目录与主渲染目标
        Screenshot.grab(client.gameDirectory, client.getMainRenderTarget(),
                message -> client.execute(() -> client.gui.getChat().addClientSystemMessage(message)));
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
        if (handCursor == 0) {
            handCursor = org.lwjgl.glfw.GLFW.glfwCreateStandardCursor(
                    org.lwjgl.glfw.GLFW.GLFW_POINTING_HAND_CURSOR);
        }
        org.lwjgl.glfw.GLFW.glfwSetCursor(clientWindowHandle(), pointer ? handCursor : 0);
    }

    /** 26.1.2 的窗口句柄是 long 字段，26.2 起是 handle() 方法 */
    private static long clientWindowHandle() {
        return Minecraft.getInstance().getWindow().handle();
    }
}
