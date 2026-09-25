package ir.muvixo.logs.spigot;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

/**
 * /vlogs — Spigot side command with a permission-aware help menu.
 *
 * @author muvixo
 */
public class VLogsCommand implements CommandExecutor, TabCompleter {

    private final SpigotLogs plugin;
    private final Config config;

    public VLogsCommand(SpigotLogs plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
    }

    // ============================================================
    //  COMMAND DISPATCH
    // ============================================================

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("help") || sub.equals("?")) {
            sendHelp(sender);
            return true;
        }

        if (sub.equals("creator") || sub.equals("author")) {
            if (!sender.hasPermission(getPerm("creator", "velocitylogs.creator"))) {
                noPerm(sender); return true;
            }
            sendCreator(sender);
            return true;
        }

        if (sub.equals("info")) {
            if (!sender.hasPermission(getPerm("use", "velocitylogs.use"))) {
                noPerm(sender); return true;
            }
            sendInfo(sender);
            return true;
        }

        if (sub.equals("status")) {
            if (!sender.hasPermission(getPerm("status", "velocitylogs.status"))) {
                noPerm(sender); return true;
            }
            sendStatus(sender);
            return true;
        }

        if (sub.equals("reload")) {
            if (!sender.hasPermission(getPerm("reload", "velocitylogs.reload"))) {
                noPerm(sender); return true;
            }
            plugin.reloadAll();
            sender.sendMessage(colorize("&a[OK] Config reloaded."));
            return true;
        }

        if (sub.equals("toggle")) {
            if (!sender.hasPermission(getPerm("toggle", "velocitylogs.toggle"))) {
                noPerm(sender); return true;
            }
            boolean now = !plugin.isForwardingEnabled();
            plugin.setForwardingEnabled(now);
            sender.sendMessage(colorize(now
                    ? "&aCommand forwarding &lENABLED&a."
                    : "&cCommand forwarding &lDISABLED&c."));
            return true;
        }

        if (sub.equals("debug")) {
            if (!sender.hasPermission(getPerm("debug", "velocitylogs.debug"))) {
                noPerm(sender); return true;
            }
            boolean now = !plugin.isDebugEnabled();
            plugin.setDebugEnabled(now);
            sender.sendMessage(colorize(now
                    ? "&aDebug mode &lENABLED&a."
                    : "&cDebug mode &lDISABLED&c."));
            return true;
        }

        sender.sendMessage(colorize("&cUnknown subcommand. Use &e/vlogs help"));
        return true;
    }

    // ============================================================
    //  PERMISSION HELPER
    //  Reads "permissions.<action>" from config.yml.
    // ============================================================
    private String getPerm(String action, String defaultPerm) {
        String value = plugin.getConfig().getString("permissions." + action);
        if (value == null || value.trim().isEmpty()) {
            return defaultPerm;
        }
        return value.trim();
    }

    // ============================================================
    //  HELP MENU — permission-aware
    // ============================================================
    private void sendHelp(CommandSender sender) {

        // ---- Load all permission nodes ----
        String permUse     = getPerm("use",     "velocitylogs.use");
        String permCreator = getPerm("creator", "velocitylogs.creator");

        String permAdmin   = getPerm("admin",   "velocitylogs.admin");
        String permReload  = getPerm("reload",  "velocitylogs.reload");
        String permStatus  = getPerm("status",  "velocitylogs.status");
        String permToggle  = getPerm("toggle",  "velocitylogs.toggle");
        String permDebug   = getPerm("debug",   "velocitylogs.debug");

        // ---- isAdmin = OR of all admin perms ----
        boolean isAdmin =
                sender.hasPermission(permAdmin)
             || sender.hasPermission(permReload)
             || sender.hasPermission(permStatus)
             || sender.hasPermission(permToggle)
             || sender.hasPermission(permDebug);

        // ---------------- HEADER ----------------
        sender.sendMessage(colorize("&8&m----------------------------------"));
        sender.sendMessage(colorize("&6&lVelocityLogs &7- &eSpigot Commands"));
        sender.sendMessage(colorize("&8&m----------------------------------"));

        // ---------------- GENERAL COMMANDS ----------------
        sender.sendMessage(colorize("&e&lGeneral Commands"));

        // /vlogs help — always visible
        sender.sendMessage(colorize("  &6/vlogs help &8- &7Show this help"));

        // /vlogs creator
        if (sender.hasPermission(permCreator)) {
            sender.sendMessage(colorize("  &6/vlogs creator &8- &7Show plugin credits"));
        }

        // /vlogs info
        if (sender.hasPermission(permUse)) {
            sender.sendMessage(colorize("  &6/vlogs info &8- &7Show plugin info"));
        }

        // ---------------- ADMIN COMMANDS ----------------
        if (isAdmin) {
            sender.sendMessage(colorize("&8&m----------------------------------"));
            sender.sendMessage(colorize("&c&lAdmin Commands"));

            if (sender.hasPermission(permStatus)) {
                sender.sendMessage(colorize("  &6/vlogs status &8- &7Show plugin status"));
            }
            if (sender.hasPermission(permToggle)) {
                sender.sendMessage(colorize("  &6/vlogs toggle &8- &7Toggle command forwarding"));
            }
            if (sender.hasPermission(permDebug)) {
                sender.sendMessage(colorize("  &6/vlogs debug &8- &7Toggle debug mode"));
            }
            if (sender.hasPermission(permReload)) {
                sender.sendMessage(colorize("  &6/vlogs reload &8- &7Reload configuration"));
            }
        }

        // ---------------- FOOTER ----------------
        sender.sendMessage(colorize("&8&m----------------------------------"));
        sender.sendMessage(colorize("&7Channel: &e" + config.getChannel()
                + " &8| &7Server: &e" + config.getServerName()));
        sender.sendMessage(colorize("&8&m----------------------------------"));
    }

    // ============================================================
    //  SUB-OUTPUT
    // ============================================================
    private void sendCreator(CommandSender sender) {
        sender.sendMessage(colorize("&8&m----------------------------------"));
        sender.sendMessage(colorize("&6&lVelocityLogs-Spigot"));
        sender.sendMessage(colorize("&7Author: &bmuvixo"));
        sender.sendMessage(colorize("&7Version: &f" + plugin.getDescription().getVersion()));
        sender.sendMessage(colorize("&7API: &fSpigot 1.8.8"));
        sender.sendMessage(colorize("&8&m----------------------------------"));
    }

    private void sendInfo(CommandSender sender) {
        sender.sendMessage(colorize("&8&m----------------------------------"));
        sender.sendMessage(colorize("&6&lVelocityLogs &7- &eInfo"));
        sender.sendMessage(colorize("&7Channel: &e" + config.getChannel()));
        sender.sendMessage(colorize("&7Server name: &e" + config.getServerName()));
        sender.sendMessage(colorize("&7Log OPs: &e" + config.isLogOps()));
        sender.sendMessage(colorize("&7Report OP status: &e" + config.isReportOpStatus()));
        sender.sendMessage(colorize("&8&m----------------------------------"));
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage(colorize("&8&m----------------------------------"));
        sender.sendMessage(colorize("&6&lVelocityLogs &7- &eStatus"));
        sender.sendMessage(colorize("&7Forwarding: "
                + (plugin.isForwardingEnabled() ? "&aENABLED" : "&cDISABLED")));
        sender.sendMessage(colorize("&7Debug: "
                + (plugin.isDebugEnabled() ? "&aON" : "&cOFF")));
        sender.sendMessage(colorize("&7Channel: &e" + config.getChannel()));
        sender.sendMessage(colorize("&7Server name: &e" + config.getServerName()));
        sender.sendMessage(colorize("&7Log OPs: &e" + config.isLogOps()));
        sender.sendMessage(colorize("&8&m----------------------------------"));
    }

    private void noPerm(CommandSender sender) {
        sender.sendMessage(colorize("&cYou do not have permission."));
    }

    // ============================================================
    //  TAB COMPLETE — permission-aware
    // ============================================================
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        List<String> out = new ArrayList<String>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<String>();
            subs.add("help");

            if (sender.hasPermission(getPerm("creator", "velocitylogs.creator"))) subs.add("creator");
            if (sender.hasPermission(getPerm("use",     "velocitylogs.use")))     subs.add("info");
            if (sender.hasPermission(getPerm("status",  "velocitylogs.status")))  subs.add("status");
            if (sender.hasPermission(getPerm("toggle",  "velocitylogs.toggle")))  subs.add("toggle");
            if (sender.hasPermission(getPerm("debug",   "velocitylogs.debug")))   subs.add("debug");
            if (sender.hasPermission(getPerm("reload",  "velocitylogs.reload")))  subs.add("reload");

            String partial = args[0].toLowerCase();
            for (String s : subs) if (s.startsWith(partial)) out.add(s);
        }
        return out;
    }

    private String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}