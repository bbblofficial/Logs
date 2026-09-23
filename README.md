# VelocityLogs

Logs **every command** any player types on any backend server (including Minecraft 1.8.*)
and broadcasts them to staff / OP players on the Velocity proxy.

**Author:** muvixo

## Features

- Logs every command on any backend server (works for OPs too)
- **Multi-source OP detection:**
  - Backend `player.isOp()`
  - Proxy OP (`ops.json`)
  - LuckPerms groups with `velocitylogs.see`
  - Wildcard perms like `*` or `velocitylogs.*`
- Full config.yml management on both sides
- `/logs reload` — reload config without restarting
- `/logs debug <player>` — see why a player is/isn't receiving logs
- Color support: `&a`, `&c`, `&l`, `&#RRGGBB`
- Works with Minecraft 1.8.* backend servers

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

1. Drop `velocity-logs-velocity-1.0.0.jar` into Velocity `plugins/`.
2. Drop `velocity-logs-spigot-1.0.0.jar` into each Spigot 1.8 backend's `plugins/`.
3. Make sure `bungee-plugin-message-channel = true` in `velocity.toml` (default).
4. Restart everything.

## Commands

| Command | Description |
|---------|-------------|
| `/logs` | Show plugin info |
| `/logs reload` | Reload config |
| `/logs help` | Show help |
| `/logs debug <player>` | Diagnose a player's permissions |

## Permissions

| Permission | Description |
|-----------|-------------|
| `velocitylogs.see` | See command logs |
| `velocitylogs.reload` | Reload config |
| `velocitylogs.admin` | Full admin |

**OPs on backend/proxy automatically see logs.**
