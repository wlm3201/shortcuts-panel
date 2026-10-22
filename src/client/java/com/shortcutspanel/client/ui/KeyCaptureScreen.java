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
        this.captured = input.isEscape() ? InputConstants.UNKNOWN : InputConstants.getKey(input);
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        this.captured = InputConstants.Type.MOUSE.getOrCreate(event.button());
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        return true;
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (this.captured != null) {
            this.close();
            return;
        }

        var font = Minecraft.getInstance().font;
        context.fill(0, 0, this.width, this.height, Theme.CAPTURE_SCRIM);

        context.centeredText(font, this.title, this.width / 2, this.height / 2 - 16, Theme.ACCENT);
        context.centeredText(
                font,
                Component.translatable("shortcuts_panel.capture.hint"),
                this.width / 2,
                this.height / 2,
                Theme.TEXT);
    }

    private void close() {
        KeyMapping mapping = this.entry.mapping();
        if (this.captured != null && mapping != null) {
            mapping.setKey(this.captured);
            Minecraft.getInstance().options.save();
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
