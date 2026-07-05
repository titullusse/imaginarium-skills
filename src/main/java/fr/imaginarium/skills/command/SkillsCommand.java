package fr.imaginarium.skills.command;

import fr.imaginarium.skills.ImaginariumSkills;
import fr.imaginarium.skills.gui.SkillsMenu;
import fr.imaginarium.skills.model.PlayerProfile;
import fr.imaginarium.skills.model.Skill;
import fr.imaginarium.skills.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** /skills : menu, infos et administration du systeme. */
public class SkillsCommand implements TabExecutor {

    private final ImaginariumSkills plugin;

    public SkillsCommand(ImaginariumSkills plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                Msg.send(sender, "&cSeul un joueur peut ouvrir le menu.");
                return true;
            }
            if (!player.hasPermission("imaskills.use")) {
                Msg.send(player, "&cVous n'avez pas la permission.");
                return true;
            }
            SkillsMenu.open(player, plugin.getPlayerDataManager().getProfile(player),
                    plugin.getSkillManager(), plugin.getLevelManager());
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "info" -> sendInfo(sender);
            case "admin" -> handleAdmin(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendInfo(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Msg.send(sender, "&cSeul un joueur peut voir ses infos.");
            return;
        }
        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player);
        Msg.send(player, "&e--- Vos statistiques ---");
        Msg.send(player, "&7Niveau : &e" + profile.getLevel() + "&7/&e" + plugin.getLevelManager().getMaxLevel());
        if (profile.getLevel() < plugin.getLevelManager().getMaxLevel()) {
            Msg.send(player, "&7XP : &b" + profile.getXp() + "&7/&b"
                    + plugin.getLevelManager().xpRequired(profile.getLevel()));
        }
        Msg.send(player, "&7Points de skill : &a" + profile.getSkillPoints());
        for (Skill skill : plugin.getSkillManager().getSkills()) {
            int level = profile.getSkillLevel(skill.id());
            if (level > 0) {
                Msg.send(player, "&8- " + skill.displayName() + "&7 : niveau &e" + level);
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        Msg.send(sender, "&e--- Aide /skills ---");
        Msg.send(sender, "&b/skills &7- Ouvre le menu des skills.");
        Msg.send(sender, "&b/skills info &7- Affiche vos statistiques.");
        if (sender.hasPermission("imaskills.admin")) {
            Msg.send(sender, "&b/skills admin addxp <joueur> <montant>");
            Msg.send(sender, "&b/skills admin setlevel <joueur> <niveau>");
            Msg.send(sender, "&b/skills admin addpoints <joueur> <points>");
            Msg.send(sender, "&b/skills admin reset <joueur>");
            Msg.send(sender, "&b/skills admin attribut <joueur> <attribut> <valeur|reset>");
            Msg.send(sender, "&b/skills admin reload");
        }
    }

    private void handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("imaskills.admin")) {
            Msg.send(sender, "&cVous n'avez pas la permission.");
            return;
        }
        if (args.length < 2) {
            sendHelp(sender);
            return;
        }

        String sub = args[1].toLowerCase(Locale.ROOT);
        if (sub.equals("reload")) {
            plugin.reloadAll();
            Msg.send(sender, "&aConfiguration rechargee.");
            return;
        }

        if (args.length < 3) {
            sendHelp(sender);
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            Msg.send(sender, "&cJoueur introuvable : " + args[2]);
            return;
        }
        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(target);

        switch (sub) {
            case "addxp" -> {
                Long amount = parseLong(sender, args, 3);
                if (amount == null) {
                    return;
                }
                plugin.getLevelManager().addXp(target, amount);
                Msg.send(sender, "&a" + amount + " XP donnee a &e" + target.getName()
                        + " &a(niveau " + profile.getLevel() + ").");
            }
            case "setlevel" -> {
                Long level = parseLong(sender, args, 3);
                if (level == null) {
                    return;
                }
                profile.setLevel((int) Math.min(level, plugin.getLevelManager().getMaxLevel()));
                profile.setXp(0);
                Msg.send(sender, "&e" + target.getName() + " &aest maintenant niveau &e" + profile.getLevel() + "&a.");
                Msg.send(target, "&aVotre niveau a ete defini sur &e" + profile.getLevel() + "&a.");
            }
            case "addpoints" -> {
                Long points = parseLong(sender, args, 3);
                if (points == null) {
                    return;
                }
                profile.addSkillPoints(points.intValue());
                Msg.send(sender, "&a" + points + " point(s) de skill donne(s) a &e" + target.getName() + "&a.");
                Msg.send(target, "&aVous avez recu &e" + points + " &apoint(s) de skill !");
            }
            case "reset" -> {
                plugin.getSkillManager().clearAll(target);
                profile.resetSkills();
                profile.setLevel(1);
                profile.setXp(0);
                profile.setSkillPoints(0);
                Msg.send(sender, "&aProfil de &e" + target.getName() + " &areinitialise.");
                Msg.send(target, "&cVotre progression de skills a ete reinitialisee.");
            }
            case "attribut" -> handleAttribute(sender, target, args);
            default -> sendHelp(sender);
        }
    }

    private void handleAttribute(CommandSender sender, Player target, String[] args) {
        if (args.length < 5) {
            Msg.send(sender, "&cUsage : /skills admin attribut <joueur> <attribut> <valeur|reset>");
            return;
        }
        Attribute attribute;
        try {
            attribute = Attribute.valueOf(args[3].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            Msg.send(sender, "&cAttribut inconnu : " + args[3]);
            return;
        }
        AttributeInstance instance = target.getAttribute(attribute);
        if (instance == null) {
            Msg.send(sender, "&cCet attribut n'existe pas sur ce joueur.");
            return;
        }
        if (args[4].equalsIgnoreCase("reset")) {
            instance.setBaseValue(instance.getDefaultValue());
            Msg.send(sender, "&aAttribut &e" + attribute.name() + " &areinitialise ("
                    + instance.getDefaultValue() + ") pour &e" + target.getName() + "&a.");
            return;
        }
        double value;
        try {
            value = Double.parseDouble(args[4]);
        } catch (NumberFormatException ex) {
            Msg.send(sender, "&cValeur invalide : " + args[4]);
            return;
        }
        instance.setBaseValue(value);
        Msg.send(sender, "&aAttribut &e" + attribute.name() + " &adefini sur &e" + value
                + " &apour &e" + target.getName() + "&a.");
    }

    private Long parseLong(CommandSender sender, String[] args, int index) {
        if (args.length <= index) {
            Msg.send(sender, "&cIl manque un nombre a la commande.");
            return null;
        }
        try {
            long value = Long.parseLong(args[index]);
            if (value < 0) {
                Msg.send(sender, "&cLe nombre doit etre positif.");
                return null;
            }
            return value;
        } catch (NumberFormatException ex) {
            Msg.send(sender, "&cNombre invalide : " + args[index]);
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(List.of("info", "help"));
            if (sender.hasPermission("imaskills.admin")) {
                options.add("admin");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            options.addAll(List.of("addxp", "setlevel", "addpoints", "reset", "attribut", "reload"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("attribut")) {
            Arrays.stream(Attribute.values()).forEach(a -> options.add(a.name()));
        } else if (args.length == 5 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("attribut")) {
            options.add("reset");
        }
        String current = args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(current)).toList();
    }
}
