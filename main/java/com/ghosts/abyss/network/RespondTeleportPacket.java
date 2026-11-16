
package com.ghosts.abyss.network;

import com.ghosts.abyss.phone.PhoneTeleportRequests;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class RespondTeleportPacket {

    private final UUID senderId;
    private final boolean accept;

    public RespondTeleportPacket(UUID senderId, boolean accept) {
        this.senderId = senderId;
        this.accept = accept;
    }

    public RespondTeleportPacket(FriendlyByteBuf buf) {
        this.senderId = buf.readUUID();
        this.accept = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(senderId);
        buf.writeBoolean(accept);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();

        ctx.enqueueWork(() -> {
            ServerPlayer recipient = ctx.getSender();
            if (recipient == null) return;

            PhoneTeleportRequests.respond(
                    recipient,
                    senderId,
                    accept
            );
        });

        ctx.setPacketHandled(true);
    }
}
