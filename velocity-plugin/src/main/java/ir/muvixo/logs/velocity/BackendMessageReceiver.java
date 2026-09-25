package ir.muvixo.logs.velocity;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Receives plugin messages from Spigot backends.
 *
 * v2.1 - The CMD message now also *syncs* OP status: if a player is no
 * longer OP, they are immediately unmarked on the proxy. This stops
 * regular players from seeing the logs.
 *
 * @author muvixo
 */
public class BackendMessageReceiver {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ProxyServer server;
    private final Logger logger;
    private final Config config;
    private final OpPlayerManager opManager;
    private final PermissionChecker permChecker;
    private final MinecraftChannelIdentifier channel;

    public BackendMessageReceiver(ProxyServer server, Logger logger, Config config,
                                  OpPlayerManager opManager, PermissionChecker permChecker) {
        this.server = server;
        this.logger = logger;
        this.config = config;
        this.opManager = opManager;
        this.permChecker = permChecker;
        this.channel = MinecraftChannelIdentifier.from(config.getChannel());
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(channel)) return;
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        if (!(event.getSource() instanceof ServerConnection connection)) return;
        String sourceServer = connection.getServerInfo().getName();

        try {
            ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
            String type = in.readUTF();

            if ("OP_STATUS".equals(type)) {
                UUID uuid = UUID.fromString(in.readUTF());
                String name = in.readUTF();
                String serverName = in.readUTF();
                boolean isOp = Boolean.parseBoolean(in.readUTF());

                if (isOp) opManager.markOp(uuid, name, serverName);
                else      opManager.unmarkOp(uuid, name);

                if (config.isDebug()) {
                    logger.info("[OP-TRACK] {} -> {} (from {})", name, isOp, serverName);
                }
            } else if ("CMD".equals(type)) {
                UUID uuid = UUID.fromString(in.readUTF());
                String name = in.readUTF();
                String serverName = in.readUTF();
                boolean isOp = Boolean.parseBoolean(in.readUTF());
                String command = in.readUTF();

                // --- FIX: Sync OP status on every command ---
                // If the player is no longer OP, remove them right away so
                // they stop receiving logs on the next broadcast.
                if (isOp) {
                    opManager.markOp(uuid, name, serverName);
                } else {
                    opManager.unmarkOp(uuid, name);
                }

                broadcast(name, serverName, command);
            } else {
                logger.warn("[VelocityLogs] Unknown message type: {}", type);
            }
        } catch (Exception e) {
            logger.warn("[VelocityLogs] Failed to decode plugin message from {}", sourceServer, e);
        }
    }

    private void broadcast(String playerName, String serverName, String command) {
        String time = LocalTime.now().format(TIME_FORMAT);
        String raw = config.getMessageFormat()
                .replace("{player}", playerName)
                .replace("{server}", serverName)
                .replace("{command}", command)
                .replace("{time}", time);
        Component message = ColorUtil.color(raw);

        int total = 0, sent = 0;
        for (Player online : server.getAllPlayers()) {
            total++;
            // Central permission check: backend-OP OR explicit Velocity permission.
            if (!permChecker.canSee(online)) continue;
            // Optional: don't show the player their own command
            if (!config.isShowToSelf() && online.getUsername().equalsIgnoreCase(playerName)) continue;

            online.sendMessage(message);
            sent++;
        }

        if (config.isLogToConsole()) {
            logger.info("[{}@{}] /{}  (online: {}, sent: {}, tracked-ops: {})",
                    playerName, serverName, command, total, sent, opManager.getOpCount());
        }
    }
}