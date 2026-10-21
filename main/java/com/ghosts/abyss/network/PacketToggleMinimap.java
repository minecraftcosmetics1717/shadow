package com.ghosts.abyss.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class PacketToggleMinimap {

    public PacketToggleMinimap() {}
    public PacketToggleMinimap(FriendlyByteBuf buf) {}
    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            if (!player.getPersistentData().getBoolean("minimap_unlocked")) {
                player.displayClientMessage(Component.literal("§cUnlock the minimap first."), true);
                return;
            }

            boolean now = !player.getPersistentData().getBoolean("minimap_enabled");
            player.getPersistentData().putBoolean("minimap_enabled", now);

            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new PacketSyncMinimapState(true, now)
            );

            player.displayClientMessage(Component.literal(now ? "§aMinimap enabled." : "§7Minimap disabled."), true);
        });
        ctx.get().setPacketHandled(true);
    }
}