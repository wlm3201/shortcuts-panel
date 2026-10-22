package com.shortcutspanel.client.data;

import java.util.Locale;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

public final class BindEntry {

    private final KeyMapping mapping;

    private final Object hotkey;

    private final String id;
    private final String name;
    private final String category;

    private final String fixedKeyText;

    private final String searchName;
    private final String searchId;
    private final String searchCategory;

    BindEntry(KeyMapping mapping) {
        this(
                mapping,
                null,
                mapping.getName(),
                Component.translatable(mapping.getName()).getString(),
                categoryOf(mapping),
                null);
    }

    BindEntry(Object hotkey, String id, String name, String category, String keyText) {
        this(null, hotkey, id, name, category, keyText);
    }

    private BindEntry(
            KeyMapping mapping,
            Object hotkey,
            String id,
            String name,
            String category,
            String keyText) {
        this.mapping = mapping;
        this.hotkey = hotkey;
        this.id = id;
        this.name = name;
        this.category = category == null ? "" : category;
        this.fixedKeyText = keyText;

        this.searchName = this.name.toLowerCase(Locale.ROOT);
        this.searchId = this.id.toLowerCase(Locale.ROOT);
        this.searchCategory = this.category.toLowerCase(Locale.ROOT);
    }

    private static String categoryOf(KeyMapping mapping) {
        var category = mapping.getCategory();
        return category == null ? "" : category.label().getString();
    }

    public KeyMapping mapping() {
        return this.mapping;
    }

    public Object hotkey() {
        return this.hotkey;
    }

    public boolean isMalilib() {
        return this.hotkey != null;
    }

    public String id() {
        return this.id;
    }

    public String name() {
        return this.name;
    }

    public String category() {
        return this.category;
    }

    public String keyText() {
        if (this.fixedKeyText != null) {
            return this.fixedKeyText.isEmpty() ? "-" : this.fixedKeyText;
        }
        return this.mapping.isUnbound() ? "-" : this.mapping.getTranslatedKeyMessage().getString();
    }

    public boolean bound() {
        if (this.fixedKeyText != null) return !this.fixedKeyText.isEmpty();
        return !this.mapping.isUnbound();
    }

    public int score(String query) {
        String[] tokens = tokenize(query);
        if (tokens.length == 0) return 0;

        int total = 0;
        for (String token : tokens) {
            int best =
                    Math.max(
                            match(this.searchName, token) * 3,
                            Math.max(
                                    match(this.searchId, token) * 2,
                                    match(this.searchCategory, token)));
            if (best == 0) return 0;
            total += best;
        }
        return total;
    }

    private static String[] tokenize(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) return new String[0];
        return normalized.split("[\\s\\u3000]+");
    }

    private static int match(String text, String query) {
        if (text.isEmpty() || query.isEmpty()) return 0;

        int index = text.indexOf(query);
        if (index == 0) return text.equals(query) ? 120 : 100;
        if (index > 0) return 80 - Math.min(index, 30);
        return isSubsequence(text, query) ? 40 : 0;
    }

    private static boolean isSubsequence(String text, String query) {
        int cursor = 0;
        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            if (c == ' ') continue;
            cursor = text.indexOf(c, cursor);
            if (cursor < 0) return false;
            cursor++;
        }
        return true;
    }
}
