package com.thanpisitkub.mod.client;

import com.thanpisitkub.mod.ThanpisitkubMod;
import com.thanpisitkub.mod.client.anim.PalBridge;
import com.thanpisitkub.mod.client.anim.PalCheck;
import com.thanpisitkub.mod.client.fx.BedrockParticle;
import com.thanpisitkub.mod.client.fx.FxDefs;
import com.thanpisitkub.mod.registry.ModParticles;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;

/** Mod-bus client registration: keybinds, particle providers, definition reload listener, animation layer. */
@Mod.EventBusSubscriber(modid = ThanpisitkubMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (PalCheck.isAvailable()) {
                PalBridge.registerLayer();
            } else {
                ThanpisitkubMod.LOGGER.warn("Player Animation Library not found - install it to see the move animations.");
            }
        });
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(ClientKeys.GALAXY_IMPACT);
        event.register(ClientKeys.GALAXY_DIVIDE);
        event.register(ClientKeys.CANCEL);
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        for (Map.Entry<String, RegistryObject<SimpleParticleType>> e : ModParticles.all().entrySet()) {
            final String name = e.getKey();
            event.registerSpriteSet(e.getValue().get(), sprites -> {
                FxDefs.putSprites(name, sprites);
                return new BedrockParticle.Provider(name, sprites);
            });
        }
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new FxDefs());
    }
}
