
package com.ghosts.abyss.network;

import com.ghosts.abyss.client.BlueControlClientState;

import net.minecraft.network.FriendlyByteBuf;

import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class PacketBlueControlState {

    private final boolean controlling;

    public PacketBlueControlState(boolean controlling) {
        this.controlling = controlling;
    }

    public PacketBlueControlState(FriendlyByteBuf buf) {
        this.controlling = buf.readBoolean();
    }

    public static void encode(
            PacketBlueControlState message,
            FriendlyByteBuf buf
    ) {
        buf.writeBoolean(message.controlling);
    }

    public static void handle(
            PacketBlueControlState message,
            Supplier<NetworkEvent.Context> supplier
    ) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            if (context.getDirection()
                    .getReceptionSide().isClient()) {

                BlueControlClientState.setControlling(
                        message.controlling
                );
            }
        });

        context.setPacketHandled(true);
    }
}
