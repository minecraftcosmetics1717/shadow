package com.ghosts.abyss.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TeleportRequestNotifyPacket {
    private final String fromName;

    public TeleportRequestNotifyPacket(String fromName) {
        this.fromName = fromName;
    }

    public TeleportRequestNotifyPacket(FriendlyByteBuf buf) {
        this.fromName = buf.readUtf(32);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(fromName, 32);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        Component.literal(fromName + " wants to teleport to you. Type /tpa accept or /tpa deny"),
                        false
                );
                // Store pending request for a simple command
                mc.player.getPersistentData().putString("jreborn_pending_tpa", fromName);
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}