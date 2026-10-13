package com.thanpisitkub.mod.fx;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * One particle "emitter + appearance" definition, parsed from
 * {@code assets/thanpisitkub/bedrock_particles/<name>.json}.
 * Pure Java so it can be tested without Minecraft.
 */
public final class EmitterDef {

    public enum Facing {CAMERA, FLAT, VERTICAL}

    public enum Mode {INSTANT, STEADY}

    public enum Shape {POINT, SPHERE}

    /** Curve that feeds a variable (only {@code size} is used by the exported files). */
    public static final class Curve {
        final Expr input;
        final Expr range;
        final double[] nodes;

        Curve(Expr input, Expr range, double[] nodes) {
            this.input = input;
            this.range = range;
            this.nodes = nodes;
        }

        double eval(Expr.Ctx c) {
            if (nodes.length == 0) return 0;
            if (nodes.length == 1) return nodes[0];
            double r = range.eval(c);
            double t = r <= 1e-9 ? 0 : input.eval(c) / r;
            t = Math.max(0, Math.min(1, t));
            double f = t * (nodes.length - 1);
            int i = (int) Math.floor(f);
            if (i >= nodes.length - 1) return nodes[nodes.length - 1];
            double u = f - i;
            return nodes[i] + (nodes[i + 1] - nodes[i]) * u;
        }
    }

    public final String name;
    public final boolean additive;
    public final Facing facing;
    public final int frames;

    public final boolean flipbook;
    public final double fps;
    public final int flipbookMax;
    public final boolean stretch;
    public final boolean loop;
    public final boolean uvRandom;

    public final Mode mode;
    public final Expr count;
    public final Expr rate;
    public final int maxParticles;
    public final double activeSeconds;

    public final Shape shape;
    public final Expr radius;
    public final Expr[] offset;

    public final Expr speed;
    public final Expr[] accel;
    public final double drag;
    public final Expr lifetime;
    public final Expr[] size;
    public final Tint tint;
    public final Expr spinRot;
    public final Expr spinRate;
    private final Map<String, Curve> curves;

    private EmitterDef(String name, JsonObject o) {
        this.name = name;
        this.additive = bool(o, "additive", false);
        this.facing = switch (str(o, "facing", "camera").toLowerCase()) {
            case "flat" -> Facing.FLAT;
            case "vertical" -> Facing.VERTICAL;
            default -> Facing.CAMERA;
        };
        this.frames = Math.max(1, (int) num(o, "frames", 1));

        JsonObject fb = o.has("flipbook") && o.get("flipbook").isJsonObject() ? o.getAsJsonObject("flipbook") : null;
        this.flipbook = fb != null;
        this.fps = fb != null ? num(fb, "fps", 12) : 0;
        this.flipbookMax = fb != null ? (int) num(fb, "max", frames) : frames;
        this.stretch = fb != null && bool(fb, "stretch", false);
        this.loop = fb != null && bool(fb, "loop", true);
        this.uvRandom = bool(o, "uv_random", false);

        JsonObject em = obj(o, "emitter");
        this.mode = str(em, "mode", "instant").equalsIgnoreCase("steady") ? Mode.STEADY : Mode.INSTANT;
        this.count = expr(em, "count", "1");
        this.rate = expr(em, "rate", "0");
        this.maxParticles = (int) num(em, "maxp", 1000);
        this.activeSeconds = num(em, "active", 1);

        JsonObject sh = obj(o, "shape");
        this.shape = str(sh, "type", "point").equalsIgnoreCase("sphere") ? Shape.SPHERE : Shape.POINT;
        this.radius = expr(sh, "radius", "0");
        this.offset = exprArray(sh, "offset", 3);

        this.speed = expr(o, "speed", "0");
        this.accel = exprArray(o, "accel", 3);
        this.drag = num(o, "drag", 0);
        this.lifetime = expr(o, "lifetime", "1");
        this.size = exprArray(o, "size", 2);
        this.tint = Tint.fromJson(o.has("tint") && o.get("tint").isJsonObject() ? o.getAsJsonObject("tint") : null);

        JsonObject spin = obj(o, "spin");
        this.spinRot = expr(spin, "rot", "0");
        this.spinRate = expr(spin, "rate", "0");

        this.curves = new HashMap<>();
        JsonObject cs = obj(o, "curves");
        for (Map.Entry<String, JsonElement> e : cs.entrySet()) {
            if (!e.getValue().isJsonObject()) continue;
            JsonObject c = e.getValue().getAsJsonObject();
            JsonArray nodes = c.has("nodes") ? c.getAsJsonArray("nodes") : new JsonArray();
            double[] n = new double[nodes.size()];
            for (int i = 0; i < n.length; i++) n[i] = nodes.get(i).getAsDouble();
            curves.put(e.getKey(), new Curve(expr(c, "input", "v.particle_age"), expr(c, "range", "v.particle_lifetime"), n));
        }
    }

    public static EmitterDef fromJson(String name, JsonObject o) {
        return new EmitterDef(o.has("name") ? o.get("name").getAsString() : name, o);
    }

    /** Evaluates the curves and the width/height expressions. {@code out} = {halfWidth, halfHeight}. */
    public void sizeAt(Expr.Ctx c, double[] out) {
        Curve sc = curves.get("size");
        if (sc != null) c.size = sc.eval(c);
        out[0] = size[0].eval(c);
        out[1] = size[1].eval(c);
    }

    /** Which sprite frame to show. {@code fixedFrame} is the particle's random frame (uv_random). */
    public int frameAt(double ageSeconds, double lifeSeconds, int fixedFrame) {
        if (frames <= 1) return 0;
        if (!flipbook) return uvRandom ? Math.floorMod(fixedFrame, frames) : 0;
        int max = Math.max(1, Math.min(flipbookMax > 0 ? flipbookMax : frames, frames));
        int f;
        if (stretch) f = (int) Math.floor(ageSeconds / Math.max(lifeSeconds, 1e-6) * max);
        else f = (int) Math.floor(ageSeconds * fps);
        f = loop ? Math.floorMod(f, max) : Math.min(f, max - 1);
        return Math.max(0, Math.min(frames - 1, f));
    }

    // ------------------------------------------------------------ json helpers

    private static JsonObject obj(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonObject() ? o.getAsJsonObject(key) : new JsonObject();
    }

    private static String str(JsonObject o, String key, String def) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : def;
    }

    private static double num(JsonObject o, String key, double def) {
        if (!o.has(key) || !o.get(key).isJsonPrimitive()) return def;
        try {
            return o.get(key).getAsDouble();
        } catch (NumberFormatException e) {
            return Expr.parse(o.get(key).getAsString()).evalPlain();
        }
    }

    private static boolean bool(JsonObject o, String key, boolean def) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsBoolean() : def;
    }

    private static Expr expr(JsonObject o, String key, String def) {
        return Expr.parse(str(o, key, def));
    }

    private static Expr[] exprArray(JsonObject o, String key, int n) {
        Expr[] r = new Expr[n];
        JsonArray a = o.has(key) && o.get(key).isJsonArray() ? o.getAsJsonArray(key) : new JsonArray();
        for (int i = 0; i < n; i++) r[i] = i < a.size() ? Expr.parse(a.get(i).getAsString()) : Expr.constant(0);
        return r;
    }
}
