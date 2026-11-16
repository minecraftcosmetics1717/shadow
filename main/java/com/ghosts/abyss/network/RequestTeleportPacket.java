
package com.ghosts.abyss.network;

import com.ghosts.abyss.phone.PhoneSignal;
import com.ghosts.abyss.phone.PhoneTeleportRequests;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestTeleportPacket {

    private final String targetName;

    public RequestTeleportPacket(String targetName) {
        this.targetName = targetName;
    }

    public RequestTeleportPacket(FriendlyByteBuf buf) {
        this.targetName = buf.readUtf(32);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(targetName, 32);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();

        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            if (PhoneSignal.blocked(sender)) {
                sender.displayClientMessage(
                        Component.literal("No phone connection."),
                        true
                );
                return;
            }

            ServerPlayer target = sender.server
                    .getPlayerList()
                    .getPlayerByName(targetName);

            if (target == null || target == sender) {
                sender.displayClientMessage(
                        Component.literal("Unable to send teleport request."),
                        true
                );
                return;
            }

            // The recipient's signal is deliberately NOT checked here.
            // Their connection status must remain private.
            PhoneTeleportRequests.create(sender, target);
        });

        ctx.setPacketHandled(true);
    }
}
