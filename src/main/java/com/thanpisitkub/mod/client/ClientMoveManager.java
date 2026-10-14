package com.thanpisitkub.mod.client;

import com.thanpisitkub.mod.PowerConfig;
import com.thanpisitkub.mod.ThanpisitkubMod;
import com.thanpisitkub.mod.client.anim.PalBridge;
import com.thanpisitkub.mod.client.anim.PalCheck;
import com.thanpisitkub.mod.client.fx.EmitterManager;
import com.thanpisitkub.mod.client.fx.FxDefs;
import com.thanpisitkub.mod.fx.EmitterDef;
import com.thanpisitkub.mod.move.MoveDef;
import com.thanpisitkub.mod.move.Moves;
import com.thanpisitkub.mod.net.MoveStatePacket;
import com.thanpisitkub.mod.registry.ModSounds;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client side of a move: plays the player animation, runs the particle / sound timeline
 * and (for the local player) freezes input and switches to third person.
 */
@Mod.EventBusSubscriber(modid = ThanpisitkubMod.MOD_ID, value = Dist.CLIENT)
public final class ClientMoveManager {
    private ClientMoveManager() {
    }

    private static final class ClientCast {
        final UUID id;
        final MoveDef move;
        final boolean local;
        final List<SoundInstance> sounds = new ArrayList<>();
        CameraType previousCamera;
        int tick = 0;
        int fxIndex = 0;
        int soundIndex = 0;
        double groundY;
        /** Recent emitters per particle: {tick, x, y, z}. Used so the same effect never stacks on itself. */
        final Map<String, List<double[]>> recentFx = new HashMap<>();

        ClientCast(UUID id, MoveDef move, boolean local) {
            this.id = id;
            this.move = move;
            this.local = local;
        }
    }

    private static final Map<UUID, ClientCast> CASTS = new HashMap<>();
    private static boolean warnedMissingAnimationLib = false;

    /** Called from MoveStatePacket on the client thread. */
    public static void onState(UUID caster, int moveIndex, int mode) {
        if (mode == MoveStatePacket.START) {
            start(caster, moveIndex);
        } else {
            end(caster, mode == MoveStatePacket.CANCEL);
        }
    }

    private static void start(UUID id, int moveIndex) {
        Minecraft mc = Minecraft.getInstance();
        MoveDef move = Moves.byIndex(moveIndex);
        if (mc.level == null || move == null) return;
        Player player = mc.level.getPlayerByUUID(id);
        if (!(player instanceof AbstractClientPlayer clientPlayer)) return;

        end(id, true); // replace a previous move of the same player

        boolean local = mc.player != null && mc.player.getUUID().equals(id);
        ClientCast cast = new ClientCast(id, move, local);
        cast.groundY = player.getY();
        CASTS.put(id, cast);

        if (PalCheck.isAvailable()) {
            PalBridge.play(clientPlayer, ThanpisitkubMod.id(move.animation));
        } else if (!warnedMissingAnimationLib) {
            warnedMissingAnimationLib = true;
            ThanpisitkubMod.LOGGER.warn("Player Animation Library is not installed: Thanpisitkub moves will play without the body animation.");
            if (local) {
                mc.player.displayClientMessage(Component.translatable("message.thanpisitkub.no_animation_lib"), false);
            }
        }

        if (local && PowerConfig.THIRD_PERSON.get()) {
            cast.previousCamera = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
    }

    /** @param cancelled true = also cut the sounds; false = let them fade out by themselves. */
    private static void end(UUID id, boolean cancelled) {
        ClientCast cast = CASTS.remove(id);
        if (cast == null) return;
        Minecraft mc = Minecraft.getInstance();

        if (cancelled) {
            for (SoundInstance s : cast.sounds) mc.getSoundManager().stop(s);
        }
        if (mc.level != null && PalCheck.isAvailable()) {
            Player p = mc.level.getPlayerByUUID(id);
            if (p instanceof AbstractClientPlayer acp) PalBridge.stop(acp);
        }
        if (cast.local && cast.previousCamera != null) {
            mc.options.setCameraType(cast.previousCamera);
        }
    }

    // ------------------------------------------------------------------------ timeline

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || CASTS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;

        List<UUID> finished = new ArrayList<>();
        List<UUID> lost = new ArrayList<>();
        Iterator<ClientCast> it = new ArrayList<>(CASTS.values()).iterator();
        while (it.hasNext()) {
            ClientCast cast = it.next();
            Player player = mc.level.getPlayerByUUID(cast.id);
            if (player == null || !player.isAlive()) {
                lost.add(cast.id);
                continue;
            }
            runTimeline(mc, cast, player);
            cast.tick++;
            if (cast.tick >= cast.move.totalTicks) finished.add(cast.id);
        }
        for (UUID id : lost) end(id, true);
        for (UUID id : finished) end(id, false);
    }

