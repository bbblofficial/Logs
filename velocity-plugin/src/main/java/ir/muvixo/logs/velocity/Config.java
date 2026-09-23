package ir.muvixo.logs.velocity;

import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Config manager for VelocityLogs.
 *
 * @author muvixo
 */
public class Config {

    private final Path dataDirectory;
    private final Logger logger;

    private String channel;
    private String messageFormat;
    private String seePermission;
    private boolean logToConsole;

    public Config(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
    }

    public void load() {
        Path file = dataDirectory.resolve("config.yml");

        if (!Files.exists(file)) {
            try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                if (in != null) Files.copy(in, file);
                else Files.writeString(file, "channel: \"velocitylogs:main\"\n");
            } catch (IOException e) {
                logger.error("Could not save default config", e);
            }
        }

        CommentedConfigurationNode root;
        try {
            root = YamlConfigurationLoader.builder().path(file).build().load();
        } catch (IOException e) {
            logger.error("Could not load config.yml", e);
            return;
        }

        this.channel = root.node("channel").getString("velocitylogs:main");
        this.messageFormat = root.node("message-format").getString(
                "&8[&cLogs&8] &e{player} &8» &b{server} &8» &f/{command}");
        this.seePermission = root.node("see-permission").getString("velocitylogs.see");
        this.logToConsole = root.node("log-to-console").getBoolean(true);
    }

    public String getChannel() { return channel; }
    public String getMessageFormat() { return messageFormat; }
    public String getSeePermission() { return seePermission; }
    public boolean isLogToConsole() { return logToConsole; }
}
