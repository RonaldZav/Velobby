package com.ronaldzav.velobby;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.regex.Pattern;

public class ConfigManager {

    private final Velobby plugin;
    private final Path dataDirectory;
    private Map<String, Object> config;
    private File configFile;

    public ConfigManager(Velobby plugin, Path dataDirectory) {
        this.plugin = plugin;
        this.dataDirectory = dataDirectory;
        loadConfig();
    }

    public synchronized void loadConfig() {
        if (!Files.exists(dataDirectory)) {
            try {
                Files.createDirectories(dataDirectory);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        configFile = new File(dataDirectory.toFile(), "config.yml");

        if (!configFile.exists()) {
            try (InputStream in = plugin.getClass().getResourceAsStream("/config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                } else {
                    configFile.createNewFile();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (FileInputStream inputStream = new FileInputStream(configFile)) {
            Yaml yaml = new Yaml();
            config = yaml.load(inputStream);
            if (config == null) {
                config = new java.util.HashMap<>();
            }
        } catch (IOException e) {
            e.printStackTrace();
            config = new java.util.HashMap<>();
        }
    }

    public synchronized void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile)) {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(options);
            yaml.dump(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized List<String> getLobbies() {
        if (config.containsKey("lobbies")) {
            Object lobbiesObj = config.get("lobbies");
            if (lobbiesObj instanceof List) {
                return new ArrayList<>((List<String>) lobbiesObj);
            }
        }
        return new ArrayList<>();
    }

    public synchronized void addLobby(String serverName) {
        List<String> lobbies = getLobbies();
        if (!lobbies.contains(serverName)) {
            lobbies.add(serverName);
            config.put("lobbies", lobbies);
            saveConfig();
        }
    }

    public synchronized void removeLobby(String serverName) {
        List<String> lobbies = getLobbies();
        if (lobbies.contains(serverName)) {
            lobbies.remove(serverName);
            config.put("lobbies", lobbies);
            saveConfig();
        }
    }

    public synchronized String getConnectionMode() {
        return (String) config.getOrDefault("connection-mode", "random");
    }

    public synchronized List<String> getBlockedServers() {
        if (config.containsKey("blocked-servers")) {
            Object blockedObj = config.get("blocked-servers");
            if (blockedObj instanceof List) {
                return new ArrayList<>((List<String>) blockedObj);
            }
        }
        return new ArrayList<>();
    }

    public boolean isServerBlocked(String serverName) {
        List<String> blockedServers = getBlockedServers();
        for (String blocked : blockedServers) {
            // Convert wildcard pattern to regex
            // Escape special regex characters except '*'
            String regex = "^" + Pattern.quote(blocked).replace("*", "\\E.*\\Q") + "$";
            if (serverName.matches(regex)) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean isCheckUpdates() {
        return (boolean) config.getOrDefault("check-updates", true);
    }

    public synchronized boolean isAutoReconnectEnabled() {
        return (boolean) config.getOrDefault("auto-reconnect", true);
    }

    public synchronized List<String> getAliases() {
        if (config.containsKey("aliases")) {
            Object aliasesObj = config.get("aliases");
            if (aliasesObj instanceof List) {
                return new ArrayList<>((List<String>) aliasesObj);
            }
        }
        return new ArrayList<>();
    }

    public synchronized String getLanguage() {
        return (String) config.getOrDefault("language", "en");
    }
}
