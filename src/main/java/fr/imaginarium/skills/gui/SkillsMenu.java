package fr.imaginarium.skills.gui;

import fr.imaginarium.skills.manager.LevelManager;
import fr.imaginarium.skills.manager.SkillManager;
import fr.imaginarium.skills.model.PlayerProfile;
import fr.imaginarium.skills.model.Skill;
import fr.imaginarium.skills.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/** Construction et rafraichissement du menu des skills. */
public final class SkillsMenu {

    public static final int SIZE = 27;
    public static final int INFO_SLOT = 4;
    public static final int CLOSE_SLOT = 22;

    private SkillsMenu() {
    }

    public static void open(Player player, PlayerProfile profile,
                            SkillManager skillManager, LevelManager levelManager) {
        SkillsMenuHolder holder = new SkillsMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, SIZE, Msg.color("&8Menu des skills"));
        holder.setInventory(inventory);
        fill(inventory, player, profile, skillManager, levelManager);
        player.openInventory(inventory);
    }

    public static void refresh(Inventory inventory, Player player, PlayerProfile profile,
                               SkillManager skillManager, LevelManager levelManager) {
        fill(inventory, player, profile, skillManager, levelManager);
    }

    private static void fill(Inventory inventory, Player player, PlayerProfile profile,
                             SkillManager skillManager, LevelManager levelManager) {
        inventory.clear();

        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        inventory.setItem(INFO_SLOT, buildInfoItem(player, profile, skillManager, levelManager));

        for (Skill skill : skillManager.getSkills()) {
            if (skill.slot() >= 0 && skill.slot() < SIZE) {
                inventory.setItem(skill.slot(), buildSkillItem(skill, profile));
            }
        }

        inventory.setItem(CLOSE_SLOT, item(Material.BARRIER, Msg.color("&cFermer"), List.of()));
    }

    private static ItemStack buildInfoItem(Player player, PlayerProfile profile,
                                           SkillManager skillManager, LevelManager levelManager) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(Msg.color("&e" + player.getName()));
            List<String> lore = new ArrayList<>();
            lore.add(Msg.color("&7Niveau : &e" + profile.getLevel() + "&7/&e" + levelManager.getMaxLevel()));
            if (profile.getLevel() < levelManager.getMaxLevel()) {
                long required = levelManager.xpRequired(profile.getLevel());
                lore.add(Msg.color("&7XP : &b" + profile.getXp() + "&7/&b" + required));
                lore.add(Msg.color("&8" + progressBar(profile.getXp(), required)));
            } else {
                lore.add(Msg.color("&6Niveau maximum atteint !"));
            }
            lore.add(Msg.color("&7Points de skill : &a" + profile.getSkillPoints()));

            // Liste de toutes les ameliorations de competences du joueur.
            lore.add("");
            lore.add(Msg.color("&e&lVos ameliorations :"));
            boolean hasAny = false;
            for (Skill skill : skillManager.getSkills()) {
                int skillLevel = profile.getSkillLevel(skill.id());
                if (skillLevel > 0) {
                    lore.add(Msg.color("&8- " + skill.displayName() + " &7: niveau &e"
                            + skillLevel + "&7/&e" + skill.maxLevel()));
                    hasAny = true;
                }
            }
            if (!hasAny) {
                lore.add(Msg.color("&7Aucune amelioration pour l'instant."));
            }

            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    private static ItemStack buildSkillItem(Skill skill, PlayerProfile profile) {
        int level = profile.getSkillLevel(skill.id());
        List<String> lore = new ArrayList<>(skill.description());
        lore.add("");
        lore.add(Msg.color("&7Niveau : &e" + level + "&7/&e" + skill.maxLevel()));
        lore.add(Msg.color("&7Cout : &a" + skill.costPerLevel() + " point(s)"));
        lore.add("");
        if (level >= skill.maxLevel()) {
            lore.add(Msg.color("&6Niveau maximum !"));
        } else if (profile.getSkillPoints() >= skill.costPerLevel()) {
            lore.add(Msg.color("&aCliquez pour ameliorer !"));
        } else {
            lore.add(Msg.color("&cPas assez de points de skill."));
        }
        return item(skill.icon(), skill.displayName(), lore);
    }

    private static String progressBar(long current, long required) {
        int bars = 20;
        int filled = required <= 0 ? bars : (int) Math.min(bars, (current * bars) / required);
        return "&a" + "|".repeat(filled) + "&7" + "|".repeat(bars - filled);
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
