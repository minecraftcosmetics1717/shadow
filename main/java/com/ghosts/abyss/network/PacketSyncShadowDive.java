package com.ghosts.abyss.network;

import com.ghosts.abyss.client.ShadowDiveSync;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class PacketSyncShadowDive {
    private final UUID playerId;
    private final boolean diving;

    public PacketSyncShadowDive(UUID playerId, boolean diving) {
        this.playerId = playerId;
        this.diving = diving;
    }

    public PacketSyncShadowDive(FriendlyByteBuf buf) {
        this.playerId = buf.readUUID();
        this.diving = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerId);
        buf.writeBoolean(diving);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ShadowDiveSync.MAP.put(playerId, diving);
        }));
        ctx.get().setPacketHandled(true);
    }
}