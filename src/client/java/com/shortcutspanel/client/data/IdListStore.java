package com.shortcutspanel.client.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

public final class IdListStore {

    private static final Gson GSON = new GsonBuilder().create();

    private final Path path;
    private final List<String> ids = new ArrayList<>();

    private IdListStore(Path path) {
        this.path = path;
    }

    public static IdListStore load(String fileName) {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("shortcutspanel/" + fileName);
        var store = new IdListStore(path);

        if (Files.isRegularFile(path)) {
            try {
                String content = Files.readString(path, StandardCharsets.UTF_8);
                if (content.startsWith("\uFEFF")) content = content.substring(1);

                String[] loaded = GSON.fromJson(content, String[].class);
                if (loaded != null) {
                    for (String id : loaded) {
                        if (id != null && !id.isBlank() && !store.ids.contains(id))
                            store.ids.add(id);
                    }
                }
            } catch (IOException | RuntimeException error) {
            }
        }

        return store;
    }

    public void save() {
        try {
            Files.createDirectories(this.path.getParent());
            Files.writeString(this.path, GSON.toJson(this.ids), StandardCharsets.UTF_8);
        } catch (IOException error) {
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

    public void pushFront(String id) {
        this.ids.remove(id);
        this.ids.add(0, id);
    }

    public boolean remove(String id) {
        return this.ids.remove(id);
    }

    public void move(int from, int to) {
        if (from < 0 || from >= this.ids.size()) return;
        if (to < 0 || to >= this.ids.size()) return;
        String id = this.ids.remove(from);
        this.ids.add(to, id);
    }

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
