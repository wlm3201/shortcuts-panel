package com.shortcutspanel.client.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * MaLiLib 快捷键的桥接层（全部走反射，不引入编译期依赖）。
 *
 * <p>MaLiLib 及其衍生 mod（Tweakeroo / Litematica / MiniHUD / ItemScroller）的快捷键
 * 不是原版 {@code KeyMapping}，而是自己的 {@code IHotkey} + {@code IKeybind}，
 * 并且都注册到同一个全局管理器，所以适配一次就全都收录了。
 *
 * <p>没有装 MaLiLib 时 {@link #isAvailable()} 返回 false，一切照旧。
 * 反射拿不到（版本 API 变了）也只是收录不到，不影响原版绑定。
 */
public final class MalilibBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger("shortcuts-panel");

    private static final String INPUT_EVENT_HANDLER = "fi.dy.masa.malilib.event.InputEventHandler";
    private static final String KEY_ACTION = "fi.dy.masa.malilib.hotkeys.KeyAction";

    private static boolean checked;
    private static boolean available;

    private static Method getKeybindManager;
    private static Method getKeybindCategories;
    private static Method categoryModName;
    private static Method categoryName;
    private static Method categoryHotkeys;
    private static Method hotkeyName;
    private static Method hotkeyTranslatedName;
    private static Method hotkeyKeybind;
    private static Method keybindStringValue;
    private static Method keybindSettings;
    private static Method settingsActivateOn;
    /**
     * 回调方法。必须在 <b>public 接口</b> {@code IHotkeyCallback} 上取，不能在回调对象的
     * 实现类上取：很多 mod 的回调是 private 内部类、private record 甚至是方法引用生成的
     * lambda（隐藏类），用实现类的 Class 反射会被 JDK 访问检查拒绝，
     * 表现为"点了没反应"（MiniHUD 渲染器开关、Litematica 通用等）。
     */
    private static Method callbackOnKeyAction;
    /** TweakerMore 这类不设翻译前缀的 mod，真正的显示名只在这里 */
    private static Method configDisplayName;

    private static Object pressAction;
    private static Object releaseAction;

    private MalilibBridge() {}

    public static boolean isAvailable() {
        if (checked) return available;
        checked = true;

        try {
            var handler = Class.forName(INPUT_EVENT_HANDLER);
            getKeybindManager = handler.getMethod("getKeybindManager");

            var managerInterface = Class.forName("fi.dy.masa.malilib.hotkeys.IKeybindManager");
            getKeybindCategories = managerInterface.getMethod("getKeybindCategories");

            var category = Class.forName("fi.dy.masa.malilib.hotkeys.KeybindCategory");
            categoryModName = category.getMethod("getModName");
            categoryName = category.getMethod("getCategory");
            categoryHotkeys = category.getMethod("getHotkeys");

            var hotkey = Class.forName("fi.dy.masa.malilib.hotkeys.IHotkey");
            hotkeyName = hotkey.getMethod("getName");
            hotkeyTranslatedName = hotkey.getMethod("getTranslatedName");
            hotkeyKeybind = hotkey.getMethod("getKeybind");

            var keybind = Class.forName("fi.dy.masa.malilib.hotkeys.IKeybind");
            keybindStringValue = keybind.getMethod("getStringValue");

            // 只在具体实现类 KeybindMulti 上，接口没有
            keybindSettings = keybind.getMethod("getSettings");
            var settingsClass = Class.forName("fi.dy.masa.malilib.hotkeys.KeybindSettings");
            settingsActivateOn = settingsClass.getMethod("getActivateOn");

            var callbackInterface = Class.forName("fi.dy.masa.malilib.hotkeys.IHotkeyCallback");
            callbackOnKeyAction = callbackInterface.getMethod("onKeyAction",
                    Class.forName(KEY_ACTION), Class.forName("fi.dy.masa.malilib.hotkeys.IKeybind"));

            try {
                configDisplayName = hotkey.getMethod("getConfigGuiDisplayName");
            } catch (Throwable ignored) {
                configDisplayName = null;
            }

            var actionEnum = Class.forName(KEY_ACTION);
            for (Object constant : actionEnum.getEnumConstants()) {
                switch (((Enum<?>) constant).name()) {
                    case "PRESS" -> pressAction = constant;
                    case "RELEASE" -> releaseAction = constant;
                    default -> {}
                }
            }

            available = pressAction != null && releaseAction != null;
        } catch (Throwable ignored) {
            available = false;
        }

        return available;
    }

    /** 收集 MaLiLib 系的全部快捷键 */
    public static List<BindEntry> collect() {
        if (!isAvailable()) return List.of();

        List<BindEntry> out = new ArrayList<>();

        try {
            Object manager = getKeybindManager.invoke(null);
            @SuppressWarnings("unchecked")
            List<Object> categories = (List<Object>) getKeybindCategories.invoke(manager);

            for (Object category : categories) {
                String mod = String.valueOf(categoryModName.invoke(category));
                String catName = String.valueOf(categoryName.invoke(category));

                @SuppressWarnings("unchecked")
                List<Object> hotkeys = (List<Object>) categoryHotkeys.invoke(category);

                for (Object hotkey : hotkeys) {
                    String rawName = String.valueOf(hotkeyName.invoke(hotkey));
                    String translated = String.valueOf(hotkeyTranslatedName.invoke(hotkey));

                    // TweakerMore 这类不设翻译前缀的 mod，getTranslatedName() 会原样返回
                    // 代码名（mainRenderingToggle），真正的显示名要靠 getConfigGuiDisplayName()
                    String displayName = resolveDisplayName(hotkey);
                    if (displayName != null) translated = displayName;

                    // 没翻译时 getTranslatedName() 要么原样返回翻译键（形如 xxx.hotkeys.yyy），
                    // 要么就是代码名本身（如 mainRenderingToggle），两种情况都要美化一下
                    String name = isUntranslated(translated) || translated.equals(rawName)
                            ? prettify(lastSegment(rawName))
                            : translated;

                    Object keybind = hotkeyKeybind.invoke(hotkey);
                    String keys = keybind == null ? "" : String.valueOf(keybindStringValue.invoke(keybind));
                    if ("null".equals(keys)) keys = "";

                    // id 要稳定且唯一，收藏和历史都靠它
                    String id = "malilib:" + mod + ":" + rawName;
                    String cleaned = cleanCategory(catName);
                    // 类别名去后缀后为空，或者和 mod 名是同一个（TweakerMore / Tweaker More
                    // 这种被拆驼峰后看起来重复），就只显示 mod 名
                    String categoryText = cleaned.isEmpty() || sameName(cleaned, mod)
                            ? mod
                            : mod + " \u00b7 " + cleaned;

                    out.add(new BindEntry(hotkey, id, name, categoryText, keys));
                }
            }
        } catch (Throwable ignored) {
            // 拿不到就当没有
        }

        LOGGER.info("MaLiLib bridge: collected {} hotkeys", out.size());
        for (int i = 0; i < Math.min(out.size(), 6); i++) {
            var entry = out.get(i);
            LOGGER.info("  [{}] '{}' | {} | {}", i, entry.name(), entry.category(), entry.keyText());
        }
        return out;
    }

    /** 没翻译出来的名字会原样返回翻译键（带点），看起来就是一坨 key */
    private static boolean isUntranslated(String text) {
        return text == null || text.isBlank() || text.contains(".");
    }

    private static String lastSegment(String key) {
        int dot = key == null ? -1 : key.lastIndexOf('.');
        return dot < 0 ? String.valueOf(key) : key.substring(dot + 1);
    }

    /**
     * 类别名清理：去掉"快捷键 / hotkeys"这类后缀，未翻译的取最后一段。
     * 例：{@code 禁用切换快捷键 -> 禁用切换}、{@code tweakeroo.hotkeys.category.generic_config_hotkeys -> Generic Config}
     *
     * <p>只有<em>没翻译</em>的键才走 {@link #prettify}：已经翻译好的名字是人工写的
     * （{@code TweakerMore}），拆驼峰会变成 {@code Tweaker More}，看着像重复。
     */
    private static String cleanCategory(String category) {
        boolean translated = !isUntranslated(category);
        if (!translated) category = lastSegment(category);
        category = category.trim();

        for (String suffix : new String[]{"\u5feb\u6377\u952e", "hotkeys", "Hotkeys", "hotkey", "Hotkey"}) {
            if (category.endsWith(suffix) && category.length() >= suffix.length()) {
                category = category.substring(0, category.length() - suffix.length());
                break;
            }
        }

        category = category.trim();
        return translated ? category : prettify(category);
    }

    /** 归一化后比较，用来判断类别名和 mod 名其实是一回事 */
    private static boolean sameName(String a, String b) {
        return normalize(a).equals(normalize(b));
    }

    private static String normalize(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\s\u00b7._-]", "").toLowerCase(Locale.ROOT);
    }

    /** 各 mod 自己的显示名（TweakerMore 靠它才能显示中文），拿不到返回 null */
    private static String resolveDisplayName(Object hotkey) {
        if (configDisplayName == null) return null;

        try {
            Object result = configDisplayName.invoke(hotkey);
            String text = result == null ? null : result.toString();
            return isUntranslated(text) ? null : text;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** 把 {@code entityDataSync} / {@code generic_config} 变成 {@code Entity Data Sync} */
    private static String prettify(String raw) {
        if (raw == null || raw.isEmpty()) return "";

        StringBuilder out = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : raw.toCharArray()) {
            if (c == '_' || c == '-' || c == ' ') {
                if (out.length() > 0 && out.charAt(out.length() - 1) != ' ') out.append(' ');
                capitalizeNext = true;
            } else if (Character.isUpperCase(c) && out.length() > 0
                    && out.charAt(out.length() - 1) != ' ') {
                out.append(' ').append(c);
                capitalizeNext = false;
            } else if (capitalizeNext) {
                out.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                out.append(c);
            }
        }

        return out.toString().trim();
    }

    /**
     * 直接调用该快捷键自己的回调。
     * 不走 malilib 的按键分派，这样只会触发选中的这一个（同一组键上其它的不被误触）。
     */
    public static boolean trigger(Object hotkey) {
        if (!isAvailable() || hotkey == null) return false;

        try {
            Object keybind = hotkeyKeybind.invoke(hotkey);
            if (keybind == null) return false;

            var callback = keybind.getClass().getMethod("getCallback");
            Object callbackInstance = callback.invoke(keybind);
            if (callbackInstance == null) return false;

            Object settings = keybindSettings.invoke(keybind);
            Object activateOn = settingsActivateOn.invoke(settings);
            String mode = activateOn instanceof Enum<?> value ? value.name() : "PRESS";

            var action = callbackOnKeyAction;

            // 只在回调关心的那半个边沿调用：PRESS 就只发按下，RELEASE 只发松开，BOTH 两个都发
            if (!"RELEASE".equals(mode)) action.invoke(callbackInstance, pressAction, keybind);
            if (!"PRESS".equals(mode)) action.invoke(callbackInstance, releaseAction, keybind);

            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
