package com.shortcutspanel.client.data;

import com.shortcutspanel.client.compat.Compat;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * 触发某个按键绑定对应的动作。
 *
 * <p>原版在 {@code Minecraft.runTick} 里只有当 {@code Gui.screen == null} 时才会
 * 处理按键绑定，所以这里采取"命令面板"式流程：先把界面关掉，
 * 等下一个 tick 屏幕为空时再触发。
 *
 * <p>触发方式是直接写目标绑定的 {@code clickCount} 并把 {@code isDown} 置真、保持
 * 约 100ms 后松开（字段经 access widener 打开）——等价于 {@code KeyMapping.click}
 * / {@code KeyMapping.set}，但不经过 {@code KeyMapping.MAP} 按键反查表，
 * 因此：
 * <ul>
 *     <li>同一按键上绑了多个动作时只会触发目标这一个；</li>
 *     <li>没有绑定按键的条目同样能触发；</li>
 *     <li>不需要临时改键，界面与 options.txt 都不受影响。</li>
 * </ul>
 *
 * <p>两个容易踩的细节：
 * <ul>
 *     <li>{@code clickCount} 是<b>赋值 1</b>而不是自增：上一次的点击若没被目标消费，
 *         累加会让它下次一次触发好几遍；</li>
 *     <li>按下后要<b>保持约 100ms</b> 再松开：有些 mod 的绑定并不每 tick 轮询，
 *         只按一个 tick 的话它根本看不到这次按下。100ms 是实测够用的下限。</li>
 * </ul>
 *
 * <p>截图、全屏这类"即时分派"的动作不消费 clickCount，
 * 另走 {@code Minecraft.handleGlobalKeyPress}（26.1.2 无此方法，见 GameCompat）。
 */
public final class BindTrigger {

    /** 按下后保持多久再松开（毫秒） */
    private static final long HOLD_MS = 100;

    private static BindEntry pending;
    private static KeyMapping holding;
    private static long releaseAt;

    private BindTrigger() {}

    /** 关闭当前界面并在下一个 tick 触发该绑定 */
    public static void request(BindEntry entry) {
        if (entry == null) return;

        Minecraft client = Minecraft.getInstance();
        pending = entry;
        Compat.get().setScreen(client, null);
    }

    /** 客户端 tick 开头调用 */
    public static void onTick(Minecraft client) {
        // 先给上一次触发收尾：到点就松开按住的键
        if (holding != null && System.currentTimeMillis() >= releaseAt) {
            holding.setDown(false);
            holding = null;
        }

        if (pending == null || Compat.get().currentScreen(client) != null) return;

        BindEntry entry = pending;
        pending = null;

        // MaLiLib 系的快捷键不走原版按键路径，直接调它自己的回调
        if (entry.isMalilib()) {
            MalilibBridge.trigger(entry.hotkey());
            return;
        }

        KeyMapping mapping = entry.mapping();

        // 截图 / 全屏 / 好友屏幕这类绑定由原版的"全局即时分派"处理，不消费 clickCount，
        // 只能按绑定身份直接调实现。
        //
        // 注意：千万不要改用 Minecraft.handleGlobalKeyPress(key, false) 一把梭——
        // 它会拿传入的 key 去比对全屏/截图/好友这几个全局键，而没绑按键的条目
        // 传进去的是 UNKNOWN，一旦某个全局键也处于未绑定状态就会撞上，
        // 表现为"点任意未绑定条目都弹出好友屏幕"。
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

        // 上一次还没松开就又触发了别的：先放开，避免卡在按下状态
        if (holding != null) {
            holding.setDown(false);
            holding = null;
        }

        // F3 本身（切换叠加层）：不在 handleDebugKeys 里，只能单独处理
        if (mapping == client.options.keyDebugOverlay) {
            Compat.get().toggleDebugOverlay(client);
            return;
        }

        // F3 系列调试键：事件驱动分派，不读 clickCount / isDown
        if (mapping.getCategory() == KeyMapping.Category.DEBUG
                && DebugKeyTrigger.trigger(client, mapping)) {
            return;
        }

        // amecs 回调型绑定（Mouse Wheelie 的整理物品 / 滚动 / 选工具 / 打开配置等）：
        // 动作写在 onPressedPriority() 里，由 amecs 在真实按键事件到达时调用，
        // 不走 clickCount / isDown，所以只能直接调它的回调。
        // 返回 false 表示它不是 amecs 绑定或前置条件没满足，回退到下面的原版路径。
        if (AmecsBridge.trigger(mapping)) return;

        mapping.clickCount = 1;
        mapping.setDown(true);
        holding = mapping;
        releaseAt = System.currentTimeMillis() + HOLD_MS;
    }
}
