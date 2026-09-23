package ir.muvixo.logs.spigot;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Config wrapper for SpigotLogs.
 *
 * @author muvixo
 */
public class Config {

    private final SpigotLogs plugin;

    private String channel;
    private String serverName;
    private boolean logOps;
    private boolean reportOpStatus;
    private int opStatusIntervalMinutes;
    private List<String> blacklist;
    private List<String> ignoredPlayers;

    public Config(SpigotLogs plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        this.channel = cfg.getString("channel", "velocitylogs:main");
        this.logOps = cfg.getBoolean("log-ops", true);
        this.reportOpStatus = cfg.getBoolean("report-op-status", true);
        this.opStatusIntervalMinutes = cfg.getInt("op-status-interval-minutes", 5);

        String configured = cfg.getString("server-name", "");
        if (configured != null && !configured.trim().isEmpty()) {
            this.serverName = configured.trim();
        } else {
            String bukkit = plugin.getServer().getServerName();
            if (bukkit == null || bukkit.isEmpty()
                    || bukkit.equalsIgnoreCase("Unknown Server")) {
                this.serverName = "server-" + plugin.getServer().getPort();
            } else {
                this.serverName = bukkit;
            }
        }

        List<String> rawBlacklist = cfg.getStringList("blacklist");
        if (rawBlacklist == null) rawBlacklist = new ArrayList<>();
        this.blacklist = rawBlacklist.stream()
                .map(s -> s.toLowerCase(Locale.ROOT).trim())
                .collect(Collectors.toList());

        List<String> rawIgnored = cfg.getStringList("ignored-players");
        if (rawIgnored == null) rawIgnored = new ArrayList<>();
        this.ignoredPlayers = rawIgnored.stream()
                .map(s -> s.toLowerCase(Locale.ROOT).trim())
                .collect(Collectors.toList());
    }

    public String getChannel() { return channel; }
    public String getServerName() { return serverName; }
    public boolean isLogOps() { return logOps; }
    public boolean isReportOpStatus() { return reportOpStatus; }
    public int getOpStatusIntervalMinutes() { return opStatusIntervalMinutes; }

    public boolean isBlacklisted(String command) {
        String base = command.split(" ", 2)[0].toLowerCase(Locale.ROOT);
        return blacklist.contains(base);
    }

    public boolean isIgnoredPlayer(String playerName) {
        return ignoredPlayers.contains(playerName.toLowerCase(Locale.ROOT));
    }
}
