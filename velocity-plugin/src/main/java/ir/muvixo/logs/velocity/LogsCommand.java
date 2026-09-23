package ir.muvixo.logs.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.util.List;

/**
 * /logs command - plugin info, help and reload.
 *
 * @author muvixo
 */
public class LogsCommand implements SimpleCommand {

    private final ProxyServer server;
    private final Logger logger;
    private final Config config;
    private final OpPlayerManager opManager;

    public LogsCommand(ProxyServer server, Logger logger, Config config, OpPlayerManager opManager) {
        this.server = server;
        this.logger = logger;
        this.config = config;
        this.opManager = opManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        // /logs reload
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!hasReloadPermission(source)) {
                source.sendMessage(ColorUtil.color(config.getNoPermissionMessage()));
                return;
            }

            config.load();
            source.sendMessage(ColorUtil.color(config.getReloadSuccessMessage()));
            logger.info("Config reloaded by {}", source);
            return;
        }

        // /logs help
        if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
            sendHelp(source);
            return;
        }

        // /logs
        source.sendMessage(Component.text("VelocityLogs ", NamedTextColor.GOLD)
                .append(Component.text("v1.0.0 ", NamedTextColor.YELLOW))
                .append(Component.text("by muvixo", NamedTextColor.AQUA)));
        source.sendMessage(Component.text("Channel: ", NamedTextColor.GRAY)
                .append(Component.text(config.getChannel(), NamedTextColor.WHITE)));
        source.sendMessage(Component.text("See permission: ", NamedTextColor.GRAY)
                .append(Component.text(config.getSeePermission(), NamedTextColor.WHITE)));
        source.sendMessage(Component.text("Tracked OPs: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(opManager.getOpCount()), NamedTextColor.WHITE)));
        source.sendMessage(Component.text("Type ", NamedTextColor.GRAY)
                .append(Component.text("/logs help", NamedTextColor.YELLOW))
                .append(Component.text(" for help.", NamedTextColor.GRAY)));
    }

    private void sendHelp(CommandSource source) {
        source.sendMessage(Component.text("--- VelocityLogs Help ---", NamedTextColor.GOLD));
        source.sendMessage(Component.text("/logs", NamedTextColor.YELLOW)
                .append(Component.text("  - show plugin info", NamedTextColor.GRAY)));
        source.sendMessage(Component.text("/logs reload", NamedTextColor.YELLOW)
                .append(Component.text("  - reload config", NamedTextColor.GRAY)));
        source.sendMessage(Component.text("/logs help", NamedTextColor.YELLOW)
                .append(Component.text("  - this help menu", NamedTextColor.GRAY)));
    }

    private boolean hasReloadPermission(CommandSource source) {
        if (source.hasPermission(config.getReloadPermission())) return true;
        if (source instanceof Player player) return opManager.isOp(player.getUniqueId());
        return source.hasPermission("velocitylogs.admin");
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        CommandSource src = invocation.source();
        if (src.hasPermission(config.getSeePermission())) return true;
        if (src.hasPermission(config.getReloadPermission())) return true;
        if (src.hasPermission("velocitylogs.admin")) return true;
        if (src instanceof Player player) return opManager.isOp(player.getUniqueId());
        return false;
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return List.of("reload", "help");
    }
}
