Adds weapon skills. Put a skill on a weapon at an Ashen Forge and use it with a key (default `V`). A skill is a special attack with its own animation and a cooldown.

<span style="color:#d6a100">**AI usage disclaimer:** This mod was developed with AI-agent assistance using [this agent workflow](https://github.com/mahghuuuls/minecraft-1.12.2-mod-agent-workflow). The project owner reviewed the work during development.</span>

## How it works

- **Ash:** an item that holds one skill. It is not used up. The mod adds no recipe or loot for Ashes. In Survival, a modpack or another mod has to provide them. They are in the Creative inventory.
- **Ashen Forge:** a craftable block (2 iron ingots, 1 blaze rod, 2 obsidian, 1 anvil, 3 stone bricks). Store Ashes in it, put a weapon in its slot, and pick a skill to put on the weapon. The same block takes a skill off or replaces it. This costs nothing.
- **Weapon types:** each skill fits certain weapon types. A weapon's tooltip shows its types and its skill.
- **Using a skill:** hold the weapon in the main hand, stand on the ground, and press the skill key. All skills share one cooldown. A HUD near the hotbar shows the skill and the cooldown. In Creative, skills have no cost and no cooldown.

## Skills

- **Powerful Slash** (sword, longsword, greatsword): a wide slash that hits the enemies in front of you.
- **Unsheathe** (katana): hold the key to take a stance, then press attack to dash forward and cut the enemies in your path.
- **Piercing Lunge** (spear, pike, rapier): a thrust that carries you forward and hits the enemies in a line.
- **Crushing Blow** (warhammer, mace): hold the key to charge an overhead strike and release it to hit. A longer charge hits harder.

Without other mods, only the five vanilla swords have a type (sword), so only Powerful Slash can be used. Swords from other mods have no type unless a modpack or that mod gives them one. The other weapon types come from Spartan Weaponry or from a modpack's configuration.

## Attributes

Players have these attributes:

- **Ashen Skill Damage** (`ashenskills.skillDamage`): more skill damage.
- **Ashen Skill Haste** (`ashenskills.skillHaste`): shorter cooldown.
- **Ashen Skill Stamina Efficiency** (`ashenskills.skillStaminaEfficiency`): lower stamina cost.
- **Ashen Skill Mana Efficiency** (`ashenskills.skillManaEfficiency`): lower mana cost.

Any mod that grants attributes by name can grant them. Examples:

- **[Quality Tools](https://www.curseforge.com/minecraft/mc-mods/quality-tools):** a quality in its configuration file.
- **[Setworks](https://www.curseforge.com/minecraft/mc-mods/setworks):** a set bonus in a CraftTweaker script.
- **[Statusworks](https://www.curseforge.com/minecraft/mc-mods/statusworks):** a status in a CraftTweaker script.

## Optional mods

- **[Spartan Weaponry](https://www.curseforge.com/minecraft/mc-mods/spartan-weaponry):** its melee weapons get the type of their category (katana, rapier, mace, and so on). Weapons from its addons get types the same way. A type that no skill lists has no skill unless a modpack configures one.
- **[Just Another Stamina System](https://www.curseforge.com/minecraft/mc-mods/just-another-stamina-system)** (1.2.0 or newer): skills cost stamina.
- **[Just Another Wizardry Mana System](https://www.curseforge.com/minecraft/mc-mods/just-another-wizardry-mana-system)** (1.5.0 or newer): a modpack can give a skill a mana cost. No skill costs mana by default.
- **[Critworks](https://www.curseforge.com/minecraft/mc-mods/critworks)** (1.1.0 or newer): skill hits can be critical hits by its rules.
- **[Just Another Food System](https://www.curseforge.com/minecraft/mc-mods/just-another-food-system)** (1.2.0 or newer): a modpack can make a food give points of the attributes above.
- **[RLCombat](https://www.curseforge.com/minecraft/mc-mods/rlcombat):** its main-hand and off-hand attacks are blocked while a skill runs, the same as normal attacks.

An older version of one of the four mods that list a minimum version stops the game at start with a message that names the needed version.

## For modpacks

- `config/ashenskills.cfg` has the client display options (HUD, charge bar, camera effect, tooltips) and the server options (Ashen Forge recipe on or off, Ash slots, Creative rules, whether a skill is free or refused when its stamina or mana mod is not installed, and others).
- Two optional files in `config/ashenskills/` set which items have which weapon types and change the values of any skill (cooldown, costs, damage, reach, which weapon types it fits). The details are in [pack-configuration.md](https://github.com/mahghuuuls/ashen-skills/blob/main/docs/pack-configuration.md).
- The server's files decide for everyone on it. Changes need a restart.

## For mod developers

Another mod can add its own skills and weapon types through an API. It may change between versions. See the [addon API](https://github.com/mahghuuuls/ashen-skills/blob/main/docs/addon-api.md).

## Installation

Install the mod on the server and on every client. It needs [MixinBooter](https://www.curseforge.com/minecraft/mc-mods/mixin-booter) 12.1 or newer. Cleanroom Loader already includes MixinBooter.

Source and issues: [GitHub](https://github.com/mahghuuuls/ashen-skills)
