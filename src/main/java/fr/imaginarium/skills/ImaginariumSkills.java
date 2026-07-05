package fr.imaginarium.skills;

import fr.imaginarium.skills.command.ItemAttrCommand;
import fr.imaginarium.skills.command.SkillsCommand;
import fr.imaginarium.skills.listener.MenuListener;
import fr.imaginarium.skills.listener.PlayerListener;
import fr.imaginarium.skills.listener.XpListener;
import fr.imaginarium.skills.manager.LevelManager;
import fr.imaginarium.skills.manager.PlayerDataManager;
import fr.imaginarium.skills.manager.SkillManager;
import fr.imaginarium.skills.util.Msg;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public class ImaginariumSkills extends JavaPlugin {

    private PlayerDataManager playerDataManager;
    private SkillManager skillManager;
    private LevelManager levelManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Msg.setPrefix(getConfig().getString("prefix", "&8[&bSkills&8] &7"));

        playerDataManager = new PlayerDataManager(this);
        skillManager = new SkillManager(this);
        skillManager.load(loadYaml("skills.yml"));
        levelManager = new LevelManager(this, playerDataManager);

        getServer().getPluginManager().registerEvents(
                new PlayerListener(this, playerDataManager, skillManager), this);
        getServer().getPluginManager().registerEvents(
                new XpListener(this, levelManager), this);
        getServer().getPluginManager().registerEvents(
                new MenuListener(skillManager, levelManager, playerDataManager), this);

        SkillsCommand skillsCommand = new SkillsCommand(this);
        Objects.requireNonNull(getCommand("skills")).setExecutor(skillsCommand);
        Objects.requireNonNull(getCommand("skills")).setTabCompleter(skillsCommand);
        ItemAttrCommand itemAttrCommand = new ItemAttrCommand(this);
        Objects.requireNonNull(getCommand("itemattr")).setExecutor(itemAttrCommand);
        Objects.requireNonNull(getCommand("itemattr")).setTabCompleter(itemAttrCommand);

        long saveInterval = 20L * 60L * Math.max(1, getConfig().getInt("sauvegarde-auto-minutes", 5));
        getServer().getScheduler().runTaskTimer(this,
                () -> playerDataManager.saveAll(), saveInterval, saveInterval);

        // Recharge les profils des joueurs deja en ligne (cas d'un /reload).
        for (Player player : getServer().getOnlinePlayers()) {
            skillManager.applyAll(player, playerDataManager.getProfile(player));
        }

        getLogger().info("ImaginariumSkills active !");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveAll();
        }
        getLogger().info("ImaginariumSkills desactive, donnees sauvegardees.");
    }

    /** Recharge config.yml, skills.yml et levels.yml puis re-applique les bonus. */
    public void reloadAll() {
        reloadConfig();
        Msg.setPrefix(getConfig().getString("prefix", "&8[&bSkills&8] &7"));
        skillManager.load(loadYaml("skills.yml"));
        levelManager.reload();
        for (Player player : getServer().getOnlinePlayers()) {
            skillManager.applyAll(player, playerDataManager.getProfile(player));
        }
    }

    private YamlConfiguration loadYaml(String fileName) {
        File file = new File(getDataFolder(), fileName);
        if (!file.exists()) {
            saveResource(fileName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public SkillManager getSkillManager() {
        return skillManager;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }
}
