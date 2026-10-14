package com.thanpisitkub.mod.move;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Describes one move (e.g. Galaxy Impact): which animation to play, when to spawn
 * which particle emitter, when to play which sound and when to hurt things.
 * Times are stored in game ticks (20 per second). Pure Java (no Minecraft classes).
 */
public final class MoveDef {

    /** A particle emitter spawned relative to the caster (forward = where the caster faces). */
    /** {@code ground}: height is measured from the ground the caster started on, not from the caster (who may be flying). */
    public record Fx(int tick, String particle, double forward, double side, double up, boolean ground) {
    }

    /** A sound that follows the caster. {@code volume} above 1.0 only widens the audible range. */
    public record Snd(int tick, String sound, float volume, float pitch) {
    }

    /** Camera shake on the caster's screen: {@code strength} fades out linearly over {@code durationTicks}. */
    public record Shake(int tick, float strength, int durationTicks) {
    }

    public record Alt(int tick, double height) {
    }

    public enum HitShape {SPHERE, LINE}

    /**
     * Damage event. SPHERE: {@code size} = radius around the caster.
     * LINE: {@code size} = length in front of the caster and {@code width} = full width.
     */
    public record Hit(int tick, HitShape shape, double size, double width, float damage, double knockback, double knockUp) {
    }

    public final String id;
    public final int index;
    public final String animation;
    public final int totalTicks;
    public final List<Fx> fx;
    public final List<Snd> sounds;
    public final List<Hit> hits;
    public final List<Shake> shakes;
    /** Flight path: height above the starting ground (blocks) over time; empty = the caster stays on the ground. */
    public final List<Alt> altitude;

    private MoveDef(Builder b) {
        this.id = b.id;
        this.index = b.index;
        this.animation = b.animation;
        this.totalTicks = b.totalTicks;
        b.fx.sort(Comparator.comparingInt(Fx::tick));
        b.sounds.sort(Comparator.comparingInt(Snd::tick));
        b.hits.sort(Comparator.comparingInt(Hit::tick));
        this.fx = List.copyOf(b.fx);
        this.sounds = List.copyOf(b.sounds);
        this.hits = List.copyOf(b.hits);
        this.shakes = List.copyOf(b.shakes);
        b.altitude.sort(Comparator.comparingInt(Alt::tick));
        this.altitude = List.copyOf(b.altitude);
    }

    public boolean hasFlight() {
        return !altitude.isEmpty();
    }

    /** Height above the starting ground at the given time (ticks, may be fractional). */
    public double altitudeAt(double tick) {
        if (altitude.isEmpty()) return 0.0;
        Alt first = altitude.get(0);
        if (tick <= first.tick()) return first.height();
        for (int i = 1; i < altitude.size(); i++) {
            Alt a = altitude.get(i - 1), c = altitude.get(i);
            if (tick <= c.tick()) {
                double u = c.tick() == a.tick() ? 1.0 : (tick - a.tick()) / (c.tick() - a.tick());
                return a.height() + (c.height() - a.height()) * u;
            }
        }
        return altitude.get(altitude.size() - 1).height();
    }

    public static Builder builder(String id, int index, String animation, double totalSeconds) {
        return new Builder(id, index, animation, totalSeconds);
    }

    public static int ticks(double seconds) {
        return (int) Math.round(seconds * 20.0);
    }

    public static final class Builder {
        private final String id;
        private final int index;
        private final String animation;
        private final int totalTicks;
        private final List<Fx> fx = new ArrayList<>();
        private final List<Snd> sounds = new ArrayList<>();
        private final List<Hit> hits = new ArrayList<>();
        private final List<Shake> shakes = new ArrayList<>();
        private final List<Alt> altitude = new ArrayList<>();
        private boolean groundMode = false;

        private Builder(String id, int index, String animation, double totalSeconds) {
            this.id = id;
            this.index = index;
            this.animation = animation;
            this.totalTicks = ticks(totalSeconds);
        }

        /** From now on, effects measure their height from the ground (true) or from the caster (false). */
        public Builder ground(boolean on) {
            this.groundMode = on;
            return this;
        }

        /** A point of the flight path: the caster should be {@code height} blocks above the start ground at {@code sec}. */
        public Builder alt(double sec, double height) {
            altitude.add(new Alt(ticks(sec), height));
            return this;
        }

        /** A smooth change of height; ease: "smooth", "in" (accelerating) or "out" (decelerating). */
        public Builder altRamp(double fromSec, double toSec, double fromHeight, double toHeight, String ease) {
            int steps = Math.max(2, (int) Math.round((toSec - fromSec) * 5));
            for (int i = 0; i <= steps; i++) {
                double u = (double) i / steps;
                double e = switch (ease) {
                    case "in" -> u * u;
                    case "out" -> 1 - (1 - u) * (1 - u);
                    default -> u * u * (3 - 2 * u);
                };
                alt(fromSec + (toSec - fromSec) * u, fromHeight + (toHeight - fromHeight) * e);
            }
            return this;
        }

