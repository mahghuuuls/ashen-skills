# Cut trails

A skill can leave a trail of light in the air when its strike starts: a crescent around the user that the blade reveals as it passes and that fades behind it. A trail is a small JSON file in a mod's or resource pack's assets. It is only a look: it changes nothing in the world, and every player who sees the user sees it.

## Where a trail is

For the skill `namespace:name`:

```text
assets/<namespace>/ashen_trails/<name>.json
```

Example: `assets/ashenskills/ashen_trails/unsheathe.json`. A skill without this file leaves no trail.

## Content

```json
{
  "startAngle": -100,
  "endAngle": 110,
  "innerRadius": 1.0,
  "outerRadius": 2.8,
  "height": 1.1,
  "tilt": 25,
  "sweep": 0.2,
  "fade": 0.3,
  "color": [255, 140, 50],
  "sparks": 10
}
```

- `startAngle`, `endAngle`: where the cut starts and ends, in degrees around the user. 0 is straight ahead, a positive angle is to the user's right. They must differ, by 360 at most.
- `innerRadius`, `outerRadius`: the near and the far edge of the crescent, in blocks from the user. The outer one is above the inner one and 16 at most.
- `height`: how high above the user's feet the crescent lies, in blocks.
- `tilt` (optional, default 0): degrees by which the crescent is rolled around the line straight ahead. A positive tilt lifts its right end and lowers its left end.
- `sweep`: seconds the blade takes from the start angle to the end angle.
- `fade`: seconds a place of the trail takes to go dark after the blade has passed it.
- `color`: red, green and blue, each 0 to 255. The middle of the trail is drawn lighter than this color.
- `sparks` (optional, default 0): how many sparks fly off along the outer edge when the cut starts; a whole number from 0 to 1000. A player's setting `maxImpactParticlesPerSkill` is the most that show.

The trail starts when the skill's execution starts and stays around the user while the user moves, so a skill that dashes carries its trail along. The direction is the one the user faced at that moment. A trail never outlives its skill: when the skill ends or is broken off, its trail is gone at once.

## What happens with a mistake

A file that is not valid, or that has a value that cannot make a trail, is named in the log with the reason, and the skill shows no trail:

```text
Trail file ashenskills:ashen_trails/unsheathe.json: outerRadius must be above innerRadius, and innerRadius not below 0; the skill ashenskills:unsheathe shows no trail
```
