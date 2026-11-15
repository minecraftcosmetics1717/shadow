
package com.ghosts.abyss.network;

import com.ghosts.abyss.client.sky.DomainSkyMaskRenderer;
import com.ghosts.abyss.domain.DomainDimension;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public final class PacketBoostDomainSky {

    public PacketBoostDomainSky() {
    }

    public PacketBoostDomainSky(FriendlyByteBuf buf) {
        // This packet does not need a payload.
    }

    public void encode(FriendlyByteBuf buf) {
        // No payload.
    }

    public void handle(
            Supplier<NetworkEvent.Context> supplier
    ) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT,
                        () -> () -> DomainSkyMaskRenderer.requestBoost()
                )
        );

        context.setPacketHandled(true);
    }

    // Notify players who are near the affected arena.
    public static void sendNear(
            ServerLevel level,
            BlockPos center
    ) {
        if (level == null || center == null) return;

        if (!level.dimension().equals(DomainDimension.ID)) {
            return;
        }

        double radius = DomainDimension.OUTER_RADIUS + 32.0D;
        double radiusSq = radius * radius;

        double x = center.getX() + 0.5D;
        double y = center.getY();
        double z = center.getZ() + 0.5D;

        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) > radiusSq) {
                continue;
            }

            ModNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new PacketBoostDomainSky()
            );
        }
    }
}
