
package com.ghosts.abyss.network;

import com.ghosts.abyss.client.PiercingBloodBeamRenderer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class PacketSyncPiercingBlood {

    public final UUID playerId;
    public final boolean active;
    public final Vec3 start;
    public final Vec3 end;

    public PacketSyncPiercingBlood(
            UUID playerId,
            boolean active,
            Vec3 start,
            Vec3 end
    ) {
        this.playerId = playerId;
        this.active = active;
        this.start = start;
        this.end = end;
    }

    public PacketSyncPiercingBlood(FriendlyByteBuf buf) {
        this.playerId = buf.readUUID();
        this.active = buf.readBoolean();

        this.start = new Vec3(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble()
        );

        this.end = new Vec3(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble()
        );
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerId);
        buf.writeBoolean(active);

        buf.writeDouble(start.x);
        buf.writeDouble(start.y);
        buf.writeDouble(start.z);

        buf.writeDouble(end.x);
        buf.writeDouble(end.y);
        buf.writeDouble(end.z);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {

        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT,
                        () -> () ->
                                PiercingBloodBeamRenderer.update(this)
                )
        );

        ctx.get().setPacketHandled(true);
    }
}
