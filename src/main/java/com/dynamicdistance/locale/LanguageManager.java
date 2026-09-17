package com.dynamicdistance.locale;

import com.dynamicdistance.DynamicDistance;
import com.dynamicdistance.util.ChatUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LanguageManager {

    private final DynamicDistance plugin;
    private final Map<String, String> messages = new HashMap<>();

    private static final List<String> SUPPORTED_LANGS = Arrays.asList(
            "en", "tr", "de", "es", "ru", "zh", "ja", "az", "fr", "ar",
            "nl", "id", "hy", "it", "gd", "sv", "ky", "ko", "hu", "cs",
            "el", "fa", "pl", "ro", "vi", "pt", "th", "uk"
    );

    public LanguageManager(DynamicDistance plugin) {
        this.plugin = plugin;
        saveDefaultLanguages();
    }

    public void loadLanguage(String langCode) {
        messages.clear();

        if (langCode == null || !SUPPORTED_LANGS.contains(langCode.toLowerCase())) {
            langCode = "tr";
        }

        langCode = langCode.toLowerCase();

        File langFolder = new File(plugin.getDataFolder(), "langs");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        ensureFile(langFolder, langCode);
        ensureFile(langFolder, "tr");
        ensureFile(langFolder, "en");

        File langFile = new File(langFolder, langCode + ".yml");
        FileConfiguration diskConfig = YamlConfiguration.loadConfiguration(langFile);

        YamlConfiguration jarLangConfig = loadJarConfig("langs/" + langCode + ".yml");
        YamlConfiguration jarTrConfig = loadJarConfig("langs/tr.yml");
        YamlConfiguration jarEnConfig = loadJarConfig("langs/en.yml");

        boolean needSave = false;

        if (jarLangConfig != null) {
            for (String key : jarLangConfig.getKeys(false)) {
                if (!diskConfig.contains(key)) {
                    diskConfig.set(key, jarLangConfig.get(key));
                    needSave = true;
                }
            }
        }

        if (needSave) {
            try {
                diskConfig.save(langFile);
            } catch (Throwable ignored) {
            }
        }

        for (String key : diskConfig.getKeys(false)) {
            messages.put(key, diskConfig.getString(key));
        }

        if (jarLangConfig != null) {
            for (String key : jarLangConfig.getKeys(false)) {
                messages.putIfAbsent(key, jarLangConfig.getString(key));
            }
        }

        if (jarTrConfig != null) {
            for (String key : jarTrConfig.getKeys(false)) {
                messages.putIfAbsent(key, jarTrConfig.getString(key));
            }
        }

        if (jarEnConfig != null) {
            for (String key : jarEnConfig.getKeys(false)) {
                messages.putIfAbsent(key, jarEnConfig.getString(key));
            }
        }
    }

    private YamlConfiguration loadJarConfig(String resourcePath) {
        try {
            InputStream is = plugin.getResource(resourcePath);
            if (is != null) {
                try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    return YamlConfiguration.loadConfiguration(reader);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void ensureFile(File langFolder, String langCode) {
        File file = new File(langFolder, langCode + ".yml");
        if (!file.exists()) {
            try {
                plugin.saveResource("langs/" + langCode + ".yml", false);
            } catch (Throwable ignored) {
            }
        }
    }

    private void saveDefaultLanguages() {
        File langFolder = new File(plugin.getDataFolder(), "langs");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        for (String lang : SUPPORTED_LANGS) {
            ensureFile(langFolder, lang);
        }
    }

    public String getMessage(String key) {
        String msg = messages.getOrDefault(key, "&c[Missing key: " + key + "]");
        String prefix = messages.getOrDefault("prefix", "&#00E5FF&l✦ &#00C6FF&lDynamic &#007CFF&lDistance &8» &r");
        return ChatUtil.color(msg.replace("{prefix}", prefix));
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        String msg = messages.getOrDefault(key, "&c[Missing key: " + key + "]");
        String prefix = messages.getOrDefault("prefix", "&#00E5FF&l✦ &#00C6FF&lDynamic &#007CFF&lDistance &8» &r");
        msg = msg.replace("{prefix}", prefix);

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
                }
            }
        }

        return ChatUtil.color(msg);
    }
}