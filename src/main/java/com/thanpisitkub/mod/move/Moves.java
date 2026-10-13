package com.thanpisitkub.mod.move;

import java.util.List;
import java.util.Locale;

/**
 * The two moves of the "thanpisitkub" power and their timelines.
 *
 * <p>Laid out against the supplied animations and audio, in "anime scale":
 * <ul>
 *   <li>Galaxy Impact (27 s): gather power 0-3.4 s, a galaxy opens overhead and stays up until the end,
 *       three punches at ~7 s / ~14 s / ~15.5 s, huge finale at ~17.4 s (the soundtrack's loudest part),
 *       then shockwaves keep rolling out while the player holds the final pose.</li>
 *   <li>Galaxy Divide (27.5 s): slashes at 1.5-3 s, travelling "divide" waves at 5-7 s, power builds at 13-16.4 s,
 *       main cut at 16.5 s, then the galaxy above gathers while the final pose is held
 *       and the sky splits at ~22.3 s.</li>
 * </ul>
 * Everything here is plain data - tweak the numbers freely. Distances are in blocks, relative to the
 * caster ({@code forward} = direction the caster faces). Particle sizes come from the particle files and are
 * multiplied by the client config {@code effectScale}.
 */
public final class Moves {
    private Moves() {
    }

    public static final MoveDef GALAXY_IMPACT = galaxyImpact();
    public static final MoveDef GALAXY_DIVIDE = galaxyDivide();

    /** Index = id sent over the network. */
    public static final List<MoveDef> ALL = List.of(GALAXY_IMPACT, GALAXY_DIVIDE);

    public static MoveDef byIndex(int i) {
        return i >= 0 && i < ALL.size() ? ALL.get(i) : null;
    }

    public static MoveDef byId(String id) {
        if (id == null) return null;
        String k = id.toLowerCase(Locale.ROOT);
        for (MoveDef m : ALL) if (m.id.equals(k)) return m;
        return null;
    }

    // ------------------------------------------------------------------------------------------

