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

public class ShortcutsPanelClient implements ClientModInitializer {

    public static final String OPEN_KEY_ID = "key.shortcuts_panel.open";

    public static KeyMapping OPEN_KEY;

    /** 右侧收藏栏，跨会话保存 */
    public static IdListStore FAVORITES;
    /** 左侧最近触发记录，跨会话保存 */
    public static IdListStore HISTORY;
    /** 被右键永久排除的条目 */
    public static IdListStore EXCLUDED;
    /** 面板开关（是否显示已绑定项） */
    public static Settings SETTINGS;

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("shortcuts-panel");

    /** 调试用：设置环境变量 SHORTCUTS_AUTOSHOT=1 时自动开屏、截图并退出 */
    private static int debugTicks = -1;

    @Override
    public void onInitializeClient() {
        OPEN_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                OPEN_KEY_ID,
                Compat.get().keyboardType(),
                InputConstants.KEY_K,
                KeyMapping.Category.MISC
        ));

        FAVORITES = IdListStore.load("favorites.json");
        HISTORY = IdListStore.load("history.json");
        EXCLUDED = IdListStore.load("excluded.json");
        SETTINGS = Settings.load();

        if ("1".equals(System.getenv("SHORTCUTS_AUTOSHOT"))) debugTicks = 0;

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            // 无论屏幕是否打开都消费掉，避免关屏后残留的点击把界面又弹出来
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
            // 打开后先敲几个字符，再清空，让结果栏列出全部条目
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
            // 滚到底，检查内容是否真的滚完
            case 120 -> {
                if (Compat.get().currentScreen(client) instanceof ShortcutsPanelScreen screen) {
                    screen.debugScrollToBottom();
                }
            }
            // 等加载层完全淡出再截
            case 200 -> Compat.get().captureScreenshot(client);
            case 220 -> client.stop();
            default -> {}
        }
    }
}