    private static void runTimeline(Minecraft mc, ClientCast cast, Player player) {
        MoveDef move = cast.move;

        int spawnedThisTick = 0;
        while (cast.fxIndex < move.fx.size() && move.fx.get(cast.fxIndex).tick() <= cast.tick) {
            if (spawnedThisTick >= MAX_EMITTERS_PER_TICK) break; // the rest runs next tick (no big lag spike)
            MoveDef.Fx fx = move.fx.get(cast.fxIndex++);
            float yaw = player.getYRot();
            double rad = Math.toRadians(yaw);
            double fwdX = -Math.sin(rad), fwdZ = Math.cos(rad);
            double leftX = Math.cos(rad), leftZ = Math.sin(rad);
            double x = player.getX() + fwdX * fx.forward() + leftX * fx.side();
            double y = (fx.ground() ? cast.groundY : player.getY()) + fx.up();
            double z = player.getZ() + fwdZ * fx.forward() + leftZ * fx.side();
            if (isStacked(cast, fx.particle(), x, y, z)) continue;
            spawnedThisTick++;
            EmitterManager.spawn(fx.particle(), mc.level, x, y, z, yaw);
        }

        while (cast.soundIndex < move.sounds.size() && move.sounds.get(cast.soundIndex).tick() <= cast.tick) {
            MoveDef.Snd snd = move.sounds.get(cast.soundIndex++);
            SoundEvent event = ModSounds.get(snd.sound());
            if (event == null) continue;
            // never let two copies of the same sound overlap: cut the previous one first
            for (Iterator<SoundInstance> si = cast.sounds.iterator(); si.hasNext(); ) {
                SoundInstance old = si.next();
                if (old.getLocation().equals(event.getLocation())) {
                    mc.getSoundManager().stop(old);
                    si.remove();
                }
            }
            float volume = (float) (snd.volume() * PowerConfig.SOUND_VOLUME.get());
            if (volume <= 0.0f) continue;
            SoundInstance instance = new EntityBoundSoundInstance(event, SoundSource.PLAYERS,
                    volume, snd.pitch(), player, player.getRandom().nextLong());
            mc.getSoundManager().play(instance);
            cast.sounds.add(instance);
        }
    }

    private static final int MAX_EMITTERS_PER_TICK = 5;
    /** Two copies of the same effect closer than this (blocks) while the first is still visible = stacked. */
    private static final double STACK_RADIUS = 5.0;

    private static boolean isStacked(ClientCast cast, String particle, double x, double y, double z) {
        EmitterDef def = FxDefs.get(particle);
        double visibleTicks = (def == null ? 1.0 : def.approxDuration()) * 20.0 * 0.55;
        List<double[]> list = cast.recentFx.computeIfAbsent(particle, k -> new ArrayList<>());
        list.removeIf(e -> cast.tick - e[0] > visibleTicks);
        for (double[] e : list) {
            double dx = e[1] - x, dy = e[2] - y, dz = e[3] - z;
            if (dx * dx + dy * dy + dz * dz < STACK_RADIUS * STACK_RADIUS) return true;
        }
        list.add(new double[]{cast.tick, x, y, z});
        return false;
    }

    // ----------------------------------------------------- freeze the local player's input

    private static boolean localIsCasting() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && CASTS.containsKey(mc.player.getUUID()) && PowerConfig.LOCK_MOVEMENT.get();
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!localIsCasting()) return;
        var input = event.getInput();
        input.forwardImpulse = 0.0f;
        input.leftImpulse = 0.0f;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!localIsCasting()) return;
        event.setCanceled(true);
        event.setSwingHand(false);
    }

    /** Camera shake for the local caster (smooth sine noise, fades out per Shake event). */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ClientCast cast = CASTS.get(mc.player.getUUID());
        if (cast == null) return;
        double userScale = PowerConfig.CAMERA_SHAKE.get();
        if (userScale <= 0.0) return;

        float amount = 0.0f;
        for (MoveDef.Shake s : cast.move.shakes) {
            float elapsed = cast.tick - s.tick();
            if (elapsed >= 0.0f && elapsed < s.durationTicks()) {
                amount += s.strength() * (1.0f - elapsed / s.durationTicks());
            }
        }
        if (amount <= 0.0f) return;

        double t = cast.tick + event.getPartialTick();
        double k = amount * userScale;
        event.setPitch(event.getPitch() + (float) ((Math.sin(t * 1.9) * 0.8 + Math.sin(t * 3.7) * 0.5) * k * 0.6));
        event.setYaw(event.getYaw() + (float) ((Math.cos(t * 1.7) * 0.8 + Math.sin(t * 4.1) * 0.5) * k * 0.6));
        event.setRoll(event.getRoll() + (float) (Math.sin(t * 2.3) * k * 0.5));
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        CASTS.clear();
    }
}
