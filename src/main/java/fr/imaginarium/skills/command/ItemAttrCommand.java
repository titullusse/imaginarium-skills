package fr.imaginarium.skills.command;

import fr.imaginarium.skills.util.Msg;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * /itemattr : modifie les caracteristiques (attributs) de l'objet tenu en main.
 * Permet de creer des armes et armures personnalisees.
 */
public class ItemAttrCommand implements TabExecutor {

    private static final List<String> SLOT_GROUPS =
            List.of("any", "hand", "mainhand", "offhand", "armor", "head", "chest", "legs", "feet", "body");

    private final JavaPlugin plugin;

    public ItemAttrCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.send(sender, "&cSeul un joueur peut modifier un objet.");
            return true;
        }
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            Msg.send(player, "&cPrenez un objet en main d'abord.");
            return true;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            Msg.send(player, "&cCet objet ne peut pas etre modifie.");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add" -> handleAdd(player, item, meta, args);
            case "remove" -> handleRemove(player, item, meta, args);
            case "list" -> handleList(player, meta);
            case "clear" -> handleClear(player, item, meta);
            case "name" -> handleName(player, item, meta, args);
            case "unbreakable" -> handleUnbreakable(player, item, meta);
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        Msg.send(player, "&e--- Aide /itemattr (objet en main) ---");
        Msg.send(player, "&b/itemattr add <attribut> <montant> [operation] [slot]");
        Msg.send(player, "&7  operations : add_number, add_scalar, multiply_scalar_1");
        Msg.send(player, "&7  slots : any, hand, mainhand, offhand, armor, head, chest, legs, feet");
        Msg.send(player, "&b/itemattr remove <attribut> &7- Retire un attribut.");
        Msg.send(player, "&b/itemattr list &7- Liste les attributs de l'objet.");
        Msg.send(player, "&b/itemattr clear &7- Retire tous les attributs.");
        Msg.send(player, "&b/itemattr name <nom...> &7- Renomme l'objet (codes &&).");
        Msg.send(player, "&b/itemattr unbreakable &7- Rend l'objet incassable (ou l'inverse).");
        Msg.send(player, "&c/!\\ Ajouter un attribut remplace les stats de base de l'objet");
        Msg.send(player, "&c(comportement vanilla) : pensez a re-ajouter les degats de base.");
    }

    private void handleAdd(Player player, ItemStack item, ItemMeta meta, String[] args) {
        if (args.length < 3) {
            Msg.send(player, "&cUsage : /itemattr add <attribut> <montant> [operation] [slot]");
            return;
        }
        Attribute attribute = parseAttribute(args[1]);
        if (attribute == null) {
            Msg.send(player, "&cAttribut inconnu : " + args[1]);
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException ex) {
            Msg.send(player, "&cMontant invalide : " + args[2]);
            return;
        }
        AttributeModifier.Operation operation = AttributeModifier.Operation.ADD_NUMBER;
        if (args.length >= 4) {
            try {
                operation = AttributeModifier.Operation.valueOf(args[3].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                Msg.send(player, "&cOperation invalide : " + args[3]
                        + " &7(add_number, add_scalar, multiply_scalar_1)");
                return;
            }
        }
        EquipmentSlotGroup slotGroup = EquipmentSlotGroup.ANY;
        if (args.length >= 5) {
            slotGroup = EquipmentSlotGroup.getByName(args[4].toLowerCase(Locale.ROOT));
            if (slotGroup == null) {
                Msg.send(player, "&cSlot invalide : " + args[4] + " &7(" + String.join(", ", SLOT_GROUPS) + ")");
                return;
            }
        }

        NamespacedKey key = new NamespacedKey(plugin,
                "item_" + UUID.randomUUID().toString().substring(0, 8));
        meta.addAttributeModifier(attribute, new AttributeModifier(key, amount, operation, slotGroup));
        item.setItemMeta(meta);
        Msg.send(player, "&aAttribut &e" + attribute.name() + " &7(" + operation.name().toLowerCase(Locale.ROOT)
                + " " + amount + ", " + slotGroup + ") &aajoute a l'objet.");
    }

    private void handleRemove(Player player, ItemStack item, ItemMeta meta, String[] args) {
        if (args.length < 2) {
            Msg.send(player, "&cUsage : /itemattr remove <attribut>");
            return;
        }
        Attribute attribute = parseAttribute(args[1]);
        if (attribute == null) {
            Msg.send(player, "&cAttribut inconnu : " + args[1]);
            return;
        }
        if (meta.removeAttributeModifier(attribute)) {
            item.setItemMeta(meta);
            Msg.send(player, "&aAttribut &e" + attribute.name() + " &aretire de l'objet.");
        } else {
            Msg.send(player, "&cCet objet n'a pas de modificateur pour cet attribut.");
        }
    }

    private void handleList(Player player, ItemMeta meta) {
        var modifiers = meta.getAttributeModifiers();
        if (modifiers == null || modifiers.isEmpty()) {
            Msg.send(player, "&7Cet objet n'a aucun modificateur d'attribut personnalise.");
            return;
        }
        Msg.send(player, "&e--- Attributs de l'objet ---");
        modifiers.forEach((attribute, modifier) ->
                Msg.send(player, "&8- &e" + attribute.name() + " &7: " + modifier.getAmount()
                        + " (" + modifier.getOperation().name().toLowerCase(Locale.ROOT)
                        + ", " + modifier.getSlotGroup() + ")"));
    }

    private void handleClear(Player player, ItemStack item, ItemMeta meta) {
        boolean removed = false;
        for (Attribute attribute : Attribute.values()) {
            removed |= meta.removeAttributeModifier(attribute);
        }
        item.setItemMeta(meta);
        Msg.send(player, removed
                ? "&aTous les attributs de l'objet ont ete retires."
                : "&7Cet objet n'avait aucun attribut personnalise.");
    }

    private void handleName(Player player, ItemStack item, ItemMeta meta, String[] args) {
        if (args.length < 2) {
            Msg.send(player, "&cUsage : /itemattr name <nom...>");
            return;
        }
        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        meta.setDisplayName(Msg.color(name));
        item.setItemMeta(meta);
        Msg.send(player, "&aObjet renomme en : &r" + Msg.color(name));
    }

    private void handleUnbreakable(Player player, ItemStack item, ItemMeta meta) {
        meta.setUnbreakable(!meta.isUnbreakable());
        item.setItemMeta(meta);
        Msg.send(player, meta.isUnbreakable()
                ? "&aL'objet est maintenant incassable."
                : "&7L'objet n'est plus incassable.");
    }

    private Attribute parseAttribute(String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        try {
            return Attribute.valueOf(upper);
        } catch (IllegalArgumentException ex) {
            // Tolere le nom court sans prefixe (ex: "attack_damage").
            try {
                return Attribute.valueOf("GENERIC_" + upper);
            } catch (IllegalArgumentException ex2) {
                return null;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(List.of("add", "remove", "list", "clear", "name", "unbreakable"));
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            Arrays.stream(Attribute.values()).forEach(a -> options.add(a.name().toLowerCase(Locale.ROOT)));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("add")) {
            Arrays.stream(AttributeModifier.Operation.values())
                    .forEach(o -> options.add(o.name().toLowerCase(Locale.ROOT)));
        } else if (args.length == 5 && args[0].equalsIgnoreCase("add")) {
            options.addAll(SLOT_GROUPS);
        }
        String current = args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.startsWith(current)).toList();
    }
}
