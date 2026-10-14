#!/usr/bin/env python3
"""
Authors the two moves as poses (see anim_rig.py for the conventions) and bakes them into
src/main/resources/assets/thanpisitkub/player_animation/<move>.json

usage: python3 tools/make_animation.py [--preview]
Times here are the SAME seconds as in Moves.java (sound / effects / flight), so keep them in sync.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from anim_rig import Timeline, bake, preview  # noqa: E402

ROOT = os.path.join(os.path.dirname(__file__), "..")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "thanpisitkub", "player_animation")
Z = (0, 0, 0)


def sym_arms(tl, t, ra, la, ease="smooth", **more):
    return tl.key(t, ease=ease, ra=ra, la=la, **more)


# ============================================================================================ Galaxy Impact
def galaxy_impact():
    tl = Timeline(27.0)

    # ---- 0 - 3.4 s  gather power on the ground: wide stance, arms lift into a V above the head, trembling
    tl.key(0.5, rl=(0, 0, 16), ll=(0, 0, -16), rt=(0, 0.5, 0), ra=(0, 0, 22), la=(0, 0, -22),
           to=(6, 0, 0), hd=(0, 0, 0))
    tl.key(1.8, ra=(0, 0, 120), la=(0, 0, -120), to=(-4, 0, 0), hd=(-18, 0, 0), rl=(0, 0, 20), ll=(0, 0, -20),
           rt=(0, 0.7, 0))
    tl.key(3.0, ra=(0, 0, 163), la=(0, 0, -163), to=(-12, 0, 0), hd=(-32, 0, 0), rl=(0, 0, 22), ll=(0, 0, -22))
    tl.shake(0.6, 3.0, ["ra", "la", "to", "rl", "ll"], (2.5, 1.5, 2.5), freq=7.0)
    # crouch + swing the arms down and back (wind-up for take-off)
    tl.key(3.3, ease="in", ra=(55, 0, 25), la=(55, 0, -25), to=(24, 0, 0), hd=(-10, 0, 0), rl=(0, 0, 26),
           ll=(0, 0, -26), rt=(0, 1.6, 0))
    # ---- 3.4 - 6.4 s  launch and spiral up into the sky, arms stretched overhead
    tl.key(3.6, ease="snap", ra=(0, 0, 172), la=(0, 0, -172), to=(-8, 0, 0), hd=(-14, 0, 0),
           rl=(10, 0, 3), ll=(10, 0, -3), rt=(0, -1.5, 0))
    tl.key(6.4, ease="smooth", rr=(0, 1080, 0), ra=(0, 0, 170), la=(0, 0, -170), to=(-6, 0, 0), hd=(-12, 0, 0),
           rl=(14, 0, 4), ll=(14, 0, -4), rt=(0, -1.0, 0))
    tl.shake(3.8, 6.3, ["ra", "la"], (2, 1, 3), freq=6.0)

    # ---- 6.4 - 6.9  wind up the right fist, then punch 1 at 6.9
    tl.key(6.45, ease="linear", rr=(0, 0, 0))   # spin ends on the same facing (1080 deg = 3 turns)
    tl.key(6.85, ease="out", rr=(12, 0, 0), to=(4, 32, 0), hd=(0, -30, 0), ra=(70, 0, 10), la=(-62, 0, -14),
           rl=(22, 0, 6), ll=(-26, 0, -6), rt=Z)
    tl.key(7.0, ease="snap", rr=(16, 0, 0), to=(12, -36, 0), hd=(0, 34, 0), ra=(-90, 36, 0), la=(40, 0, -20))
    tl.key(7.5, ease="smooth")
    # ---- 7.5 - 13.4  floating in the sky, the right fist charged high overhead
    tl.key(8.6, rr=(0, 0, 0), to=(-6, 0, 0), hd=(-10, 0, 0), ra=(0, 0, 176), la=(0, 0, -62),
           rl=(8, 0, 9), ll=(14, 0, -9))
    tl.key(11.0, to=(-8, 14, 0), hd=(-12, -14, 0), ra=(0, 0, 178), la=(0, 0, -66), rl=(10, 0, 8), ll=(16, 0, -10))
    tl.key(13.3, to=(-9, -8, 0), hd=(-12, 8, 0), ra=(0, 0, 180), la=(0, 0, -60))
    tl.shake(8.0, 13.3, ["ra", "la"], (1.8, 1.0, 2.2), freq=5.0)
    # ---- 13.4 - 13.9  arm goes behind the head, then punch 2 smashes forward-down
    tl.key(13.82, ease="out", rr=(4, 0, 0), to=(-22, 0, 0), hd=(-20, 0, 0), ra=(-205, 0, 0), la=(-60, 0, -25),
           rl=(18, 0, 6), ll=(-18, 0, -6))
    tl.key(13.95, ease="snap", rr=(14, 0, 0), to=(34, 0, 0), hd=(-24, 0, 0), ra=(-92, 0, 0), la=(46, 0, -26))
    tl.key(14.5, ease="smooth", rr=(8, 0, 0), to=(14, 0, 0), ra=(-70, 0, 0))
    # ---- 14.5 - 15.4  left fist this time: up and back, then punch 3 at 15.4
    tl.key(15.25, ease="smooth", rr=(2, 0, 0), to=(-20, 0, 0), hd=(-18, 0, 0), la=(-205, 0, 0), ra=(52, 0, 22),
           rl=(-18, 0, 6), ll=(18, 0, -6))
    tl.key(15.4, ease="snap", rr=(14, 0, 0), to=(34, 0, 0), la=(-92, 0, 0), ra=(46, 0, 26))
    # ---- 15.5 - 17.3  dive head first with both fists ahead
    tl.key(15.9, ease="out", rr=(96, 0, 0), to=(-8, 0, 0), hd=(-78, 0, 0), ra=(0, 0, 175), la=(0, 0, -175),
           rl=(6, 0, 3), ll=(6, 0, -3))
    tl.key(17.1, ease="in", rr=(108, 0, 0), hd=(-82, 0, 0))
    tl.shake(15.9, 17.2, ["ra", "la", "rl", "ll"], (2.5, 1, 2.5), freq=9.0)
    # ---- 17.4  crash landing: fist in the ground, wide crouch
    tl.key(17.4, ease="snap", rr=(22, 0, 0), to=(24, 0, 0), hd=(-52, 0, 0), ra=(-68, 0, 0), la=(52, 0, -28),
           rl=(-22, 0, 30), ll=(10, 0, -34), rt=(0, 5.0, 0))
    tl.shake(17.4, 19.0, ["rr", "to", "ra", "la"], (1.2, 0.5, 1.5), freq=11.0, fade=0.1)
    tl.key(19.0, ease="smooth", rr=(22, 0, 0))
    # ---- 19 - 27  stand up: right fist to the sky in victory, then slowly relax
    tl.key(20.8, ease="smooth", rr=Z, to=(-8, 0, 0), hd=(-18, 0, 0), ra=(0, 0, 172), la=(0, 0, -16),
           rl=(0, 0, 14), ll=(0, 0, -14), rt=(0, 0.3, 0))
    tl.key(24.0, ease="smooth", to=(-6, 0, 0), hd=(-14, 0, 0), ra=(0, 0, 170), la=(0, 0, -20))
    tl.shake(20.8, 24.0, ["ra", "to"], (1.0, 0.6, 1.0), freq=3.0)
    tl.key(26.5, ease="smooth", to=Z, hd=Z, ra=(0, 0, 4), la=(0, 0, -4), rl=Z, ll=Z, rt=Z)
    tl.key(27.0, ease="smooth", ra=Z, la=Z)
    return tl


# ============================================================================================ Galaxy Divide
def chop(tl, t, hand):
    """One hand-chop at time t with the given hand ('r' / 'l'): raise (0.28 s before), cut, recover."""
    s = 1 if hand == "r" else -1
    a = "ra" if hand == "r" else "la"
    b = "la" if hand == "r" else "ra"
    tl.key(t - 0.28, ease="out", **{a: (0, 0, 150 * s), b: (-30, 0, -30 * s)}, to=(-10, -34 * s, 0), hd=(0, 30 * s, 0))
    tl.key(t, ease="snap", **{a: (-70, 18 * s, 0), b: (35, 0, -26 * s)}, to=(22, 38 * s, 0), hd=(0, -34 * s, 0))
    tl.key(t + 0.3, ease="smooth", **{a: (-45, 8 * s, 0)}, to=(8, 12 * s, 0), hd=(0, -10 * s, 0))


def galaxy_divide():
    tl = Timeline(27.5)

    # ---- 0 - 1.4 s  ready stance (sideways, blade hand forward)
    tl.key(0.6, rl=(-20, 0, 8), ll=(22, 0, -8), to=(5, 30, 0), hd=(0, -26, 0), ra=(-82, 0, 0), la=(24, 0, -34),
           rt=(0, 0.6, 0))
    tl.key(1.2, to=(6, 34, 0), hd=(0, -28, 0), ra=(-84, 0, 0), la=(26, 0, -36))
    # ---- 1.5 - 3.0  four chops, right-left-right-left
    chop(tl, 1.5, "r")
    chop(tl, 2.0, "l")
    chop(tl, 2.5, "r")
    chop(tl, 3.0, "l")
    # ---- 3.5 - 5.0  open the arms wide and gather
    tl.key(4.0, ease="smooth", rr=Z, to=(-12, 0, 0), hd=(-24, 0, 0), ra=(0, 0, 90), la=(0, 0, -90),
           rl=(0, 0, 18), ll=(0, 0, -18), rt=(0, 0.5, 0))
    tl.key(5.0, ra=(-8, 0, 92), la=(-8, 0, -92), to=(-14, 0, 0))
    tl.shake(3.6, 5.0, ["ra", "la", "to"], (2.5, 1.5, 2.5), freq=7.0)
    # ---- 5.2 - 6.8  waves: both palms push forward, then one big sweep
    tl.key(5.4, ease="snap", rr=(12, 0, 0), to=(16, 0, 0), hd=(-14, 0, 0), ra=(-86, 14, 0), la=(-86, -14, 0),
           rl=(-22, 0, 6), ll=(20, 0, -6))
    tl.key(5.9, ease="smooth", to=(8, 0, 0), ra=(-60, 10, 0), la=(-60, -10, 0))
    tl.key(6.3, ease="out", rr=(6, 0, 0), to=(-6, 24, 0), hd=(0, -20, 0), ra=(-20, 0, 100), la=(-70, 0, -10))
    tl.key(6.5, ease="snap", rr=(16, 0, 0), to=(22, -30, 0), hd=(0, 30, 0), ra=(-84, 40, 0), la=(30, 0, -50),
           rl=(24, 0, 10), ll=(-24, 0, -10))
    # ---- 7 - 12.5  guard stance, calm breathing
    tl.key(7.6, ease="smooth", rr=Z, to=(8, 0, 0), hd=(-6, 0, 0), ra=(-62, 10, 0), la=(-62, -10, 0),
           rl=(-18, 0, 7), ll=(18, 0, -7), rt=(0, 0.6, 0))
    tl.key(10.0, to=(10, 6, 0), ra=(-66, 12, 0), la=(-58, -10, 0))
    tl.key(12.3, to=(8, -6, 0), ra=(-62, 10, 0), la=(-62, -10, 0))
    tl.shake(7.8, 12.2, ["ra", "la"], (1.0, 0.6, 1.0), freq=2.0)
    # ---- 12.5 - 16.0  rise: arms joined overhead, slow spin, ki gathering
    tl.key(13.0, ease="smooth", ra=(0, 0, 150), la=(0, 0, -150), to=(-8, 0, 0), hd=(-18, 0, 0),
           rl=(0, 0, 8), ll=(0, 0, -8), rt=(0, -0.5, 0))
    tl.key(16.0, ease="smooth", rr=(0, 720, 0), ra=(0, 0, 176), la=(0, 0, -176), to=(-8, 0, 0), hd=(-20, 0, 0),
           rl=(10, 0, 3), ll=(10, 0, -3))
    tl.shake(13.2, 16.0, ["ra", "la", "to"], (2.2, 1.2, 2.5), freq=8.0)
    tl.key(16.05, ease="linear", rr=Z)
    # ---- 16.0 - 16.75  swing the right arm up behind, then the main cut
    tl.key(16.4, ease="out", rr=(4, 0, 0), to=(-22, 0, 0), hd=(-24, 0, 0), ra=(-205, 0, 0), la=(-40, 0, -36),
           rl=(20, 0, 8), ll=(-14, 0, -8))
    tl.key(16.6, ease="snap", rr=(14, 0, 0), to=(30, 0, 0), hd=(-34, 0, 0), ra=(-72, 0, 0), la=(46, 0, -28),
           rl=(26, 0, 10), ll=(-10, 0, -10))
    # ---- 16.6 - 22  hold the cut while floating
    tl.key(18.5, ease="smooth", rr=(10, 0, 0), to=(18, 4, 0), hd=(-24, -4, 0), ra=(-78, 4, 0), la=(40, 0, -30))
    tl.key(21.8, ease="smooth", rr=(10, 0, 0), to=(16, -4, 0), ra=(-76, 2, 0))
    tl.shake(17.0, 21.8, ["ra", "la"], (1.2, 0.6, 1.2), freq=2.5)
    # ---- 22 - 23.4  spread the arms and call the sky down, then slam
    tl.key(22.15, ease="out", rr=(-4, 0, 0), to=(-16, 0, 0), hd=(-34, 0, 0), ra=(0, 0, 150), la=(0, 0, -150),
           rl=(6, 0, 10), ll=(6, 0, -10))
    tl.key(22.4, ease="snap", rr=(16, 0, 0), to=(28, 0, 0), hd=(-40, 0, 0), ra=(-82, 8, 0), la=(-82, -8, 0),
           rl=(-14, 0, 12), ll=(14, 0, -12))
    tl.shake(22.4, 24.0, ["rr", "to", "ra", "la"], (1.2, 0.5, 1.5), freq=10.0, fade=0.1)
    # ---- 23.5 - 27.5  float down, arms open, land and relax
    tl.key(24.6, ease="smooth", rr=Z, to=(-6, 0, 0), hd=(-14, 0, 0), ra=(0, 0, 50), la=(0, 0, -50),
           rl=(6, 0, 12), ll=(6, 0, -12))
    tl.key(26.5, ease="smooth", to=(2, 0, 0), hd=Z, ra=(0, 0, 14), la=(0, 0, -14), rl=(0, 0, 8), ll=(0, 0, -8),
           rt=Z)
    tl.key(27.5, ease="smooth", to=Z, ra=Z, la=Z, rl=Z, ll=Z)
    return tl


MOVES = {"galaxy_impact": galaxy_impact, "galaxy_divide": galaxy_divide}

PREVIEW_TIMES = {
    "galaxy_impact": [0.0, 1.8, 3.0, 3.3, 4.5, 5.5, 6.85, 7.05, 10.0, 13.82, 13.95, 15.25, 15.4, 16.5, 17.2,
                      17.45, 19.0, 21.0, 24.0, 26.9],
    "galaxy_divide": [0.6, 1.22, 1.5, 1.75, 2.0, 3.0, 4.5, 5.45, 6.3, 6.55, 9.0, 14.5, 16.3, 16.65, 19.0, 22.1,
                      22.45, 24.6, 26.5, 27.4],
}


def main():
    os.makedirs(OUT, exist_ok=True)
    do_preview = "--preview" in sys.argv
    for name, build in MOVES.items():
        tl = build()
        data = bake(tl, name)
        with open(os.path.join(OUT, name + ".json"), "w") as f:
            json.dump(data, f, separators=(",", ":"))
        print(name, "baked", tl.length, "s")
        if do_preview:
            outdir = os.environ.get("PREVIEW_DIR", ".")
            items = [(tl, t, f"{t:.2f}s") for t in PREVIEW_TIMES[name]]
            preview(None, items, os.path.join(outdir, name + ".png"), cols=5, azim=125)
            preview(None, items, os.path.join(outdir, name + "_side.png"), cols=5, azim=180)


if __name__ == "__main__":
    main()
