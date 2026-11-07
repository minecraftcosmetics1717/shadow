
package com.ghosts.abyss.network;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class PacketSyncSoulAwareness {

    private final int awareness;

    public PacketSyncSoulAwareness(int awareness) {
        this.awareness = awareness;
    }

    public PacketSyncSoulAwareness(FriendlyByteBuf buf) {
        this.awareness = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(awareness);
    }

    public void handle(
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context ctx = contextSupplier.get();

        ctx.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT,
                        () -> this::handleClient
                )
        );

        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        var player = Minecraft.getInstance().player;

        if (player != null) {
            player.getPersistentData().putInt(
                    "abyss_soul_awareness",
                    Math.max(0, Math.min(50, awareness))
            );

            player.getPersistentData().putBoolean(
                    "abyss_soul_awareness_initialized",
                    true
            );
        }
    }
}
