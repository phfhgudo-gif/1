#!/usr/bin/env python3
"""
Tiny rig + baker for Minecraft player animations (Player Animation Library JSON).

Why this exists: in the vanilla player model the head, arms, legs and torso are *independent* parts that all
pivot at their own joint, so a plain "bone" animation cannot fold the whole body (e.g. dive head first) without
tearing it apart. Here poses are authored on a simple hierarchy

    root (pivots at the hips)
      +-- torso (pivots at the neck)
      |     +-- head, right arm, left arm
      +-- right leg, left leg

and then baked into the flat per-part rotation + position keyframes that the game wants (one keyframe per tick).

Conventions (vanilla model space, pixels, 16 px = 1 block, y points DOWN, the front of the player is -z,
the player's RIGHT side is -x). Rotation order is the same as ModelPart: R = Rz * Ry * Rx.

  arm:  x = swing forward(-)/back(+), then z = swing sideways (right arm: + = outwards, left arm: - = outwards)
        e.g. right arm: (0,0,0) hangs, (-90,0,0) forward, (0,0,90) sideways, (0,0,180) straight up
  torso/root: x + = lean forward, y + = turn right, z + = lean to the player's right
"""
import json
import math

import numpy as np

# default pivots of the vanilla parts (x, y, z) in px
DEFAULT_PIVOT = {
    "head": (0.0, 0.0, 0.0),
    "torso": (0.0, 0.0, 0.0),
    "right_arm": (-5.0, 2.0, 0.0),
    "left_arm": (5.0, 2.0, 0.0),
    "right_leg": (-1.9, 12.0, 0.0),
    "left_leg": (1.9, 12.0, 0.0),
}
HIP = np.array([0.0, 12.0, 0.0])

# pose channel -> part it drives
CHANNELS = ["rt", "rr", "to", "hd", "ra", "la", "rl", "ll"]
PART_OF = {"hd": "head", "ra": "right_arm", "la": "left_arm", "rl": "right_leg", "ll": "left_leg"}

# boxes (min corner, max corner) relative to each part's own pivot - used for the preview only
BOX = {
    "head": ((-4, -8, -4), (4, 0, 4)),
    "torso": ((-4, 0, -2), (4, 12, 2)),
    "right_arm": ((-3, -2, -2), (1, 10, 2)),
    "left_arm": ((-1, -2, -2), (3, 10, 2)),
    "right_leg": ((-2, 0, -2), (2, 12, 2)),
    "left_leg": ((-2, 0, -2), (2, 12, 2)),
}


