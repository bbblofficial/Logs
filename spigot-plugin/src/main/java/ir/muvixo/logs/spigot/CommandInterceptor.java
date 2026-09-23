package ir.muvixo.logs.spigot;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Intercepts every command a player types and forwards it to Velocity.
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        if (!config.isLogOps() && player.isOp()) return;

        String full = event.getMessage();
        if (full.isEmpty() || full.charAt(0) != '/') return;

        String command = full.substring(1);

        if (config.isBlacklisted(command)) return;

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(player.getName());
        out.writeUTF(plugin.getServer().getServerName());
        out.writeUTF(command);

        player.sendPluginMessage(plugin, config.getChannel(), out.toByteArray());
    }
}
