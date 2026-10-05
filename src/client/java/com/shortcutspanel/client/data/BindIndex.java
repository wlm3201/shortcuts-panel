package com.shortcutspanel.client.data;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 从 {@code Minecraft.options.keyMappings} 构建的一次性索引，包含原版以及
 * 其它 mod 注册的按键绑定。
 */
public final class BindIndex {

    /** 本 mod 自己的按键不参与搜索，避免自触发 */
    private static final String OWN_PREFIX = "key.shortcuts_panel.";

    private final List<BindEntry> entries;
    private final Map<String, BindEntry> byId = new HashMap<>();

    /** 关掉之后，MaLiLib 系的条目在搜索、历史、收藏里都不出现 */
    private boolean includeMalilib = true;
    /** 被排除的条目 id；关掉"显示已排除"后它们从结果里消失 */
    private java.util.Set<String> excluded = java.util.Set.of();
    private boolean includeExcluded;

    private BindIndex(List<BindEntry> entries) {
        this.entries = entries;
        for (BindEntry entry : entries) this.byId.put(entry.id(), entry);
    }

    public static BindIndex build() {
        var options = Minecraft.getInstance().options;

        List<BindEntry> entries = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (KeyMapping mapping : options.keyMappings) {
            String name = mapping.getName();
            if (name.startsWith(OWN_PREFIX)) continue;
            if (!seen.add(name)) continue;
            entries.add(new BindEntry(mapping));
        }

        // MaLiLib 系的快捷键（含 Tweakeroo / Litematica / MiniHUD / ItemScroller）
        for (BindEntry entry : MalilibBridge.collect()) {
            if (seen.add(entry.id())) entries.add(entry);
        }

        return new BindIndex(entries);
    }

    public BindEntry byId(String id) {
        return this.byId.get(id);
    }

    public void includeMalilib(boolean includeMalilib) {
        this.includeMalilib = includeMalilib;
    }

    /** 传入排除列表本身（引用，后续改动会跟着生效） */
    public void excluded(java.util.Collection<String> ids, boolean includeExcluded) {
        this.excluded = ids == null ? java.util.Set.of() : new java.util.HashSet<>(ids);
        this.includeExcluded = includeExcluded;
    }

    public boolean isExcluded(BindEntry entry) {
        return this.excluded.contains(entry.id());
    }

    private boolean visible(BindEntry entry) {
        if (!this.includeMalilib && entry.isMalilib()) return false;
        if (!this.includeExcluded && this.excluded.contains(entry.id())) return false;
        return true;
    }

    /** 判断某条目是否命中搜索词（空词视为全部命中） */
    public boolean matches(BindEntry entry, String query) {
        if (!this.visible(entry)) return false;

        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) return true;
        return entry.score(normalized) > 0;
    }

    /**
     * @param query        搜索词（大小写不敏感，可为 null / 空）
     * @param includeBound 是否包含已经绑定了按键的条目
     */
    public List<BindEntry> search(String query, boolean includeBound) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        boolean filtering = !normalized.isEmpty();

        List<Scored> scored = new ArrayList<>();
        for (int i = 0; i < this.entries.size(); i++) {
            BindEntry entry = this.entries.get(i);
            if (!this.visible(entry)) continue;
            if (!includeBound && entry.bound()) continue;

            int score = filtering ? entry.score(normalized) : 1;
            if (score > 0) scored.add(new Scored(entry, score, i));
        }

        if (filtering) {
            scored.sort(Comparator.<Scored>comparingInt(value -> -value.score)
                    .thenComparingInt(value -> value.order));
        }

        List<BindEntry> result = new ArrayList<>(scored.size());
        for (Scored value : scored) {
            result.add(value.entry);
        }
        return result;
    }

    private record Scored(BindEntry entry, int score, int order) {}
}
