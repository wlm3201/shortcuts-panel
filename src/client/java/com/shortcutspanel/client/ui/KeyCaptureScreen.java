package com.shortcutspanel.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.shortcutspanel.client.ShortcutsPanelScreen;
import com.shortcutspanel.client.compat.Compat;
import com.shortcutspanel.client.data.BindEntry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * 捕获一次按键，把目标绑定项绑到该键上。键盘键和鼠标键都行。
 *
 * <p>Esc 与原版按键设置界面保持一致：<strong>设为无</strong>（解除绑定），而不是取消本次操作。
 *
 * <p>鼠标键与原版一致：按下哪个键就绑哪个（左键/右键/中键/侧键都算），
 * {@code KeyMapping.matchesMouse} 会在游戏里分派它，不用额外处理。
 *
 * <p>捕获到的键不立刻返回面板：等下一帧再切回去，否则同帧的 {@code charTyped}
 * 会落到面板的搜索框里，把要绑定的键当成输入。
 */
public final class KeyCaptureScreen extends Screen {

    private final BindEntry entry;
    private final ShortcutsPanelScreen parent;

    private InputConstants.Key captured;

    public KeyCaptureScreen(BindEntry entry, ShortcutsPanelScreen parent) {
        super(Component.translatable("shortcuts_panel.capture.title", entry.name()));
        this.entry = entry;
        this.parent = parent;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        // 与原版一致：Esc = 把绑定设为无，不是放弃这次修改
        this.captured = input.isEscape() ? InputConstants.UNKNOWN : InputConstants.getKey(input);
        return true;
    }

    /**
     * 鼠标键同样是合法绑定：原版按键设置界面就是这么做的（{@code KeyBindsScreen.mouseClicked}）。
     * 按到哪个键绑哪个，包括左右键——所以想绑右键就在这里点一下右键。
     *
     * <p>不需要担心"打开本界面的那一次点击"被算进来：那次按下是面板在处理，
     * 本界面是在它的处理过程中才被 setScreen 换上来的，收不到那一下。
     */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        this.captured = InputConstants.Type.MOUSE.getOrCreate(event.button());
        return true;
    }

    /**
     * 吞掉这次点击的松开。按下已经记下了，本界面要等下一帧才切走，
     * 松手多半落在这之前；不吞的话会穿透到面板上。
     */
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        // 吞掉字符输入，避免它跑到搜索框里
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (this.captured != null) {
            this.close();
            return;
        }

        var font = Minecraft.getInstance().font;
        context.fill(0, 0, this.width, this.height, Theme.CAPTURE_SCRIM);

        context.centeredText(font, this.title, this.width / 2, this.height / 2 - 16, Theme.ACCENT);
        context.centeredText(font, Component.translatable("shortcuts_panel.capture.hint"),
                this.width / 2, this.height / 2, Theme.TEXT);
    }

    private void close() {
        // MaLiLib 系没有 KeyMapping，理论上走不到这里（面板上不给它们"改键"按钮），兜个底
        KeyMapping mapping = this.entry.mapping();
        if (this.captured != null && mapping != null) {
            mapping.setKey(this.captured);
            // 走原版保存，写进 options.txt
            Minecraft.getInstance().options.save();
            // setKey 只改键位字段（界面显示是对的），不会更新 KeyMapping.MAP 反查表，
            // 不重建的话按旧键仍然会触发。原版按键设置界面保存时也是这么做的。
            KeyMapping.resetMapping();
        }

        if (this.minecraft != null) Compat.get().setScreen(this.minecraft, this.parent);
        this.parent.refreshAll();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
