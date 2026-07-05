package fr.imaginarium.skills.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/** Petit utilitaire pour les messages colores avec prefixe. */
public final class Msg {

    private static String prefix = "";

    private Msg() {
    }

    public static void setPrefix(String rawPrefix) {
        prefix = color(rawPrefix);
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static void send(CommandSender to, String message) {
        to.sendMessage(prefix + color(message));
    }
}
