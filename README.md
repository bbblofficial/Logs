# VelocityLogs

Logs **every command** any player types on any backend server (including Minecraft 1.8.*)
and broadcasts them to staff / OP players on the Velocity proxy.

**Author:** muvixo

## Features

- Logs every command a player runs on any backend server (works for OPs too)
- Automatic OP detection across the whole network
- Full config.yml management on both sides
- `/logs reload` — reload config without restarting
- Color support: `&a`, `&c`, `&l`, `&#RRGGBB`
- Works with Minecraft 1.8.* backend servers

## Architecture

- **`spigot-plugin`** — Spigot 1.8.* plugin. Listens to `PlayerCommandPreprocessEvent`
  and forwards the raw command via plugin messaging to the proxy.
- **`velocity-plugin`** — Velocity plugin. Receives forwarded messages and displays
  them to everyone with `velocitylogs.see` **or** who is OP on a backend server.

## Compatibility

| Component | Version |
|-----------|---------|
| Velocity  | 3.3.0+  |
| Java (proxy) | 17+ (tested on JDK 21 / 25) |
| Spigot/Paper (backend) | 1.8.8 – 1.8.9 |
| Java (backend) | 8+ |

## Build

```bash
mvn clean package
```

Outputs:
- `velocity-plugin/target/velocity-logs-velocity-1.0.0.jar`
- `spigot-plugin/target/velocity-logs-spigot-1.0.0.jar`

## Install

1. Drop `velocity-logs-velocity-1.0.0.jar` into the Velocity `plugins/` folder.
2. Drop `velocity-logs-spigot-1.0.0.jar` into each Spigot 1.8 backend server's `plugins/` folder.
3. Make sure `bungee-plugin-message-channel = true` in Velocity `velocity.toml` (default).
4. Restart everything.

## Commands

| Command | Description |
|---------|-------------|
| `/logs` | Show plugin info |
| `/logs reload` | Reload config files |
| `/logs help` | Show help |

## Permissions

| Permission | Description |
|-----------|-------------|
| `velocitylogs.see` | See command logs |
| `velocitylogs.reload` | Reload the plugin config |
| `velocitylogs.admin` | Full admin access |

**OP players on backend servers automatically see logs.**

## Config Files

- Velocity: `plugins/velocity-logs/config.yml`
- Spigot:   `plugins/VelocityLogs-Spigot/config.yml`
