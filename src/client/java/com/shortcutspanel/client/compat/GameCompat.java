package com.shortcutspanel.client.compat;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;

/**
 * 与 MC 版本耦合的调用都收在这里。共享源码只面向这个接口编程，
 * 真正的实现由每个版本子项目提供：{@code com.shortcutspanel.client.compat.GameCompatImpl}。
 *
 * <ul>
 *     <li>26.1.2：{@code Minecraft.screen} 字段 + {@code Minecraft.setScreen}</li>
 *     <li>26.2 / 26.3：{@code Minecraft.gui.screen()} + {@code Minecraft.gui.setScreen}</li>
 * </ul>
 */
public interface GameCompat {

    /** 当前打开的屏幕，没有则为 null */
    Screen currentScreen(Minecraft client);

    void setScreen(Minecraft client, Screen screen);

    /** 仅调试用的自动截图，失败也不影响功能 */
    void captureScreenshot(Minecraft client);

    /**
     * 由键码构造一个键盘按键。
     * 26.1.2 / 26.2 是 {@code Type.KEYSYM}，26.3 改名成了 {@code Type.KEYBOARD}。
     */
    InputConstants.Key keyFromCode(int code);

    /** 注册 KeyMapping 时用的键盘类型（同上，26.3 改了名） */
    InputConstants.Type keyboardType();

    /**
     * 构造一个键盘按键事件。F3 调试键是事件驱动的（{@code KeyboardHandler.handleDebugKeys}
     * 里用 {@code KeyMapping.matches(KeyEvent)} 比对），必须喂一个事件才能匹配到。
     */
    KeyEvent keyEvent(int key);

    /**
     * 切换 F3 调试叠加层。
     *
     * <p>它不在 {@code handleDebugKeys} 里（那里只有 F3+组合）：F3 既是修饰键又是
     * 独立功能键，靠"按下 / 松开"来区分，这种判断只能在真实按键事件流里做，
     * 所以要像截图、全屏那样单独处理。
     */
    void toggleDebugOverlay(Minecraft client);

    /**
     * 截图键：直接调用截图实现。
     * 这类"全局即时分派"的键不消费 {@code clickCount}，只能按绑定身份直接调实现。
     */
    void takeScreenshot(Minecraft client);

    /** 全屏键：直接切全屏并把状态写回 options */
    void toggleFullscreen(Minecraft client);

    /**
     * 是不是"好友屏幕"那个绑定。
     * 26.1.2 的 {@code Options} 里还没有这个字段，返回 false。
     */
    boolean isFriendsKey(Minecraft client, KeyMapping mapping);

    /** 打开好友/社交屏幕 */
    void openSocialScreen(Minecraft client);

    /** 鼠标指针：{@code pointer} 为 true 时显示手指（可点击），false 恢复箭头 */
    void setPointerCursor(boolean pointer);
}
