package ir.muvixo.logs.velocity;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

/**
 * /logs - plugin info.
 *
 * @author muvixo
 */
public class LogsCommand implements SimpleCommand {

    private final ProxyServer server;
    private final Config config;

    public LogsCommand(ProxyServer server, Config config) {
        this.server = server;
        this.config = config;
    }

    @Override
    public void execute(Invocation invocation) {
        invocation.source().sendMessage(
                Component.text("VelocityLogs ", NamedTextColor.GOLD)
                        .append(Component.text("v1.0.0 ", NamedTextColor.YELLOW))
                        .append(Component.text("by muvixo", NamedTextColor.AQUA))
        );
        invocation.source().sendMessage(
                Component.text("Channel: ", NamedTextColor.GRAY)
                        .append(Component.text(config.getChannel(), NamedTextColor.WHITE))
        );
        invocation.source().sendMessage(
                Component.text("See permission: ", NamedTextColor.GRAY)
                        .append(Component.text(config.getSeePermission(), NamedTextColor.WHITE))
        );
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(config.getSeePermission());
    }

    @Override
    public List<String> suggest(Invocation invocation) { return List.of(); }
}
