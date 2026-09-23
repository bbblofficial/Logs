package ir.muvixo.logs.spigot;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * SpigotLogs - forwards every command executed by a player to the Velocity proxy.
 * Works on Minecraft 1.8.8 / 1.8.9.
 *
 * @author muvixo
 */
public class SpigotLogs extends JavaPlugin {

    private Config config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.config = new Config(this);
        this.config.load();

        // Register the outgoing plugin messaging channel
        getServer().getMessenger().registerOutgoingPluginChannel(this, config.getChannel());

        // Register the event listener
        getServer().getPluginManager().registerEvents(new CommandInterceptor(this, config), this);

        getLogger().info("===========================================");
        getLogger().info("  VelocityLogs-Spigot v" + getDescription().getVersion());
        getLogger().info("  Channel: " + config.getChannel());
        getLogger().info("  Server name: " + config.getServerName());
        getLogger().info("  Report OP status: " + config.isReportOpStatus());
        getLogger().info("===========================================");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getLogger().info("VelocityLogs-Spigot disabled.");
    }

    public Config getPluginConfig() {
        return config;
    }
}
