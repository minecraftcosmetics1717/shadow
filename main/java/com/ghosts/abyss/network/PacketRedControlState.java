package com.ghosts.abyss.network;

import com.ghosts.abyss.client.RedControlClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public final class PacketRedControlState {
    private final boolean controlling;
    public PacketRedControlState(boolean controlling) {
        this.controlling = controlling;
    }
    public PacketRedControlState(FriendlyByteBuf buf) {
        this.controlling = buf.readBoolean();
    }
    public static void encode(PacketRedControlState message,
                              FriendlyByteBuf buf) {
        buf.writeBoolean(message.controlling);
    }
    public static void handle(PacketRedControlState message,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                RedControlClientState.setControlling(message.controlling);
            }
        });
        context.setPacketHandled(true);
    }
}
