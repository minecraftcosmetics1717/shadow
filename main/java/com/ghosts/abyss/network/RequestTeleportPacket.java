package com.ghosts.abyss.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

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

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) return;

            // Later: block teleport under specific circumstances
            if (sender.getPersistentData().getBoolean("jreborn_teleport_disabled")) {
                sender.displayClientMessage(Component.literal("Teleport is currently disabled."), true);
                return;
            }

            ServerPlayer target = sender.server.getPlayerList().getPlayerByName(targetName);
            if (target == null) {
                sender.displayClientMessage(Component.literal("Player not found."), true);
                return;
            }

            if (target.getPersistentData().getBoolean("jreborn_teleport_disabled")) {
                sender.displayClientMessage(Component.literal(targetName + " cannot receive teleports right now."), true);
                return;
            }

            // Send request to target
            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> target),
                    new TeleportRequestNotifyPacket(sender.getGameProfile().getName())
            );

            sender.displayClientMessage(Component.literal("Request sent to " + targetName), true);
        });
        ctx.get().setPacketHandled(true);
    }
}