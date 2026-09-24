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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * /logs command with info, help, reload, debug, status, list.
 * The /logs op and /logs unop subcommands have been removed.
 * Only players who are actually OP on a backend (as reported by the
 * Spigot plugin) can see the logs.
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

        if (args.length == 0) { sendInfo(source); return; }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload":
                if (!canReload(source)) { noPerm(source); return; }
                config.load();
                source.sendMessage(ColorUtil.color(config.getReloadSuccessMessage()));
                logger.info("Config reloaded by {}", source);
                return;

            case "help":
                sendHelp(source);
                return;

            case "list": {
                if (!canReload(source)) { noPerm(source); return; }
                sendOpList(source);
                return;
            }

            case "debug":
            case "status": {
                if (!canReload(source)) { noPerm(source); return; }
                if (args.length < 2) {
                    source.sendMessage(Component.text("Usage: /logs " + sub + " <player>", NamedTextColor.RED));
                    return;
                }
                Optional<Player> opt = server.getPlayer(args[1]);
                if (opt.isEmpty()) {
                    source.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
                    return;
                }
                String report = permChecker.explain(opt.get());
                for (String line : report.split("\n")) {
                    source.sendMessage(Component.text(line, NamedTextColor.GRAY));
                }
                return;
            }

            default:
                sendInfo(source);
        }
    }

    private void sendInfo(CommandSource source) {
        source.sendMessage(Component.text("VelocityLogs ", NamedTextColor.GOLD)
                .append(Component.text("v2.0.0 ", NamedTextColor.YELLOW))
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
        row(source, "/logs", "show plugin info");
        row(source, "/logs help", "this help menu");
        row(source, "/logs reload", "reload config");
        row(source, "/logs debug <player>", "diagnose player");
        row(source, "/logs status <player>", "same as debug");
        row(source, "/logs list", "list tracked OPs");
    }

    private void row(CommandSource source, String cmd, String desc) {
        source.sendMessage(Component.text(cmd, NamedTextColor.YELLOW)
                .append(Component.text("  - " + desc, NamedTextColor.GRAY)));
    }

    private void sendOpList(CommandSource source) {
        Map<UUID, OpPlayerManager.OpRecord> ops = opManager.getOpPlayers();
        source.sendMessage(Component.text("--- Tracked OPs (" + ops.size() + ") ---", NamedTextColor.GOLD));
        if (ops.isEmpty()) {
            source.sendMessage(Component.text("  (none)", NamedTextColor.GRAY));
            return;
        }
        long now = System.currentTimeMillis();
        for (OpPlayerManager.OpRecord r : ops.values()) {
            long ageSec = (now - r.lastSeen) / 1000;
            String line = String.format("  %s | server=%s | lastSeen=%ds ago",
                    r.name, r.serverName, ageSec);
            source.sendMessage(Component.text(line, NamedTextColor.GRAY));
        }
    }

    private void noPerm(CommandSource source) {
        source.sendMessage(ColorUtil.color(config.getNoPermissionMessage()));
    }

    private boolean canReload(CommandSource source) {
        if (!(source instanceof Player player)) return true; // console always allowed
        if (player.hasPermission(config.getReloadPermission())) return true;
        if (player.hasPermission(config.getAdminPermission())) return true;
        return permChecker.canReload(player);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            return List.of("reload", "help", "debug", "status", "list");
        }
        String sub = args[0].toLowerCase();
        if (args.length == 2 && (sub.equals("debug") || sub.equals("status"))) {
            List<String> names = new ArrayList<>();
            for (Player p : server.getAllPlayers()) names.add(p.getUsername());
            return names;
        }
        return List.of();
    }
}