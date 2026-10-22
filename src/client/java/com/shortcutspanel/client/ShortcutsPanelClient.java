package com.shortcutspanel.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.shortcutspanel.client.compat.Compat;
import com.shortcutspanel.client.data.BindTrigger;
import com.shortcutspanel.client.data.IdListStore;
import com.shortcutspanel.client.data.Settings;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShortcutsPanelClient implements ClientModInitializer {

    public static final String MOD_ID = "shortcutspanel";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static final String OPEN_KEY_ID = "key.shortcuts_panel.open";

    public static KeyMapping OPEN_KEY;

    public static IdListStore FAVORITES;

    public static IdListStore HISTORY;

    public static IdListStore EXCLUDED;

    public static Settings SETTINGS;

    private static int debugTicks = -1;

    @Override
    public void onInitializeClient() {
        OPEN_KEY =
                KeyMappingHelper.registerKeyMapping(
                        new KeyMapping(
                                OPEN_KEY_ID,
                                Compat.get().keyboardType(),
                                InputConstants.KEY_K,
                                KeyMapping.Category.MISC));

        FAVORITES = IdListStore.load("favorites.json");
        HISTORY = IdListStore.load("history.json");
        EXCLUDED = IdListStore.load("excluded.json");
        SETTINGS = Settings.load();

        if ("1".equals(System.getenv("SHORTCUTS_AUTOSHOT"))) debugTicks = 0;

        ClientTickEvents.START_CLIENT_TICK.register(
                client -> {
                    if (OPEN_KEY.consumeClick() && Compat.get().currentScreen(client) == null) {
                        Compat.get().setScreen(client, new ShortcutsPanelScreen());
                    }

                    BindTrigger.onTick(client);

                    if (debugTicks >= 0) debugTick(client);
                });
    }

    private static void debugTick(Minecraft client) {
        debugTicks++;

        switch (debugTicks) {
            case 40 -> Compat.get().setScreen(client, new ShortcutsPanelScreen());
            case 60 -> {
                if (Compat.get().currentScreen(client) instanceof ShortcutsPanelScreen screen) {
                    for (char c : "screenshot".toCharArray()) {
                        screen.charTyped(new CharacterEvent(c));
                    }
                }
            }
            case 80 -> {
                if (Compat.get().currentScreen(client) instanceof ShortcutsPanelScreen screen) {
                    screen.debugSetSearch("测试test1234");
                }
            }
            case 120 -> {
                if (Compat.get().currentScreen(client) instanceof ShortcutsPanelScreen screen) {
                    screen.debugScrollToBottom();
                }
            }
            case 200 -> Compat.get().captureScreenshot(client);
            case 220 -> client.stop();
            default -> {}
        }
    }
}
