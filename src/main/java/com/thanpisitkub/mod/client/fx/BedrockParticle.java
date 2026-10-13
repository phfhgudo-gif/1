package com.thanpisitkub.mod.client.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.thanpisitkub.mod.PowerConfig;
import com.thanpisitkub.mod.fx.EmitterDef;
import com.thanpisitkub.mod.fx.Expr;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * A particle driven by an {@link EmitterDef}: flip-book animation (the "Play Animation" part),
 * tint over lifetime, size curves, spin, drag/acceleration and three facing modes
 * (camera / flat on the ground / vertical pillar).
 */
public class BedrockParticle extends TextureSheetParticle {
    private static final double DT = 0.05; // one game tick in seconds
    private static final int FULL_BRIGHT = 0xF000F0;

    private final EmitterDef def;
    private final SpriteSet sprites;
    private final double scale;
    private final double lifeSeconds;
    private final int fixedFrame;
    private final double ax, ay, az; // world-space acceleration (blocks / s^2)
    private final double drag;
    private final double spinStart, spinRate; // degrees, degrees / s
    private final Expr.Ctx ctx = new Expr.Ctx();
    private final double[] size = new double[2];
    private final float[] rgba = new float[4];
    private double vx, vy, vz; // blocks / s
    private int lastFrame = -1;
    @Nullable
    private EmitterManager.Emitter owner;

    private BedrockParticle(ClientLevel level, double x, double y, double z, EmitterDef def, SpriteSet sprites,
                            double scale, double lifeSeconds, int fixedFrame,
                            double vx, double vy, double vz, double ax, double ay, double az,
                            double spinStart, double spinRate, double r1, double r2, double r3, double r4,
                            @Nullable EmitterManager.Emitter owner) {
        super(level, x, y, z);
        this.def = def;
        this.sprites = sprites;
        this.scale = scale;
        this.lifeSeconds = lifeSeconds;
        this.fixedFrame = fixedFrame;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.ax = ax;
        this.ay = ay;
        this.az = az;
        this.drag = def.drag;
        this.spinStart = spinStart;
        this.spinRate = spinRate;
        this.owner = owner;
        this.ctx.r1 = r1;
        this.ctx.r2 = r2;
        this.ctx.r3 = r3;
        this.ctx.r4 = r4;
        this.ctx.lifetime = lifeSeconds;
        this.ctx.rng = new Random();
        this.lifetime = Math.max(1, (int) Math.ceil(lifeSeconds / DT));
        this.hasPhysics = false;
        this.gravity = 0.0f;
        setSprite(sprites.get(0, 1));
        updateBounds();
    }

    /**
     * Builds one particle of {@code def} at the emitter origin, evaluating shape / speed / lifetime
     * expressions once. {@code scale} = visual scale (config effectScale); {@code yawDeg} orients the
     * emitter's local axes (+z = forward).
     */
    public static BedrockParticle create(ClientLevel level, EmitterDef def, SpriteSet sprites,
                                         double ox, double oy, double oz, float yawDeg, double scale,
                                         Random rng, @Nullable EmitterManager.Emitter owner, double emitterAge) {
        Expr.Ctx c = new Expr.Ctx();
        c.rng = rng;
        c.r1 = rng.nextDouble();
        c.r2 = rng.nextDouble();
        c.r3 = rng.nextDouble();
        c.r4 = rng.nextDouble();
        c.emitterAge = emitterAge;
        double life = Math.max(0.05, def.lifetime.eval(c));
        c.lifetime = life;

        // uniform random unit direction: used for the sphere surface and for the initial velocity
        double dirY = rng.nextDouble() * 2.0 - 1.0;
        double angle = rng.nextDouble() * Math.PI * 2.0;
        double ring = Math.sqrt(Math.max(0.0, 1.0 - dirY * dirY));
        double dx = ring * Math.cos(angle), dy = dirY, dzz = ring * Math.sin(angle);

        double lx = def.offset[0].eval(c), ly = def.offset[1].eval(c), lz = def.offset[2].eval(c);
        if (def.shape == EmitterDef.Shape.SPHERE) {
            double radius = def.radius.eval(c);
            lx += dx * radius;
            ly += dy * radius;
            lz += dzz * radius;
        }
        double speed = def.speed.eval(c);
        double lvx = dx * speed, lvy = dy * speed, lvz = dzz * speed;
        double lax = def.accel[0].eval(c), lay = def.accel[1].eval(c), laz = def.accel[2].eval(c);

        double yaw = Math.toRadians(yawDeg);
        double fwdX = -Math.sin(yaw), fwdZ = Math.cos(yaw);
        double leftX = Math.cos(yaw), leftZ = Math.sin(yaw);

        double px = ox + (lx * leftX + lz * fwdX) * scale;
        double py = oy + ly * scale;
        double pz = oz + (lx * leftZ + lz * fwdZ) * scale;
        double wvx = (lvx * leftX + lvz * fwdX) * scale;
        double wvy = lvy * scale;
        double wvz = (lvx * leftZ + lvz * fwdZ) * scale;
        double wax = (lax * leftX + laz * fwdX) * scale;
        double way = lay * scale;
        double waz = (lax * leftZ + laz * fwdZ) * scale;

        double spinStart = def.spinRot.eval(c);
        double spinRate = def.spinRate.eval(c);
        int frame = def.frames > 1 ? rng.nextInt(def.frames) : 0;

        return new BedrockParticle(level, px, py, pz, def, sprites, scale, life, frame,
                wvx, wvy, wvz, wax, way, waz, spinStart, spinRate, c.r1, c.r2, c.r3, c.r4, owner);
    }

