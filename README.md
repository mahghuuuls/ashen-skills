# Ashen Skills

Ashen Skills adds reusable weapon skills that players attach to their weapons at an Ashen Forge and use with a skill key. Packs decide which weapons fit each skill and how Ashes are obtained.

Minecraft 1.12.2, Forge. Mod id `ashenskills`.

- Player-facing description: [MOD-PAGE.md](MOD-PAGE.md)
- Release history: [CHANGELOG.md](CHANGELOG.md)

## Install sides and dependencies

- Required on the server and on every client. The server decides every skill use; a client is sent the server's skill values.
- Requires MixinBooter 12.1 or newer (Cleanroom Loader includes it).
- Optional, used when present: Spartan Weaponry, Just Another Stamina System 1.2.0 or newer, Just Another Wizardry Mana System 1.5.0 or newer, Critworks 1.1.0 or newer, Just Another Food System 1.2.0 or newer. An older version of one of the last four stops the game at start with a message that names the needed version. RLCombat has no minimum version: its attacks are blocked during a skill like normal attacks.

## Documentation

- [docs/pack-configuration.md](docs/pack-configuration.md): `config/ashenskills/weapon_types.json` and `skills.json` for modpacks, and food properties with JAFS.
- [docs/addon-api.md](docs/addon-api.md): adding skills and weapon types from another mod (package `com.mahghuuuls.ashenskills.api` only).
- [docs/clip-format.md](docs/clip-format.md), [docs/cut-trails.md](docs/cut-trails.md), [docs/skill-pictures.md](docs/skill-pictures.md): animation clips, cut trails and pictures of a skill.

## Configuration

`config/ashenskills.cfg` has a `client` part (skill HUD, charge bar, camera effect, tooltips) and a `server` part (Forge recipe, Ash slots of a Forge, target caps, Creative rules, what happens to costs when no stamina or mana mod is installed, diagnostic logging). The skill key is in the Controls screen (default `V`).

## Scope

- Four built-in skills. Ashes have no recipe and no loot of their own: a pack or another mod adds them.
- The operator command `/ashenskills` inspects and resets skill state. Diagnostic log lines are off by default.

## License

MIT. See `LICENSE` and `THIRD-PARTY-NOTICES.md`.
