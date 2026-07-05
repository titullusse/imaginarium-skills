package fr.imaginarium.skills.listener;

import fr.imaginarium.skills.manager.LevelManager;
import org.bukkit.GameMode;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

/** Attribue de l'XP de plugin pour les kills et le minage. */
public class XpListener implements Listener {

    private static final String PLACED_METADATA = "imaskills_placed";

    private final JavaPlugin plugin;
    private final LevelManager levelManager;

    public XpListener(JavaPlugin plugin, LevelManager levelManager) {
        this.plugin = plugin;
        this.levelManager = levelManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        String key = event.getEntity().getType().name();
        long xp = plugin.getConfig().getLong("xp-sources.kills." + key, -1);
        if (xp < 0 && event.getEntity() instanceof Monster) {
            xp = plugin.getConfig().getLong("xp-sources.kills.defaut", 0);
        }
        if (xp > 0) {
            levelManager.addXp(killer, xp);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        // Marque les blocs poses par des joueurs pour empecher le farm pose/casse.
        // (Le marquage est perdu au redemarrage du serveur, ce qui reste acceptable.)
        if (plugin.getConfig().contains("xp-sources.blocs." + event.getBlock().getType().name())) {
            event.getBlock().setMetadata(PLACED_METADATA, new FixedMetadataValue(plugin, true));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        if (event.getBlock().hasMetadata(PLACED_METADATA)) {
            event.getBlock().removeMetadata(PLACED_METADATA, plugin);
            return;
        }
        long xp = plugin.getConfig().getLong("xp-sources.blocs." + event.getBlock().getType().name(), 0);
        if (xp > 0) {
            levelManager.addXp(player, xp);
        }
    }
}
