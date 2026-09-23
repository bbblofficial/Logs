package ir.muvixo.logs.spigot;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * SpigotLogs v2.0 - forwards every command to the Velocity proxy.
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
        getLogger().info("  Report OP: " + config.isReportOpStatus());
        getLogger().info("===========================================");
    }

    @Override
    public void onDisable() {
        try {
            getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        } catch (Exception ignored) {}
        getLogger().info("VelocityLogs-Spigot disabled.");
    }

    public void sendOpStatus(Player player, boolean isOp) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("OP_STATUS");
            out.writeUTF(player.getUniqueId().toString());
            out.writeUTF(player.getName());
            out.writeUTF(config.getServerName());
            out.writeUTF(Boolean.toString(isOp));
            player.sendPluginMessage(this, config.getChannel(), out.toByteArray());
        } catch (Exception e) {
            getLogger().warning("Failed to send OP_STATUS: " + e.getMessage());
        }
    }

    public Config getPluginConfig() {
        return config;
    }
}
