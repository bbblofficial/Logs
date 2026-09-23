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
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Receives plugin messages from Spigot backends and broadcasts them to staff.
 *
 * Message format (UTF):
 *   [0] String  playerName
 *   [1] String  serverName
 *   [2] String  command (without leading slash)
 *
 * @author muvixo
 */
public class BackendMessageReceiver {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ProxyServer server;
    private final Logger logger;
    private final Config config;
    private final MinecraftChannelIdentifier channel;

    public BackendMessageReceiver(ProxyServer server, Logger logger, Config config) {
        this.server = server;
        this.logger = logger;
        this.config = config;
        this.channel = MinecraftChannelIdentifier.from(config.getChannel());
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(channel)) return;

        if (!(event.getSource() instanceof ServerConnection connection)) return;

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        try {
            ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
            String playerName = in.readUTF();
            String serverName = in.readUTF();
            String command = in.readUTF();

            broadcast(playerName, serverName, command);
        } catch (Exception e) {
            logger.warn("Failed to decode command-log message", e);
        }
    }

    private void broadcast(String playerName, String serverName, String command) {
        String time = LocalTime.now().format(TIME_FORMAT);

        String raw = config.getMessageFormat()
                .replace("{player}", playerName)
                .replace("{server}", serverName)
                .replace("{command}", command)
                .replace("{time}", time);

        Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(raw);

        for (Player online : server.getAllPlayers()) {
            if (online.hasPermission(config.getSeePermission())) {
                online.sendMessage(message);
            }
        }

        if (config.isLogToConsole()) {
            logger.info("[{}@{}] /{}", playerName, serverName, command);
        }
    }
}
