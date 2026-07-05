package fr.imaginarium.skills.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Donnees de progression d'un joueur (niveau, xp, points, skills). */
public class PlayerProfile {

    private final UUID uuid;
    private int level = 1;
    private long xp = 0;
    private int skillPoints = 0;
    private final Map<String, Integer> skillLevels = new HashMap<>();

    public PlayerProfile(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    public long getXp() {
        return xp;
    }

    public void setXp(long xp) {
        this.xp = Math.max(0, xp);
    }

    public int getSkillPoints() {
        return skillPoints;
    }

    public void setSkillPoints(int skillPoints) {
        this.skillPoints = Math.max(0, skillPoints);
    }

    public void addSkillPoints(int amount) {
        setSkillPoints(this.skillPoints + amount);
    }

    public int getSkillLevel(String skillId) {
        return skillLevels.getOrDefault(skillId, 0);
    }

    public void setSkillLevel(String skillId, int level) {
        if (level <= 0) {
            skillLevels.remove(skillId);
        } else {
            skillLevels.put(skillId, level);
        }
    }

    public Map<String, Integer> getSkillLevels() {
        return skillLevels;
    }

    public void resetSkills() {
        skillLevels.clear();
    }
}
