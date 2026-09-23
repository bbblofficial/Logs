package ir.muvixo.logs.spigot;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Intercepts every command a player types and forwards it to Velocity.
 * Also reports OP status on join.
 *
 * Works for ALL players - including OPs and staff.
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

    /**
     * On join: if the player is OP, tell Velocity so they auto-see logs.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!config.isReportOpStatus()) return;

        Player player = event.getPlayer();
        if (!player.isOp()) return;

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("OP_STATUS");
        out.writeUTF(player.getName());
        out.writeUTF("true");

        player.sendPluginMessage(plugin, config.getChannel(), out.toByteArray());
    }

    /**
     * Intercept every command and forward it to the proxy.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        // Optional: skip OPs entirely if log-ops is false
        if (!config.isLogOps() && player.isOp()) return;

        // Skip ignored players
        if (config.isIgnoredPlayer(player.getName())) return;

        String full = event.getMessage();
        if (full.isEmpty() || full.charAt(0) != '/') return;

        String command = full.substring(1).trim();
        if (command.isEmpty()) return;

        if (config.isBlacklisted(command)) return;

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("CMD");
        out.writeUTF(player.getName());
        out.writeUTF(config.getServerName());
        out.writeUTF(command);

        player.sendPluginMessage(plugin, config.getChannel(), out.toByteArray());
    }
}
