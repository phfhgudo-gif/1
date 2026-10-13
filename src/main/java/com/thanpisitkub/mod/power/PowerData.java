package com.thanpisitkub.mod.power;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.thanpisitkub.mod.ThanpisitkubMod;

/**
 * Whether a player owns the "thanpisitkub" power. Stored as a boolean in the player's
 * persistent data, so it survives restarts and death.
 */
@Mod.EventBusSubscriber(modid = ThanpisitkubMod.MOD_ID)
public final class PowerData {
    private PowerData() {
    }

    private static final String KEY = "thanpisitkub_power";

    public static boolean has(Player player) {
        return player.getPersistentData().getBoolean(KEY);
    }

    public static void set(Player player, boolean value) {
        player.getPersistentData().putBoolean(KEY, value);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        CompoundTag old = event.getOriginal().getPersistentData();
        if (old.contains(KEY)) {
            event.getEntity().getPersistentData().putBoolean(KEY, old.getBoolean(KEY));
        }
    }
}
