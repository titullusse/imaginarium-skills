# ImaginariumSkills

Plugin **Spigot 1.21.1** : système de skills, niveaux, récompenses configurables et
modification des attributs du joueur et des caractéristiques des armes/armures.

## Fonctionnalités

- **Système de skills** : 7 skills par défaut (Force, Vitalité, Agilité, Résistance,
  Rapidité, Robustesse, Chance) entièrement configurables dans `skills.yml`.
  Chaque skill modifie un attribut du joueur (dégâts, cœurs, vitesse, armure...).
- **Menu graphique** : `/skills` ouvre un menu où le joueur dépense ses points
  de skill en cliquant.
- **Système de niveaux** : XP gagnée en tuant des monstres et en minant
  (sources et montants configurables dans `config.yml`, avec protection
  anti-farm pose/casse de blocs).
- **Récompenses de niveaux** (`levels.yml`) : points de skill, commandes console,
  messages, broadcasts et sons — par niveau ou par défaut à chaque montée.
- **Modification d'objets** : `/itemattr` pour ajouter/retirer des attributs
  sur les armes et armures (dégâts, vitesse d'attaque, armure, etc.),
  renommer un objet ou le rendre incassable.
- **Administration** : `/skills admin` pour gérer XP, niveaux, points,
  reset et attributs de base des joueurs.
- Sauvegarde automatique des données joueurs (un fichier YAML par joueur).

## Compilation

Prérequis : **Java 21** et **Maven**.

```bash
mvn package
```

Le jar est généré dans `target/ImaginariumSkills-1.0.0.jar`.
Placez-le dans le dossier `plugins/` de votre serveur Spigot 1.21.1.

## Commandes

| Commande | Description | Permission |
|---|---|---|
| `/skills` | Ouvre le menu des skills | `imaskills.use` (tous) |
| `/skills info` | Affiche vos statistiques | `imaskills.use` |
| `/skills admin addxp <joueur> <montant>` | Donne de l'XP | `imaskills.admin` (op) |
| `/skills admin setlevel <joueur> <niveau>` | Définit le niveau | `imaskills.admin` |
| `/skills admin addpoints <joueur> <points>` | Donne des points de skill | `imaskills.admin` |
| `/skills admin reset <joueur>` | Réinitialise un profil | `imaskills.admin` |
| `/skills admin attribut <joueur> <attribut> <valeur\|reset>` | Modifie un attribut de base du joueur | `imaskills.admin` |
| `/skills admin reload` | Recharge la configuration | `imaskills.admin` |
| `/itemattr add <attribut> <montant> [operation] [slot]` | Ajoute un attribut à l'objet en main | `imaskills.itemattr` (op) |
| `/itemattr remove <attribut>` | Retire un attribut | `imaskills.itemattr` |
| `/itemattr list` | Liste les attributs de l'objet | `imaskills.itemattr` |
| `/itemattr clear` | Retire tous les attributs | `imaskills.itemattr` |
| `/itemattr name <nom...>` | Renomme l'objet (codes `&`) | `imaskills.itemattr` |
| `/itemattr unbreakable` | Rend l'objet incassable | `imaskills.itemattr` |

### Exemples `/itemattr`

```
/itemattr add generic_attack_damage 12 add_number hand
/itemattr add generic_movement_speed 0.1 multiply_scalar_1 legs
/itemattr add generic_max_health 4 add_number chest
/itemattr name &6Épée &clégendaire
```

**Emplacements pris en charge** (dernier argument de `add`) :

| Nom | Emplacement |
|---|---|
| `any` | N'importe quel emplacement |
| `hand` | Main droite (main principale) uniquement |
| `off_hand` | Main gauche (main secondaire) uniquement |
| `head` | Casque |
| `chest` | Plastron |
| `legs` | Jambières |
| `feet` | Bottes |
| `armor` | Les 4 pièces d'armure |

> ℹ️ Les statistiques de base de l'objet sont **conservées** : dès le premier
> `add`, le plugin recopie les attributs vanilla d'origine (dégâts d'une épée,
> armure d'un plastron...) puis applique votre modificateur par-dessus. Vos
> ajouts s'additionnent donc aux stats de base au lieu de les effacer.

## Configuration

- `config.yml` — courbe d'XP (base/par-niveau/quadratique), niveau maximum,
  sources d'XP (kills et blocs), multiplicateur global, sauvegarde auto,
  et `points-de-depart` (points de skill offerts à chaque nouveau joueur
  lors de sa première connexion).
- `skills.yml` — définition des skills : attribut modifié, opération, bonus
  par niveau, niveau max, coût, icône et position dans le menu. Vous pouvez
  en ajouter/supprimer librement.
- `levels.yml` — récompenses : section `defaut` appliquée à chaque montée de
  niveau + sections par niveau précis (`5`, `10`, `25`...). Options :
  `points-de-skill`, `commandes` (avec `%player%` et `%level%`), `message`,
  `broadcast`, `son`.

Après modification : `/skills admin reload`.
