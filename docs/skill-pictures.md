# Skill pictures

A skill has two pictures. A resource pack or an addon can give or replace both. They are plain files.

| Picture | Where the player sees it | File for the skill `<namespace>:<name>` |
| --- | --- | --- |
| Skill icon | The skill HUD beside the hotbar; each row of the Ashen Forge's skill list | `assets/<namespace>/textures/skills/<name>.png` |
| Ash icon | The Ash item: inventories, the Ashen Forge's storage, the Creative tab, the hand | The item model `assets/<namespace>/models/item/ash_<name>.json` and the texture it names |

Example for `ashenskills:powerful_slash`:

```text
assets/ashenskills/textures/skills/powerful_slash.png
assets/ashenskills/models/item/ash_powerful_slash.json
assets/ashenskills/textures/items/ash_powerful_slash.png
```

## Skill icon

- Draw it at 16 by 16 pixels. A larger square file works too; the game draws it at 16 by 16.
- Use pixels that are fully opaque or fully clear. A partly transparent pixel is blended, and it can look a little different in the HUD and in the skill list.
- A skill without this file shows a general "unknown skill" picture, and the game log names the missing file once.

## Ash icon

- The item model is a normal item model, for example:

```json
{
  "parent": "item/generated",
  "textures": {
    "layer0": "ashenskills:items/ash_powerful_slash"
  }
}
```

- An Ash of a skill that is not installed shows a general "unknown Ash" picture.
