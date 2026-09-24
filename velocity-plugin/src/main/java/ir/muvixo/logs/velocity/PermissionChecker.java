package ir.muvixo.logs.velocity;

import com.velocitypowered.api.proxy.Player;
import org.slf4j.Logger;

import java.util.List;

/**
 * Central permission checker.
 *
 * In this revision, ONLY players who are actually OP on a backend
 * (as reported by the Spigot plugin via OP_STATUS / CMD messages) are
 * allowed to see the command logs. Permission nodes and the
 * force-see-players list are intentionally ignored so that OP status
 * is the single source of truth.
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

    /**
     * A player can see logs ONLY if they are tracked as OP on a backend.
     */
    public boolean canSee(Player player) {
        if (player == null) return false;
        return opManager.isOp(player.getUniqueId());
    }

    /**
     * A player can use admin-only subcommands (/logs reload, /logs list,
     * /logs debug, /logs status) if they are a tracked backend OP.
     */
    public boolean canReload(Player player) {
        if (player == null) return false;
        return opManager.isOp(player.getUniqueId());
    }

    /**
     * Human-readable diagnostic report for /logs debug <player>.
     */
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