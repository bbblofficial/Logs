package ir.muvixo.logs.spigot;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

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
     * PUBLIC - called from CommandInterceptor.
     */
    public void sendOpStatus(Player player, boolean isOp) {
        if (player == null || !player.isOnline()) return;

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("OP_STATUS");
        out.writeUTF(player.getName());
        out.writeUTF(Boolean.toString(isOp));

        try {
            player.sendPluginMessage(this, config.getChannel(), out.toByteArray());
        } catch (Exception e) {
            getLogger().warning("Failed to send OP_STATUS for " + player.getName() + ": " + e.getMessage());
        }
    }

    public Config getPluginConfig() {
        return config;
    }
}