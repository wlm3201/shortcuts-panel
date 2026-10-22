package com.shortcutspanel.client.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;

public interface GameCompat {

    Screen currentScreen(Minecraft client);

    void setScreen(Minecraft client, Screen screen);

    void captureScreenshot(Minecraft client);

    InputConstants.Key keyFromCode(int code);

    InputConstants.Type keyboardType();

    KeyEvent keyEvent(int key);

    void toggleDebugOverlay(Minecraft client);

    void takeScreenshot(Minecraft client);

    void toggleFullscreen(Minecraft client);

    boolean isFriendsKey(Minecraft client, KeyMapping mapping);

    void openSocialScreen(Minecraft client);

    void setPointerCursor(boolean pointer);
}
