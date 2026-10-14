package com.thanpisitkub.mod.server;

import com.thanpisitkub.mod.PowerConfig;
import com.thanpisitkub.mod.ThanpisitkubMod;
import com.thanpisitkub.mod.move.MoveDef;
import com.thanpisitkub.mod.net.MoveStatePacket;
import com.thanpisitkub.mod.net.NetworkHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server side of the moves: starts / ends them, applies the damage events of the timeline,
 * and tells every client that can see the caster to play the visuals.
 */
@Mod.EventBusSubscriber(modid = ThanpisitkubMod.MOD_ID)
public final class ServerMoveManager {
    private ServerMoveManager() {
    }

    private static final class ActiveCast {
        final MoveDef move;
        final double groundY;
        final boolean savedMayFly;
        final boolean savedNoGravity;
        int tick = 0;
        int hitIndex = 0;

        ActiveCast(MoveDef move, ServerPlayer player) {
            this.move = move;
            this.groundY = player.getY();
            this.savedMayFly = player.getAbilities().mayfly;
            this.savedNoGravity = player.isNoGravity();
        }
    }

    private static final Map<UUID, ActiveCast> CASTS = new HashMap<>();

    public static boolean isCasting(UUID id) {
        return CASTS.containsKey(id);
    }

    /** Starts a move for the player (replaces a move that is still running). Cooldown is handled by the Origins power. */
    public static boolean cast(ServerPlayer player, MoveDef move) {
        if (!player.isAlive() || player.isSpectator()) return false;
        cancel(player);
        ActiveCast cast = new ActiveCast(move, player);
        CASTS.put(player.getUUID(), cast);
        if (move.hasFlight()) {
            // "may fly" stops the vanilla anti-fly kick while we carry the player through the sky
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
            player.setNoGravity(true);
        }
        send(player, move.index, MoveStatePacket.START);
        return true;
    }

    public static void cancel(ServerPlayer player) {
        ActiveCast cast = CASTS.remove(player.getUUID());
        if (cast != null) {
            endFlight(player, cast);
            send(player, cast.move.index, MoveStatePacket.CANCEL);
        }
    }

    /** Gives gravity / flight permission back and cushions the landing. */
    private static void endFlight(ServerPlayer player, ActiveCast cast) {
        if (!cast.move.hasFlight()) return;
        player.setNoGravity(cast.savedNoGravity);
        player.getAbilities().mayfly = cast.savedMayFly;
        player.onUpdateAbilities();
        player.fallDistance = 0;
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, false, false, false));
    }

    private static void send(ServerPlayer player, int moveIndex, int mode) {
        NetworkHandler.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new MoveStatePacket(player.getUUID(), moveIndex, mode));
    }

    // ---------------------------------------------------------------------------- ticking

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || CASTS.isEmpty()) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        List<Runnable> afterLoop = new ArrayList<>();
        // iterate a snapshot: a hit can kill another caster, which cancels (removes) their cast
        for (Map.Entry<UUID, ActiveCast> entry : new ArrayList<>(CASTS.entrySet())) {
            UUID uuid = entry.getKey();
            ActiveCast cast = entry.getValue();
            if (CASTS.get(uuid) != cast) continue; // cancelled meanwhile
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null || !player.isAlive() || player.isRemoved()) {
                CASTS.remove(uuid);
                continue;
            }
            // flight: steer the player to where the flight path wants them next tick (no horizontal drift)
            if (cast.move.hasFlight()) {
                double target = cast.groundY + cast.move.altitudeAt(cast.tick + 1);
                double vy = Mth.clamp((target - player.getY()) * 0.9, -2.6, 2.6);
                player.setDeltaMovement(0.0, vy, 0.0);
                player.hurtMarked = true;
            }
            // timeline hits that are due now
            List<MoveDef.Hit> hits = cast.move.hits;
            while (cast.hitIndex < hits.size() && hits.get(cast.hitIndex).tick() <= cast.tick) {
                doHit(player, cast, hits.get(cast.hitIndex));
                cast.hitIndex++;
            }
            player.fallDistance = 0;
            cast.tick++;
            if (cast.tick >= cast.move.totalTicks && CASTS.get(uuid) == cast) {
                CASTS.remove(uuid);
                endFlight(player, cast);
                final ServerPlayer p = player;
                final int idx = cast.move.index;
                afterLoop.add(() -> send(p, idx, MoveStatePacket.FINISH));
            }
        }
        afterLoop.forEach(Runnable::run);
    }

    private static void doHit(ServerPlayer caster, ActiveCast cast, MoveDef.Hit hit) {
        ServerLevel level = caster.serverLevel();
        // hits are measured from the GROUND under the caster (the caster may be flying high above it)
        Vec3 origin = new Vec3(caster.getX(), cast.groundY, caster.getZ());
        Vec3 forward = Vec3.directionFromRotation(0.0f, caster.getYRot());
        float damage = (float) (hit.damage() * PowerConfig.DAMAGE_MULTIPLIER.get());

        AABB area;
        if (hit.shape() == MoveDef.HitShape.SPHERE) {
            area = new AABB(origin.x - hit.size(), origin.y - 4.0, origin.z - hit.size(),
                    origin.x + hit.size(), origin.y + hit.size(), origin.z + hit.size());
        } else {
            Vec3 end = origin.add(forward.scale(hit.size()));
            area = new AABB(origin, end).inflate(hit.width() / 2.0 + 1.0, 4.0, hit.width() / 2.0 + 1.0);
        }

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (target == caster || !target.isAlive() || target instanceof ArmorStand) continue;
            if (target instanceof TamableAnimal pet && pet.isOwnedBy(caster)) continue;

            Vec3 rel = target.position().subtract(origin);
            Vec3 push;
            if (hit.shape() == MoveDef.HitShape.SPHERE) {
                if (rel.length() > hit.size() + target.getBbWidth() / 2.0) continue;
                push = new Vec3(rel.x, 0, rel.z);
                push = push.lengthSqr() < 1.0e-4 ? forward : push.normalize();
            } else {
                double along = rel.x * forward.x + rel.z * forward.z;
                double perpX = rel.x - forward.x * along;
                double perpZ = rel.z - forward.z * along;
                double perp = Math.sqrt(perpX * perpX + perpZ * perpZ);
                if (along < -target.getBbWidth() / 2.0 || along > hit.size() + target.getBbWidth() / 2.0) continue;
                if (perp > hit.width() / 2.0 + target.getBbWidth() / 2.0) continue;
                if (Math.abs(rel.y) > 6.0) continue;
                push = forward;
            }

            if (damage > 0 && target.hurt(caster.damageSources().playerAttack(caster), damage)) {
                if (hit.knockback() > 0 || hit.knockUp() > 0) {
                    target.push(push.x * hit.knockback(), hit.knockUp(), push.z * hit.knockback());
                    target.hurtMarked = true;
                }
            }
        }
    }

    // ----------------------------------------------------------------- caster protection

    @SubscribeEvent
    public static void onAttacked(LivingAttackEvent event) {
        if (!PowerConfig.INVULNERABLE_WHILE_CASTING.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!CASTS.containsKey(player.getUUID())) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return; // /kill, void
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) cancel(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ActiveCast cast = CASTS.remove(player.getUUID());
            if (cast != null) endFlight(player, cast);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) cancel(player);
    }

    /** Before the players are saved: give everybody their normal abilities back. */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (Map.Entry<UUID, ActiveCast> e : new ArrayList<>(CASTS.entrySet())) {
                ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
                if (p != null) endFlight(p, e.getValue());
            }
        }
        CASTS.clear();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        CASTS.clear();
    }
}
