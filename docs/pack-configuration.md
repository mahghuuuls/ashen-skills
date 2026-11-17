# Pack configuration

A modpack can decide which items count as which weapon type, and change the values of any skill. Two optional files do this. Both live in the game's `config/ashenskills/` folder:

- `weapon_types.json`: weapon types
- `skills.json`: skill values

The files are read once, when the game has finished loading. A change needs a restart. On a server, the server's files decide; a client that joins is sent the server's values and uses them in place of its own files.

A key that starts with `_` is ignored in these places, so `"_comment": "..."` can be used for notes there: at the top of either file, among the item names of `weapon_types.json`, and among the values of a skill in `skills.json`.

Do not write the same key twice in one object. JSON then keeps only the last one, without a word in the log.

## What happens with a mistake

Each wrong entry gets one `ERROR` line in the log that names the file, the entry's key, and the wrong value. That entry is ignored and the built-in behavior is used for it. Every other entry still applies. A file that is not valid JSON is named in the log and ignored as a whole.

```text
Pack file skills.json: key 'ashenskills:powerful_slash.cooldown': value -5: allowed is 1.0 to 60.0; this entry is ignored
Pack file weapon_types.json: key 'items.minecraft:no_such_item': no item with the name minecraft:no_such_item is registered; this entry is ignored
```

A summary line follows:

```text
Pack configuration: weapon_types.json read (1 type(s), 3 item rule(s), 1 problem(s)); skills.json read (1 problem(s))
```

## Weapon types without a pack

- The five vanilla swords have the type `sword`.
- With Spartan Weaponry installed, each of its melee weapons has the type of its category: `dagger`, `parrying_dagger`, `longsword`, `katana`, `saber`, `rapier`, `greatsword`, `hammer`, `warhammer`, `spear`, `halberd`, `pike`, `lance`, `battleaxe`, `mace`, `glaive`, `quarterstaff`, `club`, `caestus`, `scythe`. A weapon that another mod builds on one of these categories has the same type.
- Every other item has no type. A type is never guessed from an item's name or damage.

One type never implies another: a `katana` is not a `sword` unless a rule says so.

## `weapon_types.json`

```json
{
  "types": ["club_heavy"],
  "items": {
    "minecraft:stick": { "add": ["club_heavy"] },
    "minecraft:diamond_sword": { "add": ["katana"] },
    "spartanweaponry:katana_iron": { "replace": ["sword"] }
  }
}
```

- `types`: new type ids of the pack. An id uses lower-case letters, digits and `_ . : -`.
- `items`: one entry per item, by registry name. Each entry has exactly one of:
  - `add`: the item gets these types in addition to the ones it has without a pack.
  - `replace`: the item has exactly these types. An empty list takes all types away.
- Every type in `add` or `replace` must be a built-in type or be listed under `types`.

## `skills.json`

One entry per skill, by skill id. Each entry names the values to change; a skill keeps its own value for everything else.

```json
{
  "ashenskills:powerful_slash": {
    "cooldown": 20,
    "damageMultiplier": 2.2,
    "weaponTypes": ["sword", "longsword", "greatsword", "club_heavy"],
    "looting": false
  }
}
```

Values every skill has:

| Name | Meaning | Allowed |
| --- | --- | --- |
| `cooldown` | seconds of the shared skill cooldown | 1 to 60 |
| `staminaCost`, `manaCost` | cost of one use | 0 to 200 |
| `damageMultiplier` | weapon damage times this | 1 to 4 |
| `executionLength` | seconds of the execution | above 0 |
| `recovery`, `interruptedRecovery` | seconds of recovery | 0.1 to 3 |
| `executionSpeed`, `recoverySpeed` | movement speed as a share of normal | 0 to 1 |
| `reach`, `reachBelow`, `reachAbove` | blocks | 0 or more |
| `knockbackStrength` | push on a hit | 0 or more |
| `impactParticles` | sparks per target hit | 0 or more |
| `hitWindows` | when the strikes land, for example `[{"at": 0.3}]`, in seconds from the start of the execution; none may lie after `executionLength`; at most 255 | |
| `weaponTypes` | the weapon types the skill fits; at least one | known type ids |
| `allowedStartStates` | where the player may be: `grounded`, `airborne`, `swimming`; at least one | |

Values only some skills have (naming one a skill does not have is a mistake): `arc` (degrees), `width` (blocks), and for a skill that moves its user `travelDistance` (blocks on open ground) and `travelTime` (seconds, from the start of the execution; a `travelTime` above `executionLength` is a mistake). The game cannot move a player faster than about 3.9 blocks in a twentieth of a second: a travel that asks for more takes the longer time that speed needs, and what is not done when the execution ends is lost. The strike of such a skill also covers the ground the player crossed.

Skills with a preparation (a held stance, a charge) also have: `preparationSpeed` (movement speed as a share of normal, 0 to 1), `preparationStaminaRegen` (what stamina regeneration is multiplied by during the preparation, 0 to 1; it needs a stamina mod), for a stance `minPreparation` (the wind-up, in seconds, 0 to 5: how long the stance must be held before an attack executes the skill; an attack pressed sooner is kept and the skill executes when the wind-up is over, if the key is still held; 0 for no wind-up; a wind-up shorter than the skill's way into its stance, 0.5 seconds for Unsheathe, lets the cut start from a half-finished pose; a stance skill of another mod that does not name it has none), and the yes-or-no rules `preparationCanceledByDamage` and `preparationCanceledByKnockback`. A charged skill also has `minCharge` (seconds; a shorter charge does nothing when the key is let go), `fullCharge` (seconds; the charge that gives the whole bonus) and `maxChargeBonus` (the extra share of damage at full charge: 0.8 is 80 percent more), each 0 or more; a `fullCharge` below `minCharge` is a mistake. The charge animation is made for the skill's own `fullCharge`: with another value the weapon is fully raised earlier or later than the bar is full. A preparation costs nothing and starts no cooldown. Letting go of the skill key ends a stance; it ends a charge too, and executes the skill when the charge lasted its minimum. A preparation also ends, with nothing executed, when a screen opens (inventory, chat, pause), or when the player changes the item in the main hand, starts to use an item or raise a shield, or is no longer in an allowed state.

Yes-or-no rules, each `true` or `false`: `mountedAllowed`, `executionInterruptedByDamage`, `executionInterruptedByKnockback`, `blockingDuringExecution`, `itemUseDuringExecution`, `blockingDuringRecovery`, `itemUseDuringRecovery`, `jumpDuringExecution`, `sneakingDuringSkill`, `solidBlocksObstruct`, `damageEnchantments`, `fireAspect`, `knockback`, `looting`.

The animations of a skill's execution and recovery are stretched to the lengths the file sets; the animation of a preparation plays once at its own speed and then holds. How a skill is activated (instant, stance, charged) cannot be changed.

## Checking the result

An operator can hold an item and run `/ashenskills inspect`. The answer names the item's weapon types, its skill, and whether the skill fits.