        /** One emitter at a moment in time. */
        public Builder fx(double sec, String particle, double forward, double side, double up) {
            fx.add(new Fx(ticks(sec), particle, forward, side, up, groundMode));
            return this;
        }

        /** The same emitter repeated every {@code stepSec} from {@code startSec} to {@code endSec} (inclusive). */
        public Builder fxEvery(double startSec, double endSec, double stepSec, String particle, double forward, double side, double up) {
            for (double t = startSec; t <= endSec + 1e-9; t += stepSec) fx(t, particle, forward, side, up);
            return this;
        }

        /**
         * A wave travelling away from the caster: the first emitter at {@code fromForward}
         * at {@code startSec}, the next {@code stepForward} blocks further {@code stepSec} later, and so on.
         */
        public Builder fxLine(double startSec, double stepSec, String particle, double fromForward, double toForward, double stepForward, double side, double up) {
            double t = startSec;
            for (double d = fromForward; d <= toForward + 1e-9; d += stepForward) {
                fx(t, particle, d, side, up);
                t += stepSec;
            }
            return this;
        }

        /**
         * {@code count} emitters evenly spread on a circle of {@code radius} blocks around the caster
         * (all at the same moment). {@code phaseDeg} rotates the ring.
         */
        public Builder fxRing(double sec, String particle, int count, double radius, double up, double phaseDeg) {
            for (int i = 0; i < count; i++) {
                double a = Math.toRadians(phaseDeg + 360.0 * i / count);
                fx(sec, particle, Math.cos(a) * radius, Math.sin(a) * radius, up);
            }
            return this;
        }

        /** A ring that expands: ring i appears {@code stepSec} after ring i-1 and is {@code stepRadius} wider. */
        public Builder fxRingWave(double startSec, double stepSec, String particle, int count,
                                  double fromRadius, double toRadius, double stepRadius, double up) {
            double t = startSec;
            int n = 0;
            for (double r = fromRadius; r <= toRadius + 1e-9; r += stepRadius) {
                fxRing(t, particle, count, r, up, (n++ % 2) * (180.0 / count));
                t += stepSec;
            }
            return this;
        }

        /**
         * Emitters scattered all around the caster, deterministic (golden angle), so the effect is spread
         * over the whole area instead of piling up on the player. {@code perStep} emitters every {@code stepSec}.
         */
        public Builder fxScatter(double startSec, double endSec, double stepSec, int perStep, String particle,
                                 double minR, double maxR, double up) {
            final double golden = Math.toRadians(137.50776405);
            int n = 0;
            for (double t = startSec; t <= endSec + 1e-9; t += stepSec) {
                for (int k = 0; k < perStep; k++, n++) {
                    double a = n * golden;
                    double f = ((n * 0.6180339887) % 1.0);
                    double r = minR + (maxR - minR) * f;
                    fx(t, particle, Math.cos(a) * r, Math.sin(a) * r, up);
                }
            }
            return this;
        }

        /**
         * Emitters spread along the forward line (random-looking but deterministic side offsets up to
         * {@code maxSide}), from {@code fromForward} to {@code toForward}, one every {@code stepForward}.
         */
        public Builder fxLineSpread(double startSec, double stepSec, String particle, double fromForward,
                                    double toForward, double stepForward, double maxSide, double up) {
            double t = startSec;
            int n = 0;
            for (double d = fromForward; d <= toForward + 1e-9; d += stepForward, n++) {
                double side = ((n * 0.6180339887) % 1.0 * 2.0 - 1.0) * maxSide;
                fx(t, particle, d, side, up);
                t += stepSec;
            }
            return this;
        }

        public Builder sound(double sec, String sound, float volume, float pitch) {
            sounds.add(new Snd(ticks(sec), sound, volume, pitch));
            return this;
        }

        public Builder shake(double sec, double strength, double durationSec) {
            shakes.add(new Shake(ticks(sec), (float) strength, Math.max(1, ticks(durationSec))));
            return this;
        }

        public Builder sphereHit(double sec, double radius, float damage, double knockback, double knockUp) {
            hits.add(new Hit(ticks(sec), HitShape.SPHERE, radius, radius * 2, damage, knockback, knockUp));
            return this;
        }

        public Builder lineHit(double sec, double length, double width, float damage, double knockback, double knockUp) {
            hits.add(new Hit(ticks(sec), HitShape.LINE, length, width, damage, knockback, knockUp));
            return this;
        }

        public MoveDef build() {
            return new MoveDef(this);
        }
    }
}
