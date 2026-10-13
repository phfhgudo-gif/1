package com.thanpisitkub.mod.registry;

import com.thanpisitkub.mod.ThanpisitkubMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

/** Sound events (files in assets/thanpisitkub/sounds, mapped in sounds.json). */
public final class ModSounds {
    private ModSounds() {
    }

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ThanpisitkubMod.MOD_ID);

    private static final Map<String, RegistryObject<SoundEvent>> BY_NAME = new LinkedHashMap<>();

    public static final RegistryObject<SoundEvent> GARP_ULT1 = reg("garp_ult1");
    public static final RegistryObject<SoundEvent> GARP_GALAXYDIVIDE = reg("garp_galaxydivide");
    public static final RegistryObject<SoundEvent> GALAXY_IMPACT_THEME = reg("galaxy_impact_theme");
    public static final RegistryObject<SoundEvent> GALAXY = reg("galaxy");
    public static final RegistryObject<SoundEvent> IMPACT = reg("impact");

    private static RegistryObject<SoundEvent> reg(String name) {
        RegistryObject<SoundEvent> ro = SOUNDS.register(name,
                () -> SoundEvent.createVariableRangeEvent(ThanpisitkubMod.id(name)));
        BY_NAME.put(name, ro);
        return ro;
    }

    /** @return the sound event for a name used in Moves.java, or null. */
    public static SoundEvent get(String name) {
        RegistryObject<SoundEvent> ro = BY_NAME.get(name);
        return ro != null && ro.isPresent() ? ro.get() : null;
    }
}
