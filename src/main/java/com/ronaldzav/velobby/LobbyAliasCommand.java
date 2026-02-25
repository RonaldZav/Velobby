package com.ronaldzav.velobby;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class LobbyAliasCommand implements SimpleCommand {

    private final Velobby plugin;

    public LobbyAliasCommand(Velobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        com.velocitypowered.api.command.CommandSource source = invocation.source();

        if (!(source instanceof Player)) {
            source.sendMessage(plugin.getLangManager().getMessage("only_players"));
            return;
        }

        Player player = (Player) source;
        
        Optional<RegisteredServer> currentServer = player.getCurrentServer().map(s -> s.getServer());
        if (currentServer.isPresent()) {
            String serverName = currentServer.get().getServerInfo().getName();
            if (plugin.getConfigManager().isServerBlocked(serverName)) {
                source.sendMessage(plugin.getLangManager().getMessage("command_blocked"));
                return;
            }
            
            // Check if already in a lobby
            if (plugin.getConfigManager().getLobbies().contains(serverName)) {
                source.sendMessage(plugin.getLangManager().getMessage("already_in_lobby"));
                return;
            }
        }

        sendPlayerToLobby(player);
    }

    private void sendPlayerToLobby(Player player) {
        List<String> lobbies = plugin.getConfigManager().getLobbies();
        if (lobbies.isEmpty()) {
            player.sendMessage(plugin.getLangManager().getMessage("no_lobbies"));
            return;
        }

        String mode = plugin.getConfigManager().getConnectionMode();
        RegisteredServer targetServer = null;

        if (mode.equalsIgnoreCase("random")) {
            String randomLobby = lobbies.get(new Random().nextInt(lobbies.size()));
            Optional<RegisteredServer> s = plugin.getServer().getServer(randomLobby);
            if (s.isPresent()) targetServer = s.get();
        } else if (mode.equalsIgnoreCase("balance")) {
            RegisteredServer bestServer = null;
            int minPlayers = Integer.MAX_VALUE;
            for (String lobbyName : lobbies) {
                Optional<RegisteredServer> server = plugin.getServer().getServer(lobbyName);
                if (server.isPresent()) {
                    int playerCount = server.get().getPlayersConnected().size();
                    if (playerCount < minPlayers) {
                        minPlayers = playerCount;
                        bestServer = server.get();
                    }
                }
            }
            targetServer = bestServer;
        } else if (mode.equalsIgnoreCase("order")) {
             for (String lobbyName : lobbies) {
                Optional<RegisteredServer> server = plugin.getServer().getServer(lobbyName);
                if (server.isPresent()) {
                    targetServer = server.get();
                    break;
                }
            }
        }

        if (targetServer != null) {
            player.createConnectionRequest(targetServer).connect().thenAccept(result -> {
                if (result.isSuccessful()) {
                    Component titleText = plugin.getLangManager().getMessage("lobby_connect_title");
                    Component subtitleText = plugin.getLangManager().getMessage("lobby_connect_subtitle");
                    
                    Title title = Title.title(
                        titleText,
                        subtitleText,
                        Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000))
                    );
                    player.showTitle(title);
                }
            });
        } else {
            player.sendMessage(plugin.getLangManager().getMessage("no_suitable_lobby"));
        }
    }
}
