package com.thanpisitkub.mod.net;

import com.thanpisitkub.mod.server.ServerMoveManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: "stop my move". */
public record CancelRequestPacket() {

    public static void encode(CancelRequestPacket msg, FriendlyByteBuf buf) {
    }

    public static CancelRequestPacket decode(FriendlyByteBuf buf) {
        return new CancelRequestPacket();
    }

    public static void handle(CancelRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = c.getSender();
            if (player != null) ServerMoveManager.cancel(player);
        });
        c.setPacketHandled(true);
    }
}
