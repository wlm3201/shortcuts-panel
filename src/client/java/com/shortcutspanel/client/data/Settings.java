package com.shortcutspanel.client.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** 面板自身的一些开关，落盘到 {@code config/shortcuts-panel/config.json} */
public final class Settings {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("shortcuts-panel/config.json");

    private boolean showBound = true;
    private boolean showMalilib = true;
    private boolean showExcluded = false;

    private Settings() {}

    public static Settings load() {
        var settings = new Settings();

        if (Files.isRegularFile(PATH)) {
            try (var reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json != null && json.has("showBound")) {
                    settings.showBound = json.get("showBound").getAsBoolean();
                }
                if (json != null && json.has("showMalilib")) {
                    settings.showMalilib = json.get("showMalilib").getAsBoolean();
                }
                if (json != null && json.has("showExcluded")) {
                    settings.showExcluded = json.get("showExcluded").getAsBoolean();
                }
            } catch (IOException | RuntimeException error) {
                // 读不出来就用默认值
            }
        }

        return settings;
    }

    public void save() {
        var json = new JsonObject();
        json.addProperty("showBound", this.showBound);
        json.addProperty("showMalilib", this.showMalilib);
        json.addProperty("showExcluded", this.showExcluded);

        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(json), StandardCharsets.UTF_8);
        } catch (IOException error) {
            // 忽略
        }
    }

    /** 「已绑定：显示 / 隐藏」开关 */
    public boolean showBound() {
        return this.showBound;
    }

    public void showBound(boolean showBound) {
        this.showBound = showBound;
    }

    /** 是否把 MaLiLib 系（malilib / tweakeroo / litematica / minihud / itemscroller）的快捷键列出来 */
    public boolean showMalilib() {
        return this.showMalilib;
    }

    public void showMalilib(boolean showMalilib) {
        this.showMalilib = showMalilib;
    }

    /** 是否把已排除的条目也列出来（列出来时是灰的、带删除线，方便再右键放回来） */
    public boolean showExcluded() {
        return this.showExcluded;
    }

    public void showExcluded(boolean showExcluded) {
        this.showExcluded = showExcluded;
    }
}
