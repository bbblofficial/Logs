# VelocityLogs

Logs **every command** any player types on any backend server (including Minecraft 1.8.*) and broadcasts them to staff on the Velocity proxy.

**Author:** muvixo

## Architecture

- **`spigot-plugin`** — a lightweight Spigot 1.8.* plugin. Listens to `PlayerCommandPreprocessEvent`
  and forwards the raw command via BungeeCord plugin messaging to the proxy.
- **`velocity-plugin`** — a Velocity plugin. Receives the forwarded messages and displays them
  to everyone with `velocitylogs.see`.

## Compatibility

| Component | Version |
|-----------|---------|
| Velocity  | 3.3.0+  |
| Java (proxy) | 17+ |
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
3. In Velocity `velocity.toml`, set `bungee-plugin-message-channel = true` (default).
4. Restart everything.

## Permissions

`velocitylogs.see` — see command logs (grant to staff / OP).

## Config

Both `config.yml` files are generated on first run.
