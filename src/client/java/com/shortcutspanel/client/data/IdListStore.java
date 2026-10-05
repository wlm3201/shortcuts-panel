package com.shortcutspanel.client.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 一串按顺序保存的 {@code KeyMapping.getName()}（如 {@code key.inventory}），
 * 与游戏语言无关。收藏栏与历史栏都用它落盘到 {@code config/shortcuts-panel/} 下。
 */
public final class IdListStore {

    private static final Gson GSON = new GsonBuilder().create();

    private final Path path;
    private final List<String> ids = new ArrayList<>();

    private IdListStore(Path path) {
        this.path = path;
    }

    public static IdListStore load(String fileName) {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("shortcuts-panel/" + fileName);
        var store = new IdListStore(path);

        if (Files.isRegularFile(path)) {
            try {
                // 手动剥掉 BOM：Windows 记事本保存的 UTF-8 带 BOM，Gson 会直接解析失败
                String content = Files.readString(path, StandardCharsets.UTF_8);
                if (content.startsWith("\uFEFF")) content = content.substring(1);

                String[] loaded = GSON.fromJson(content, String[].class);
                if (loaded != null) {
                    for (String id : loaded) {
                        if (id != null && !id.isBlank() && !store.ids.contains(id)) store.ids.add(id);
                    }
                }
            } catch (IOException | RuntimeException error) {
                // 读不出来就当空的，下次保存时会覆盖
            }
        }

        return store;
    }

    public void save() {
        try {
            Files.createDirectories(this.path.getParent());
            Files.writeString(this.path, GSON.toJson(this.ids), StandardCharsets.UTF_8);
        } catch (IOException error) {
            // 丢数据不影响主流程
        }
    }

    public List<String> ids() {
        return this.ids;
    }

    public int size() {
        return this.ids.size();
    }

    public boolean contains(String id) {
        return this.ids.contains(id);
    }

    public boolean add(String id) {
        if (this.ids.contains(id)) return false;
        this.ids.add(id);
        return true;
    }

    /** 移到最前面（历史记录用） */
    public void pushFront(String id) {
        this.ids.remove(id);
        this.ids.add(0, id);
    }

    public boolean remove(String id) {
        return this.ids.remove(id);
    }

    /** 预留给后续的拖拽排序 */
    public void move(int from, int to) {
        if (from < 0 || from >= this.ids.size()) return;
        if (to < 0 || to >= this.ids.size()) return;
        String id = this.ids.remove(from);
        this.ids.add(to, id);
    }

    /**
     * 按给定顺序重排其中的一部分：{@code visible} 里的项按顺序放回原来的槽位，
     * 没列进来的项保持不动。收藏栏被搜索词过滤时拖拽排序必须走这条，
     * 否则可见顺序和落盘顺序不一致会排错。
     */
    public void reorder(List<String> visible) {
        var slots = new ArrayList<Integer>(visible.size());
        for (String id : visible) {
            int index = this.ids.indexOf(id);
            if (index >= 0) slots.add(index);
        }
        if (slots.size() != visible.size()) return;

        java.util.Collections.sort(slots);
        for (int i = 0; i < slots.size(); i++) {
            this.ids.set(slots.get(i), visible.get(i));
        }
    }

    public void trim(int maxSize) {
        while (this.ids.size() > maxSize) {
            this.ids.remove(this.ids.size() - 1);
        }
    }
}
