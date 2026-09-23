package ir.muvixo.logs.velocity;

import com.velocitypowered.api.proxy.Player;
import org.slf4j.Logger;

import java.util.List;
import java.util.Locale;

/**
 * Central permission checker.
 *
 * Order of checks:
 *   1. force-see-players list (from config)
 *   2. explicit see-permission
 *   3. admin permission
 *   4. extra see-permissions from config
 *   5. tracked-as-OP (from backend)
 *   6. wildcard permissions ("*", "velocity.*")
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

        // 1. force-see list
        if (config.isForceSee(player.getUsername())) return true;

        // 2. explicit see permission
        if (safeHas(player, config.getSeePermission())) return true;

        // 3. admin permission
        if (safeHas(player, config.getAdminPermission())) return true;

        // 4. extra permissions
        List<String> extras = config.getExtraSeePermissions();
        if (extras != null) {
            for (String perm : extras) {
                if (perm == null || perm.isEmpty()) continue;
                if (safeHas(player, perm)) return true;
            }
        }

        // 5. tracked as OP on a backend
        if (opManager.isOp(player.getUniqueId())) return true;

        // 6. wildcards
        if (safeHas(player, "*")) return true;
        if (safeHas(player, "velocity.*")) return true;

        return false;
    }

    private boolean safeHas(Player player, String permission) {
        if (permission == null || permission.isEmpty()) return false;
        try {
            return player.hasPermission(permission);
        } catch (Exception e) {
            if (config.isDebug() && logger != null) {
                logger.warn("Permission check failed for {} on {}: {}",
                        player.getUsername(), permission, e.getMessage());
            }
            return false;
        }
    }

    public String explain(Player player) {
        StringBuilder sb = new StringBuilder();
        sb.append("Player: ").append(player.getUsername()).append(" (").append(player.getUniqueId()).append(")\n");
        sb.append("  force-see: ").append(config.isForceSee(player.getUsername())).append("\n");
        sb.append("  see-permission (").append(config.getSeePermission()).append("): ")
          .append(safeHas(player, config.getSeePermission())).append("\n");
        sb.append("  admin-permission (").append(config.getAdminPermission()).append("): ")
          .append(safeHas(player, config.getAdminPermission())).append("\n");
        sb.append("  tracked-as-op: ").append(opManager.isOp(player.getUniqueId())).append("\n");
        sb.append("  op-server: ").append(opManager.getOpServer(player.getUniqueId())).append("\n");
        sb.append("  wildcard-*: ").append(safeHas(player, "*")).append("\n");
        sb.append("  wildcard-velocity.*: ").append(safeHas(player, "velocity.*")).append("\n");

        List<String> extras = config.getExtraSeePermissions();
        if (extras != null && !extras.isEmpty()) {
            sb.append("  extra-see-permissions:\n");
            for (String p : extras) {
                sb.append("    ").append(p).append(" -> ").append(safeHas(player, p)).append("\n");
            }
        }
        sb.append("  RESULT: ").append(canSee(player) ? "CAN SEE LOGS" : "CANNOT SEE LOGS");
        return sb.toString();
    }

    public boolean canReload(Player player) {
        if (safeHas(player, config.getReloadPermission())) return true;
        if (safeHas(player, config.getAdminPermission())) return true;
        if (opManager.isOp(player.getUniqueId())) return true;
        if (safeHas(player, "*")) return true;
        if (config.isForceSee(player.getUsername())) return true;
        return false;
    }
}
