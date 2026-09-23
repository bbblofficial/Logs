package ir.muvixo.logs.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks players who are OP on their current backend server.
 *
 * @author muvixo
 */
public class OpPlayerManager {

    private final ProxyServer server;
    private final Logger logger;
    private final Set<UUID> opPlayers = ConcurrentHashMap.newKeySet();

    public OpPlayerManager(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    public void markOp(UUID uuid, String name) {
        if (opPlayers.add(uuid)) {
            logger.info("Marked {} as OP (backend)", name);
        }
    }

    public void unmarkOp(UUID uuid) {
        opPlayers.remove(uuid);
    }

    public boolean isOp(UUID uuid) {
        return opPlayers.contains(uuid);
    }

    public int getOpCount() {
        return opPlayers.size();
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        opPlayers.remove(event.getPlayer().getUniqueId());
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        // When switching servers, clear the old OP status.
        // The new backend will re-send it via OP_STATUS on PlayerJoinEvent.
        if (event.getPreviousServer().isPresent()) {
            opPlayers.remove(event.getPlayer().getUniqueId());
        }
    }
}
