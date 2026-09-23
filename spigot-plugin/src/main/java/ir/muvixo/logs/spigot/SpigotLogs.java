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

        getServer().getMessenger().registerOutgoingPluginChannel(this, config.getChannel());
        getServer().getPluginManager().registerEvents(new CommandInterceptor(this, config), this);

        getLogger().info("VelocityLogs-Spigot enabled. Channel: " + config.getChannel());
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getLogger().info("VelocityLogs-Spigot disabled.");
    }
}
