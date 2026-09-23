package ir.muvixo.logs.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks players who are OP on their current backend.
 *
 * v2.0 changes:
 *   - UUID-based tracking
 *   - lastSeen timestamp so we can expire stale entries
 *   - manualOverride flag for /logs op
 *
 * @author muvixo
 */
public class OpPlayerManager {

    public static class OpRecord {
        public final UUID uuid;
        public volatile String name;
        public volatile String serverName;
        public volatile long lastSeen;
        public volatile boolean manual;

        public OpRecord(UUID uuid, String name, String serverName, boolean manual) {
            this.uuid = uuid;
            this.name = name;
            this.serverName = serverName;
            this.lastSeen = System.currentTimeMillis();
            this.manual = manual;
        }

        public void refresh(String serverName) {
            this.serverName = serverName;
            this.lastSeen = System.currentTimeMillis();
        }
    }

    private final ProxyServer server;
    private final Logger logger;
    private final Config config;
    private final Map<UUID, OpRecord> opPlayers = new ConcurrentHashMap<>();
    private final Map<String, UUID> nameIndex = new ConcurrentHashMap<>();

    public OpPlayerManager(ProxyServer server, Logger logger, Config config) {
        this.server = server;
        this.logger = logger;
        this.config = config;
    }

    public void markOp(UUID uuid, String playerName, String serverName) {
        OpRecord existing = opPlayers.get(uuid);
        if (existing != null) {
            existing.refresh(serverName);
            existing.name = playerName;
            return;
        }
        OpRecord rec = new OpRecord(uuid, playerName, serverName, false);
        opPlayers.put(uuid, rec);
        nameIndex.put(playerName.toLowerCase(), uuid);
        if (logger != null) {
            logger.info("[OP-TRACK] Marked {} ({}) as OP (backend: {})",
                    playerName, uuid, serverName);
        }
    }

    public void markOpManual(UUID uuid, String playerName) {
        OpRecord rec = new OpRecord(uuid, playerName, "manual", true);
        opPlayers.put(uuid, rec);
        nameIndex.put(playerName.toLowerCase(), uuid);
        if (logger != null) {
            logger.info("[OP-TRACK] Manually marked {} ({}) as OP", playerName, uuid);
        }
    }

    public void unmarkOp(UUID uuid, String playerName) {
        OpRecord removed = opPlayers.remove(uuid);
        if (playerName != null) nameIndex.remove(playerName.toLowerCase());
        if (removed != null && logger != null) {
            logger.info("[OP-TRACK] Unmarked {} ({})", playerName, uuid);
        }
    }

    public boolean isOp(UUID uuid) {
        if (uuid == null) return false;
        OpRecord rec = opPlayers.get(uuid);
        if (rec == null) return false;
        // expire stale non-manual entries when player is offline
        int expire = config != null ? config.getOpCacheExpireMinutes() : 30;
        if (expire > 0 && !rec.manual) {
            boolean online = server.getPlayer(uuid).isPresent();
            long age = System.currentTimeMillis() - rec.lastSeen;
            if (!online && age > expire * 60_000L) {
                opPlayers.remove(uuid);
                nameIndex.remove(rec.name.toLowerCase());
                return false;
            }
        }
        return true;
    }

    public boolean isOpByName(String name) {
        if (name == null) return false;
        UUID uuid = nameIndex.get(name.toLowerCase());
        return uuid != null && isOp(uuid);
    }

    public String getOpServer(UUID uuid) {
        OpRecord rec = opPlayers.get(uuid);
        return rec == null ? null : rec.serverName;
    }

    public int getOpCount() {
        return opPlayers.size();
    }

    public Map<UUID, OpRecord> getOpPlayers() {
        return opPlayers;
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        OpRecord rec = opPlayers.get(uuid);
        if (rec != null && !rec.manual) {
            // Keep record but mark lastSeen old; isOp() will expire it.
            // This preserves OP across quick reconnects.
            rec.lastSeen = System.currentTimeMillis();
        }
    }

    @Subscribe
    public void onServerPostConnect(ServerPostConnectEvent event) {
        if (logger != null && config != null && config.isDebug()) {
            logger.info("[OP-TRACK] {} connected to {} (OP status: {})",
                    event.getPlayer().getUsername(),
                    event.getPlayer().getCurrentServer()
                            .map(s -> s.getServerInfo().getName()).orElse("?"),
                    isOp(event.getPlayer().getUniqueId()));
        }
    }
}