    private static MoveDef galaxyImpact() {
        MoveDef.Builder b = MoveDef.builder("galaxy_impact", 0, "galaxy_impact", 27.0);

        // --- sound -------------------------------------------------------------------------
        b.sound(0.0, "garp_ult1", 3.0f, 1.0f);
        b.sound(0.0, "galaxy_impact_theme", 0.5f, 1.0f);
        b.sound(3.4, "galaxy", 3.0f, 1.0f);
        b.sound(6.9, "impact", 3.0f, 1.0f);
        b.sound(13.9, "impact", 3.0f, 1.05f);
        b.sound(15.4, "impact", 3.0f, 1.1f);
        b.sound(17.4, "impact", 3.0f, 0.8f);
        b.sound(19.0, "impact", 3.0f, 0.7f);

        // --- camera ------------------------------------------------------------------------------
        b.shake(0.5, 0.4, 2.8);
        b.shake(3.4, 0.9, 2.0);
        b.shake(6.9, 2.0, 0.9);
        b.shake(13.9, 2.4, 1.0);
        b.shake(15.4, 3.0, 1.2);
        b.shake(17.4, 6.0, 4.5);
        b.shake(19.0, 2.5, 2.5);
        b.shake(21.5, 1.2, 3.0);

        // --- gather power (0 - 3.4 s) ------------------------------------------------------
        b.fxEvery(0.0, 3.0, 0.5, "garp_rubi2", 0, 0, 0.1);
        b.fx(1.0, "garp_ult1_6_2", 0, 0, 0.15);
        b.fx(2.0, "garp_ult1_6_2", 0, 0, 0.15);
        b.fx(3.0, "garp_ult1_6_2", 0, 0, 0.15);

        // --- a galaxy opens overhead and stays (3.4 s ->) ---------------------------------------------
        b.fxEvery(3.4, 25.4, 1.0, "garp_ult1_1_2", 0, 0, 24);
        b.fx(9.0, "garp_ult1_1", 0, 0, 0.1);
        b.fxEvery(3.6, 13.6, 1.5, "garp_ult1_3_3", 0, 0, 1.0);

        // --- punch 1 (6.9 s) -----------------------------------------------------------------
        punch(b, 6.9, 2.4);
        b.fx(6.9, "garp_ult1_6", 0, 0, 0.1);
        b.lineHit(7.0, 14, 8, 6f, 0.8, 0.3);

        // --- punch 2 (13.9 s) and 3 (15.4 s) ---------------------------------------------------
        punch(b, 13.9, 2.6);
        b.fx(13.9, "garp_ult1_6", 0, 0, 0.1);
        b.fx(14.0, "garp_ult1_2_2", 3.0, 0, 1.4);
        b.sphereHit(14.0, 14, 8f, 1.0, 0.4);

        punch(b, 15.4, 2.8);
        b.fx(15.4, "garp_ult1_6", 0, 0, 0.1);
        b.fx(15.5, "garp_ult1_2_2", 3.0, 0, 1.4);
        b.sphereHit(15.5, 18, 10f, 1.2, 0.5);

        // --- finale (17.4 s) ---------------------------------------------------------------------
        b.fx(17.4, "garp_ult1_2_2", 3.0, 0, 1.5);
        b.fx(17.7, "garp_ult1_2_2", 3.2, 0, 1.5);
        b.fx(18.0, "garp_ult1_2_2", 3.2, 0, 1.5);
        b.fx(17.4, "garp_ult1_4_2", 3.0, 0, 1.3);
        b.fx(17.5, "garp_ult1_4_2", 3.5, 1.6, 1.3);
        b.fx(17.6, "garp_ult1_4_2", 3.5, -1.6, 1.3);
        b.fx(17.8, "garp_ult1_4_2", 4.5, 0, 1.3);
        b.fx(17.4, "garp_skills4_1", 3.0, 0, 1.4);
        b.fx(17.5, "garp_skills4_1", 4.0, 1.5, 1.2);
        b.fx(17.6, "garp_skills4_1", 4.0, -1.5, 1.2);
        b.fxEvery(17.4, 24.0, 0.6, "garp_ult1_6", 0, 0, 0.1);
        b.fxEvery(17.4, 22.0, 1.2, "garp_ult1_6_2", 0, 0, 0.2);
        b.fxEvery(17.4, 24.0, 0.8, "garp_ult1_8_2", 0, 0, 0.3);
        b.fx(17.6, "garp_ult1_1", 0, 0, 0.1);
        b.fx(19.2, "garp_ult1_1", 0, 0, 0.1);
        b.fx(21.5, "garp_ult1_1", 0, 0, 0.1);
        b.fxEvery(17.4, 24.4, 1.0, "garp_rubi2_4", 0, 0, 0.2);
        b.fxEvery(19.0, 23.0, 1.5, "garp_ult1_3_3", 0, 0, 1.0);
        b.fx(19.0, "garp_ult1_2_2", 3.0, 0, 1.5);
        b.fx(21.0, "garp_ult1_2_2", 3.0, 0, 1.5);
        b.fx(23.0, "garp_ult1_2_2", 3.0, 0, 1.5);
        // spread wide: shockwave rings rolling outward from the caster (not just on the caster)
        b.fxRingWave(17.5, 0.35, "garp_ult1_2_2", 6, 8, 32, 6, 1.0);
        b.fxRingWave(17.6, 0.35, "garp_skills4_1", 8, 6, 30, 6, 1.0);
        b.fxRingWave(17.7, 0.5, "garp_ult1_4_2", 6, 10, 30, 10, 1.2);
        b.fxRingWave(17.5, 0.3, "garp_ult1_6", 10, 5, 35, 5, 0.1);
        // explosions and fire scattered over the whole area through the aftermath
        b.fxScatter(17.8, 24.0, 0.4, 2, "garp_ult1_2_2", 6, 30, 1.2);
        b.fxScatter(17.8, 24.0, 0.5, 2, "garp_ult1_4_2", 5, 28, 1.0);
        b.fxScatter(17.9, 24.0, 0.6, 2, "garp_ult1_8_2", 8, 30, 0.3);
        b.fxScatter(18.0, 23.0, 0.7, 1, "garp_ult1_3_3", 10, 30, 1.0);
        b.fxScatter(18.2, 23.5, 0.9, 1, "garp_ult1_1", 8, 28, 0.1);
        // more galaxies around the one overhead
        b.fxRing(17.6, "garp_ult1_1_2", 4, 16, 22, 45);
        b.fxRing(19.4, "garp_ult1_1_2", 4, 16, 22, 0);
        b.sphereHit(17.6, 40, 40f, 2.5, 0.9);
        b.sphereHit(19.0, 26, 12f, 1.5, 0.5);

        return b.build();
    }

