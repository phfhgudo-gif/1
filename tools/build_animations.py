#!/usr/bin/env python3
"""
Builds the two animation variants the mod ships, from the untouched originals in tools/original_animations/:

  player_animation/<move>.json           "safe": extreme torso flips and body offsets are limited
  player_animation/<move>_original.json  the original poses, only resampled to one keyframe per tick

(client config  originalAnimation = true  selects the second one)

usage: python3 tools/build_animations.py
"""
import json, os, sys

sys.path.insert(0, os.path.dirname(__file__))
from resample_animation import resample  # noqa: E402

ROOT = os.path.join(os.path.dirname(__file__), "..")
SRC = os.path.join(ROOT, "tools", "original_animations")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "thanpisitkub", "player_animation")

# bone -> channel -> per-axis (min, max)
SAFE_LIMITS = {
    "torso": {"rotation": [(-25, 55), (-45, 45), (-30, 30)], "position": [(-2, 2), (-2, 2), (-2, 2)]},
    "right_arm": {"position": [(-4, 4)] * 3},
    "left_arm": {"position": [(-4, 4)] * 3},
    "right_leg": {"position": [(-4, 4)] * 3},
    "left_leg": {"position": [(-4, 4)] * 3},
}


def make_safe(src):
    out = json.loads(json.dumps(src))
    for anim in out["animations"].values():
        for bone, channels in SAFE_LIMITS.items():
            for channel, limits in channels.items():
                track = anim["bones"].get(bone, {}).get(channel)
                if not isinstance(track, dict):
                    continue
                for k, v in track.items():
                    track[k] = [min(max(x, lo), hi) for x, (lo, hi) in zip(v, limits)]
    return out


def rename(data, new_name):
    anims = data["animations"]
    (old, body), = anims.items()
    data["animations"] = {new_name: body}
    return data


def main():
    for move in ("galaxy_impact", "galaxy_divide"):
        src = json.load(open(os.path.join(SRC, move + ".json")))
        safe = rename(resample(make_safe(src)), move)
        orig = rename(resample(src), move + "_original")
        json.dump(safe, open(os.path.join(OUT, move + ".json"), "w"), separators=(",", ":"))
        json.dump(orig, open(os.path.join(OUT, move + "_original.json"), "w"), separators=(",", ":"))
        print(move, "ok")


if __name__ == "__main__":
    main()