    // ------------------------------------------------------------------------------ logic

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        vx += (ax - drag * vx) * DT;
        vy += (ay - drag * vy) * DT;
        vz += (az - drag * vz) * DT;
        this.x += vx * DT;
        this.y += vy * DT;
        this.z += vz * DT;
        updateBounds();
    }

    @Override
    public void remove() {
        super.remove();
        if (owner != null) {
            owner.onParticleRemoved();
            owner = null;
        }
    }

    /** Keep the bounding box as big as the quad so vanilla frustum culling never hides a huge sprite. */
    private void updateBounds() {
        ctx.age = Math.min(age * DT, lifeSeconds);
        def.sizeAt(ctx, size);
        double r = Math.max(Math.abs(size[0]), Math.abs(size[1])) * scale + 0.5;
        this.setBoundingBox(new AABB(x - r, y - r, z - r, x + r, y + r, z + r));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return def.additive ? FxRenderTypes.ADDITIVE : FxRenderTypes.TRANSLUCENT;
    }

    // ---------------------------------------------------------------------------- rendering

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        double ageSec = Math.min((age + partialTicks) * DT, lifeSeconds);
        ctx.age = ageSec;
        def.sizeAt(ctx, size);
        float w = (float) (size[0] * scale);
        float h = (float) (size[1] * scale);
        if (w <= 0.0f || h <= 0.0f) return;

        def.tint.eval(Mth.clamp(ageSec / lifeSeconds, 0.0, 1.0), rgba);
        if (rgba[3] <= 0.003f) return;

        int frame = def.frameAt(ageSec, lifeSeconds, fixedFrame);
        if (frame != lastFrame) {
            setSprite(sprites.get(frame, Math.max(1, def.frames - 1)));
            lastFrame = frame;
        }

        Vec3 cam = camera.getPosition();
        float px = (float) (Mth.lerp((double) partialTicks, this.xo, this.x) - cam.x());
        float py = (float) (Mth.lerp((double) partialTicks, this.yo, this.y) - cam.y());
        float pz = (float) (Mth.lerp((double) partialTicks, this.zo, this.z) - cam.z());

        // basis vectors of the quad: "right" and "up" as seen on the sprite
        float rx, ry, rz, ux, uy, uz;
        float roll = (float) Math.toRadians(spinStart + spinRate * ageSec);
        switch (def.facing) {
            case FLAT -> {
                rx = 1;
                ry = 0;
                rz = 0;
                ux = 0;
                uy = 0;
                uz = -1;
            }
            case VERTICAL -> {
                float len = (float) Math.sqrt(px * px + pz * pz);
                if (len < 1.0e-4f) {
                    rx = 1;
                    rz = 0;
                } else {
                    rx = -pz / len;
                    rz = px / len;
                }
                ry = 0;
                ux = 0;
                uy = 1;
                uz = 0;
                roll = 0.0f;
            }
            default -> {
                Vector3f left = camera.getLeftVector();
                Vector3f up = camera.getUpVector();
                rx = -left.x();
                ry = -left.y();
                rz = -left.z();
                ux = up.x();
                uy = up.y();
                uz = up.z();
            }
        }

        float cos = Mth.cos(roll), sin = Mth.sin(roll);
        float u0 = getU0(), u1 = getU1(), v0 = getV0(), v1 = getV1();
        float r = rgba[0], g = rgba[1], b = rgba[2], a = rgba[3];

        // corners (sx, sy): (-1,-1) (-1,1) (1,1) (1,-1)
        vertex(buffer, px, py, pz, rx, ry, rz, ux, uy, uz, -w, -h, cos, sin, u0, v1, r, g, b, a);
        vertex(buffer, px, py, pz, rx, ry, rz, ux, uy, uz, -w, h, cos, sin, u0, v0, r, g, b, a);
        vertex(buffer, px, py, pz, rx, ry, rz, ux, uy, uz, w, h, cos, sin, u1, v0, r, g, b, a);
        vertex(buffer, px, py, pz, rx, ry, rz, ux, uy, uz, w, -h, cos, sin, u1, v1, r, g, b, a);
    }

    private static void vertex(VertexConsumer buf, float px, float py, float pz,
                               float rx, float ry, float rz, float ux, float uy, float uz,
                               float a, float b, float cos, float sin, float u, float v,
                               float r, float g, float bl, float alpha) {
        float ox = a * cos - b * sin; // rotate (a, b) by the spin angle
        float oy = a * sin + b * cos;
        buf.vertex(px + rx * ox + ux * oy, py + ry * ox + uy * oy, pz + rz * ox + uz * oy)
                .uv(u, v)
                .color(r, g, bl, alpha)
                .uv2(FULL_BRIGHT)
                .endVertex();
    }

    // ------------------------------------------------------------------------------ provider

    /** Used by {@code /particle thanpisitkub:garp_...}: spawns one particle with default settings. */
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final String name;
        private final SpriteSet sprites;

        public Provider(String name, SpriteSet sprites) {
            this.name = name;
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd) {
            EmitterDef def = FxDefs.get(name);
            if (def == null) return null;
            return create(level, def, sprites, x, y, z, 0.0f, PowerConfig.EFFECT_SCALE.get(), new Random(), null, 0.0);
        }
    }
}
