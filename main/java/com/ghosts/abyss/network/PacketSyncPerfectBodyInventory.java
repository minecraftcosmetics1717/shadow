
package com.ghosts.abyss.network;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class PacketSyncPerfectBodyInventory {

    public static final Map<UUID, Boolean> CLIENT_STATES =
            new ConcurrentHashMap<>();

    private final UUID playerId;
    private final boolean perfectBody;

    public PacketSyncPerfectBodyInventory(
            UUID playerId,
            boolean perfectBody
    ) {
        this.playerId = playerId;
        this.perfectBody = perfectBody;
    }

    public PacketSyncPerfectBodyInventory(FriendlyByteBuf buf) {
        this.playerId = buf.readUUID();
        this.perfectBody = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerId);
        buf.writeBoolean(perfectBody);
    }

    public void handle(
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context ctx = contextSupplier.get();

        ctx.enqueueWork(() -> {
            CLIENT_STATES.put(playerId, perfectBody);
        });

        ctx.setPacketHandled(true);
    }
}
