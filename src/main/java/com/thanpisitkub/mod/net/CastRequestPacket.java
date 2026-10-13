package com.thanpisitkub.mod.net;

import com.thanpisitkub.mod.PowerConfig;
import com.thanpisitkub.mod.move.MoveDef;
import com.thanpisitkub.mod.move.Moves;
import com.thanpisitkub.mod.power.PowerData;
import com.thanpisitkub.mod.server.ServerMoveManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: "I pressed the key for move N". */
public record CastRequestPacket(int moveIndex) {

    public static void encode(CastRequestPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.moveIndex);
    }

    public static CastRequestPacket decode(FriendlyByteBuf buf) {
        return new CastRequestPacket(buf.readVarInt());
    }

    public static void handle(CastRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer player = c.getSender();
            MoveDef move = Moves.byIndex(msg.moveIndex);
            if (player == null || move == null) return;
            if (PowerConfig.REQUIRE_POWER.get() && !PowerData.has(player)) {
                player.displayClientMessage(Component.translatable("message.thanpisitkub.no_power"), true);
                return;
            }
            ServerMoveManager.tryCast(player, move);
        });
        c.setPacketHandled(true);
    }
}
