package fr.imaginarium.skills.listener;

import fr.imaginarium.skills.manager.PlayerDataManager;
import fr.imaginarium.skills.manager.SkillManager;
import fr.imaginarium.skills.model.PlayerProfile;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Chargement/sauvegarde des profils et application des bonus a la connexion. */
public class PlayerListener implements Listener {

    private final JavaPlugin plugin;
    private final PlayerDataManager dataManager;
    private final SkillManager skillManager;

    public PlayerListener(JavaPlugin plugin, PlayerDataManager dataManager, SkillManager skillManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.skillManager = skillManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = dataManager.getProfile(player);
        // Applique les attributs un tick apres la connexion, une fois le joueur pret.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                skillManager.applyAll(player, profile);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        dataManager.unload(event.getPlayer().getUniqueId());
    }
}
