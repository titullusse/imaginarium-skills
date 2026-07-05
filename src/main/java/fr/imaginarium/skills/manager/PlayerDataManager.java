package fr.imaginarium.skills.manager;

import fr.imaginarium.skills.model.PlayerProfile;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Sauvegarde et chargement des profils joueurs (un fichier YAML par joueur). */
public class PlayerDataManager {

    private final JavaPlugin plugin;
    private final File dataFolder;
    private final Map<UUID, PlayerProfile> cache = new ConcurrentHashMap<>();

    public PlayerDataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Impossible de creer le dossier playerdata.");
        }
    }

    public PlayerProfile getProfile(Player player) {
        return getProfile(player.getUniqueId());
    }

    public PlayerProfile getProfile(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::load);
    }

    private PlayerProfile load(UUID uuid) {
        PlayerProfile profile = new PlayerProfile(uuid);
        File file = fileOf(uuid);
        if (file.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            profile.setLevel(yaml.getInt("niveau", 1));
            profile.setXp(yaml.getLong("xp", 0));
            profile.setSkillPoints(yaml.getInt("points", 0));
            ConfigurationSection skillsSection = yaml.getConfigurationSection("skills");
            if (skillsSection != null) {
                for (String id : skillsSection.getKeys(false)) {
                    profile.setSkillLevel(id, skillsSection.getInt(id));
                }
            }
        } else {
            // Nouveau joueur : on lui attribue les points de skill de depart
            // et on sauvegarde tout de suite pour ne pas les redonner a la prochaine connexion.
            int startingPoints = plugin.getConfig().getInt("points-de-depart", 0);
            if (startingPoints > 0) {
                profile.setSkillPoints(startingPoints);
            }
            save(profile);
        }
        return profile;
    }

    public void save(PlayerProfile profile) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("niveau", profile.getLevel());
        yaml.set("xp", profile.getXp());
        yaml.set("points", profile.getSkillPoints());
        for (Map.Entry<String, Integer> entry : profile.getSkillLevels().entrySet()) {
            yaml.set("skills." + entry.getKey(), entry.getValue());
        }
        try {
            yaml.save(fileOf(profile.getUuid()));
        } catch (IOException ex) {
            plugin.getLogger().severe("Echec de sauvegarde du profil " + profile.getUuid() + " : " + ex.getMessage());
        }
    }

    public void unload(UUID uuid) {
        PlayerProfile profile = cache.remove(uuid);
        if (profile != null) {
            save(profile);
        }
    }

    public void saveAll() {
        cache.values().forEach(this::save);
    }

    private File fileOf(UUID uuid) {
        return new File(dataFolder, uuid + ".yml");
    }
}
