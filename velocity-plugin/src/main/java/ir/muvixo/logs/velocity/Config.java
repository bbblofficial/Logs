package ir.muvixo.logs.velocity;

import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Config manager for VelocityLogs.
 *
 * @author muvixo
 */
public class Config {

    private final Path dataDirectory;
    private final Logger logger;

    private String channel;
    private String seePermission;
    private String reloadPermission;
    private String adminPermission;
    private List<String> extraSeePermissions;
    private String messageFormat;
    private String reloadSuccessMessage;
    private String noPermissionMessage;
    private boolean showToSelf;
    private boolean logToConsole;
    private boolean debug;

    public Config(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
    }

    public void load() {
        Path file = dataDirectory.resolve("config.yml");

        if (!Files.exists(file)) {
            try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                if (in != null) Files.copy(in, file);
                else Files.writeString(file, "# default config\n");
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
        this.seePermission = root.node("see-permission").getString("velocitylogs.see");
        this.reloadPermission = root.node("reload-permission").getString("velocitylogs.reload");
        this.adminPermission = root.node("admin-permission").getString("velocitylogs.admin");

        List<String> extras = root.node("extra-see-permissions").getList(String.class, new ArrayList<>());
        if (extras == null) extras = new ArrayList<>();
        this.extraSeePermissions = extras;

        this.showToSelf = root.node("show-to-self").getBoolean(false);
        this.logToConsole = root.node("log-to-console").getBoolean(true);
        this.debug = root.node("debug").getBoolean(true);

        this.messageFormat = root.node("message-format").getString(
                "&8[&cLogs&8] &e{player} &8>> &b{server} &8>> &f/{command}");
        this.reloadSuccessMessage = root.node("reload-success-message").getString(
                "&a[OK] Config reloaded successfully!");
        this.noPermissionMessage = root.node("no-permission-message").getString(
                "&c[!] You don't have permission to do that!");
    }

    public String getChannel() { return channel; }
    public String getSeePermission() { return seePermission; }
    public String getReloadPermission() { return reloadPermission; }
    public String getAdminPermission() { return adminPermission; }
    public List<String> getExtraSeePermissions() { return extraSeePermissions; }
    public String getMessageFormat() { return messageFormat; }
    public String getReloadSuccessMessage() { return reloadSuccessMessage; }
    public String getNoPermissionMessage() { return noPermissionMessage; }
    public boolean isShowToSelf() { return showToSelf; }
    public boolean isLogToConsole() { return logToConsole; }
    public boolean isDebug() { return debug; }
}
