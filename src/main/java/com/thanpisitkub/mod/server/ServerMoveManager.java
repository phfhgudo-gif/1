package com.thanpisitkub.mod.server;

import com.thanpisitkub.mod.PowerConfig;
import com.thanpisitkub.mod.ThanpisitkubMod;
import com.thanpisitkub.mod.move.MoveDef;
import com.thanpisitkub.mod.net.MoveStatePacket;
import com.thanpisitkub.mod.net.NetworkHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
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
 * handles cooldowns and tells every client that can see the caster to play the visuals.
 */
@Mod.EventBusSubscriber(modid = ThanpisitkubMod.MOD_ID)
public final class ServerMoveManager {
    private ServerMoveManager() {
    }

    private static final class ActiveCast {
        final MoveDef move;
        int tick = 0;
        int hitIndex = 0;

        ActiveCast(MoveDef move) {
            this.move = move;
        }
    }

    private static final Map<UUID, ActiveCast> CASTS = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWN_UNTIL = new HashMap<>(); // game time (ticks)

    public static boolean isCasting(UUID id) {
        return CASTS.containsKey(id);
    }

    /** Normal cast (checks cooldown + not already casting). The power check is done by the caller. */
    public static boolean tryCast(ServerPlayer player, MoveDef move) {
        if (!player.isAlive() || player.isSpectator()) return false;
        if (CASTS.containsKey(player.getUUID())) return false;

        long now = player.serverLevel().getGameTime();
        Long until = COOLDOWN_UNTIL.get(player.getUUID());
        if (until != null && until > now) {
            long secondsLeft = (until - now + 19) / 20;
            player.displayClientMessage(Component.translatable("message.thanpisitkub.cooldown", secondsLeft), true);
            return false;
        }

        CASTS.put(player.getUUID(), new ActiveCast(move));
        int cooldownSeconds = move.index == 0 ? PowerConfig.COOLDOWN_IMPACT_SECONDS.get() : PowerConfig.COOLDOWN_DIVIDE_SECONDS.get();
        COOLDOWN_UNTIL.put(player.getUUID(), now + move.totalTicks + cooldownSeconds * 20L);
        send(player, move.index, MoveStatePacket.START);
        return true;
    }

    /** Admin / datapack cast: ignores the cooldown and replaces a running move. */
    public static boolean forceCast(ServerPlayer player, MoveDef move) {
        if (!player.isAlive() || player.isSpectator()) return false;
        cancel(player);
        COOLDOWN_UNTIL.remove(player.getUUID());
        return tryCast(player, move);
    }

    public static void cancel(ServerPlayer player) {
        ActiveCast cast = CASTS.remove(player.getUUID());
        if (cast != null) send(player, cast.move.index, MoveStatePacket.CANCEL);
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
            // timeline hits that are due now
            List<MoveDef.Hit> hits = cast.move.hits;
            while (cast.hitIndex < hits.size() && hits.get(cast.hitIndex).tick() <= cast.tick) {
                doHit(player, hits.get(cast.hitIndex));
                cast.hitIndex++;
            }
            player.fallDistance = 0;
            cast.tick++;
            if (cast.tick >= cast.move.totalTicks && CASTS.get(uuid) == cast) {
                CASTS.remove(uuid);
                final ServerPlayer p = player;
                final int idx = cast.move.index;
                afterLoop.add(() -> send(p, idx, MoveStatePacket.FINISH));
            }
        }
        afterLoop.forEach(Runnable::run);
    }

    private static void doHit(ServerPlayer caster, MoveDef.Hit hit) {
        ServerLevel level = caster.serverLevel();
        Vec3 origin = caster.position();
        Vec3 forward = Vec3.directionFromRotation(0.0f, caster.getYRot());
        float damage = (float) (hit.damage() * PowerConfig.DAMAGE_MULTIPLIER.get());

        AABB area;
        if (hit.shape() == MoveDef.HitShape.SPHERE) {
            area = caster.getBoundingBox().inflate(hit.size());
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
        if (event.getEntity() instanceof ServerPlayer player) CASTS.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) cancel(player);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        CASTS.clear();
        COOLDOWN_UNTIL.clear();
    }
}
