package com.ghosts.abyss.network;

import com.ghosts.abyss.gojo.ControlledRedManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public final class PacketRedScroll {
    private final int direction;
    public PacketRedScroll(int direction) {
        this.direction = direction;
    }
    public PacketRedScroll(FriendlyByteBuf buf) {
        this.direction = buf.readInt();
    }
    public static void encode(PacketRedScroll message, FriendlyByteBuf buf) {
        buf.writeInt(message.direction);
    }
    public static void handle(PacketRedScroll message,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ControlledRedManager.scrollDistance(player, message.direction);
            }
        });
        context.setPacketHandled(true);
    }
}
