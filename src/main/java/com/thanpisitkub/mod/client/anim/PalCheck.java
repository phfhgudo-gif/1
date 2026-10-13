package com.thanpisitkub.mod.client.anim;

/**
 * Detects the Player Animation Library at runtime without touching any of its classes,
 * so the mod still loads (without animations) if the library is missing.
 */
public final class PalCheck {
    private PalCheck() {
    }

    private static Boolean available;

    public static boolean isAvailable() {
        if (available == null) {
            try {
                Class.forName("dev.kosmx.playerAnim.api.layered.ModifierLayer", false, PalCheck.class.getClassLoader());
                available = Boolean.TRUE;
            } catch (Throwable t) {
                available = Boolean.FALSE;
            }
        }
        return available;
    }
}
