package fr.imaginarium.skills.manager;

import fr.imaginarium.skills.model.PlayerProfile;
import fr.imaginarium.skills.model.Skill;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Charge les skills depuis skills.yml et applique leurs bonus d'attributs. */
public class SkillManager {

    private final JavaPlugin plugin;
    private final Map<String, Skill> skills = new LinkedHashMap<>();

    public SkillManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load(FileConfiguration skillsConfig) {
        skills.clear();
        ConfigurationSection root = skillsConfig.getConfigurationSection("skills");
        if (root == null) {
            plugin.getLogger().warning("skills.yml ne contient aucune section 'skills'.");
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            Skill skill = Skill.fromConfig(id.toLowerCase(Locale.ROOT), section, plugin.getLogger());
            if (skill != null) {
                skills.put(skill.id(), skill);
            }
        }
        plugin.getLogger().info(skills.size() + " skill(s) charge(s).");
    }

    public Collection<Skill> getSkills() {
        return skills.values();
    }

    public Skill getSkill(String id) {
        return skills.get(id.toLowerCase(Locale.ROOT));
    }

    public Skill getSkillBySlot(int slot) {
        for (Skill skill : skills.values()) {
            if (skill.slot() == slot) {
                return skill;
            }
        }
        return null;
    }

    private NamespacedKey keyOf(Skill skill) {
        return new NamespacedKey(plugin, "skill_" + skill.id());
    }

    /** (Re)applique tous les bonus de skills du profil sur le joueur. */
    public void applyAll(Player player, PlayerProfile profile) {
        for (Skill skill : skills.values()) {
            AttributeInstance instance = player.getAttribute(skill.attribute());
            if (instance == null) {
                continue;
            }
            removeModifier(instance, keyOf(skill));
            int level = profile.getSkillLevel(skill.id());
            if (level > 0) {
                instance.addModifier(new AttributeModifier(
                        keyOf(skill),
                        skill.amountPerLevel() * level,
                        skill.operation(),
                        EquipmentSlotGroup.ANY));
            }
        }
        // Evite un joueur avec plus de vie que son nouveau maximum.
        AttributeInstance maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null && player.getHealth() > maxHealth.getValue()) {
            player.setHealth(maxHealth.getValue());
        }
    }

    /** Retire tous les bonus de skills du joueur (reset). */
    public void clearAll(Player player) {
        for (Skill skill : skills.values()) {
            AttributeInstance instance = player.getAttribute(skill.attribute());
            if (instance != null) {
                removeModifier(instance, keyOf(skill));
            }
        }
    }

    private void removeModifier(AttributeInstance instance, NamespacedKey key) {
        for (AttributeModifier modifier : new ArrayList<>(instance.getModifiers())) {
            if (key.equals(modifier.getKey())) {
                instance.removeModifier(modifier);
            }
        }
    }
}
