package com.ghosts.abyss.network;

import com.ghosts.abyss.AdvTechPoints;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketUnlockMinimap {

    public PacketUnlockMinimap() {}

    public PacketUnlockMinimap(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            if (player.getPersistentData().getBoolean("minimap_unlocked")) {
                player.displayClientMessage(Component.literal("§aMinimap already unlocked."), true);
                return;
            }

            int at = AdvTechPoints.getAvailable(player);
            if (at < 5) {
                player.displayClientMessage(Component.literal("§cNeed 5 Adv Tech Points."), true);
                return;
            }

            // spend 5
            player.getPersistentData().putDouble("advtech_points_available", at - 5);
            player.getPersistentData().putBoolean("minimap_unlocked", true);
            player.getPersistentData().putBoolean("minimap_enabled", true); // on by default after unlock

            AdvTechPoints.sync(player);
            // sync minimap flags to client
            ModNetwork.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new PacketSyncMinimapState(true, true)
            );

            player.displayClientMessage(Component.literal("§aMinimap unlocked!"), true);
        });
        ctx.get().setPacketHandled(true);
    }
}