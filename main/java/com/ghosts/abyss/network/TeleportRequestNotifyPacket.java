
package com.ghosts.abyss.network;

import com.ghosts.abyss.client.phone.PhoneTeleportClient;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class TeleportRequestNotifyPacket {

    private final List<PhoneTeleportClient.Incoming> incoming;
    private final List<PhoneTeleportClient.Outgoing> outgoing;
    private final long serverTick;

    public TeleportRequestNotifyPacket(
            List<PhoneTeleportClient.Incoming> incoming,
            List<PhoneTeleportClient.Outgoing> outgoing,
            long serverTick
    ) {
        this.incoming = new ArrayList<>(incoming);
        this.outgoing = new ArrayList<>(outgoing);
        this.serverTick = serverTick;
    }

    public TeleportRequestNotifyPacket(FriendlyByteBuf buf) {
        serverTick = buf.readLong();

        int incomingCount = buf.readVarInt();
        incoming = new ArrayList<>();

        for (int i = 0; i < incomingCount; i++) {
            incoming.add(new PhoneTeleportClient.Incoming(
                    buf.readUUID(),
                    buf.readUtf(32),
                    buf.readLong()
            ));
        }

        int outgoingCount = buf.readVarInt();
        outgoing = new ArrayList<>();

        for (int i = 0; i < outgoingCount; i++) {
            outgoing.add(new PhoneTeleportClient.Outgoing(
                    buf.readUUID(),
                    buf.readLong()
            ));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeLong(serverTick);

        buf.writeVarInt(incoming.size());
        for (var request : incoming) {
            buf.writeUUID(request.senderId());
            buf.writeUtf(request.senderName(), 32);
            buf.writeLong(request.expiresAt());
        }

        buf.writeVarInt(outgoing.size());
        for (var request : outgoing) {
            buf.writeUUID(request.recipientId());
            buf.writeLong(request.expiresAt());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();

        ctx.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    Minecraft mc = Minecraft.getInstance();

                    if (mc.level != null) {
                        PhoneTeleportClient.update(
                                incoming,
                                outgoing,
                                serverTick,
                                mc.level.getGameTime()
                        );
                    }
                })
        );

        ctx.setPacketHandled(true);
    }
}
