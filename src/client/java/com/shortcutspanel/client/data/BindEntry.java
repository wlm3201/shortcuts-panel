package com.shortcutspanel.client.data;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * 一条可被搜索的按键绑定记录。搜索关键字同时覆盖：翻译后的显示名、原始
 * 翻译键（如 {@code key.inventory}）以及翻译后的类别名。
 *
 * <p>来源有两种：原版 / 其它 mod 注册的 {@link KeyMapping}，以及 MaLiLib 系的
 * {@code IHotkey}（后者不走原版 KeyMapping，见 {@link MalilibBridge}）。
 */
public final class BindEntry {

    private final KeyMapping mapping;
    /** MaLiLib 系快捷键对象（{@code IHotkey}），原版条目为 null */
    private final Object hotkey;

    private final String id;
    private final String name;
    private final String category;
    /** MaLiLib 条目的按键文本（原版的是动态取，因为玩家可能随时改键） */
    private final String fixedKeyText;

    private final String searchName;
    private final String searchId;
    private final String searchCategory;

    BindEntry(KeyMapping mapping) {
        this(mapping, null, mapping.getName(), Component.translatable(mapping.getName()).getString(),
                categoryOf(mapping), null);
    }

    /** MaLiLib 系快捷键 */
    BindEntry(Object hotkey, String id, String name, String category, String keyText) {
        this(null, hotkey, id, name, category, keyText);
    }

    private BindEntry(KeyMapping mapping, Object hotkey, String id, String name, String category, String keyText) {
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

    /** 非 null 表示这是 MaLiLib 系的快捷键 */
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

    /** 当前绑定的按键文本，未绑定时为 {@code -} */
    public String keyText() {
        if (this.fixedKeyText != null) {
            return this.fixedKeyText.isEmpty() ? "-" : this.fixedKeyText;
        }
        return this.mapping.isUnbound() ? "-" : this.mapping.getTranslatedKeyMessage().getString();
    }

    /** 是否已经绑定了某个按键 */
    public boolean bound() {
        if (this.fixedKeyText != null) return !this.fixedKeyText.isEmpty();
        return !this.mapping.isUnbound();
    }

    /**
     * @return 0 表示不匹配，越大越靠前
     */
    public int score(String query) {
        int best = 0;
        best = Math.max(best, match(this.searchName, query) * 3);
        best = Math.max(best, match(this.searchId, query) * 2);
        best = Math.max(best, match(this.searchCategory, query));
        return best;
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
