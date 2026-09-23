package ir.muvixo.logs.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks players who are OP on their current backend server.
 *
 * Unlike the old version, we do NOT clear OP status on server switch.
 * Instead, each backend reports its own OP status on join and we keep
 * a per-player record until they disconnect from the network entirely.
 *
 * @author muvixo
 */
public class OpPlayerManager {

    private final ProxyServer server;
    private final Logger logger;
    private final Set<UUID> opPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> opServer = new ConcurrentHashMap<>();

    public OpPlayerManager(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    public void markOp(UUID uuid, String playerName, String serverName) {
        boolean added = opPlayers.add(uuid);
        opServer.put(uuid, serverName);
        if (added && logger != null) {
            logger.info("[OP-TRACK] Marked {} as OP (backend: {})", playerName, serverName);
        }
    }

    public void unmarkOp(UUID uuid, String playerName) {
        boolean removed = opPlayers.remove(uuid);
        opServer.remove(uuid);
        if (removed && logger != null) {
            logger.info("[OP-TRACK] Unmarked {} (no longer OP)", playerName);
        }
    }

    public boolean isOp(UUID uuid) {
        return opPlayers.contains(uuid);
    }

    public String getOpServer(UUID uuid) {
        return opServer.get(uuid);
    }

    public int getOpCount() {
        return opPlayers.size();
    }

    public Set<UUID> getOpPlayers() {
        return opPlayers;
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        opPlayers.remove(uuid);
        opServer.remove(uuid);
    }

    @Subscribe
    public void onServerPostConnect(ServerPostConnectEvent event) {
        // When the player switches to a new backend, the new server
        // will send OP_STATUS on PlayerJoinEvent. We don't clear here,
        // so the player retains their status if the new server also says they're OP.
        // If the new server does NOT send OP_STATUS, the player keeps the old status
        // until they disconnect — better UX than losing it on every switch.
        if (logger != null) {
            logger.info("[OP-TRACK] {} connected to {} (current OP status: {})",
                    event.getPlayer().getUsername(),
                    event.getPlayer().getCurrentServer().map(s -> s.getServerInfo().getName()).orElse("?"),
                    opPlayers.contains(event.getPlayer().getUniqueId()));
        }
    }
}
