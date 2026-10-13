package com.thanpisitkub.mod.client.anim;

import com.thanpisitkub.mod.ThanpisitkubMod;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Plays assets/thanpisitkub/player_animation/*.json on a player through the Player Animation Library.
 * Only call this class when {@link PalCheck#isAvailable()} is true.
 */
public final class PalBridge {
    private PalBridge() {
    }

    private static final ResourceLocation LAYER = new ResourceLocation(ThanpisitkubMod.MOD_ID, "moves");

    /** Adds our animation layer to every client player (call once during client setup). */
    public static void registerLayer() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 1000, player -> new ModifierLayer<>());
    }

    @SuppressWarnings("unchecked")
    private static ModifierLayer<IAnimation> layer(AbstractClientPlayer player) {
        return (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER);
    }

    public static void play(AbstractClientPlayer player, ResourceLocation animation) {
        ModifierLayer<IAnimation> layer = layer(player);
        if (layer == null) return;
        KeyframeAnimation anim = PlayerAnimationRegistry.getAnimation(animation);
        if (anim == null) {
            ThanpisitkubMod.LOGGER.warn("Player animation {} was not found/parsed", animation);
            return;
        }
        layer.setAnimation(new KeyframeAnimationPlayer(anim));
    }

    public static void stop(AbstractClientPlayer player) {
        ModifierLayer<IAnimation> layer = layer(player);
        if (layer != null) layer.setAnimation(null);
    }
}
