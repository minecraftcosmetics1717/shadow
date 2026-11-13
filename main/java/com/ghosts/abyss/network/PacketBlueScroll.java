
package com.ghosts.abyss.network;

import com.ghosts.abyss.gojo.ControlledBlueManager;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class PacketBlueScroll {

    private final int direction;

    public PacketBlueScroll(int direction) {
        this.direction = direction;
    }

    public PacketBlueScroll(FriendlyByteBuf buf) {
        this.direction = buf.readInt();
    }

    public static void encode(
            PacketBlueScroll message,
            FriendlyByteBuf buf
    ) {
        buf.writeInt(message.direction);
    }

    public static void handle(
            PacketBlueScroll message,
            Supplier<NetworkEvent.Context> supplier
    ) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player != null) {
                ControlledBlueManager.scrollDistance(
                        player,
                        message.direction
                );
            }
        });

        context.setPacketHandled(true);
    }
}
