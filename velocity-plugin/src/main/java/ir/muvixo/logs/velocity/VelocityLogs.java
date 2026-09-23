package ir.muvixo.logs.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * VelocityLogs - receives command logs from Spigot backends and broadcasts them
 * to staff (permission holders + backend OPs + proxy OPs).
 *
 * @author muvixo
 */
@Plugin(
        id = "velocity-logs",
        name = "VelocityLogs",
        version = "1.0.0",
        description = "Broadcasts commands from backend Spigot servers to staff",
        authors = {"muvixo"}
)
public class VelocityLogs {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private Config config;
    private OpPlayerManager opManager;
    private PermissionChecker permissionChecker;
    private BackendMessageReceiver receiver;
    private MinecraftChannelIdentifier channel;

    @Inject
    public VelocityLogs(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        if (!Files.exists(dataDirectory)) {
            try { Files.createDirectories(dataDirectory); }
            catch (IOException e) { logger.error("Could not create data directory", e); }
        }

        // ---------- Config ----------
        this.config = new Config(dataDirectory, logger);
        this.config.load();

        // ---------- OP tracking ----------
        this.opManager = new OpPlayerManager(server, logger);
        server.getEventManager().register(this, opManager);

        // ---------- Permission checker ----------
        this.permissionChecker = new PermissionChecker(config, opManager, logger);

        // ---------- Channel ----------
        this.channel = MinecraftChannelIdentifier.from(config.getChannel());
        server.getChannelRegistrar().register(channel);

        // ---------- Message receiver ----------
        this.receiver = new BackendMessageReceiver(server, logger, config, opManager, permissionChecker);
        server.getEventManager().register(this, receiver);

        // ---------- /logs command ----------
        CommandManager cm = server.getCommandManager();
        cm.register(
                cm.metaBuilder("logs")
                        .aliases("cmdlogs", "commandlogs", "vlogs")
                        .plugin(this)
                        .build(),
                new LogsCommand(server, logger, config, opManager, permissionChecker)
        );

        logger.info("===========================================");
        logger.info("  VelocityLogs v1.0.0 by muvixo");
        logger.info("  Channel: {}", config.getChannel());
        logger.info("  See permission: {}", config.getSeePermission());
        logger.info("  Reload permission: {}", config.getReloadPermission());
        logger.info("  Debug: {}", config.isDebug());
        logger.info("===========================================");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (channel != null) {
            server.getChannelRegistrar().unregister(channel);
        }
        logger.info("VelocityLogs disabled.");
    }

    public Config getConfig() { return config; }
}
