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

Values only some skills have (naming one a skill does not have is a mistake): `arc` (degrees), `width` (blocks).

Yes-or-no rules, each `true` or `false`: `mountedAllowed`, `executionInterruptedByDamage`, `executionInterruptedByKnockback`, `blockingDuringExecution`, `itemUseDuringExecution`, `blockingDuringRecovery`, `itemUseDuringRecovery`, `jumpDuringExecution`, `sneakingDuringSkill`, `solidBlocksObstruct`, `damageEnchantments`, `fireAspect`, `knockback`, `looting`.

The animations of a skill are stretched to the lengths the file sets. How a skill is activated (instant, stance, charged) cannot be changed.

## Checking the result

An operator can hold an item and run `/ashenskills inspect`. The answer names the item's weapon types, its skill, and whether the skill fits.
