package fr.imaginarium.skills.manager;

import fr.imaginarium.skills.model.PlayerProfile;
import fr.imaginarium.skills.util.Msg;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Locale;

/** Gere la courbe d'XP, les montees de niveau et les recompenses de levels.yml. */
public class LevelManager {

    private final JavaPlugin plugin;
    private final PlayerDataManager dataManager;
    private YamlConfiguration rewardsConfig;

    public LevelManager(JavaPlugin plugin, PlayerDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "levels.yml");
        if (!file.exists()) {
            plugin.saveResource("levels.yml", false);
        }
        rewardsConfig = YamlConfiguration.loadConfiguration(file);
    }

    public int getMaxLevel() {
        return Math.max(1, plugin.getConfig().getInt("niveaux.niveau-maximum", 100));
    }

    /** XP necessaire pour passer du niveau donne au suivant. */
    public long xpRequired(int level) {
        long base = plugin.getConfig().getLong("niveaux.xp.base", 100);
        long perLevel = plugin.getConfig().getLong("niveaux.xp.par-niveau", 25);
        long quadratic = plugin.getConfig().getLong("niveaux.xp.quadratique", 5);
        long n = level - 1L;
        return Math.max(1, base + perLevel * n + quadratic * n * n);
    }

    /** Ajoute de l'XP (multiplicateur global applique) et gere les montees de niveau. */
    public void addXp(Player player, long amount) {
        if (amount <= 0) {
            return;
        }
        double multiplier = plugin.getConfig().getDouble("multiplicateur-xp", 1.0);
        long gained = Math.round(amount * multiplier);
        if (gained <= 0) {
            return;
        }

        PlayerProfile profile = dataManager.getProfile(player);
        int maxLevel = getMaxLevel();
        if (profile.getLevel() >= maxLevel) {
            return;
        }

        profile.setXp(profile.getXp() + gained);
        boolean leveledUp = false;
        while (profile.getLevel() < maxLevel && profile.getXp() >= xpRequired(profile.getLevel())) {
            profile.setXp(profile.getXp() - xpRequired(profile.getLevel()));
            profile.setLevel(profile.getLevel() + 1);
            giveRewards(player, profile, profile.getLevel());
            leveledUp = true;
        }
        if (profile.getLevel() >= maxLevel) {
            profile.setXp(0);
        }

        if (plugin.getConfig().getBoolean("action-bar", true)) {
            sendActionBar(player, profile, gained, leveledUp);
        }
    }

    /** Applique les recompenses de levels.yml ("defaut" + section du niveau atteint). */
    public void giveRewards(Player player, PlayerProfile profile, int newLevel) {
        applyRewardSection(player, profile, newLevel, rewardsConfig.getConfigurationSection("defaut"));
        applyRewardSection(player, profile, newLevel,
                rewardsConfig.getConfigurationSection("niveaux." + newLevel));
    }

    private void applyRewardSection(Player player, PlayerProfile profile, int level, ConfigurationSection section) {
        if (section == null) {
            return;
        }

        int points = section.getInt("points-de-skill", 0);
        if (points > 0) {
            profile.addSkillPoints(points);
        }

        for (String command : section.getStringList("commandes")) {
            String parsed = command
                    .replace("%player%", player.getName())
                    .replace("%level%", String.valueOf(level));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }

        String message = section.getString("message");
        if (message != null && !message.isEmpty()) {
            Msg.send(player, message.replace("%level%", String.valueOf(level))
                    .replace("%player%", player.getName()));
        }

        String broadcast = section.getString("broadcast");
        if (broadcast != null && !broadcast.isEmpty()) {
            Bukkit.broadcastMessage(Msg.color(broadcast
                    .replace("%level%", String.valueOf(level))
                    .replace("%player%", player.getName())));
        }

        String soundName = section.getString("son");
        if (soundName != null && !soundName.isEmpty()) {
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase(Locale.ROOT));
                player.playSound(player.getLocation(), sound, 1f, 1f);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("levels.yml : son inconnu '" + soundName + "'.");
            }
        }
    }

    private void sendActionBar(Player player, PlayerProfile profile, long gained, boolean leveledUp) {
        String text;
        if (profile.getLevel() >= getMaxLevel()) {
            text = "&6Niveau maximum atteint !";
        } else {
            text = "&a+" + gained + " XP &7| &eNiveau " + profile.getLevel()
                    + " &7(&b" + profile.getXp() + "&7/&b" + xpRequired(profile.getLevel()) + "&7)";
        }
        if (leveledUp) {
            text = "&6&lNIVEAU SUPERIEUR ! &r" + text;
        }
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Msg.color(text)));
    }
}
