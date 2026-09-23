# VelocityLogs v2.0

Logs **every command** any player types on any backend server and broadcasts
them to staff / OP players on the Velocity proxy.

**Author:** muvixo

## What's new in v2.0

- **Bulletproof OP detection** — OP status is attached to *every* command,
  so it works even if the join message got lost.
- **UUID-based tracking** — no more name-change or case bugs.
- **`/logs op <player>` / `/logs unop <player>`** — manual overrides.
- **`/logs list`** — see all currently tracked OPs.
- **`force-see-players`** — a config list of players who always see logs.

## Commands (Velocity)

| Command | Description |
|---------|-------------|
| `/logs` | Show plugin info |
| `/logs help` | Show help |
| `/logs reload` | Reload config |
| `/logs debug <player>` | Diagnose a player's permissions |
| `/logs status <player>` | Same as debug |
| `/logs op <player>` | Manually mark player as OP |
| `/logs unop <player>` | Manually unmark player |
| `/logs list` | List all tracked OPs |

## Build

```bash
mvn clean package
```

Outputs:
- `velocity-plugin/target/velocity-logs-velocity-2.0.0.jar`
- `spigot-plugin/target/velocity-logs-spigot-2.0.0.jar`
