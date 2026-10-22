package com.ghosts.abyss.network;

import com.ghosts.abyss.client.XaeroMinimapBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSyncMinimapState {
    private final boolean unlocked;
    private final boolean enabled;

    public PacketSyncMinimapState(boolean unlocked, boolean enabled) {
        this.unlocked = unlocked;
        this.enabled = enabled;
    }

    public PacketSyncMinimapState(FriendlyByteBuf buf) {
        this.unlocked = buf.readBoolean();
        this.enabled = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(unlocked);
        buf.writeBoolean(enabled);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            XaeroMinimapBridge.clientUnlocked = unlocked;
            XaeroMinimapBridge.clientEnabled = enabled;
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null) {
                XaeroMinimapBridge.applyTo(player);
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}