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
 * Tracks which players are OP on a backend server.
 *
 * Entries are created when a backend reports OP_STATUS=true or when a
 * command arrives with op=true. They are removed:
 *   - Immediately when OP_STATUS=false arrives (e.g. player quit or de-opped).
 *   - Immediately when a CMD arrives with op=false (fixes stale entries).
 *   - After {@code op-cache-expire-minutes} once the player is offline.
 *
 * @author muvixo
 */
public class OpPlayerManager {

    public static class OpRecord {
        public final UUID uuid;
        public volatile String name;
        public volatile String serverName;
        public volatile long lastSeen;

        public OpRecord(UUID uuid, String name, String serverName) {
            this.uuid = uuid;
            this.name = name;
            this.serverName = serverName;
            this.lastSeen = System.currentTimeMillis();
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

    /**
     * Mark a player as OP. If already tracked, refresh their server + name.
     */
    public void markOp(UUID uuid, String playerName, String serverName) {
        if (uuid == null) return;

        OpRecord existing = opPlayers.get(uuid);
        if (existing != null) {
            // Update name index if the name changed
            if (playerName != null && !playerName.equalsIgnoreCase(existing.name)) {
                nameIndex.remove(existing.name.toLowerCase());
                nameIndex.put(playerName.toLowerCase(), uuid);
                existing.name = playerName;
            }
            existing.refresh(serverName);
            return;
        }

        OpRecord rec = new OpRecord(uuid, playerName, serverName);
        opPlayers.put(uuid, rec);
        if (playerName != null) {
            nameIndex.put(playerName.toLowerCase(), uuid);
        }
        if (logger != null) {
            logger.info("[OP-TRACK] Marked {} ({}) as OP (backend: {})",
                    playerName, uuid, serverName);
        }
    }

    /**
     * Remove a player from the OP list. Uses the stored record's name for
     * cleanup so a null/incorrect playerName argument can't leave a stale
     * entry in the name index.
     */
    public void unmarkOp(UUID uuid, String playerName) {
        if (uuid == null) return;

        OpRecord removed = opPlayers.remove(uuid);
        if (removed != null) {
            nameIndex.remove(removed.name.toLowerCase());
            if (logger != null) {
                logger.info("[OP-TRACK] Unmarked {} ({})", removed.name, uuid);
            }
        }
        // Clean up any leftover name-index entries for the supplied name too.
        if (playerName != null) {
            nameIndex.remove(playerName.toLowerCase());
        }
    }

    /**
     * Is this UUID currently considered an OP?
     * If the player is offline and the cache has expired, they are dropped.
     */
    public boolean isOp(UUID uuid) {
        if (uuid == null) return false;
        OpRecord rec = opPlayers.get(uuid);
        if (rec == null) return false;

        int expire = config != null ? config.getOpCacheExpireMinutes() : 30;
        if (expire > 0) {
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

    // ============================================================
    //  Events
    // ============================================================

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        OpRecord rec = opPlayers.get(uuid);
        if (rec != null) {
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