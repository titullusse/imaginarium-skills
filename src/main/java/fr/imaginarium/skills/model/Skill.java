package fr.imaginarium.skills.model;

import fr.imaginarium.skills.util.Msg;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/** Un skill configurable qui modifie un attribut du joueur. */
public record Skill(
        String id,
        String displayName,
        List<String> description,
        Material icon,
        int slot,
        Attribute attribute,
        AttributeModifier.Operation operation,
        double amountPerLevel,
        int maxLevel,
        int costPerLevel) {

    /** Construit un skill depuis sa section de skills.yml, ou null si invalide. */
    public static Skill fromConfig(String id, ConfigurationSection section, Logger logger) {
        String attributeName = section.getString("attribut", "");
        Attribute attribute;
        try {
            attribute = Attribute.valueOf(attributeName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            logger.warning("Skill '" + id + "' ignore : attribut inconnu '" + attributeName + "'.");
            return null;
        }

        AttributeModifier.Operation operation;
        try {
            operation = AttributeModifier.Operation.valueOf(
                    section.getString("operation", "ADD_NUMBER").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            logger.warning("Skill '" + id + "' : operation invalide, ADD_NUMBER utilise.");
            operation = AttributeModifier.Operation.ADD_NUMBER;
        }

        Material icon = Material.matchMaterial(section.getString("icone", "BOOK"));
        if (icon == null) {
            icon = Material.BOOK;
        }

        return new Skill(
                id,
                Msg.color(section.getString("nom", id)),
                section.getStringList("description").stream().map(Msg::color).toList(),
                icon,
                section.getInt("slot", 0),
                attribute,
                operation,
                section.getDouble("montant-par-niveau", 1.0),
                Math.max(1, section.getInt("niveau-max", 10)),
                Math.max(1, section.getInt("cout", 1)));
    }
}
