package com.thanpisitkub.mod;

import com.mojang.logging.LogUtils;
import com.thanpisitkub.mod.net.NetworkHandler;
import com.thanpisitkub.mod.registry.ModItems;
import com.thanpisitkub.mod.registry.ModParticles;
import com.thanpisitkub.mod.registry.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Thanpisitkub Power - a new power with two moves: Galaxy Impact and Galaxy Divide.
 */
@Mod(ThanpisitkubMod.MOD_ID)
public class ThanpisitkubMod {
    public static final String MOD_ID = "thanpisitkub";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ThanpisitkubMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModParticles.PARTICLES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModItems.TABS.register(modBus);

        PowerConfig.register();
        NetworkHandler.register();
        // Event handlers are registered with @Mod.EventBusSubscriber on their classes.
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
