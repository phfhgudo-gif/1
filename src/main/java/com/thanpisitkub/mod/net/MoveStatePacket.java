package com.thanpisitkub.mod.net;

import com.thanpisitkub.mod.client.ClientMoveManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Server -> clients tracking the caster: a move started, finished or was cancelled.
 *
 * @param mode 0 = started, 1 = finished normally, 2 = cancelled
 */
public record MoveStatePacket(UUID caster, int moveIndex, int mode) {

    public static final int START = 0;
    public static final int FINISH = 1;
    public static final int CANCEL = 2;

    public static void encode(MoveStatePacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.caster);
        buf.writeVarInt(msg.moveIndex);
        buf.writeVarInt(msg.mode);
    }

    public static MoveStatePacket decode(FriendlyByteBuf buf) {
        return new MoveStatePacket(buf.readUUID(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(MoveStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientMoveManager.onState(msg.caster, msg.moveIndex, msg.mode)));
        c.setPacketHandled(true);
    }
}
