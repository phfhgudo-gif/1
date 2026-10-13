#!/usr/bin/env python3
"""
Resample a Bedrock/GeckoLib-style player animation to one keyframe per game tick (20 per second).

Why: Player Animation Library 1.0.2 (1.20.1) reads GeckoLib rotation keyframes that are written as plain
arrays with a "constant" (step) easing, so sparse keyframes make the body snap from pose to pose. Writing a
keyframe every tick, already linearly interpolated, gives smooth motion with that version (and any other).

usage: python3 resample_animation.py in.json out.json
"""
import json, math, sys

TICK = 0.05


def sample(track, t):
    keys = sorted((float(k), v) for k, v in track.items())
    if t <= keys[0][0]:
        return list(keys[0][1])
    for (t0, v0), (t1, v1) in zip(keys, keys[1:]):
        if t0 <= t <= t1:
            u = 0.0 if t1 == t0 else (t - t0) / (t1 - t0)
            return [a + (b - a) * u for a, b in zip(v0, v1)]
    return list(keys[-1][1])


def key_for_tick(tick):
    # PAL computes (int)(Float.parseFloat(key) * 20); aim slightly past the tick to dodge float rounding
    return f"{(tick + 0.1) / 20:.5f}".rstrip("0").rstrip(".")


def resample(src):
    out = json.loads(json.dumps(src))
    for name, anim in out["animations"].items():
        length = anim["animation_length"]
        last_tick = int(math.ceil(length * 20))
        for bone in anim["bones"].values():
            for channel, track in list(bone.items()):
                if not isinstance(track, dict):
                    continue
                dense = {}
                for tick in range(0, last_tick + 1):
                    t = min(tick * TICK, max(float(k) for k in track))
                    dense[key_for_tick(tick)] = [round(x, 4) for x in sample(track, t)]
                bone[channel] = dense
    return out


if __name__ == "__main__":
    data = json.load(open(sys.argv[1]))
    json.dump(resample(data), open(sys.argv[2], "w"), separators=(",", ":"))
