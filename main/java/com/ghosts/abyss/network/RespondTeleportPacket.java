package com.ghosts.abyss.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RespondTeleportPacket {
    private final boolean accept;

    public RespondTeleportPacket(boolean accept) {
        this.accept = accept;
    }

    public RespondTeleportPacket(FriendlyByteBuf buf) {
        this.accept = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(accept);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer target = ctx.get().getSender();
            if (target == null) return;

            String fromName = target.getPersistentData().getString("jreborn_pending_tpa");
            if (fromName == null || fromName.isEmpty()) {
                target.displayClientMessage(Component.literal("No pending teleport request."), true);
                return;
            }

            ServerPlayer requester = target.server.getPlayerList().getPlayerByName(fromName);
            target.getPersistentData().remove("jreborn_pending_tpa");

            if (requester == null) {
                target.displayClientMessage(Component.literal("Player is no longer online."), true);
                return;
            }

            if (accept) {
                // Teleport the requester TO the target
                requester.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(),
                        target.getYRot(), target.getXRot());
                requester.displayClientMessage(Component.literal("Teleport accepted by " + target.getGameProfile().getName()), true);
                target.displayClientMessage(Component.literal("You accepted " + fromName + "'s teleport."), true);
            } else {
                requester.displayClientMessage(Component.literal(target.getGameProfile().getName() + " denied your teleport request."), true);
                target.displayClientMessage(Component.literal("You denied the teleport request."), true);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}