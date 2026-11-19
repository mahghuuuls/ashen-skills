# Changelog

## 1.0.0

Initial release.

### Added

- The Ashen Forge block: stores Ashes and puts a skill on a weapon or takes it off.
- Ashes: one reusable item per skill, in the Creative inventory.
- Four skills, used with the skill key (default `V`): Powerful Slash, Unsheathe, Piercing Lunge, Crushing Blow.
- Weapon types that decide which skills fit a weapon. Vanilla swords have the type sword.
- One shared skill cooldown, a skill HUD, a charge bar, and tooltips for the type and the skill of a weapon.
- Four player attributes: Ashen Skill Damage, Ashen Skill Haste, Ashen Skill Stamina Efficiency, Ashen Skill Mana Efficiency.
- Pack files `config/ashenskills/weapon_types.json` and `skills.json`.
- An API for other mods to add skills and weapon types.

### Compatibility

- Spartan Weaponry: its weapons get the type of their category.
- Just Another Stamina System and Just Another Wizardry Mana System pay the stamina and mana costs of skills.
- Critworks: skill hits can be critical hits.
- Just Another Food System: foods can give points of the four attributes.
- RLCombat: its attacks are blocked while a skill runs.