    /** Fist impact flash: burst + flare + fire puff in front of the caster. */
    private static void punch(MoveDef.Builder b, double t, double reach) {
        b.fx(t, "garp_skills4_1", reach, 0, 1.3);
        b.fx(t, "garp_ult1_2", reach, 0, 1.3);
        b.fx(t + 0.05, "garp_ult1_4_2", reach + 0.3, 0, 1.2);
    }

    // ------------------------------------------------------------------------------------------

    private static MoveDef galaxyDivide() {
        MoveDef.Builder b = MoveDef.builder("galaxy_divide", 1, "galaxy_divide", 27.5);

        // --- sound -------------------------------------------------------------------------
        b.sound(0.0, "garp_galaxydivide", 3.0f, 1.0f);
        b.sound(1.5, "impact", 2.0f, 1.2f);
        b.sound(6.5, "impact", 2.0f, 1.1f);
        b.sound(16.4, "impact", 3.0f, 0.9f);
        b.sound(22.0, "galaxy", 3.0f, 0.9f);
        b.sound(22.4, "impact", 3.0f, 0.8f);

        // --- camera ------------------------------------------------------------------------------
        b.shake(0.0, 0.6, 1.5);
        b.shake(1.5, 1.6, 0.6);
        b.shake(2.5, 1.8, 0.6);
        b.shake(3.5, 1.8, 0.7);
        b.shake(6.4, 2.5, 1.2);
        b.shake(13.0, 0.8, 3.4);
        b.shake(16.5, 5.0, 2.5);
        b.shake(18.0, 1.0, 4.0);
        b.shake(22.3, 7.0, 5.0);

        // --- opening: flame burst + rapid slashes (0 - 3.6 s) ----------------------------------
        b.fx(0.0, "garp_ult4_5_2", 0, 0, 0.3);
        b.fx(0.5, "garp_rubi2", 0, 0, 0.1);
        slash(b, 1.5, 4.0, 1.5);
        slash(b, 2.0, 4.5, -1.5);
        slash(b, 2.5, 5.0, 1.5);
        slash(b, 3.0, 5.5, -1.5);
        b.lineHit(1.5, 14, 8, 5f, 0.6, 0.2);
        b.lineHit(2.5, 16, 8, 6f, 0.8, 0.2);

        // --- travelling divide waves (5 - 7.5 s) ----------------------------------------------
        b.fxLine(5.2, 0.3, "garp_galaxydivide2", 3, 36, 4, 0, 0.2);
        b.fx(5.5, "garp_ult4_2", 4, 0, 0.5);
        b.fx(6.5, "garp_ult4_2", 8, 0, 0.5);
        b.fx(6.4, "garp_galaxydivide1", 10, 0, 1.4);
        b.lineHit(6.5, 36, 8, 8f, 1.0, 0.3);

        // --- gather (13 - 16.4 s) ---------------------------------------------------------------
        b.fxEvery(13.0, 16.0, 1.0, "garp_ult4_5_2", 0, 0, 0.3);
        b.fxEvery(13.5, 15.5, 1.0, "garp_rubi2", 0, 0, 0.1);
        b.fx(13.5, "garp_ult4_2", 0, 0, 0.5);
        b.fx(15.0, "garp_ult4_2", 0, 0, 0.5);
        b.fxEvery(13.0, 16.0, 0.75, "garp_galaxydivide2", 0, 0, 0.2);

        // --- main divide (16.5 s): a wave cuts 64 blocks straight ahead ------------------------------
        b.fx(16.5, "garp_galaxydivide1", 4, 0, 1.4);
        b.fx(16.7, "garp_galaxydivide1", 14, 0, 1.4);
        b.fx(16.9, "garp_galaxydivide1", 26, 0, 1.4);
        b.fx(17.1, "garp_galaxydivide1", 38, 0, 1.4);
        b.fx(17.3, "garp_galaxydivide1", 50, 0, 1.4);
        b.fx(17.5, "garp_galaxydivide1", 62, 0, 1.4);
        b.fxLine(16.5, 0.08, "garp_galaxydivide2", 2, 64, 4, 0, 0.2);
        b.fxLine(16.5, 0.1, "garp_skills4_1", 6, 60, 6, 0, 1.2);
        b.fx(16.6, "garp_ult4_5", 6, 0, 0.3);
        b.fx(16.9, "garp_ult4_5", 22, 0, 0.3);
        b.fx(17.2, "garp_ult4_5", 38, 0, 0.3);
        b.lineHit(16.8, 64, 10, 25f, 1.5, 0.5);

        // --- while the final pose is held (17.5 - 22.3 s): the galaxy gathers above --------------------
        b.fxEvery(18.0, 22.0, 1.0, "garp_ult1_1_2", 0, 0, 24);
        b.fxEvery(17.8, 22.0, 0.7, "garp_ult4_5_2", 0, 0, 0.4);
        b.fxEvery(18.0, 22.0, 1.0, "garp_rubi2", 0, 0, 0.1);
        b.fxEvery(18.2, 22.0, 0.9, "garp_galaxydivide2", 0, 0, 0.2);
        b.fxEvery(18.0, 22.0, 1.0, "garp_ult4_2", 0, 0, 0.5);
        b.fx(19.5, "garp_galaxydivide1", 5, 0, 1.4);
        b.fx(20.5, "garp_galaxydivide1", 5, 0, 1.4);
        b.fx(21.5, "garp_galaxydivide1", 5, 0, 1.4);

        // --- climax (22.3 s): light pillars along the cut, then the shockwave rolls on ---------------
        b.fx(22.3, "garp_ult4_2_3", 8, 0, 30);
        b.fx(22.4, "garp_ult4_2_3", 20, 0, 30);
        b.fx(22.5, "garp_ult4_2_3", 32, 0, 30);
        b.fx(22.6, "garp_ult4_2_3", 44, 0, 30);
        b.fx(22.7, "garp_ult4_2_3", 56, 0, 30);
        b.fx(22.8, "garp_ult4_2_3", 68, 0, 30);
        b.fxLine(22.3, 0.2, "garp_galaxydivide1", 8, 68, 12, 0, 1.4);
        b.fxLine(22.3, 0.06, "garp_galaxydivide2", 0, 72, 3, 0, 0.2);
        b.fxLine(22.3, 0.2, "garp_skills4_1", 6, 66, 10, 0, 1.2);
        b.fx(22.4, "garp_ult4_5_2", 10, 0, 0.5);
        b.fx(22.7, "garp_ult4_5_2", 24, 0, 0.5);
        b.fx(23.0, "garp_ult4_5_2", 38, 0, 0.5);
        b.fx(23.3, "garp_ult4_5_2", 52, 0, 0.5);
        b.fxEvery(23.4, 26.4, 1.0, "garp_galaxydivide2", 0, 0, 0.2);
        b.fxEvery(23.4, 26.4, 1.0, "garp_rubi2", 0, 0, 0.1);
        // spread wide: pillars / explosions on both sides of the cut, not only on the centre line
        b.fxLineSpread(22.3, 0.08, "garp_ult4_2_3", 6, 70, 8, 9, 30);
        b.fxLineSpread(22.4, 0.1, "garp_ult4_2", 4, 70, 5, 10, 0.5);
        b.fxLineSpread(22.4, 0.1, "garp_skills4_1", 5, 70, 6, 8, 1.2);
        b.fxLineSpread(22.6, 0.12, "garp_ult4_5_2", 8, 70, 8, 10, 0.5);
        b.fxLineSpread(16.6, 0.1, "garp_ult4_5", 6, 62, 6, 6, 0.3);
        b.fxScatter(22.5, 26.0, 0.5, 2, "garp_galaxydivide1", 8, 30, 1.4);
        b.fxScatter(22.5, 26.0, 0.6, 2, "garp_rubi2", 4, 26, 0.1);
        b.lineHit(22.4, 80, 14, 45f, 2.0, 0.8);

        return b.build();
    }

    private static void slash(MoveDef.Builder b, double t, double forward, double side) {
        b.fx(t, "garp_galaxydivide1", forward, side, 1.4);
        b.fx(t, "garp_skills4_1", forward, side, 1.3);
        b.fx(t + 0.1, "garp_ult4_2", forward, side, 0.8);
    }
}
