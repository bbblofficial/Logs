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
 * Intercepts EVERY command a player types (even unknown ones, even ones
 * that will be cancelled by other plugins) and forwards them to Velocity.
 *
 * v2.2 - Uses ignoreCancelled = false so cancelled/unknown commands still
 * get logged. Every command is reported together with the current OP status
 * so the proxy never keeps a stale OP entry.
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
        plugin.sendOpStatus(event.getPlayer(), false);
    }

    /**
     * IMPORTANT:
     *   priority         = MONITOR      -> runs after every other plugin
     *   ignoreCancelled  = false        -> we still see commands other plugins cancelled
     *
     * This makes us catch:
     *   - valid commands
     *   - invalid/unknown commands
     *   - commands blocked by other plugins
     *   - commands the player lacks permission for
     *
     * Basically anything the client sends that starts with "/".
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onCommand(PlayerCommandPreprocessEvent event) {

        if (!plugin.isForwardingEnabled()) return;

        Player player = event.getPlayer();

        // Always sync the current OP status first (clears stale entries).
        if (config.isReportOpStatus()) {
            plugin.sendOpStatus(player, player.isOp());
        }

        // Config filters (optional)
        if (!config.isLogOps() && player.isOp()) return;
        if (config.isIgnoredPlayer(player.getName())) return;

        String full = event.getMessage();
        if (full == null || full.isEmpty()) return;

        // Must start with "/"
        if (full.charAt(0) != '/') return;

        String command = full.substring(1).trim();
        if (command.isEmpty()) return;

        // Do not forward our own local helper command
        String base = command.split(" ", 2)[0].toLowerCase();
        if (base.equals("vlogs") || base.equals("velogs") || base.equals("velocitylogs")) return;

        // Blacklist check (optional)
        if (config.isBlacklisted(command)) return;

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
                        + " (op=" + player.isOp() + ") -> /" + command);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to forward command: " + e.getMessage());
        }
    }
}