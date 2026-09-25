package ir.muvixo.logs.spigot;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Intercepts commands and forwards them with OP status to Velocity.
 *
 * @author muvixo
 */
public class CommandInterceptor implements Listener {

    private final SpigotLogs plugin;
    private final Config config;

    public CommandInterceptor(SpigotLogs plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!config.isReportOpStatus()) return;
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.sendOpStatus(player, player.isOp());
            }
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (!config.isReportOpStatus()) return;
        if (event.getPlayer().isOp()) {
            plugin.sendOpStatus(event.getPlayer(), false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {

        // Runtime toggle
        if (!plugin.isForwardingEnabled()) return;

        Player player = event.getPlayer();

        if (!config.isLogOps() && player.isOp()) return;
        if (config.isIgnoredPlayer(player.getName())) return;

        String full = event.getMessage();
        if (full == null || full.isEmpty() || full.charAt(0) != '/') return;

        String command = full.substring(1).trim();
        if (command.isEmpty()) return;
        if (config.isBlacklisted(command)) return;

        // Don't forward /vlogs (it's a local command)
        String base = command.split(" ", 2)[0].toLowerCase();
        if (base.equals("vlogs") || base.equals("velogs") || base.equals("velocitylogs")) return;

        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("CMD");
            out.writeUTF(player.getUniqueId().toString());
            out.writeUTF(player.getName());
            out.writeUTF(config.getServerName());
            out.writeUTF(Boolean.toString(player.isOp()));
            out.writeUTF(command);
            player.sendPluginMessage(plugin, config.getChannel(), out.toByteArray());

            if (plugin.isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG] Forwarded: " + player.getName()
                        + " -> /" + command);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to forward command: " + e.getMessage());
        }
    }
}