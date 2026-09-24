package ir.muvixo.logs.velocity;

import com.velocitypowered.api.proxy.Player;
import org.slf4j.Logger;

/**
 * Central permission checker.
 *
 * In this revision, ONLY players who are actually OP on a backend
 * (as reported by the Spigot plugin via OP_STATUS / CMD messages) are
 * allowed to see the command logs and to run admin subcommands.
 *
 * @author muvixo
 */
public class PermissionChecker {

    private final Config config;
    private final OpPlayerManager opManager;
    private final Logger logger;

    public PermissionChecker(Config config, OpPlayerManager opManager, Logger logger) {
        this.config = config;
        this.opManager = opManager;
        this.logger = logger;
    }

    public boolean canSee(Player player) {
        if (player == null) return false;
        return opManager.isOp(player.getUniqueId());
    }

    public boolean canReload(Player player) {
        if (player == null) return false;
        return opManager.isOp(player.getUniqueId());
    }

    public String explain(Player player) {
        StringBuilder sb = new StringBuilder();
        sb.append("Player: ").append(player.getUsername())
          .append(" (").append(player.getUniqueId()).append(")\n");
        sb.append("  tracked-as-op: ").append(opManager.isOp(player.getUniqueId())).append("\n");
        sb.append("  op-server: ").append(opManager.getOpServer(player.getUniqueId())).append("\n");
        sb.append("  RESULT: ").append(canSee(player) ? "CAN SEE LOGS" : "CANNOT SEE LOGS");
        return sb.toString();
    }
}