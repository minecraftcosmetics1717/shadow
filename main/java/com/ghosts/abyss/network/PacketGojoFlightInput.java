
package com.ghosts.abyss.network;

import com.ghosts.abyss.gojo.GojoFlightHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketGojoFlightInput {

    private final int mode;

    public PacketGojoFlightInput(int mode) {
        this.mode = mode;
    }

    public static PacketGojoFlightInput decode(FriendlyByteBuf buf) {
        return new PacketGojoFlightInput(buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(mode);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();

        ServerPlayer player = ctx.getSender();

        if (player != null) {
            ctx.enqueueWork(() ->
                    GojoFlightHandler.setMode(player, mode)
            );
        }

        ctx.setPacketHandled(true);
    }
}
