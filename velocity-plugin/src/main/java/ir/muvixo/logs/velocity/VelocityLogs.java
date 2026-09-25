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

@Plugin(
        id = "velocity-logs",
        name = "VelocityLogs",
        version = "2.0.0",
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
        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }
        } catch (IOException e) {
            logger.error("Could not create data directory", e);
        }

        this.config = new Config(dataDirectory, logger);
        this.config.load();

        this.opManager = new OpPlayerManager(server, logger, config);
        server.getEventManager().register(this, opManager);

        this.permissionChecker = new PermissionChecker(config, opManager, logger);

        this.channel = MinecraftChannelIdentifier.from(config.getChannel());
        server.getChannelRegistrar().register(channel);
        this.receiver = new BackendMessageReceiver(server, logger, config, opManager, permissionChecker);
        server.getEventManager().register(this, receiver);

        CommandManager cm = server.getCommandManager();
        cm.register(
                cm.metaBuilder("logs")
                        .aliases("cmdlogs", "commandlogs", "vlogs")
                        .plugin(this)
                        .build(),
                new LogsCommand(server, logger, config, opManager, permissionChecker)
        );

        logger.info("===========================================");
        logger.info("  VelocityLogs v2.0.0 by muvixo");
        logger.info("  Channel: {}", config.getChannel());
        logger.info("  Visibility: only backend OPs");
        logger.info("  Debug: {}", config.isDebug());
        logger.info("===========================================");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (channel != null) {
            try { server.getChannelRegistrar().unregister(channel); } catch (Exception ignored) {}
        }
        logger.info("VelocityLogs disabled.");
    }

    public Config getConfig() { return config; }
}