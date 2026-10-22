package com.shortcutspanel.client.data;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MalilibBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger("shortcutspanel");

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

    private static Method callbackOnKeyAction;

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

            keybindSettings = keybind.getMethod("getSettings");
            var settingsClass = Class.forName("fi.dy.masa.malilib.hotkeys.KeybindSettings");
            settingsActivateOn = settingsClass.getMethod("getActivateOn");

            var callbackInterface = Class.forName("fi.dy.masa.malilib.hotkeys.IHotkeyCallback");
            callbackOnKeyAction =
                    callbackInterface.getMethod(
                            "onKeyAction",
                            Class.forName(KEY_ACTION),
                            Class.forName("fi.dy.masa.malilib.hotkeys.IKeybind"));

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

                    String displayName = resolveDisplayName(hotkey);
                    if (displayName != null) translated = displayName;

                    String name =
                            isUntranslated(translated) || translated.equals(rawName)
                                    ? prettify(lastSegment(rawName))
                                    : translated;

                    Object keybind = hotkeyKeybind.invoke(hotkey);
                    String keys =
                            keybind == null
                                    ? ""
                                    : String.valueOf(keybindStringValue.invoke(keybind));
                    if ("null".equals(keys)) keys = "";

                    String id = "malilib:" + mod + ":" + rawName;
                    String cleaned = cleanCategory(catName);
                    String categoryText =
                            cleaned.isEmpty() || sameName(cleaned, mod)
                                    ? mod
                                    : mod + " \u00b7 " + cleaned;

                    out.add(new BindEntry(hotkey, id, name, categoryText, keys));
                }
            }
        } catch (Throwable ignored) {
        }

        LOGGER.info("MaLiLib bridge: collected {} hotkeys", out.size());
        for (int i = 0; i < Math.min(out.size(), 6); i++) {
            var entry = out.get(i);
            LOGGER.info(
                    "  [{}] '{}' | {} | {}", i, entry.name(), entry.category(), entry.keyText());
        }
        return out;
    }

    private static boolean isUntranslated(String text) {
        return text == null || text.isBlank() || text.contains(".");
    }

    private static String lastSegment(String key) {
        int dot = key == null ? -1 : key.lastIndexOf('.');
        return dot < 0 ? String.valueOf(key) : key.substring(dot + 1);
    }

    private static String cleanCategory(String category) {
        boolean translated = !isUntranslated(category);
        if (!translated) category = lastSegment(category);
        category = category.trim();

        for (String suffix :
                new String[] {"\u5feb\u6377\u952e", "hotkeys", "Hotkeys", "hotkey", "Hotkey"}) {
            if (category.endsWith(suffix) && category.length() >= suffix.length()) {
                category = category.substring(0, category.length() - suffix.length());
                break;
            }
        }

        category = category.trim();
        return translated ? category : prettify(category);
    }

    private static boolean sameName(String a, String b) {
        return normalize(a).equals(normalize(b));
    }

    private static String normalize(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\s\u00b7._-]", "").toLowerCase(Locale.ROOT);
    }

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

    private static String prettify(String raw) {
        if (raw == null || raw.isEmpty()) return "";

        StringBuilder out = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : raw.toCharArray()) {
            if (c == '_' || c == '-' || c == ' ') {
                if (out.length() > 0 && out.charAt(out.length() - 1) != ' ') out.append(' ');
                capitalizeNext = true;
            } else if (Character.isUpperCase(c)
                    && out.length() > 0
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

            if (!"RELEASE".equals(mode)) action.invoke(callbackInstance, pressAction, keybind);
            if (!"PRESS".equals(mode)) action.invoke(callbackInstance, releaseAction, keybind);

            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
