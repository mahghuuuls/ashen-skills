# Animation clip format

A clip is one animation of one skill phase for one view. It is a JSON file in a mod's or resource pack's assets.

## Where a clip is

For the skill `namespace:name`:

```text
assets/<namespace>/ashen_clips/<name>/<view>_<phase>.json
```

- `<view>` is `third` (the whole body, as other players see it) or `first` (the user's own hands).
- `<phase>` is `execution`, `recovery`, or `interrupted_recovery`.

Example: `assets/ashenskills/ashen_clips/powerful_slash/third_execution.json`.

A skill with no file for a view and phase shows no animation there, and the log names the missing file once.

## Content

```json
{
  "length": 0.5,
  "bones": {
    "right_arm": {
      "rotation": [
        { "t": 0.0,  "v": [-20, 0, 5] },
        { "t": 0.18, "v": [100, 0, 90], "ease": "ease_out" }
      ],
      "position": [
        { "t": 0.0, "v": [0, 0, 0] }
      ]
    }
  }
}
```

- `length`: seconds, above 0. Write it as the default length of the phase. A clip always covers its whole phase: when a pack makes the phase longer or shorter, the clip is stretched to fit.
- `bones`: `head`, `body`, `right_arm`, `left_arm`, `right_leg`, `left_leg`, `item`, `camera`. A bone that is not named keeps the pose the game gives it.
- `rotation`: degrees around the x, y and z axis of the game's player model. The value replaces the game's own rotation of that bone.
- `position`: offset from the bone's normal place, in model units (1/16 block).
- Each track is a list of keys in rising time order. `t` is seconds from the start of the clip, from 0 to `length`. `v` is three numbers.
- `ease` says how the motion arrives at a key from the key before: `linear` (default), `ease_in` (slow then fast), `ease_out` (fast then slow), `ease_in_out`.

## Axes of the player model

For an arm, with the player seen from behind:

- x: negative lifts the arm forward (-90 points forward, -180 points up); positive swings it back.
- y: for the right arm, positive turns it out to the right and negative across the body; for the left arm the signs are opposite.
- z: for the right arm, positive lifts it sideways away from the body; for the left arm, negative does.

For the body, positive y turns the right shoulder back.

## What the game does around a clip

- Clips are written for a right-handed player. For a left-handed player the pose is mirrored.
- While a clip plays for a player, the game's own attack swing is not shown on that player's body.
- A bone the clip does not name still moves as the game moves it (walking legs, looking head).

## First-person clips

A first-person clip draws the user's weapon arm with the weapon in its hand, in place of the game's own main hand. It needs one bone, `right_arm`, with both tracks:

- `position`: where the shoulder is, seen from the eyes, in model units (1/16 block): x to the right, y up, z toward the viewer (negative z is in front).
- `rotation`: the same meaning as on the body, with the body facing away from the viewer. -90 on x points the arm straight ahead.

A clip without both tracks for `right_arm` leaves the game's own hand in place. The other hand is always the game's own.

The optional `camera` bone moves the view: its `rotation` is pitch (x), yaw (y) and roll (z) in degrees, added to the camera at full `cameraEffectIntensity` and scaled down with that setting. End the last clip of a skill at 0, 0, 0. For a left-handed player the yaw and roll are mirrored.

The `item` bone is reserved and has no effect yet.

## Third-person limits

The third-person view uses rotations only. It ignores `position` tracks, the `item` bone, and the z rotation of `head` and `body`: the game never resets those values, so a clip that set them would leave the model bent after the clip. When the body turns around y, the shoulders follow it.

A file with an unknown bone, an unknown track, keys out of order, a time outside the clip, or a value that is not three numbers is refused as a whole. The log says which file and what is wrong.

## Checking a clip in the game

With `animationCheckControls=true` in the client part of `ashenskills.cfg`, an operator can run:

```text
/ashenskills pose <player> pin <skill> <phase> <seconds>
/ashenskills pose <player> dump
/ashenskills pose <player> view <0|1|2>
/ashenskills pose <player> unpin
```

`pin` holds the clip at that time on that player's own client. `dump` writes the pose values to that client's log. `view` sets the camera: 0 first person, 1 from behind, 2 from the front. Press F3+T to reload clips after an edit.
