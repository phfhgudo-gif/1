package com.thanpisitkub.mod;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/** Config files: config/thanpisitkub-common-v2.toml (server rules) and config/thanpisitkub-client-v2.toml (visuals). */
public final class PowerConfig {
    private PowerConfig() {
    }

    // ------------------------------------------------------------------ common (gameplay)
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_POWER;
    public static final ForgeConfigSpec.IntValue COOLDOWN_IMPACT_SECONDS;
    public static final ForgeConfigSpec.IntValue COOLDOWN_DIVIDE_SECONDS;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue INVULNERABLE_WHILE_CASTING;

    // ------------------------------------------------------------------ client (visuals)
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.DoubleValue EFFECT_SCALE;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_DENSITY;
    public static final ForgeConfigSpec.BooleanValue LOCK_MOVEMENT;
    public static final ForgeConfigSpec.BooleanValue THIRD_PERSON;
    public static final ForgeConfigSpec.DoubleValue CAMERA_SHAKE;

    static {
        ForgeConfigSpec.Builder c = new ForgeConfigSpec.Builder();
        c.push("power");
        REQUIRE_POWER = c.comment("If true, only players that own the thanpisitkub power can use the keybinds.",
                "(Use the Thanpisitkub Core item or /thanpisitkub power on <player> to give it.)")
                .define("requirePower", true);
        COOLDOWN_IMPACT_SECONDS = c.comment("Cooldown after Galaxy Impact, in seconds.")
                .defineInRange("cooldownGalaxyImpactSeconds", 60, 0, 3600);
        COOLDOWN_DIVIDE_SECONDS = c.comment("Cooldown after Galaxy Divide, in seconds.")
                .defineInRange("cooldownGalaxyDivideSeconds", 60, 0, 3600);
        DAMAGE_MULTIPLIER = c.comment("Multiplier for all move damage (1.0 = defaults in Moves.java).")
                .defineInRange("damageMultiplier", 1.0, 0.0, 1000.0);
        INVULNERABLE_WHILE_CASTING = c.comment("The caster takes no damage while a move is playing.")
                .define("invulnerableWhileCasting", true);
        c.pop();
        COMMON_SPEC = c.build();

        ForgeConfigSpec.Builder v = new ForgeConfigSpec.Builder();
        v.push("visuals");
        EFFECT_SCALE = v.comment("Scales particle size, spawn radius, speed and acceleration.",
                "1.0 (default) = the numbers exactly as exported in the particle files: anime-sized effects,",
                "tens of blocks wide. Use 0.25 for small, player-sized effects.")
                .defineInRange("effectScale", 1.0, 0.01, 4.0);
        PARTICLE_DENSITY = v.comment("Multiplier for how many particles are spawned. The effects are huge, so lower this (0.2-0.4)",
                "on phones / weak GPUs if the game lags, or raise it to 1.0+ for the full look.")
                .defineInRange("particleDensity", 0.6, 0.05, 2.0);
        LOCK_MOVEMENT = v.comment("Freeze your own movement/attack input while your move plays.")
                .define("lockMovementWhileCasting", true);
        THIRD_PERSON = v.comment("Switch to third-person view while your move plays.")
                .define("thirdPersonWhileCasting", true);
        CAMERA_SHAKE = v.comment("Camera shake strength during impacts (0 = off, 1 = default).")
                .defineInRange("cameraShake", 1.0, 0.0, 5.0);
        v.pop();
        CLIENT_SPEC = v.build();
    }

    public static void register() {
        ModLoadingContext ctx = ModLoadingContext.get();
        // "-v2" file names: an older build wrote effectScale = 0.25 into thanpisitkub-client.toml,
        // and Forge keeps whatever is in an existing file. New names = fresh defaults.
        ctx.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC, "thanpisitkub-common-v2.toml");
        ctx.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, "thanpisitkub-client-v2.toml");
    }
}
