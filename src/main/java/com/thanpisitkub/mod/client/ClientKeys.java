package com.thanpisitkub.mod.client;

import com.thanpisitkub.mod.ThanpisitkubMod;
import com.thanpisitkub.mod.net.CancelRequestPacket;
import com.thanpisitkub.mod.net.CastRequestPacket;
import com.thanpisitkub.mod.net.NetworkHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Keybinds: Z = Galaxy Impact, X = Galaxy Divide, V = cancel (rebindable in Controls). */
@Mod.EventBusSubscriber(modid = ThanpisitkubMod.MOD_ID, value = Dist.CLIENT)
public final class ClientKeys {
    private ClientKeys() {
    }

    private static final String CATEGORY = "key.categories.thanpisitkub";

    public static final KeyMapping GALAXY_IMPACT = new KeyMapping("key.thanpisitkub.galaxy_impact", GLFW.GLFW_KEY_Z, CATEGORY);
    public static final KeyMapping GALAXY_DIVIDE = new KeyMapping("key.thanpisitkub.galaxy_divide", GLFW.GLFW_KEY_X, CATEGORY);
    public static final KeyMapping CANCEL = new KeyMapping("key.thanpisitkub.cancel", GLFW.GLFW_KEY_V, CATEGORY);

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        while (GALAXY_IMPACT.consumeClick()) {
            NetworkHandler.CHANNEL.sendToServer(new CastRequestPacket(0));
        }
        while (GALAXY_DIVIDE.consumeClick()) {
            NetworkHandler.CHANNEL.sendToServer(new CastRequestPacket(1));
        }
        while (CANCEL.consumeClick()) {
            NetworkHandler.CHANNEL.sendToServer(new CancelRequestPacket());
        }
    }
}
