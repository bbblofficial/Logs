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
    private boolean logOps;
    private List<String> blacklist;

    public Config(SpigotLogs plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        this.channel = cfg.getString("channel", "velocitylogs:main");
        this.logOps = cfg.getBoolean("log-ops", true);

        List<String> raw = cfg.getStringList("blacklist");
        if (raw == null) raw = new ArrayList<>();
        this.blacklist = raw.stream()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());
    }

    public String getChannel() { return channel; }
    public boolean isLogOps() { return logOps; }

    public boolean isBlacklisted(String command) {
        String base = command.split(" ", 2)[0].toLowerCase(Locale.ROOT);
        return blacklist.contains(base);
    }
}
