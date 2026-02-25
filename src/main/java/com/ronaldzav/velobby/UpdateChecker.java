package com.ronaldzav.velobby;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class UpdateChecker {

    private final Velobby plugin;
    private final String currentVersion;
    private String latestVersion;
    private boolean updateAvailable = false;

    public UpdateChecker(Velobby plugin, String currentVersion) {
        this.plugin = plugin;
        this.currentVersion = currentVersion;
    }

    public void check() {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL("https://clients.ronaldzav.com/api/public/v1/product/com.ronaldzav.velobby");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() == 200) {
                    InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    latestVersion = json.get("version").getAsString();

                    if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                        updateAvailable = true;
                        plugin.getLogger().warn("A new version of Velobby is available: " + latestVersion);
                        plugin.getLogger().warn("You are currently using version: " + currentVersion);
                    } else {
                        plugin.getLogger().info("You are using the latest version of Velobby.");
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().error("Failed to check for updates: " + e.getMessage());
            }
        });
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}
