package com.ronaldzav.velobby;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import java.io.FileInputStream;
import java.util.HashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class LangManager {

    private final Velobby plugin;
    private final Path dataDirectory;
    private Map<String, String> messages;
    private File langFile;
    private final MiniMessage miniMessage;

    public LangManager(Velobby plugin, Path dataDirectory) {
        this.plugin = plugin;
        this.dataDirectory = dataDirectory;
        this.miniMessage = MiniMessage.miniMessage();
        loadLang();
    }

    public void loadLang() {
        if (!Files.exists(dataDirectory)) {
            try {
                Files.createDirectories(dataDirectory);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File langFolder = new File(dataDirectory.toFile(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        String lang = plugin.getConfigManager().getLanguage();
        String fileName = lang + ".yml";
        langFile = new File(langFolder, fileName);

        // Copy default language files if they don't exist
        copyDefaultLang("en.yml", langFolder);
        copyDefaultLang("es.yml", langFolder);

        if (!langFile.exists()) {
            // Fallback to en.yml if selected language file doesn't exist
            langFile = new File(langFolder, "en.yml");
            plugin.getLogger().warn("Language file " + fileName + " not found. Falling back to en.yml");
        }

        try (FileInputStream inputStream = new FileInputStream(langFile)) {
            Yaml yaml = new Yaml();
            messages = yaml.load(inputStream);
            if (messages == null) {
                messages = new HashMap<>();
            }
        } catch (IOException e) {
            e.printStackTrace();
            messages = new HashMap<>();
        }
    }

    private void copyDefaultLang(String fileName, File langFolder) {
        File file = new File(langFolder, fileName);
        if (!file.exists()) {
            try (InputStream in = plugin.getClass().getResourceAsStream("/lang/" + fileName)) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getRawMessage(String key) {
        return messages.getOrDefault(key, key);
    }

    public Component getMessage(String key) {
        String raw = getRawMessage(key);
        // Support both MiniMessage (<red>, <#hex>) and Legacy (&c, &4)
        // First, convert legacy '&' to section symbol '§' and serialize to Component
        // But MiniMessage is more powerful.
        // Let's try to detect if it uses MiniMessage tags.
        
        if (raw.contains("&")) {
             return LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
        }

        return miniMessage.deserialize(raw);
    }
}
