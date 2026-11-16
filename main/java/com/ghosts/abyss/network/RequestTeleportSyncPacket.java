
package com.ghosts.abyss.network;

import com.ghosts.abyss.phone.PhoneTeleportRequests;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestTeleportSyncPacket {

    public RequestTeleportSyncPacket() {}

    public RequestTeleportSyncPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();

        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                PhoneTeleportRequests.sync(player);
            }
        });

        ctx.setPacketHandled(true);
    }
}
