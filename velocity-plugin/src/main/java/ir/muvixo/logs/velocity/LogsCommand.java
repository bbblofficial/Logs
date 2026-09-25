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
 * /logs command with a permission-aware help menu.
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

    // ============================================================
    //  EXECUTE
    // ============================================================
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) { sendHelp(source); return; }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "help":
            case "?":
                sendHelp(source);
                return;

            case "info":
                if (!canUse(source)) { noPerm(source); return; }
                sendInfo(source);
                return;

            case "creator":
            case "author":
                if (!canUse(source)) { noPerm(source); return; }
                sendCreator(source);
                return;

            case "reload":
                if (!canReload(source)) { noPerm(source); return; }
                config.load();
                source.sendMessage(ColorUtil.color(config.getReloadSuccessMessage()));
                logger.info("Config reloaded by {}", source);
                return;

            case "list":
                if (!canReload(source)) { noPerm(source); return; }
                sendOpList(source);
                return;

            case "debug":
            case "status": {
                if (!canReload(source)) { noPerm(source); return; }
                if (args.length < 2) {
                    source.sendMessage(Component.text(
                            "Usage: /logs " + sub + " <player>", NamedTextColor.RED));
                    return;
                }
                Optional<Player> opt = server.getPlayer(args[1]);
                if (opt.isEmpty()) {
                    source.sendMessage(Component.text(
                            "Player not found: " + args[1], NamedTextColor.RED));
                    return;
                }
                String report = permChecker.explain(opt.get());
                for (String line : report.split("\n")) {
                    source.sendMessage(Component.text(line, NamedTextColor.GRAY));
                }
                return;
            }

            default:
                sendHelp(source);
        }
    }

    // ============================================================
    //  PERMISSION HELPERS
    // ============================================================
    private boolean canUse(CommandSource source) {
        if (!(source instanceof Player)) return true; // console
        return permChecker.canSee((Player) source);
    }

    private boolean canReload(CommandSource source) {
        if (!(source instanceof Player)) return true; // console
        return permChecker.canReload((Player) source);
    }

    // ============================================================
    //  HELP MENU — permission-aware
    // ============================================================
    private void sendHelp(CommandSource source) {

        // ---- Determine admin status once ----
        boolean isAdmin = canReload(source);   // "admin" = can run reload/list/debug
        boolean isUser  = canUse(source);      // can run info/creator

        // ---------------- HEADER ----------------
        header(source, "VelocityLogs - Velocity Commands");

        // ---------------- GENERAL COMMANDS ----------------
        source.sendMessage(Component.text("General Commands", NamedTextColor.YELLOW));

        // /logs help — always visible
        row(source, "/logs help", "Show this help");

        if (isUser) {
            row(source, "/logs info", "Show plugin info");
            row(source, "/logs creator", "Show plugin credits");
        }

        // ---------------- ADMIN COMMANDS ----------------
        if (isAdmin) {
            separator(source);
            source.sendMessage(Component.text("Admin Commands", NamedTextColor.RED));

            row(source, "/logs reload", "Reload config");
            row(source, "/logs list", "List tracked OPs");
            row(source, "/logs debug <player>", "Diagnose player");
            row(source, "/logs status <player>", "Same as debug");
        }

        // ---------------- FOOTER ----------------
        separator(source);
        source.sendMessage(Component.text("Channel: ", NamedTextColor.GRAY)
                .append(Component.text(config.getChannel(), NamedTextColor.YELLOW)));
        source.sendMessage(Component.text("Tracked OPs: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(opManager.getOpCount()), NamedTextColor.YELLOW)));
        separator(source);
    }

    // ============================================================
    //  SUB-OUTPUT
    // ============================================================
    private void sendInfo(CommandSource source) {
        source.sendMessage(Component.text("VelocityLogs ", NamedTextColor.GOLD)
                .append(Component.text("v2.0.0 ", NamedTextColor.YELLOW))
                .append(Component.text("by muvixo", NamedTextColor.AQUA)));
        source.sendMessage(Component.text("Channel: ", NamedTextColor.GRAY)
                .append(Component.text(config.getChannel(), NamedTextColor.WHITE)));
        source.sendMessage(Component.text("Tracked OPs: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(opManager.getOpCount()), NamedTextColor.WHITE)));
    }

    private void sendCreator(CommandSource source) {
        source.sendMessage(Component.text("VelocityLogs ", NamedTextColor.GOLD)
                .append(Component.text("v2.0.0", NamedTextColor.YELLOW)));
        source.sendMessage(Component.text("Author: ", NamedTextColor.GRAY)
                .append(Component.text("muvixo", NamedTextColor.AQUA)));
        source.sendMessage(Component.text("API: ", NamedTextColor.GRAY)
                .append(Component.text("Velocity 3.x", NamedTextColor.WHITE)));
    }

    private void sendOpList(CommandSource source) {
        Map<UUID, OpPlayerManager.OpRecord> ops = opManager.getOpPlayers();
        header(source, "Tracked OPs (" + ops.size() + ")");

        if (ops.isEmpty()) {
            source.sendMessage(Component.text("  (none)", NamedTextColor.GRAY));
            separator(source);
            return;
        }

        long now = System.currentTimeMillis();
        for (OpPlayerManager.OpRecord r : ops.values()) {
            long ageSec = (now - r.lastSeen) / 1000;
            String line = String.format("  %s | server=%s | lastSeen=%ds ago",
                    r.name, r.serverName, ageSec);
            source.sendMessage(Component.text(line, NamedTextColor.GRAY));
        }
        separator(source);
    }

    // ============================================================
    //  FORMATTING HELPERS
    // ============================================================
    private void header(CommandSource source, String title) {
        separator(source);
        source.sendMessage(Component.text(title, NamedTextColor.GOLD));
        separator(source);
    }

    private void separator(CommandSource source) {
        source.sendMessage(Component.text("----------------------------------",
                NamedTextColor.DARK_GRAY));
    }

    private void row(CommandSource source, String cmd, String desc) {
        source.sendMessage(Component.text("  " + cmd, NamedTextColor.GOLD)
                .append(Component.text(" - " + desc, NamedTextColor.GRAY)));
    }

    private void noPerm(CommandSource source) {
        source.sendMessage(ColorUtil.color(config.getNoPermissionMessage()));
    }

    // ============================================================
    //  TAB COMPLETE — permission-aware
    // ============================================================
    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        List<String> out = new ArrayList<>();

        if (args.length <= 1) {
            List<String> subs = new ArrayList<>();
            subs.add("help");

            if (canUse(source)) {
                subs.add("info");
                subs.add("creator");
            }
            if (canReload(source)) {
                subs.add("reload");
                subs.add("list");
                subs.add("debug");
                subs.add("status");
            }

            String partial = args.length == 0 ? "" : args[0].toLowerCase();
            for (String s : subs) if (s.startsWith(partial)) out.add(s);
            return out;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if ((sub.equals("debug") || sub.equals("status")) && canReload(source)) {
                String partial = args[1].toLowerCase();
                for (Player p : server.getAllPlayers()) {
                    if (p.getUsername().toLowerCase().startsWith(partial)) {
                        out.add(p.getUsername());
                    }
                }
            }
        }
        return out;
    }
}