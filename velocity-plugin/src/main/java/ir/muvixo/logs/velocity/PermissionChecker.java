package ir.muvixo.logs.velocity;

import com.velocitypowered.api.proxy.Player;
import org.slf4j.Logger;

import java.util.List;

/**
 * Central place to decide whether a player should see logs.
 *
 * Checks (in order):
 *   1. Explicit see-permission ("velocitylogs.see")
 *   2. Admin permission ("velocitylogs.admin")
 *   3. Extra see-permissions from config (e.g. "*", "velocitylogs.*")
 *   4. Tracked as OP on a backend server
 *   5. Proxy-level wildcard permissions: "*" or "velocity.*"
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
     * Returns true if the given player should receive command logs.
     */
    public boolean canSee(Player player) {
        // 1. explicit see permission
        if (player.hasPermission(config.getSeePermission())) return true;

        // 2. admin permission
        if (player.hasPermission(config.getAdminPermission())) return true;

        // 3. extra see permissions from config
        List<String> extras = config.getExtraSeePermissions();
        if (extras != null) {
            for (String perm : extras) {
                if (perm == null || perm.isEmpty()) continue;
                try {
                    if (player.hasPermission(perm)) return true;
                } catch (Exception ignored) {}
            }
        }

        // 4. tracked as OP on a backend server
        if (opManager.isOp(player.getUniqueId())) return true;

        // 5. proxy-level wildcard permissions
        try {
            if (player.hasPermission("*")) return true;
            if (player.hasPermission("velocity.*")) return true;
        } catch (Exception ignored) {}

        return false;
    }

    /**
     * Explains WHY a player can/can't see logs. Used by /logs debug.
     */
    public String explain(Player player) {
        StringBuilder sb = new StringBuilder();
        sb.append("Player: ").append(player.getUsername()).append("\n");
        sb.append("  see-permission (").append(config.getSeePermission()).append("): ")
          .append(player.hasPermission(config.getSeePermission())).append("\n");
        sb.append("  admin-permission (").append(config.getAdminPermission()).append("): ")
          .append(player.hasPermission(config.getAdminPermission())).append("\n");
        sb.append("  tracked-as-op: ").append(opManager.isOp(player.getUniqueId())).append("\n");
        try {
            sb.append("  wildcard-*: ").append(player.hasPermission("*")).append("\n");
        } catch (Exception ignored) {}
        List<String> extras = config.getExtraSeePermissions();
        if (extras != null && !extras.isEmpty()) {
            sb.append("  extra-see-permissions:\n");
            for (String p : extras) {
                try {
                    sb.append("    ").append(p).append(" -> ")
                      .append(player.hasPermission(p)).append("\n");
                } catch (Exception ignored) {}
            }
        }
        sb.append("  RESULT: ").append(canSee(player) ? "CAN SEE LOGS" : "CANNOT SEE LOGS");
        return sb.toString();
    }

    /**
     * Can the player reload the config?
     */
    public boolean canReload(Player player) {
        if (player.hasPermission(config.getReloadPermission())) return true;
        if (player.hasPermission(config.getAdminPermission())) return true;
        if (opManager.isOp(player.getUniqueId())) return true;
        try {
            if (player.hasPermission("*")) return true;
        } catch (Exception ignored) {}
        return false;
    }
}
