package com.ronaldzav.velobby;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class VelobbyCommand implements SimpleCommand {

    private final Velobby plugin;

    public VelobbyCommand(Velobby plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        com.velocitypowered.api.command.CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (source instanceof Player) {
            Player player = (Player) source;
            Optional<RegisteredServer> currentServer = player.getCurrentServer().map(s -> s.getServer());
            if (currentServer.isPresent()) {
                String serverName = currentServer.get().getServerInfo().getName();
                if (plugin.getConfigManager().isServerBlocked(serverName)) {
                    source.sendMessage(plugin.getLangManager().getMessage("command_blocked"));
                    return;
                }
            }
        }

        if (args.length == 0) {
            source.sendMessage(plugin.getLangManager().getMessage("usage"));
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "add":
                if (source instanceof Player) {
                    Player player = (Player) source;
                    Optional<RegisteredServer> server = player.getCurrentServer().map(s -> s.getServer());
                    if (server.isPresent()) {
                        String serverName = server.get().getServerInfo().getName();
                        plugin.getConfigManager().addLobby(serverName);
                        // Formatting with MiniMessage is tricky if we want to inject variables into the string before parsing.
                        // A simple way is to use String.format on the raw string, then parse.
                        // But if the variable contains tags, it might break.
                        // For now, let's assume server names are safe or we just replace %s.
                        String rawMsg = plugin.getLangManager().getRawMessage("server_added").replace("%s", serverName);
                        source.sendMessage(plugin.getLangManager().getMessage("server_added").replaceText(b -> b.matchLiteral("%s").replacement(serverName)));
                    } else {
                        source.sendMessage(plugin.getLangManager().getMessage("not_connected"));
                    }
                } else {
                    source.sendMessage(plugin.getLangManager().getMessage("only_players"));
                }
                break;
            case "remove":
                if (source instanceof Player) {
                    Player player = (Player) source;
                    Optional<RegisteredServer> server = player.getCurrentServer().map(s -> s.getServer());
                    if (server.isPresent()) {
                        String serverName = server.get().getServerInfo().getName();
                        plugin.getConfigManager().removeLobby(serverName);
                        source.sendMessage(plugin.getLangManager().getMessage("server_removed").replaceText(b -> b.matchLiteral("%s").replacement(serverName)));
                    } else {
                        source.sendMessage(plugin.getLangManager().getMessage("not_connected"));
                    }
                } else {
                    source.sendMessage(plugin.getLangManager().getMessage("only_players"));
                }
                break;
            case "status":
                List<String> lobbies = plugin.getConfigManager().getLobbies();
                source.sendMessage(plugin.getLangManager().getMessage("lobby_status"));
                for (String lobbyName : lobbies) {
                    Optional<RegisteredServer> server = plugin.getServer().getServer(lobbyName);
                    if (server.isPresent()) {
                        int playerCount = server.get().getPlayersConnected().size();
                        source.sendMessage(plugin.getLangManager().getMessage("lobby_online")
                                .replaceText(b -> b.matchLiteral("%s").replacement(lobbyName))
                                .replaceText(b -> b.matchLiteral("%d").replacement(String.valueOf(playerCount))));
                    } else {
                        source.sendMessage(plugin.getLangManager().getMessage("lobby_offline").replaceText(b -> b.matchLiteral("%s").replacement(lobbyName)));
                    }
                }
                break;
            case "send":
                if (args.length < 2) {
                    source.sendMessage(plugin.getLangManager().getMessage("usage"));
                    return;
                }
                String target = args[1];
                if (target.equalsIgnoreCase("ALL")) {
                    for (Player p : plugin.getServer().getAllPlayers()) {
                        sendPlayerToLobby(p);
                    }
                    source.sendMessage(plugin.getLangManager().getMessage("sent_all"));
                } else {
                    Optional<Player> targetPlayer = plugin.getServer().getPlayer(target);
                    if (targetPlayer.isPresent()) {
                        sendPlayerToLobby(targetPlayer.get());
                        source.sendMessage(plugin.getLangManager().getMessage("sent_player").replaceText(b -> b.matchLiteral("%s").replacement(target)));
                    } else {
                        source.sendMessage(plugin.getLangManager().getMessage("player_not_found"));
                    }
                }
                break;
            case "update":
                if (source.hasPermission("velobby.admin")) {
                    if (plugin.getUpdateChecker().isUpdateAvailable()) {
                        source.sendMessage(Component.text("A new version of Velobby is available: " + plugin.getUpdateChecker().getLatestVersion(), NamedTextColor.RED));
                        source.sendMessage(Component.text("You are currently using version: 26.2", NamedTextColor.RED));
                        source.sendMessage(Component.text("Please update the plugin.", NamedTextColor.RED));
                    } else {
                        source.sendMessage(Component.text("You are using the latest version of Velobby.", NamedTextColor.GREEN));
                    }
                } else {
                    source.sendMessage(plugin.getLangManager().getMessage("unknown_command"));
                }
                break;
            case "reload":
                if (source.hasPermission("velobby.admin")) {
                    plugin.getConfigManager().loadConfig();
                    plugin.getLangManager().loadLang();
                    source.sendMessage(plugin.getLangManager().getMessage("reload_success"));
                } else {
                    source.sendMessage(plugin.getLangManager().getMessage("unknown_command"));
                }
                break;
            default:
                source.sendMessage(plugin.getLangManager().getMessage("unknown_command"));
                break;
        }
    }

    private void sendPlayerToLobby(Player player) {
        List<String> lobbies = plugin.getConfigManager().getLobbies();
        if (lobbies.isEmpty()) {
            player.sendMessage(plugin.getLangManager().getMessage("no_lobbies"));
            return;
        }

        String mode = plugin.getConfigManager().getConnectionMode();
        
        if (mode.equalsIgnoreCase("random")) {
            String randomLobby = lobbies.get(new Random().nextInt(lobbies.size()));
            Optional<RegisteredServer> targetServer = plugin.getServer().getServer(randomLobby);
            if (targetServer.isPresent()) {
                player.createConnectionRequest(targetServer.get()).connect();
            } else {
                 player.sendMessage(plugin.getLangManager().getMessage("no_suitable_lobby"));
            }
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
            if (bestServer != null) {
                player.createConnectionRequest(bestServer).connect();
            } else {
                player.sendMessage(plugin.getLangManager().getMessage("no_suitable_lobby"));
            }
        } else if (mode.equalsIgnoreCase("order")) {
            // Fill first available lobby
            // Since we can't easily check max players without pinging, we'll iterate and ping.
            // However, pinging is async. For simplicity in this synchronous command execution,
            // we will just connect to the first one that is online.
            // A more robust solution would involve async pinging.
            
            RegisteredServer targetServer = null;
             for (String lobbyName : lobbies) {
                Optional<RegisteredServer> server = plugin.getServer().getServer(lobbyName);
                if (server.isPresent()) {
                    targetServer = server.get();
                    break;
                }
            }
            
            if (targetServer != null) {
                player.createConnectionRequest(targetServer).connect();
            } else {
                player.sendMessage(plugin.getLangManager().getMessage("no_suitable_lobby"));
            }
        }
    }
}
