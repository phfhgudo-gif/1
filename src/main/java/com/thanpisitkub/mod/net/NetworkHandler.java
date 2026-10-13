package com.thanpisitkub.mod.net;

import com.thanpisitkub.mod.ThanpisitkubMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class NetworkHandler {
    private NetworkHandler() {
    }

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ThanpisitkubMod.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, CastRequestPacket.class,
                CastRequestPacket::encode, CastRequestPacket::decode, CastRequestPacket::handle);
        CHANNEL.registerMessage(id++, CancelRequestPacket.class,
                CancelRequestPacket::encode, CancelRequestPacket::decode, CancelRequestPacket::handle);
        CHANNEL.registerMessage(id++, MoveStatePacket.class,
                MoveStatePacket::encode, MoveStatePacket::decode, MoveStatePacket::handle);
    }
}
