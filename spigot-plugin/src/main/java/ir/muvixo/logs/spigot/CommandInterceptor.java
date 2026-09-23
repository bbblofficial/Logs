package ir.muvixo.logs.spigot;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Intercepts every command a player types and forwards it to Velocity.
 * Also reports OP status on join / quit / /op / /deop.
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

    // ------------------------------------------------------------
    //  JOIN / QUIT
    // ------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!config.isReportOpStatus()) return;
        Player player = event.getPlayer();
        plugin.sendOpStatus(player, player.isOp());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (!config.isReportOpStatus()) return;
        if (event.getPlayer().isOp()) {
            plugin.sendOpStatus(event.getPlayer(), false);
        }
    }

    // ------------------------------------------------------------
    //  COMMAND INTERCEPT
    // ------------------------------------------------------------

    /**
     * We listen at MONITOR (last) so the /op command has already been executed
     * by the time we run — that way we can read the fresh OP status.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        String full = event.getMessage();
        if (full.isEmpty() || full.charAt(0) != '/') return;

        String command = full.substring(1).trim();
        if (command.isEmpty()) return;

        // ----- 1. Detect /op and /deop to refresh OP status -----
        handleOpCommand(player, command);

        // ----- 2. Forward the command to Velocity -----
        if (!config.isLogOps() && player.isOp()) return;
        if (config.isIgnoredPlayer(player.getName())) return;
        if (config.isBlacklisted(command)) return;

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("CMD");
        out.writeUTF(player.getName());
        out.writeUTF(config.getServerName());
        out.writeUTF(command);

        player.sendPluginMessage(plugin, config.getChannel(), out.toByteArray());
    }

    /**
     * If the command is /op or /deop, re-send the target's OP status to Velocity.
     *
     * Runs at MONITOR priority, but since /op is executed synchronously by
     * Bukkit before the event returns to us... actually it isn't — /op is
     * executed after the event. So we schedule a delayed task (1 tick) to
     * read the fresh OP status.
     */
    private void handleOpCommand(Player sender, String command) {
        if (!config.isReportOpStatus()) return;

        String[] parts = command.split("\\s+");
        if (parts.length < 2) return;

        String base = parts[0].toLowerCase();
        if (!base.equals("op") && !base.equals("deop")) return;

        String targetName = parts[1];

        // Schedule 1 tick later so Bukkit has applied the OP change
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) return;

            boolean isOp = target.isOp();
            plugin.sendOpStatus(target, isOp);

            plugin.getLogger().info("[OP-TRACK] /" + base + " " + targetName
                    + " -> fresh status sent: " + isOp);
        }, 1L);
    }
}