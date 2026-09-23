package ir.muvixo.logs.spigot;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
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

        // Periodic OP status re-broadcast (safety net)
        long intervalTicks = config.getOpStatusIntervalMinutes() * 60L * 20L;
        if (intervalTicks > 0) {
            getServer().getScheduler().runTaskTimer(this, () -> {
                for (Player p : getServer().getOnlinePlayers()) {
                    if (p.isOp()) sendOpStatus(p, true);
                }
            }, intervalTicks, intervalTicks);
        }

        getLogger().info("===========================================");
        getLogger().info("  VelocityLogs-Spigot v" + getDescription().getVersion());
        getLogger().info("  Channel: " + config.getChannel());
        getLogger().info("  Server name: " + config.getServerName());
        getLogger().info("  Report OP status: " + config.isReportOpStatus());
        getLogger().info("  OP re-broadcast interval: " + config.getOpStatusIntervalMinutes() + " min");
        getLogger().info("===========================================");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getLogger().info("VelocityLogs-Spigot disabled.");
    }

    /**
     * Sends the OP status of a player to the proxy.
     */
    public void sendOpStatus(Player player, boolean isOp) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("OP_STATUS");
        out.writeUTF(player.getName());
        out.writeUTF(Boolean.toString(isOp));
        player.sendPluginMessage(this, config.getChannel(), out.toByteArray());
    }

    public Config getPluginConfig() {
        return config;
    }
}
