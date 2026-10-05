package com.shortcutspanel.client.data;

import com.mojang.blaze3d.platform.InputConstants;
import com.shortcutspanel.client.compat.Compat;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

/**
 * F3 系列调试键（{@code KeyMapping.Category.DEBUG}）的触发。
 *
 * <p>这批键不走 {@code clickCount / isDown}：原版在
 * {@code KeyboardHandler.handleDebugKeys(KeyEvent)} 里用
 * {@code KeyMapping.matches(KeyEvent)} 逐个比对，纯粹事件驱动。
 * 所以注入 {@code clickCount} 对它们完全无效，必须把事件送到那个方法。
 *
 * <p>麻烦在于它们默认都是<em>未绑定</em>的，而 {@code matches(KeyEvent)} 对未绑定键
 * 会退化成比对 scancode（源码：{@code event.key() == UNKNOWN.getValue() ?
 * 比 scancode : 比 keysym}），scancode 我们无从得知。
 * 因此这里临时把键换成一个真实键盘上不存在的虚拟键，分派完立刻还原。
 *
 * <p>整个过程是同步的（同一个调用内完成），不写 options、不进 {@code KeyMapping.MAP}
 * （我们压根不走那张表），所以没有污染窗口。
 */
public final class DebugKeyTrigger {

    private static final String METHOD = "handleDebugKeys";

    /**
     * 借给调试键用的虚拟键码：比当前所有绑定里的最大键码还大 1。
     *
     * <p>不能写死某个值——26.x 里调试键的默认键码就是 300（高级提示框、切换游戏模式、
     * FPS 图表都是），而 handleDebugKeys 是一串独立的 if、全都拿同一个事件去
     * {@code matches}，撞上的话会把那几个一起触发。
     */
    private static int virtualKeyCode(Minecraft client) {
        int max = 0;
        for (KeyMapping mapping : client.options.keyMappings) {
            max = Math.max(max, KeyMappingHelper.getBoundKeyOf(mapping).getValue());
        }
        return max + 1;
    }

    private DebugKeyTrigger() {}

    /**
     * @return 是否命中了某个调试功能。false 表示没能触发（反射失败、或该事件没匹配到
     *         任何调试键），调用方应回退到原版路径。
     */
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
