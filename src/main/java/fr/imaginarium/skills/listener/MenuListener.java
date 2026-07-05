package fr.imaginarium.skills.listener;

import fr.imaginarium.skills.gui.SkillsMenu;
import fr.imaginarium.skills.gui.SkillsMenuHolder;
import fr.imaginarium.skills.manager.LevelManager;
import fr.imaginarium.skills.manager.PlayerDataManager;
import fr.imaginarium.skills.manager.SkillManager;
import fr.imaginarium.skills.model.PlayerProfile;
import fr.imaginarium.skills.model.Skill;
import fr.imaginarium.skills.util.Msg;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/** Gere les clics dans le menu des skills. */
public class MenuListener implements Listener {

    private final SkillManager skillManager;
    private final LevelManager levelManager;
    private final PlayerDataManager dataManager;

    public MenuListener(SkillManager skillManager, LevelManager levelManager, PlayerDataManager dataManager) {
        this.skillManager = skillManager;
        this.levelManager = levelManager;
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof SkillsMenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SkillsMenuHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        // Ignore les clics hors du menu (inventaire du joueur).
        if (event.getClickedInventory() != event.getInventory()) {
            return;
        }

        int slot = event.getSlot();
        if (slot == SkillsMenu.CLOSE_SLOT) {
            player.closeInventory();
            return;
        }

        Skill skill = skillManager.getSkillBySlot(slot);
        if (skill == null) {
            return;
        }

        PlayerProfile profile = dataManager.getProfile(player);
        int currentLevel = profile.getSkillLevel(skill.id());
        if (currentLevel >= skill.maxLevel()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f);
            Msg.send(player, "&cCe skill est deja au niveau maximum.");
            return;
        }
        if (profile.getSkillPoints() < skill.costPerLevel()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f);
            Msg.send(player, "&cIl vous faut &e" + skill.costPerLevel()
                    + " &cpoint(s) de skill pour ameliorer ce skill.");
            return;
        }

        profile.addSkillPoints(-skill.costPerLevel());
        profile.setSkillLevel(skill.id(), currentLevel + 1);
        skillManager.applyAll(player, profile);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
        Msg.send(player, skill.displayName() + " &7amene au niveau &e" + (currentLevel + 1) + "&7 !");

        SkillsMenu.refresh(event.getInventory(), player, profile, skillManager, levelManager);
    }
}
