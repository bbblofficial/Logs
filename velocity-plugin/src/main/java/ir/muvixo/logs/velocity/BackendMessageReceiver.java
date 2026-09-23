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
import java.util.Optional;

/**
 * Receives plugin messages from Spigot backends and broadcasts them to staff.
 *
 * Message types:
 *   OP_STATUS  -> [type] [playerName] [true/false]
 *   CMD        -> [type] [playerName] [serverName] [command]
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

        // Mark as handled FIRST so Velocity doesn't forward it
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        // Only accept messages from a backend server connection
        if (!(event.getSource() instanceof ServerConnection connection)) return;

        String sourceServer = connection.getServerInfo().getName();

        try {
            ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
            String type = in.readUTF();

            switch (type) {
                case "OP_STATUS": {
                    String playerName = in.readUTF();
                    boolean isOp = Boolean.parseBoolean(in.readUTF());

                    Optional<Player> opt = server.getPlayer(playerName);
                    if (opt.isPresent()) {
                        if (isOp) opManager.markOp(opt.get().getUniqueId(), playerName, sourceServer);
                        else      opManager.unmarkOp(opt.get().getUniqueId(), playerName);
                    } else {
                        if (config.isDebug()) {
                            logger.warn("[OP-TRACK] OP_STATUS for {} but player not online", playerName);
                        }
                    }
                    break;
                }
                case "CMD": {
                    String playerName = in.readUTF();
                    String serverName = in.readUTF();
                    String command = in.readUTF();
                    broadcast(playerName, serverName, command);
                    break;
                }
                default:
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

        int total = 0;
        int sent = 0;

        for (Player online : server.getAllPlayers()) {
            total++;

            boolean canSee = permChecker.canSee(online);
            boolean isSelf = config.isShowToSelf() && online.getUsername().equalsIgnoreCase(playerName);

            if (canSee || isSelf) {
                online.sendMessage(message);
                sent++;
            }

            if (config.isDebug() && online.getUsername().equalsIgnoreCase(playerName)) {
                logger.info("[DEBUG] Executor {} -> canSee={}, isSelf={}",
                        playerName, canSee, isSelf);
            }
        }

        if (config.isLogToConsole()) {
            logger.info("[{}@{}] /{}  (online: {}, sent: {}, tracked-ops: {})",
                    playerName, serverName, command, total, sent, opManager.getOpCount());
        }
    }
}
