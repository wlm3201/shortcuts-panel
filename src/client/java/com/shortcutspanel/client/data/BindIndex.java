package com.shortcutspanel.client.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class BindIndex {

    private static final String OWN_PREFIX = "key.shortcuts_panel.";

    private final List<BindEntry> entries;
    private final Map<String, BindEntry> byId = new HashMap<>();

    private boolean includeMalilib = true;

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

    public boolean matches(BindEntry entry, String query) {
        if (!this.visible(entry)) return false;

        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) return true;
        return entry.score(normalized) > 0;
    }

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
            scored.sort(
                    Comparator.<Scored>comparingInt(value -> -value.score)
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
