package ir.muvixo.logs.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * /logs command - info, help, reload, debug.
 *
 * @author muvixo
 */
public class LogsCommand implements SimpleCommand {

    private final ProxyServer server;
    private final Logger logger;
    private final Config config;
    private final OpPlayerManager opManager;
    private final PermissionChecker permChecker;

    public LogsCommand(ProxyServer server, Logger logger, Config config,
                       OpPlayerManager opManager, PermissionChecker permChecker) {
        this.server = server;
        this.logger = logger;
        this.config = config;
        this.opManager = opManager;
        this.permChecker = permChecker;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        // ---- /logs reload ----
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!canReload(source)) {
                source.sendMessage(ColorUtil.color(config.getNoPermissionMessage()));
                return;
            }
            config.load();
            source.sendMessage(ColorUtil.color(config.getReloadSuccessMessage()));
            logger.info("Config reloaded by {}", source);
            return;
        }

        // ---- /logs debug <player> ----
        if (args.length > 0 && args[0].equalsIgnoreCase("debug")) {
            if (!canReload(source)) {
                source.sendMessage(ColorUtil.color(config.getNoPermissionMessage()));
                return;
            }
            if (args.length < 2) {
                source.sendMessage(Component.text("Usage: /logs debug <player>", NamedTextColor.RED));
                return;
            }
            java.util.Optional<Player> opt = server.getPlayer(args[1]);
            if (opt.isEmpty()) {
                source.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
                return;
            }
            Player target = opt.get();
            String report = permChecker.explain(target);
            for (String line : report.split("\n")) {
                source.sendMessage(Component.text(line, NamedTextColor.GRAY));
            }
            return;
        }

        // ---- /logs help ----
        if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
            sendHelp(source);
            return;
        }

        // ---- /logs (info) ----
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
        source.sendMessage(Component.text("/logs debug <player>", NamedTextColor.YELLOW)
                .append(Component.text("  - diagnose player permissions", NamedTextColor.GRAY)));
        source.sendMessage(Component.text("/logs help", NamedTextColor.YELLOW)
                .append(Component.text("  - this help menu", NamedTextColor.GRAY)));
    }

    private boolean canReload(CommandSource source) {
        if (source.hasPermission(config.getReloadPermission())) return true;
        if (source.hasPermission(config.getAdminPermission())) return true;
        if (source instanceof Player player) {
            return permChecker.canReload(player);
        }
        return true; // console always allowed
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        // We return true always so unknown-command paths never trigger
        // a false "no permission" message. Actual permission checks
        // happen inside execute() for reload/debug.
        return true;
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            List<String> out = new ArrayList<>();
            out.add("reload");
            out.add("help");
            out.add("debug");
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            List<String> names = new ArrayList<>();
            for (Player p : server.getAllPlayers()) names.add(p.getUsername());
            return names;
        }
        return List.of();
    }
}
