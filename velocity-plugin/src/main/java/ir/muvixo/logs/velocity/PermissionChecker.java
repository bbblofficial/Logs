package ir.muvixo.logs.velocity;

import com.velocitypowered.api.proxy.Player;
import org.slf4j.Logger;

/**
 * Central permission checker.
 *
 * A player is allowed to see the command logs and to run admin
 * subcommands if ANY of these is true:
 *
 *   1. They are reported as OP by a backend Spigot server
 *      (via OP_STATUS / CMD messages).
 *   2. They have the {@code velocitylogs.see} permission
 *      (granted by LuckPerms or any other permission plugin).
 *   3. They have the {@code velocitylogs.admin} permission.
 *
 * @author muvixo
 */
public class PermissionChecker {

    /** Permission node that grants log visibility. */
    public static final String PERM_SEE = "velocitylogs.see";

    /** Permission node that grants admin subcommands. */
    public static final String PERM_ADMIN = "velocitylogs.admin";

    private final Config config;
    private final OpPlayerManager opManager;
    private final Logger logger;

    public PermissionChecker(Config config, OpPlayerManager opManager, Logger logger) {
        this.config = config;
        this.opManager = opManager;
        this.logger = logger;
    }

    /**
     * Can this player SEE the command logs?
     * True if backend-OP, or has {@code velocitylogs.see} / {@code velocitylogs.admin}.
     */
    public boolean canSee(Player player) {
        if (player == null) return false;

        // 1) Backend OP
        if (opManager.isOp(player.getUniqueId())) return true;

        // 2) Explicit Velocity permission
        if (player.hasPermission(PERM_SEE))   return true;
        if (player.hasPermission(PERM_ADMIN)) return true;

        return false;
    }

    /**
     * Can this player run admin subcommands (reload / list / debug / status)?
     */
    public boolean canReload(Player player) {
        if (player == null) return false;
        if (player.hasPermission(PERM_ADMIN)) return true;
        return opManager.isOp(player.getUniqueId());
    }

    /**
     * Detailed explanation used by /logs debug <player>.
     */
    public String explain(Player player) {
        StringBuilder sb = new StringBuilder();
        sb.append("Player: ").append(player.getUsername())
          .append(" (").append(player.getUniqueId()).append(")\n");
        sb.append("  tracked-as-op: ").append(opManager.isOp(player.getUniqueId())).append("\n");
        sb.append("  op-server: ").append(opManager.getOpServer(player.getUniqueId())).append("\n");
        sb.append("  has ").append(PERM_SEE).append(": ")
          .append(player.hasPermission(PERM_SEE)).append("\n");
        sb.append("  has ").append(PERM_ADMIN).append(": ")
          .append(player.hasPermission(PERM_ADMIN)).append("\n");
        sb.append("  RESULT: ").append(canSee(player) ? "CAN SEE LOGS" : "CANNOT SEE LOGS");
        return sb.toString();
    }
}