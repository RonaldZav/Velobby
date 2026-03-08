package com.ronaldzav.velobby;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.title.Title;
import java.time.Duration;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ArrayList;

import java.nio.file.Path;

@Plugin(id = "velobby", name = "Velobby", version = "26.3", authors = {"RonaldZav"})
public class Velobby {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private ConfigManager configManager;
    private LangManager langManager;
    private UpdateChecker updateChecker;

    @Inject
    public Velobby(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Loading Velobby...");

        this.configManager = new ConfigManager(this, dataDirectory);
        this.langManager = new LangManager(this, dataDirectory);

        CommandManager commandManager = server.getCommandManager();
        
        // Register main command
        CommandMeta commandMeta = commandManager.metaBuilder("velobby")
                .plugin(this)
                .build();
        commandManager.register(commandMeta, new VelobbyCommand(this));
        
        // Register aliases
        List<String> aliases = configManager.getAliases();
        for (String alias : aliases) {
             CommandMeta aliasMeta = commandManager.metaBuilder(alias)
                .plugin(this)
                .build();
             commandManager.register(aliasMeta, new LobbyAliasCommand(this));
        }
        
        // Check for updates
        if (configManager.isCheckUpdates()) {
            this.updateChecker = new UpdateChecker(this, "26.3");
            this.updateChecker.check();
        }

        logger.info("Velobby loaded successfully!");
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        if (!configManager.isCheckUpdates()) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPermission("velobby.admin") && updateChecker != null && updateChecker.isUpdateAvailable()) {
            player.sendMessage(Component.text("A new version of Velobby is available: " + updateChecker.getLatestVersion(), NamedTextColor.RED));
            player.sendMessage(Component.text("You are currently using version: 26.3", NamedTextColor.RED));
            player.sendMessage(Component.text("Please update the plugin.", NamedTextColor.RED));
        }
    }

    @Subscribe
    public void onKickedFromServer(KickedFromServerEvent event) {
        if (!configManager.isAutoReconnectEnabled()) {
            return;
        }

        if (event.kickedDuringServerConnect()) {
            // If kicked during initial connection, we might not want to interfere or handle differently
            // But usually we want to send them to a lobby if they were trying to go somewhere else
            // However, if they were trying to connect to a lobby and failed, we should be careful to avoid loops.
        }

        RegisteredServer kickedFrom = event.getServer();
        String kickedFromName = kickedFrom.getServerInfo().getName();
        
        // Check for special lobbies first for reconnect
        RegisteredServer targetServer = null;
        Player player = event.getPlayer();

        if (configManager.isSpecialLobbiesDefaultEnabled()) {
            Map<String, String> specialLobbies = configManager.getSpecialLobbies();
            for (Map.Entry<String, String> entry : specialLobbies.entrySet()) {
                String lobbyName = entry.getKey();
                String permission = entry.getValue();
                
                if (lobbyName.equals("default")) continue; // Skip the default key
                if (lobbyName.equals(kickedFromName)) continue; // Don't reconnect to same server

                if (player.hasPermission(permission)) {
                    Optional<RegisteredServer> server = this.server.getServer(lobbyName);
                    if (server.isPresent()) {
                        targetServer = server.get();
                        break; // Found a special lobby
                    }
                }
            }
        }

        if (targetServer == null) {
            List<String> lobbies = configManager.getLobbies();
            String mode = configManager.getConnectionMode();

            List<String> availableLobbies = new ArrayList<>();
            for (String lobby : lobbies) {
                if (!lobby.equals(kickedFromName)) {
                    availableLobbies.add(lobby);
                }
            }
            
            if (!availableLobbies.isEmpty()) {
                if (mode.equalsIgnoreCase("random")) {
                    String randomLobby = availableLobbies.get(new Random().nextInt(availableLobbies.size()));
                    Optional<RegisteredServer> s = server.getServer(randomLobby);
                    if (s.isPresent()) {
                        targetServer = s.get();
                    }
                } else {
                    for (String lobbyName : availableLobbies) {
                        Optional<RegisteredServer> s = server.getServer(lobbyName);
                        if (s.isPresent()) {
                            targetServer = s.get();
                            break;
                        }
                    }
                }
            }
        }

        if (targetServer != null) {
            event.setResult(KickedFromServerEvent.RedirectPlayer.create(targetServer));
            
            Optional<Component> kickReason = event.getServerKickReason();
            Component reason = kickReason.orElse(Component.text("Unknown reason"));
            
            event.getPlayer().sendMessage(Component.text("You were kicked from " + kickedFromName + ": ", NamedTextColor.RED).append(reason));
            event.getPlayer().sendMessage(Component.text("Sending you to a lobby...", NamedTextColor.GREEN));
            
            // Send title
            Component titleText = langManager.getMessage("lobby_reconnect_title");
            Component subtitleText = langManager.getMessage("lobby_reconnect_subtitle");
            
            Title title = Title.title(
                titleText,
                subtitleText,
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000))
            );
            event.getPlayer().showTitle(title);
        }
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LangManager getLangManager() {
        return langManager;
    }
    
    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }
}
