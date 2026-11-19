# Adding skills from another mod

This page is for mod developers. It describes the API of this version; it may change in a later version.

Another mod can add skills and weapon types to Ashen Skills. It uses only the package `com.mahghuuuls.ashenskills.api`. Everything else in the mod is internal and can change without notice. Inside that package, `AshenSkillsApi.attach` and `AshenSkillsApi.Host` are for Ashen Skills itself, not for other mods.

A skill you add obeys every rule of the mod: its costs, the shared cooldown, the checks before it starts, PvP and team rules, target caps, and interruption. It shows in Ashen Forges, tooltips, the skill HUD, the `/ashenskills` commands and the pack files like the mod's own skills. The API gives your code no way to switch these rules off.

## What you need

- Compile against the Ashen Skills jar. Do not pack it into your jar. The mod is not on a Maven repository: put the released jar in your project and add it as a compile-only file dependency. The released jar uses the game's obfuscated names, so add it through your build's deobfuscation step.
- Declare the dependency in your `@Mod`: `dependencies = "required-after:ashenskills"`.
- Make your calls in your mod's pre-initialization (`FMLPreInitializationEvent`). The game chooses item pictures right after it, so a skill added later has no Ash icon. When the game has finished loading mods, the sets are closed: a later call throws `IllegalStateException` with a message that says so. A call made before Ashen Skills started throws the same exception with a message that names the dependency line.

## The calls

All are static methods of `AshenSkillsApi`.

| Call | What it does |
| --- | --- |
| `registerSkill(SkillDefinition, SkillBehavior)` | Adds a skill. Fails when the id is taken, a required value is missing, or the behavior is null |
| `registerWeaponType(String)` | Adds a weapon type id (lower-case letters, digits, `_ . : -`). A type never implies another type |
| `addItemType(ResourceLocation item, String type)` | Gives an item a type without a pack file. The type must exist |
| `ash(ResourceLocation skill)` | One Ash of that skill, for a recipe or loot. Allowed at any time after Ashen Skills has started; the id is not checked |

## A skill is data and code

**The data** is a `SkillDefinition`. Build it with `SkillDefinition.builder(id, activationKind)`:

- `id`: `yourmodid:name`. It is permanent. Weapons and Ashes store it.
- `ActivationKind`: `INSTANT` (the key executes it), `STANCE` (the key holds a stance, an attack executes it), `CHARGED` (holding the key charges it, letting go executes it).
- `weaponTypes(...)`: the types the skill fits. A weapon needs at least one of them.
- `allowedStates(...)`: `SkillDefinition.STATE_GROUNDED`, `STATE_AIRBORNE`, `STATE_SWIMMING`.
- `hitWindows(...)`: when each hit window opens, in seconds from the start of the execution.
- `number(SkillNumber, value)` and `flag(SkillFlag, value)`: the numbers and yes-or-no rules. Each constant's comment says what it means and whether every skill needs it (source: `src/main/java/com/mahghuuuls/ashenskills/api/`). `SkillDefinition.missingValues()` names what is still missing; registration refuses an incomplete skill.

Packs can change these values in `config/ashenskills/skills.json`, by the same names (`docs/pack-configuration.md`).

**The code** is a `SkillBehavior`:

```java
public final class Lash implements SkillBehavior {
    @Override
    public void onHitWindow(SkillContext context, int windowIndex) {
        context.hit(HitShape.arc(
                context.skill().number(SkillNumber.REACH),
                context.skill().number(SkillNumber.ARC)));
    }
}
```

- `onHitWindow` runs on the server each time a hit window opens. `context.hit(shape)` strikes every valid target in the shape once, with the skill's damage, and returns how many took damage. `HitShape` has the shapes. You may call `hit` more than once in a window, with different shapes: the target cap counts over the whole window, and a target is damaged once per window.
- A `SkillContext` is valid only during the call it was given to. `hit` on a kept context, or outside a hit window, throws `IllegalStateException`.
- `motionAtExecution` (optional) returns a `SkillMotion` when the skill moves its user: a distance and a time. The mod does the moving, with the game's collision.
- `SkillContext` gives the user, the weapon, the skill with its values in force, the direction the user faced when the skill executed, the charge bonus, and how far the motion has carried the user.

Read numbers from `context.skill()`, not from constants in your code, so that a pack's changes take effect.

## Files in your assets

Your jar needs a `pack.mcmeta` with `"pack_format": 3`, as every mod with lower-case file names does; without it the game does not read `en_us.lang`, and your skill shows as its id.

The mod finds the rest of a skill by its id `yourmodid:name`. A missing file never breaks the game: the skill shows a generic picture, plays no sound, or keeps the game's own pose.

| What | File |
| --- | --- |
| Name and one-sentence description | `assets/yourmodid/lang/en_us.lang`: `skill.yourmodid.name.name=` and `skill.yourmodid.name.description=` |
| Ash icon | item model `assets/yourmodid/models/item/ash_name.json` with its texture |
| Skill icon (HUD, Forge list) | `assets/yourmodid/textures/skills/name.png` (`docs/skill-pictures.md`) |
| Sounds | `assets/yourmodid/sounds.json`: `skill.name.swing`, `skill.name.impact`, and `skill.name.charged` for a charged skill |
| Animations | `assets/yourmodid/ashen_clips/name/<view>_<phase>.json` (`docs/clip-format.md`) |
| Cut trail | `assets/yourmodid/ashen_trails/name.json` (`docs/cut-trails.md`) |
| Sparks at a damaged target | no file: the number `impactParticles` of the skill. This version has no other particles for a skill |

The Ash is one item for all skills; which skill it holds is in its data (`{ashenskills:{ash:"yourmodid:name"}}`). The Creative tab of Ashen Skills shows one Ash for every installed skill, yours included.

## Servers and clients

Install your mod on the server and on every client, like any mod that adds content. The weapon types and item types your code adds are not sent from the server to a client: each side has them from your mod.

## When your mod is removed

A weapon keeps the skill id. Its tooltip shows the id as unavailable, the skill key does nothing but say so, and an Ashen Forge can replace or remove the skill. An Ash of that skill shows the unknown icon and the id. When your mod is installed again, the weapon works as before.

## An example

The repository has a small second mod as an example, in `src/testAddon/`. It adds the type `whip`, gives it to the game's lead, and adds the skill `ashentestaddon:lash`: one instant skill with an arc hit, its texts, Ash picture, skill picture, sounds and clips. It has no stance or charged skill, no motion, and no cut trail.

## Not in this version

- No events or callbacks other than the ones of `SkillBehavior`.
- No call that reads a player's skill phase or cooldown.
- Hit shapes are an arc or a line; motion is straight ahead.
- No client-side hook of its own; pictures, sounds, clips and trails are files.

The example mod is built for tests only and is not part of the released jar.
