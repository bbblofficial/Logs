package ir.muvixo.logs.velocity;

import com.velocitypowered.api.proxy.Player;
import org.slf4j.Logger;

/**
 * Central permission checker.
 *
 * A player can see the logs if:
 *   1. They are reported as OP by a backend Spigot server, OR
 *   2. They have the configured "see" permission, OR
 *   3. They have the configured "admin" permission.
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

        // 1) Backend OP
        if (opManager.isOp(player.getUniqueId())) return true;

        // 2) Explicit Velocity permissions (loaded from config)
        String seePerm = config.getSeePermission();
        String adminPerm = config.getAdminPermission();
        if (seePerm != null && !seePerm.isEmpty() && player.hasPermission(seePerm)) return true;
        if (adminPerm != null && !adminPerm.isEmpty() && player.hasPermission(adminPerm)) return true;

        return false;
    }

    public boolean canReload(Player player) {
        if (player == null) return false;
        String adminPerm = config.getAdminPermission();
        if (adminPerm != null && !adminPerm.isEmpty() && player.hasPermission(adminPerm)) return true;
        return opManager.isOp(player.getUniqueId());
    }

    public String explain(Player player) {
        String seePerm = config.getSeePermission();
        String adminPerm = config.getAdminPermission();
        StringBuilder sb = new StringBuilder();
        sb.append("Player: ").append(player.getUsername())
          .append(" (").append(player.getUniqueId()).append(")\n");
        sb.append("  tracked-as-op: ").append(opManager.isOp(player.getUniqueId())).append("\n");
        sb.append("  op-server: ").append(opManager.getOpServer(player.getUniqueId())).append("\n");
        sb.append("  has ").append(seePerm).append(": ")
          .append(player.hasPermission(seePerm)).append("\n");
        sb.append("  has ").append(adminPerm).append(": ")
          .append(player.hasPermission(adminPerm)).append("\n");
        sb.append("  RESULT: ").append(canSee(player) ? "CAN SEE LOGS" : "CANNOT SEE LOGS");
        return sb.toString();
    }
}