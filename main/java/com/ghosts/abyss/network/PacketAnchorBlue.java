
package com.ghosts.abyss.network;

import com.ghosts.abyss.gojo.ControlledBlueManager;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketAnchorBlue {

    public PacketAnchorBlue() {}

    public PacketAnchorBlue(FriendlyByteBuf buf) {}

    public static void encode(
            PacketAnchorBlue message,
            FriendlyByteBuf buf
    ) {
        // No payload required.
    }

    public static void handle(
            PacketAnchorBlue message,
            Supplier<NetworkEvent.Context> supplier
    ) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player != null) {
                ControlledBlueManager.anchor(player);
            }
        });

        context.setPacketHandled(true);
    }
}
