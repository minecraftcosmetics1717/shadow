package com.ghosts.abyss.network;

import com.ghosts.abyss.gojo.ControlledRedManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public final class PacketFireRed {
    public PacketFireRed() {}
    public PacketFireRed(FriendlyByteBuf buf) {}
    public static void encode(PacketFireRed message, FriendlyByteBuf buf) {}
    public static void handle(PacketFireRed message,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ControlledRedManager.fire(player);
            }
        });
        context.setPacketHandled(true);
    }
}
