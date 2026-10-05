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
import net.minecraft.network.chat.Component;

/**
 * 捕获一次按键，把目标绑定项绑到该键上。
 *
 * <p>Esc 与原版按键设置界面保持一致：<strong>设为无</strong>（解除绑定），而不是取消本次操作。
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
        if (this.captured != null) {
            this.entry.mapping().setKey(this.captured);
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
