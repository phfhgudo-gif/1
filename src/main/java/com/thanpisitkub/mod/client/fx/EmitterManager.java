package com.thanpisitkub.mod.client.fx;

import com.thanpisitkub.mod.PowerConfig;
import com.thanpisitkub.mod.ThanpisitkubMod;
import com.thanpisitkub.mod.fx.EmitterDef;
import com.thanpisitkub.mod.fx.Expr;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Runs particle emitters ("instant" bursts and "steady" streams) and turns them into
 * {@link BedrockParticle}s. Client only.
 */
@Mod.EventBusSubscriber(modid = ThanpisitkubMod.MOD_ID, value = Dist.CLIENT)
public final class EmitterManager {
    private EmitterManager() {
    }

    private static final double DT = 0.05;
    private static final List<Emitter> ACTIVE = new ArrayList<>();

    /** Starts an emitter. {@code yawDeg}: direction of the emitter's local +z axis (Minecraft yaw). */
    public static void spawn(String particle, ClientLevel level, double x, double y, double z, float yawDeg) {
        EmitterDef def = FxDefs.get(particle);
        SpriteSet sprites = FxDefs.sprites(particle);
        if (def == null || sprites == null) {
            ThanpisitkubMod.LOGGER.warn("Particle '{}' is not loaded (definition: {}, sprites: {})",
                    particle, def != null, sprites != null);
            return;
        }
        ACTIVE.add(new Emitter(def, sprites, level, x, y, z, yawDeg));
    }

    public static void clear() {
        ACTIVE.clear();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            ACTIVE.clear();
            return;
        }
        if (mc.isPaused()) return;
        Iterator<Emitter> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Emitter e = it.next();
            if (e.level != mc.level || !e.tick(mc)) it.remove();
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    /** One running emitter. */
    public static final class Emitter {
        private final EmitterDef def;
        private final SpriteSet sprites;
        private final ClientLevel level;
        private final double x, y, z;
        private final float yaw;
        private final double scale;
        private final double density;
        private final Random rng = new Random();
        private int age = 0;
        private double carry = 0.0;
        private int alive = 0;

        private Emitter(EmitterDef def, SpriteSet sprites, ClientLevel level, double x, double y, double z, float yaw) {
            this.def = def;
            this.sprites = sprites;
            this.level = level;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.scale = PowerConfig.EFFECT_SCALE.get();
            this.density = PowerConfig.PARTICLE_DENSITY.get();
        }

        void onParticleRemoved() {
            alive = Math.max(0, alive - 1);
        }

        /** @return true while the emitter should keep running. */
        boolean tick(Minecraft mc) {
            double time = age * DT;
            Expr.Ctx c = new Expr.Ctx();
            c.rng = rng;
            c.emitterAge = time;
            int toSpawn;
            boolean keepRunning;
            if (def.mode == EmitterDef.Mode.INSTANT) {
                double raw = def.count.eval(c);
                toSpawn = (int) Math.round(raw * density);
                if (raw >= 1.0 && toSpawn < 1) toSpawn = 1;
                keepRunning = false;
            } else {
                if (time >= def.activeSeconds) return false;
                carry += def.rate.eval(c) * density * DT;
                toSpawn = (int) carry;
                carry -= toSpawn;
                keepRunning = true;
            }
            toSpawn = Math.min(toSpawn, Math.max(0, def.maxParticles - alive));
            for (int i = 0; i < toSpawn; i++) {
                BedrockParticle p = BedrockParticle.create(level, def, sprites, x, y, z, yaw, scale, rng, this, time);
                mc.particleEngine.add(p);
                alive++;
            }
            age++;
            return keepRunning;
        }
    }
}
