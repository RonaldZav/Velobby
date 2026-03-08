# Velobby

**Velobby** is a modern, lightweight, and feature-rich lobby management plugin for **Velocity Proxy**. It provides advanced load balancing, auto-reconnection systems, premium lobby support, and full customization.

## 🚀 Features

*   **Multiple Lobby Support**: Add as many lobby servers as you need.
*   **Premium/Special Lobbies**: Automatically route VIP players to specific lobbies based on permissions.
*   **Load Balancing**: Distribute players among lobbies using different strategies:
    *   `random`: Connects to a random lobby.
    *   `balance`: Connects to the lobby with the fewest players.
    *   `order`: Fills lobbies in order (first available).
*   **Auto-Reconnect (Fallback)**: Automatically redirects players to a lobby if they are kicked from a backend server (prevents them from being disconnected from the proxy).
*   **Command Aliases**: Custom aliases like `/lobby` or `/hub`.
*   **Blocked Servers**: Prevent the use of lobby commands in specific servers (e.g., Auth, Login).
*   **Modern Messaging**: Full support for **MiniMessage** (Gradients, Hex colors, etc.) and legacy color codes.
*   **Multi-Language**: Built-in English (`en`), Spanish (`es`), and German (`de`) support.
*   **Update Checker**: Automatically checks for new versions.

## 📥 Installation

1.  Download the `Velobby.jar`.
2.  Place it in your Velocity `plugins` folder.
3.  Restart your proxy.
4.  Configure `config.yml` and language files in `plugins/velobby/`.

## 🛠 Commands & Permissions

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/velobby add` | Adds the server you are currently on to the lobby list. | `velobby.admin` |
| `/velobby remove` | Removes the current server from the lobby list. | `velobby.admin` |
| `/velobby status` | Shows the status and player count of all lobbies. | `velobby.admin` |
| `/velobby send <player\|ALL>` | Sends a specific player or all players to a lobby. | `velobby.admin` |
| `/velobby reload` | Reloads configuration and language files. | `velobby.admin` |
| `/lobby`, `/hub` | Connects the player to a lobby (Aliases are configurable). | None (Default) |

## ⚙️ Configuration

### `config.yml`

```yaml
lobbies:
  - lobby1
  - lobby2

# Premium/Special Lobbies
# If 'default' is true, players with the specified permission will be sent
# to the special lobby instead of the normal load-balanced lobbies.
special_lobbies:
  default: true
  lobby-vip: velobby.vip # Players with this permission go to 'lobby-vip'

# Connection modes:
# random  - Picks a random lobby.
# balance - Picks the lobby with the fewest players.
# order   - Picks the first available lobby in the list.
connection-mode: random

# Servers where /lobby commands are disabled (supports wildcards like auth*)
blocked-servers:
  - auth*
  - login

# Check for updates on startup
check-updates: true

# Redirect players to lobby if kicked from a server
auto-reconnect: true

# Custom aliases for the lobby command
aliases:
  - lobby
  - hub

# Language: en (English), es (Spanish), de (German)
language: en
```

## 🎨 Customization (MiniMessage)

Velobby uses **MiniMessage** for text formatting. You can use gradients, hex colors, and more in `lang/en.yml`, `lang/es.yml`, or `lang/de.yml`.

**Examples:**
*   **Red**: `<red>Hello</red>` or `<#ff0000>Hello</#ff0000>`
*   **Gradient**: `<gradient:#00AAFF:#00FFAA>Velobby</gradient>`
*   **Bold**: `<bold>Important</bold>`
*   **Legacy**: `&cHello` (Supported but MiniMessage is recommended)

## 🔄 Auto-Reconnect Logic

If a player is kicked from a server (e.g., a Survival server restarts):
1.  Velobby intercepts the kick event.
2.  It checks if `auto-reconnect` is enabled.
3.  It checks if the player has permission for a **Special Lobby**. If so, it tries to connect them there first.
4.  If not, it finds a suitable normal lobby (excluding the server the player was kicked from).
5.  It sends the player to that lobby and displays a title/message explaining the redirection.

## 👥 Contributors

*   **lukasbeppi**: German translation (`de.yml`)

---
**Developed by RonaldZav**