def rx(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def ry(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def rz(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def rot(xyz_deg):
    x, y, z = (math.radians(v) for v in xyz_deg)
    return rz(z) @ ry(y) @ rx(x)


def solve(pose):
    """pose: dict channel -> (a, b, c). Returns part -> (rotation matrix, world pivot)."""
    r_root = rot(pose["rr"])
    t = np.array(pose["rt"], dtype=float)

    def root_tf(v):
        return HIP + r_root @ (np.asarray(v, dtype=float) - HIP) + t

    r_t = rot(pose["to"])
    out = {"torso": (r_root @ r_t, root_tf((0, 0, 0)))}
    for ch in ("hd", "ra", "la"):
        part = PART_OF[ch]
        out[part] = (r_root @ r_t @ rot(pose[ch]), root_tf(r_t @ np.array(DEFAULT_PIVOT[part])))
    for ch in ("rl", "ll"):
        part = PART_OF[ch]
        out[part] = (r_root @ rot(pose[ch]), root_tf(DEFAULT_PIVOT[part]))
    return out


def euler_zyx(r, prev=None):
    """R = Rz*Ry*Rx -> (x, y, z) radians, choosing the equivalent solution closest to prev."""
    y = -math.asin(max(-1.0, min(1.0, r[2][0])))
    if abs(math.cos(y)) > 1e-6:
        x = math.atan2(r[2][1], r[2][2])
        z = math.atan2(r[1][0], r[0][0])
    else:  # gimbal lock: put everything in z
        x = 0.0
        z = math.atan2(-r[0][1], r[1][1])
    cands = [(x, y, z), (x + math.pi, math.pi - y, z + math.pi)]
    if prev is None:
        return cands[0]
    best, best_d = None, 1e18
    for c in cands:
        c2 = []
        for v, p in zip(c, prev):
            while v - p > math.pi:
                v -= 2 * math.pi
            while v - p < -math.pi:
                v += 2 * math.pi
            c2.append(v)
        d = sum((a - b) ** 2 for a, b in zip(c2, prev))
        if d < best_d:
            best, best_d = tuple(c2), d
    return best


# ------------------------------------------------------------------------------------------ timeline

EASES = {
    "linear": lambda u: u,
    "smooth": lambda u: u * u * (3 - 2 * u),
    "in": lambda u: u * u,
    "out": lambda u: 1 - (1 - u) ** 2,
    "snap": lambda u: 1 - (1 - u) ** 4,
}

ZERO = (0.0, 0.0, 0.0)


class Timeline:
    def __init__(self, length_sec):
        self.length = length_sec
        self.keys = []  # (t, full pose, ease)
        self.overlays = []
        self.cur = {c: ZERO for c in ["rt", "rr", "to", "hd", "ra", "la", "rl", "ll"]}
        self.key(0.0, ease="linear")

    def key(self, t, ease="smooth", **ch):
        for k, v in ch.items():
            assert k in self.cur, k
            self.cur[k] = tuple(float(x) for x in v)
        self.keys.append((t, dict(self.cur), ease))
        return self

    def shake(self, t0, t1, channels, amp, freq=6.0, fade=0.25):
        """Adds a trembling to the given channels (amp = degrees / px per axis)."""
        self.overlays.append((t0, t1, tuple(channels), tuple(amp), freq, fade))
        return self

    def pose_at(self, t):
        keys = self.keys
        if t <= keys[0][0]:
            pose = dict(keys[0][1])
        elif t >= keys[-1][0]:
            pose = dict(keys[-1][1])
        else:
            pose = None
            for (t0, p0, _), (t1, p1, ease) in zip(keys, keys[1:]):
                if t0 <= t <= t1:
                    u = 0.0 if t1 == t0 else (t - t0) / (t1 - t0)
                    e = EASES[ease](u)
                    pose = {c: tuple(a + (b - a) * e for a, b in zip(p0[c], p1[c])) for c in p0}
                    break
        for t0, t1, chans, amp, freq, fade in self.overlays:
            if t0 <= t <= t1:
                env = min(1.0, (t - t0) / fade, (t1 - t) / fade) if fade > 0 else 1.0
                env = max(0.0, env)
                for i, c in enumerate(chans):
                    ph = i * 1.7
                    pose[c] = tuple(
                        v + amp[a] * env * (math.sin(2 * math.pi * freq * t + ph + a * 2.1) * 0.6
                                            + math.sin(2 * math.pi * freq * 1.9 * t + ph * 1.3 + a) * 0.4)
                        for a, v in enumerate(pose[c]))
        return pose


# ------------------------------------------------------------------------------------------ baking

def tick_key(tick):
    # PAL reads (int)(Float.parseFloat(key) * 20): aim a little past the tick to dodge float rounding
    return f"{(tick + 0.1) / 20:.5f}".rstrip("0").rstrip(".")


def bake(tl, name):
    n = int(round(tl.length * 20))
    bones = {p: {"rotation": {}, "position": {}} for p in DEFAULT_PIVOT}
    prev = {p: None for p in DEFAULT_PIVOT}
    for tick in range(n + 1):
        sol = solve(tl.pose_at(tick / 20.0))
        for part, (r, piv) in sol.items():
            e = euler_zyx(r, prev[part])
            prev[part] = e
            off = piv - np.array(DEFAULT_PIVOT[part])
            key = tick_key(tick)
            bones[part]["rotation"][key] = [round(math.degrees(v), 3) for v in e]
            bones[part]["position"][key] = [round(float(off[0]), 3), round(float(-off[1]), 3), round(float(off[2]), 3)]
    return {"format_version": "1.8.0",
            "animations": {name: {"loop": "hold_on_last_frame", "animation_length": round(n / 20.0, 4), "bones": bones}}}


# ------------------------------------------------------------------------------------------ preview

def preview(timelines, times, path, cols=5, azim=120):
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    from mpl_toolkits.mplot3d.art3d import Poly3DCollection

    rows = len(times) // cols + (1 if len(times) % cols else 0)
    fig = plt.figure(figsize=(4.2 * cols, 4.2 * rows))
    light = np.array([0.4, -0.6, 0.7])
    light /= np.linalg.norm(light)
    colors = {"head": "#e8b88a", "torso": "#4c6ef5", "right_arm": "#f08c00", "left_arm": "#37b24d",
              "right_leg": "#c2255c", "left_leg": "#1c7ed6"}
    faces = [((0, 1, 3, 2), (-1, 0, 0)), ((4, 5, 7, 6), (1, 0, 0)), ((0, 1, 5, 4), (0, 0, -1)),
             ((2, 3, 7, 6), (0, 0, 1)), ((0, 2, 6, 4), (0, -1, 0)), ((1, 3, 7, 5), (0, 1, 0))]
    for i, (tl, t, label) in enumerate(times):
        ax = fig.add_subplot(rows, cols, i + 1, projection="3d")
        sol = solve(tl.pose_at(t))
        for part, (r, piv) in sol.items():
            lo, hi = (np.array(v, dtype=float) for v in BOX[part])
            corners = np.array([[(lo, hi)[(k >> 2) & 1][0], (lo, hi)[(k >> 1) & 1][1], (lo, hi)[k & 1][2]] for k in range(8)])
            # corner index bits: x=bit2, y=bit1, z=bit0
            world = (r @ corners.T).T + piv
            # model (x, y-down, z-back) -> plot (X = x, Y = z, Z = -y)
            pts = np.stack([world[:, 0], world[:, 2], -world[:, 1]], axis=1)
            polys, shades = [], []
            for idx, nrm in faces:
                n_world = r @ np.array(nrm, dtype=float)
                n_plot = np.array([n_world[0], n_world[2], -n_world[1]])
                shade = 0.55 + 0.45 * max(0.0, float(np.dot(n_plot, light)))
                polys.append(pts[list(idx)])
                shades.append(shade)
            base = np.array(matplotlib.colors.to_rgb(colors[part]))
            fc = [tuple(np.clip(base * s, 0, 1)) for s in shades]
            ax.add_collection3d(Poly3DCollection(polys, facecolors=fc, edgecolors="#222", linewidths=0.4))
            if part == "head":  # face marker on the front (-z)
                c = (lo + hi) / 2
                q = np.array([[-2.5, -5.5, -4.05], [2.5, -5.5, -4.05], [2.5, -3.5, -4.05], [-2.5, -3.5, -4.05]])
                wq = (r @ q.T).T + piv
                ax.add_collection3d(Poly3DCollection([np.stack([wq[:, 0], wq[:, 2], -wq[:, 1]], axis=1)],
                                                     facecolors="#222"))
        ax.plot([-30, 30], [0, 0], [-24, -24], color="#888", lw=0.8)
        for a, b in (("set_xlim", (-26, 26)), ("set_ylim", (-26, 26)), ("set_zlim", (-30, 22))):
            getattr(ax, a)(*b)
        ax.set_box_aspect((1, 1, 1))
        ax.view_init(elev=14, azim=azim)
        ax.set_axis_off()
        ax.set_title(label, fontsize=9)
    fig.tight_layout()
    fig.savefig(path, dpi=60)
    plt.close(fig)
