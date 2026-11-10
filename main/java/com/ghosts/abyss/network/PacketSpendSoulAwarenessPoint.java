
package com.ghosts.abyss.network;

import com.ghosts.abyss.soul.SoulAwareness;
import com.ghosts.abyss.soul.SoulAwarenessPoints;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.network.NetworkEvent;

public class PacketSpendSoulAwarenessPoint {

    public PacketSpendSoulAwarenessPoint() {
    }

    public PacketSpendSoulAwarenessPoint(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context ctx = contextSupplier.get();

        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();

            if (player == null) {
                return;
            }

            boolean success =
                    SoulAwarenessPoints.tryUpgrade(player);

            if (success) {
                player.displayClientMessage(
                        Component.literal(
                                "Soul Awareness: "
                                        + SoulAwareness.get(player)
                                        + "/50"
                        ),
                        true
                );
            }
        });

        ctx.setPacketHandled(true);
    }
}
