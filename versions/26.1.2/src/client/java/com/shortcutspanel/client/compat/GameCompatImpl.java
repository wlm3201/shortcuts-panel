package com.shortcutspanel.client.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;

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
        }
    }

    @Override
    public void takeScreenshot(Minecraft client) {
        Screenshot.grab(
                client.gameDirectory,
                client.getMainRenderTarget(),
                message ->
                        client.execute(() -> client.gui.getChat().addClientSystemMessage(message)));
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
            handCursor =
                    org.lwjgl.glfw.GLFW.glfwCreateStandardCursor(
                            org.lwjgl.glfw.GLFW.GLFW_POINTING_HAND_CURSOR);
        }
        org.lwjgl.glfw.GLFW.glfwSetCursor(clientWindowHandle(), pointer ? handCursor : 0);
    }

    private static long clientWindowHandle() {
        return Minecraft.getInstance().getWindow().handle();
    }
}
